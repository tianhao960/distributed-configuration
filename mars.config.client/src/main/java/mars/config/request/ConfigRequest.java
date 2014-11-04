package mars.config.request;

import lombok.Data;

@Data
public class ConfigRequest {
    private String appgroup;
    private String app;
    private String key;
}
