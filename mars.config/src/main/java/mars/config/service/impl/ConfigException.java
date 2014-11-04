package mars.config.service.impl;

public class ConfigException extends RuntimeException{

    public ConfigException(String message){
        super(message);
    }

    public ConfigException(String message, Exception e){
        super(message,e);
    }
}
