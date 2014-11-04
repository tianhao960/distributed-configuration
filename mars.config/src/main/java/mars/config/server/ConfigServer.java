package mars.config.server;

import lombok.extern.apachecommons.CommonsLog;
import mars.config.service.ConfigService;
import mars.config.service.impl.ConfigException;
import mars.config.util.ZooKeeperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

@Service
@CommonsLog
public class ConfigServer implements Runnable{

    @Value("${config.server.port}")
    private Integer port;

    @Autowired
    private ZooKeeperUtil zooKeeperUtil;

    @Autowired
    private ConfigService configService;

    private ServerSocketChannel ss;

    private Selector selector;

    private Thread thread;

    //TODO REMOVE CONN WHICH NOT SEND HEART BEAT

    @PostConstruct
    public void init(){
        InetSocketAddress addr = new InetSocketAddress(port);
        try{
            thread = new Thread(this, "config server:" + addr);
            thread.setDaemon(true);
            selector = Selector.open();
            this.ss = ServerSocketChannel.open();
            ss.socket().setReuseAddress(true);
            log.info("binding to port " + addr);
            ss.socket().bind(addr);
            ss.configureBlocking(false);
            ss.register(selector, SelectionKey.OP_ACCEPT);
            thread.start();
            log.info("config server started...");
        }catch (Exception e){
            log.error("init server fail.");
            throw  new ConfigException("init server fail.",e);
        }

    }

    @Override
    public void run() {
        while (!ss.socket().isClosed()) {
            try{
                selector.select(1000);
                Set<SelectionKey> selected;
                synchronized (this) {
                    selected = selector.selectedKeys();
                }
                ArrayList<SelectionKey> selectedList = new ArrayList<>(selected);
                Collections.shuffle(selectedList);
                for (SelectionKey k : selectedList) {
                    if ((k.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
                        SocketChannel sc = ((ServerSocketChannel) k.channel()).accept();
                        log.info("Accepted socket connection from "+ sc.socket().getRemoteSocketAddress());
                        sc.configureBlocking(false);

                        SelectionKey sk = sc.register(selector,SelectionKey.OP_READ);
                        ConfigServerConnection conn = new ConfigServerConnection(sc, sk,zooKeeperUtil,configService);
                        sk.attach(conn);
                    } else if ((k.readyOps() & (SelectionKey.OP_READ | SelectionKey.OP_WRITE)) != 0) {
                        ConfigServerConnection c = (ConfigServerConnection) k.attachment();
                        c.doIO(k);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Unexpected ops in select " + k.readyOps());
                        }
                    }
                }
                selected.clear();
            }catch (Exception e){
                log.warn("Ignoring exception", e);
            }
        }
    }

    @PreDestroy
    public void destroy(){
        try{
            ss.socket().close();
            ConfigServerConnection.processor.shutdown();
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }

    }
}
