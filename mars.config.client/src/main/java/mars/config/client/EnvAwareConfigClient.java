package mars.config.client;

import lombok.extern.log4j.Log4j;
import mars.config.request.*;
import mars.config.request.response.*;

@Log4j
public class EnvAwareConfigClient extends ConfigClient{

    private String env;
    public EnvAwareConfigClient(String host, Integer port, Integer listenPort) {
        super(host, port, listenPort);
        this.env = System.getenv("environment") == null ? "dev" : System.getenv("environment");
    }

    public EnvAwareConfigClient(String host, Integer port, Integer listenPort, String env) {
        super(host, port, listenPort);
        this.env = env;
    }

    public ListenConfigResponse call(ListenConfigRequest request){
        request.setAppgroup(env+"-"+request.getAppgroup());
        return super.call(request);
    }

    public CreateConfigResponse call(CreateConfigRequest createConfigRequest){
        createConfigRequest.setAppgroup(env+"-"+createConfigRequest.getAppgroup());
        return super.call(createConfigRequest);
    }

    public GetConfigResponse call(GetConfigRequest getConfigRequest){
        getConfigRequest.setAppgroup(env+"-"+getConfigRequest.getAppgroup());
        return super.call(getConfigRequest);
    }


    public UpdateConfigResponse call(UpdateConfigRequest updateConfigRequest){
        updateConfigRequest.setAppgroup(env+"-"+updateConfigRequest.getAppgroup());
        return super.call(updateConfigRequest);
    }

    public DeleteConfigResponse call(DeleteConfigRequest deleteConfigRequest){
        deleteConfigRequest.setAppgroup(env+"-"+deleteConfigRequest.getAppgroup());
        return super.call(deleteConfigRequest);
    }
}
