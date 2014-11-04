package mars.config.client.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

@Log4j
public class RestClient {

    public static String postRequest(String url,Object requestPara){
        DefaultHttpClient httpClient = null;
        HttpPost postRequest = new HttpPost(url);

        ObjectMapper om = new ObjectMapper();
        try{
            httpClient = new DefaultHttpClient();
            String requestStr = om.writeValueAsString(requestPara);
            StringEntity input = new StringEntity(requestStr);
            input.setContentType("application/json");
            postRequest.setEntity(input);

            HttpResponse response = httpClient.execute(postRequest);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatusLine().getStatusCode());
            }

            String responseStr = EntityUtils.toString(response.getEntity());

            return responseStr;
        }catch (Exception e){
            log.error(String.format("process request fail,%s,%s.",url,requestPara));
            throw  new RuntimeException(e);
        }finally {
            if(httpClient!=null){
                httpClient.getConnectionManager().shutdown();
            }
        }
    }
}
