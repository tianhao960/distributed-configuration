package mars.config.client.pool;

import lombok.extern.log4j.Log4j;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

@Log4j
public abstract class Pool<T> {
    protected GenericObjectPool<T> internalPool;

    public Pool(GenericObjectPoolConfig config,PooledObjectFactory factory){
        internalPool = new GenericObjectPool<T>(factory,config);
    }

    public T getResource(){
        try {
            return internalPool.borrowObject();
        }catch (Exception e){
            log.error("get resource fail.");
            throw new RuntimeException("could not get resource",e);
        }
    }

    public void returnResource(T resource){
        if(resource!=null){
            try{
                internalPool.returnObject(resource);
            }catch (Exception e){
                log.error("return resource fail.");
                throw new RuntimeException("could not return resource",e);
            }
        }
    }

    public void close(){
        try{
            internalPool.close();
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }
}
