package mars.config.service;

import mars.config.request.ListenConfigRequest;
import mars.config.request.response.*;
import mars.config.service.impl.ConfigException;


public interface ConfigService {

    /**
     * 创建配置，“/”是保留字符，不要包含在appgroup, app, key里头
     * @param appgroup
     * @param app
     * @param key
     * @param value
     * @throws ConfigException
     */
    public CreateConfigResponse createConfig(String appgroup, String app, String key, String value);

    /**
     * 删除配置
     * @param appgroup
     * @param app
     * @param key
     * @return
     */
    public DeleteConfigResponse deleteConfig(String appgroup, String app, String key);

    /**
     * 更新配置
     * @param appgroup
     * @param app
     * @param key
     * @param value
     * @return
     */
    public UpdateConfigResponse updateConfig(String appgroup, String app, String key, String value);

    /**
     * 取得配置
     * @param appgroup
     * @param app
     * @param key
     * @return
     */
    public GetConfigResponse getConfig(String appgroup, String app, String key);

    /**
     * 监听配置改动
     * @param request
     * @return
     */
    public ListenConfigResponse listenConfig(ListenConfigRequest request);
}
