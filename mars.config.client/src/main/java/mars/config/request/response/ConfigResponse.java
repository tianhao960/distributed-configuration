package mars.config.request.response;

import lombok.Data;
import mars.config.util.ErrorCode;

@Data
public class ConfigResponse {
    private Boolean isSuccess;

    private ErrorCode errorCode;
}
