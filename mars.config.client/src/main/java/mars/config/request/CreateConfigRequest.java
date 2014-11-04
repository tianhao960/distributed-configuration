package mars.config.request;

import lombok.Data;

@Data
public class CreateConfigRequest {

    private String appgroup;
    private String app;
    private String key;
    private String value;
}
