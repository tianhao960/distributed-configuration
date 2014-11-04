package mars.config.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import mars.config.client.rest.RestClient;
import mars.config.request.*;
import mars.config.request.response.*;

import java.io.IOException;

@Data
@Log4j
public class ConfigClient {

    protected String host;

    protected Integer port;

    protected Integer listenPort;

    protected ConfigClientConnection conn;

    protected String httpPath;

    public ConfigClient(String host, Integer port, Integer listenPort){
        this.port = port;
        this.host = host;
        this.listenPort = listenPort;
        this.conn = new ConfigClientConnection(this);
        httpPath = "http://"+host+":"+port+"/config/";
    }

    public ListenConfigResponse call(ListenConfigRequest request){
        request.setId(conn.getRequestId());
        request.setAction("0");
        conn.sendRequest(request);
        return conn.getResponse(request,10);
    }

    public CreateConfigResponse call(CreateConfigRequest createConfigRequest){
        try{
            ObjectMapper om = new ObjectMapper();
            String responseStr = RestClient.postRequest(httpPath+"create/",createConfigRequest);
            CreateConfigResponse createConfigResponse = om.readValue(responseStr,CreateConfigResponse.class);
            return  createConfigResponse;
        }catch (Exception e){
            log.error("get config "+createConfigRequest.toString()+" fail.");
            throw  new RuntimeException(e);
        }
    }

    public GetConfigResponse call(GetConfigRequest getConfigRequest){

        try{
            ObjectMapper om = new ObjectMapper();
            String responseStr = RestClient.postRequest(httpPath+"get/",getConfigRequest);
            GetConfigResponse getConfigResponse = om.readValue(responseStr,GetConfigResponse.class);
            return  getConfigResponse;
        }catch (Exception e){
            log.error("get config "+getConfigRequest.toString()+" fail.");
            throw  new RuntimeException(e);
        }
    }


    public UpdateConfigResponse call(UpdateConfigRequest updateConfigRequest){
        try{
            ObjectMapper om = new ObjectMapper();
            String responseStr = RestClient.postRequest(httpPath+"update/",updateConfigRequest);
            UpdateConfigResponse updateConfigResponse = om.readValue(responseStr,UpdateConfigResponse.class);
            return  updateConfigResponse;
        }catch (Exception e){
            log.error("get config "+updateConfigRequest.toString()+" fail.");
            throw  new RuntimeException(e);
        }
    }

    public DeleteConfigResponse call(DeleteConfigRequest deleteConfigRequest){
        try{
            ObjectMapper om = new ObjectMapper();
            String responseStr = RestClient.postRequest(httpPath+"delete/",deleteConfigRequest);
            DeleteConfigResponse deleteConfigResponse = om.readValue(responseStr,DeleteConfigResponse.class);
            return  deleteConfigResponse;
        }catch (Exception e){
            log.error("get config "+deleteConfigRequest.toString()+" fail.");
            throw  new RuntimeException(e);
        }
    }

    public boolean call(PingRequest pingRequest){
        pingRequest.setId(conn.getRequestId());
        pingRequest.setAction("-1");
        conn.sendRequest(pingRequest);
        return conn.getResponse(pingRequest,10).getIsSuccess();
    }

    public void closeConn() throws IOException {
        this.conn.closeSock();
    }
}
