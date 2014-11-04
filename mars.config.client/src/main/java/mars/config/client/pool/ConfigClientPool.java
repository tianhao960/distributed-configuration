package mars.config.client.pool;

import mars.config.client.ConfigClient;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class ConfigClientPool extends Pool<ConfigClient> {

    public ConfigClientPool(GenericObjectPoolConfig config, PooledObjectFactory factory) {
        super(config, factory);
    }

    public ConfigClient getClient(){
        return super.getResource();
    }

    public void returnClient(ConfigClient client){
        super.returnResource(client);
    }
}
