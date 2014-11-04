package mars.config.client;

import lombok.Data;

@Data
public class ConfigEvent {
    private String data;
    private String path;
}
