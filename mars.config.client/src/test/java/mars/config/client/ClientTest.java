package mars.config.client;

import mars.config.request.PingRequest;

import java.io.IOException;

public class ClientTest {

    public static void main(String[] args) throws InterruptedException {
        ConfigClient configClient = new ConfigClient("localhost",8080,51132);
        /*ListenConfigRequest request = new ListenConfigRequest();
        request.setAppgroup("config");
        request.setApp("db");
        request.setKey("host");
        request.setUrl("http://www.sina.com.cn");
        request.setIsListenEvent(true);
        request.setWatcher(new ConfigClientWatcher() {
            @Override
            public void process(ConfigEvent event) {
                System.out.println(event.getData());
            }
        });
        ListenConfigResponse response = configClient.call(request);
        System.out.println(response.toString());
*/
       /* GetConfigRequest getConfigRequest = new GetConfigRequest();
        getConfigRequest.setAppgroup("config");
        getConfigRequest.setApp("db");
        getConfigRequest.setKey("host");
        GetConfigResponse getConfigResponse = configClient.call(getConfigRequest);
        System.out.println(getConfigResponse.toString());

        CreateConfigRequest createConfigRequest = new CreateConfigRequest();
        createConfigRequest.setAppgroup("config");
        createConfigRequest.setApp("db");
        createConfigRequest.setKey("port");
        createConfigRequest.setValue("3306");
        CreateConfigResponse createConfigResponse = configClient.call(createConfigRequest);
        System.out.println(createConfigResponse.toString());

        UpdateConfigRequest updateConfigRequest = new UpdateConfigRequest();
        updateConfigRequest.setAppgroup("config");
        updateConfigRequest.setApp("db");
        updateConfigRequest.setKey("port");
        updateConfigRequest.setValue("3307");
        UpdateConfigResponse updateConfigResponse = configClient.call(updateConfigRequest);
        System.out.println(updateConfigResponse.toString());

        DeleteConfigRequest deleteConfigRequest = new DeleteConfigRequest();
        deleteConfigRequest.setAppgroup("config");
        deleteConfigRequest.setApp("db");
        deleteConfigRequest.setKey("port");
        DeleteConfigResponse deleteConfigResponse = configClient.call(deleteConfigRequest);
        System.out.println(deleteConfigResponse.toString());*/
        try {
            //TimeUnit.SECONDS.sleep(100);

            PingRequest pingRequest = new PingRequest();
            if(configClient.call(pingRequest)){
                System.out.println("success");
            }else{
                System.out.println("fail");
            }
            configClient.closeConn();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
