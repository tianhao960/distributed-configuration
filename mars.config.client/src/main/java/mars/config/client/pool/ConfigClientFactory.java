package mars.config.client.pool;

import lombok.Data;
import lombok.NoArgsConstructor;
import mars.config.client.ConfigClient;
import mars.config.client.EnvAwareConfigClient;
import mars.config.request.PingRequest;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

@Data
@NoArgsConstructor
public class ConfigClientFactory implements PooledObjectFactory<ConfigClient> {

    private  String host;
    private  Integer port;
    private  Integer listenPort;
    private String env;

    public ConfigClientFactory(String host, Integer port,Integer listenPort, String env){
        this.host = host;
        this.port = port;
        this.listenPort = listenPort;
        this.env = env;
    }

    @Override
    public PooledObject<ConfigClient> makeObject() throws Exception {
        try{
            ConfigClient client = new EnvAwareConfigClient(host,port,listenPort,env);
            return new DefaultPooledObject<ConfigClient>(client);
        }catch (Exception e){
            throw e;
        }
    }

    @Override
    public void destroyObject(PooledObject<ConfigClient> p) throws Exception {
        ConfigClient client = p.getObject();
        client.closeConn();
    }

    @Override
    public boolean validateObject(PooledObject<ConfigClient> p) {
        try{
            ConfigClient client = p.getObject();
            return client.call(new PingRequest());
        }catch (Exception e){
            //ignore
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<ConfigClient> p) throws Exception {
        //ignore
    }

    @Override
    public void passivateObject(PooledObject<ConfigClient> p) throws Exception {
        //ignore
    }
}
