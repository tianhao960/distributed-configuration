package mars.config.request;

import lombok.Data;

@Data
public class UpdateConfigRequest extends ConfigRequest {

    private String value;
}
