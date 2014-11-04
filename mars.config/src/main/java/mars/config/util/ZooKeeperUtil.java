package mars.config.util;

import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@CommonsLog
@Component
public class ZooKeeperUtil {

    @Getter
    private CuratorFramework client;

    @Value("${zookeeper.host}")
    private String host;

    @Value("${zookeeper.port}")
    private String port;




    @PostConstruct
    public synchronized void init(){
        if(client==null){
            String connectStr = host+":"+port;
            ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
            log.info(String.format("connect to zookeeper,host:%s",connectStr));
            client = CuratorFrameworkFactory.newClient(connectStr, retryPolicy);
            client.start();
        }
    }



    @PreDestroy
    public synchronized void close(){
        CloseableUtils.closeQuietly(client);
    }
}
