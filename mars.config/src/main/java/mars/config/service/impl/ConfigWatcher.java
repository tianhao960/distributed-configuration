package mars.config.service.impl;

import lombok.extern.apachecommons.CommonsLog;
import mars.config.util.DistributedMap;
import mars.config.util.ZooKeeperUtil;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.utils.EnsurePath;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@CommonsLog
@Component
public class ConfigWatcher implements CuratorWatcher {

    //private  ExecutorService executor;

    private static final String metaListenPath ="/meta/listen";
    private static final String metaListenLockPath ="reserve_lock";

    @Autowired
    private ZooKeeperUtil zooKeeperUtil;

    //private Set<String> listPath;

    //private static final ConfigWatcher watcher = new ConfigWatcher();

    public ConfigWatcher(){
        //executor = Executors.newFixedThreadPool(1);
        //listPath = new CopyOnWriteArraySet<>();
    }

    @PostConstruct
    public void initialListen(){
        try{
            EnsurePath ensurePath = new EnsurePath(metaListenPath);
            ensurePath.ensure(zooKeeperUtil.getClient().getZookeeperClient());
            List<String> children = zooKeeperUtil.getClient().getChildren().forPath(metaListenPath);
            if(children!=null){
                for(String child : children){
                    if(child.equals(metaListenLockPath)){
                        continue;
                    }
                    String watchPath = child.replaceAll("-","/");

                    if(!watchPath.startsWith("/")){
                        log.info("watching path must start with /, doesn't watch "+watchPath);
                        continue;
                    }
                    log.info("watching path "+watchPath);
                    //listPath.add(watchPath);
                    if(zooKeeperUtil.getClient().checkExists().forPath(watchPath)!=null){
                        zooKeeperUtil.getClient().getData().usingWatcher(this).forPath(watchPath);
                    }else {
                        log.warn("does not exist node "+watchPath);
                    }
                }
            }
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
    }


    public String addListener(String path, String url){
        InterProcessReadWriteLock lock = null;
        try {
            lock = DistributedMap.getListenMap(zooKeeperUtil.getClient()).getLock();
            if(log.isDebugEnabled()){
                log.debug("acquire lock...");
            }
            if(lock.writeLock().acquire(10, TimeUnit.SECONDS)){
                if(log.isDebugEnabled()){
                    log.debug("acquire lock success...");
                }
            }else{
               log.debug("acquire lock fail...");
                throw new RuntimeException("acquire lock fail...");
            }

            Map<String, ArrayList<String>> listeners = DistributedMap.getListenMap(zooKeeperUtil.getClient());
            ArrayList<String> urls = listeners.get(path);
            if(urls!=null){
                urls.add(url);
                listeners.put(path, urls);
            }else{
                urls = new ArrayList<>();
                urls.add(url);
                listeners.put(path,urls);
            }
            //listPath.add(path);
        }catch (Exception e){
            log.error("acquire lock fail.",e);
            throw new RuntimeException("acquire lock fail.");
        }finally {
            try {
                if(lock!=null){
                    if(log.isDebugEnabled()){
                        log.info("release lock....");
                    }
                    if(lock.writeLock().isAcquiredInThisProcess()){
                        lock.writeLock().release();
                    }
                }
            } catch (Exception e) {
                log.error("release lock fail.",e);
                throw new RuntimeException("release lock fail.");
            }
        }
        return DistributedMap.getListenMap(zooKeeperUtil.getClient()).getPath()+"/"+path.replaceAll("/","-");
    }

    /*public static ConfigWatcher getWatcher(){
        return watcher;
    }*/

    @Override
    public void process(final WatchedEvent event) throws Exception {
        if(event.getType().equals(Watcher.Event.EventType.NodeDataChanged)){
            if(log.isDebugEnabled()){
                log.debug(String.format("node %s data changed",event.getPath()));
            }
            final CuratorWatcher watcher = this;
            ExecutorService executor = Executors.newFixedThreadPool(1);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    log.info("process event:" + event.toString());
                    String path = event.getPath();

                    try {
                        //need regist watcher
                        zooKeeperUtil.getClient().getData().usingWatcher(watcher).forPath(path);
                        DistributedMap<String,ArrayList<String>> listenMap = DistributedMap.getListenMap(zooKeeperUtil.getClient());
                        ArrayList<String> urls = listenMap.get(path);
                        if(urls!=null){
                            for(String url : urls){
                                log.info("notify "+url);
                                DefaultHttpClient httpClient = null;
                                try{
                                    httpClient = new DefaultHttpClient();
                                    HttpGet getRequest = new HttpGet(url);
                                    HttpResponse response = httpClient.execute(getRequest);

                                    if (response.getStatusLine().getStatusCode() != 200) {
                                        log.error("Failed : HTTP error code : "
                                                + response.getStatusLine().getStatusCode());
                                    }else{
                                        log.info("notify success.");
                                    }
                                }catch (Exception e){
                                    log.error(e.getMessage(),e);
                                }finally {
                                    if(httpClient!=null){
                                        httpClient.getConnectionManager().shutdown();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
            executor.shutdown();
        }
    }

}
