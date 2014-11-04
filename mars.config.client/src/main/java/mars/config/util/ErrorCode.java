package mars.config.util;

import lombok.Data;

@Data
public class ErrorCode {


    public static final ErrorCode INVALID_APP_GROUP_NAME = new ErrorCode(1,"App Group 不能为空或者包含/");
    public static final ErrorCode INVALID_APP_NAME = new ErrorCode(2,"App  不能为空或者包含/");
    public static final ErrorCode INVALID_KEY_NAME = new ErrorCode(3,"KEY 不能为空或者包含/");
    public static final ErrorCode INVALID_CONFIG_VALUE = new ErrorCode(4,"Value 不能为空");
    public static final ErrorCode CONFIG_NOT_EXISIT = new ErrorCode(5,"配置不存在。");
    public static final ErrorCode CONFIG_EXISITS = new ErrorCode(6,"配置已经存在。");
    public static final ErrorCode UNEXPECT_ERROR = new ErrorCode(-1,"对不起，系统错误。请联系我们。");
    public static final ErrorCode TIMEOUT_ERROR = new ErrorCode(7,"请求超时");


    private Integer errorCode;
    private String errorMessage;

    public ErrorCode(){

    }

    public ErrorCode(Integer errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
