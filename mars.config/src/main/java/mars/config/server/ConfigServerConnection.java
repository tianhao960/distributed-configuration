package mars.config.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.apachecommons.CommonsLog;
import mars.config.client.ConfigEvent;
import mars.config.request.ListenConfigRequest;
import mars.config.request.response.ListenConfigResponse;
import mars.config.service.ConfigService;
import mars.config.service.impl.ConfigException;
import mars.config.util.ErrorCode;
import mars.config.util.ZooKeeperUtil;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@CommonsLog
public class ConfigServerConnection {

    public static final ExecutorService processor = Executors.newFixedThreadPool(1);

    private SocketChannel socketChannel;

    private SelectionKey selectionKey;

    private ConfigService configService;

    private ZooKeeperUtil zooKeeperUtil;

    private LinkedBlockingQueue<ListenConfigResponse> listenConfigResponses= new LinkedBlockingQueue<>();

    //private ConcurrentSkipListSet<String> watchPaths = new ConcurrentSkipListSet<>();

    private ConfigServerWatcher configServerWatcher;

    /**
     * This buffer is only used to read the length of the incoming message.
     */
    private final ByteBuffer lenBuffer = ByteBuffer.allocateDirect(4);

    /**
     * After the length is read, a new incomingBuffer is allocated in
     * readLength() to receive the full message.
     */
    private ByteBuffer incomingBuffer = lenBuffer;

    private AtomicLong count;

    public ConfigServerConnection(SocketChannel socketChannel,SelectionKey selectionKey, ZooKeeperUtil zooKeeperUtil,ConfigService configService){
        Assert.notNull(selectionKey);
        Assert.notNull(socketChannel);
        this.selectionKey =selectionKey;
        this.socketChannel = socketChannel;
        this.configService = configService;
        this.zooKeeperUtil = zooKeeperUtil;
        this.configServerWatcher = new ConfigServerWatcher();
        count = new AtomicLong(0);
    }


    private class ConfigServerWatcher implements CuratorWatcher {

        @Override
        public void process(final WatchedEvent event) throws Exception {
            if(!socketChannel.isOpen())
                return;
            if(event.getType().equals(Watcher.Event.EventType.NodeDataChanged)){
                if(log.isDebugEnabled()){
                    log.debug(String.format("node %s data changed",event.getPath()));
                }

                log.info("process event:" + event.toString());
                String path = event.getPath();
                try {
                    //need regist watcher
                    zooKeeperUtil.getClient().getData().usingWatcher(this).forPath(path);
                    ListenConfigResponse response = new ListenConfigResponse();
                    //notify
                    response.setId("-2");
                    response.setIsSuccess(true);
                    ConfigEvent configEvent = new ConfigEvent();
                    configEvent.setPath(path);
                    String data = new String(zooKeeperUtil.getClient().getData().forPath(path));
                    configEvent.setData(data);
                    response.setConfigEvent(configEvent);
                    synchronized (listenConfigResponses){
                        listenConfigResponses.offer(response,10,TimeUnit.SECONDS);
                        if (selectionKey.isValid()) {
                            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public void doIO(final SelectionKey key){
        processor.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    if(!socketChannel.isOpen()){
                       /* if(log.isDebugEnabled()){
                            Long times = count.incrementAndGet();
                            log.debug("socket closed "+times);
                        }*/
                        return;
                    }
                    if(key.isReadable()){
                        synchronized (lenBuffer){
                            //can't assume you can read how much data from channel/socket
                            int count = socketChannel.read(incomingBuffer);
                            if(count<0){
                                //check EOF
                                closeSock();
                                return;
                            }
                            if(!incomingBuffer.hasRemaining()){
                                incomingBuffer.flip();
                                if (incomingBuffer == lenBuffer) {
                                    int len = incomingBuffer.getInt();
                                    incomingBuffer = ByteBuffer.allocate(len);
                                }else{
                                    ObjectMapper om = new ObjectMapper();
                                    ListenConfigRequest request = om.readValue(incomingBuffer.array(),ListenConfigRequest.class);
                                    log.info("process request:"+request);
                                    ListenConfigResponse response = null;
                                    try{
                                        //response = configService.listenConfig(request);
                                        if(request.getAction().equals("-1")){
                                            //ping
                                            response = new ListenConfigResponse();
                                            response.setId(request.getId());
                                            response.setIsSuccess(true);
                                        }else if(request.getAction().equals("0")){
                                            StringBuilder path = new StringBuilder("/");
                                            path.append(request.getAppgroup()).append("/");
                                            path.append(request.getApp()).append("/");
                                            path.append(request.getKey());
                                            //watchPaths.add(path.toString());
                                            if(request.getIsListenEvent()){
                                                zooKeeperUtil.getClient().getData().usingWatcher(configServerWatcher).forPath(path.toString());
                                            }

                                            if(!StringUtils.isEmpty(request.getUrl())){
                                                response = configService.listenConfig(request);
                                            }
                                            if(response==null){
                                                response = new ListenConfigResponse();
                                                response.setId(request.getId());
                                                response.setIsSuccess(true);
                                            }

                                        }else{
                                            response = new ListenConfigResponse();
                                            if(request.getId()!=null){
                                                response.setId(request.getId());
                                            }
                                            response.setIsSuccess(false);
                                            response.setErrorCode(ErrorCode.UNEXPECT_ERROR);
                                        }
                                    }catch (Exception e){
                                        log.warn("process request:"+request +" fail.");
                                        response = new ListenConfigResponse();
                                        if(request.getId()!=null){
                                            response.setId(request.getId());
                                        }
                                        response.setIsSuccess(false);
                                        response.setErrorCode(ErrorCode.UNEXPECT_ERROR);
                                    }

                                    log.info("response:"+response);
                                    synchronized (listenConfigResponses){
                                        if(listenConfigResponses.offer(response,10,TimeUnit.SECONDS)){

                                            if (selectionKey.isValid()) {
                                                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                                            }

                                        }else{
                                            log.error("put response to queue fail.");
                                            throw new ConfigException("can't put response to queue");
                                        }
                                    }
                                    lenBuffer.clear();
                                    incomingBuffer = lenBuffer;
                                }
                            }
                        }
                    }
                    if(key.isWritable()){
                        ListenConfigResponse response;
                        while((response = listenConfigResponses.poll(1,TimeUnit.SECONDS))!=null){
                            try{
                                ObjectMapper om = new ObjectMapper();
                                byte[] bytes = om.writeValueAsBytes(response);
                                Integer len = bytes.length;
                                ByteBuffer byteBuffer = ByteBuffer.allocate(len+4);
                                byteBuffer.putInt(len).put(bytes);
                                byteBuffer.flip();
                                socketChannel.write(byteBuffer);
                            }catch (Exception e){
                                log.error("process response fail.",e);
                            }
                        }
                        synchronized (listenConfigResponses){
                            if(listenConfigResponses.size()>0){
                                if (selectionKey.isValid()) {
                                    selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                                }

                            }else {
                                if (selectionKey.isValid()) {
                                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                                }
                            }

                        }
                    }
                }catch (Exception e){
                    if(!socketChannel.isOpen()){
                        log.info(String.format("local socket %d closed.",socketChannel.socket().getLocalPort()));
                    }else{
                        log.error("unhandled exception.",e);
                    }
                    closeSock();
                }

            }
        });
    }

    private void closeSock() {
        if (socketChannel.isOpen() == false) {
            return;
        }
        log.info("closing sock.");
        try {
            /*
             * The following sequence of code is stupid! You would think that
             * only sock.close() is needed, but alas, it doesn't work that way.
             * If you just do sock.close() there are cases where the socket
             * doesn't actually close...
             */
            socketChannel.socket().shutdownOutput();
        } catch (IOException e) {
            // This is a relatively common exception that we can't avoid
            if (log.isDebugEnabled()) {
                log.debug("ignoring exception during output shutdown", e);
            }
        }
        try {
            socketChannel.socket().shutdownInput();
        } catch (IOException e) {
            // This is a relatively common exception that we can't avoid
            if (log.isDebugEnabled()) {
                log.debug("ignoring exception during input shutdown", e);
            }
        }
        try {
            socketChannel.socket().close();
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("ignoring exception during socket close", e);
            }
        }
        try {
            socketChannel.close();
            // XXX The next line doesn't seem to be needed, but some posts
            // to forums suggest that it is needed. Keep in mind if errors in
            // this section arise.
            // factory.selector.wakeup();
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("ignoring exception during socketchannel close", e);
            }
        }

        if(selectionKey!=null){
            try {
                // need to cancel this selection key from the selector
                selectionKey.cancel();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("ignoring exception during selectionkey cancel", e);
                }
            }
        }
    }
}
