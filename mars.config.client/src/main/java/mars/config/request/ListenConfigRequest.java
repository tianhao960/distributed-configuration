package mars.config.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import mars.config.client.ConfigClientWatcher;

@Data
public class ListenConfigRequest extends ConfigRequest {

    /**
     * url 用于server端通知地址
     *
     * isListenEvent为true，则sever端会发消息回客户端，
     * 客户端则会调用相应的ConfigClientWatcher
     * isListenEvent为true，则sever端不会发消息回客户端，
     *
     * isListenEvent不影响设置url的功能
     *
     */

    private String url;

    private String id;

    /**
     * ping -1
     * listen 0
     */
    private String action;

    private Boolean isListenEvent = false;

   @JsonIgnore
    private ConfigClientWatcher watcher;
}
