package mars.config.request.response;

import lombok.Data;
import mars.config.util.ErrorCode;

@Data
public class CreateConfigResponse {

    private Boolean isSuccess;

    private ErrorCode errorCode;
}
