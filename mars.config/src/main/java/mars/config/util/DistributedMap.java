package mars.config.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.utils.EnsurePath;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZKUtil;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@CommonsLog
public class DistributedMap<K,V> implements Map<K,V>,Closeable{

    private static final String basePath = "/meta/";

    private InterProcessReadWriteLock lock;

    private CuratorFramework client;

    private String path;

    private String lockPath;

    public DistributedMap(CuratorFramework client){
        this(client,UUID.randomUUID().toString());
    }

    public DistributedMap(CuratorFramework client,String path){
        path = basePath+path;
        try {
            this.client = client;
            this.path = path;
            lockPath = path+"/reserve_lock";
            EnsurePath ensurePath = new EnsurePath(lockPath);
            ensurePath.ensure(client.getZookeeperClient());
        } catch (Exception e) {
            log.error(String.format("can't create path %s", path));
            throw new IllegalStateException(e);
        }
        //must keep reserve lock path clean, don't store any other things except the lock data
        //one lock object get write lock, then any other lock object couldn't get any lock before release
        lock = new InterProcessReadWriteLock(client,lockPath);
    }

    public InterProcessReadWriteLock getLock(){
        return lock;
    }

    public String getPath(){
        return path;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V get(Object key) {
        InterProcessReadWriteLock lock = null;
        try{
            K kkey = (K)key;
            lock = getLock();
            if(lock.readLock().acquire(10, TimeUnit.SECONDS)){
                String keyPath = path+"/"+kkey.toString().replaceAll("/","-");
                byte[] bytes = client.getData().forPath(keyPath);
                ObjectMapper om = new ObjectMapper();
                return (V)om.readValue(bytes,Object.class);
            }else{
                throw  new RuntimeException("get from map, but acquire lock fail.");
            }
        }catch (KeeperException keepException){
            if(keepException.code().equals(KeeperException.Code.NONODE)){
                //ignore
            }else{
                log.error("get value fail");
                throw  new RuntimeException(keepException);
            }
        }catch (Exception e) {
           log.error("get value fail");
            throw  new RuntimeException(e);
        } finally {
            try {
                if(lock!=null && lock.readLock().isAcquiredInThisProcess()){
                    lock.readLock().release();
                }
            } catch (Exception e) {
                log.error("release read lock fail",e);
            }
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        InterProcessReadWriteLock lock = null;
        try {
            if(log.isDebugEnabled()){
                log.info("put value "+value);
            }
            K kkey = (K)key;
            V vvalue = (V)value;
            lock = getLock();
            if(lock.writeLock().acquire(10, TimeUnit.SECONDS)){
                String keyPath = path+"/"+kkey.toString().replaceAll("/","-");
                Assert.isTrue(!keyPath.equals(lockPath));
                EnsurePath ensurePath = new EnsurePath(keyPath);
                ensurePath.ensure(client.getZookeeperClient());
                ObjectMapper om = new ObjectMapper();
                byte[] bytes = om.writeValueAsBytes(vvalue);
                client.setData().forPath(keyPath,bytes);
                return value;
            }else{
                throw  new RuntimeException("put to map, but acquire lock fail.");
            }

        } catch (Exception e) {
            log.error("write value fail");
            throw  new RuntimeException(e);
        } finally {
            try {
                if(lock!=null && lock.writeLock().isAcquiredInThisProcess()){
                    lock.writeLock().release();
                }
            } catch (Exception e) {
                log.error("release read lock fail", e);
            }
        }
    }

    @Override
    public V remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        try{
            ZKUtil.deleteRecursive(client.getZookeeperClient().getZooKeeper(),path);
        }catch (Exception e){
            log.info("delete config fail.");
            throw  new IOException(e.getMessage(),e);
        }
    }

    private static DistributedMap<String,ArrayList<String>> listenMap;

    private static final Object lockObject = new Object();
    public static DistributedMap<String,ArrayList<String>> getListenMap(CuratorFramework client){
        if(listenMap==null){
            synchronized (lockObject){
                if(listenMap==null){
                    listenMap = new DistributedMap<>(client,"listen");
                }
            }
        }
        return  listenMap;
    }
}
