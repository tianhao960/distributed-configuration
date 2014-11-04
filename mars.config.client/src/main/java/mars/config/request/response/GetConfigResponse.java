package mars.config.request.response;

import lombok.Data;

@Data
public class GetConfigResponse extends ConfigResponse {

    private String value;
}
