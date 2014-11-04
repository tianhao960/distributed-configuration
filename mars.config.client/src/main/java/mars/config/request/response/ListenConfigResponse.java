package mars.config.request.response;

import lombok.Data;
import mars.config.client.ConfigEvent;

@Data
public class ListenConfigResponse extends ConfigResponse {

    private String id;

    private ConfigEvent configEvent;
}
