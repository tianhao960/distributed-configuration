package mars.config.service.impl;

import lombok.extern.apachecommons.CommonsLog;
import mars.config.request.ListenConfigRequest;
import mars.config.request.response.*;
import mars.config.service.ConfigService;
import mars.config.util.ErrorCode;
import mars.config.util.ZooKeeperUtil;
import org.apache.curator.utils.EnsurePath;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@CommonsLog
public class ConfigServiceImpl implements ConfigService{

    @Autowired
    private ZooKeeperUtil zooKeeperUtil;

    @Autowired
    private ConfigWatcher watcher;

    @Override
    public CreateConfigResponse createConfig(String appgroup, String app, String key, String value){
        log.info(String.format("create config for %s,%s,%s,%s",appgroup,app,key,value));
        CreateConfigResponse response = new CreateConfigResponse();
        if(appgroup==null || appgroup.indexOf("/")>=0 || appgroup.equals("meta")){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_APP_GROUP_NAME);
            return  response;
        }
        if(app==null || app.indexOf("/")>=0){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_APP_NAME);
            return  response;
        }
        if(key==null || key.indexOf("/")>=0){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_KEY_NAME);
            return  response;
        }

        if(StringUtils.isEmpty(value)){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_CONFIG_VALUE);
            return  response;
        }

        StringBuilder sb = new StringBuilder("/").append(appgroup);
        sb.append("/").append(app);
        sb.append("/").append(key);
        try{
            String path = sb.toString();
            EnsurePath ensurePath = new EnsurePath(sb.toString()).excludingLast();
            ensurePath.ensure(zooKeeperUtil.getClient().getZookeeperClient());
            zooKeeperUtil.getClient().create().forPath(path,value.getBytes());
            response.setIsSuccess(true);
            log.info("create config success.");
            return  response;
        }catch (KeeperException keepException){
            if(keepException.code().equals(KeeperException.Code.NODEEXISTS)){
                response.setIsSuccess(false);
                response.setErrorCode(ErrorCode.CONFIG_EXISITS);
                return response;
            }else{
                log.info("create config fail.");
                throw  new ConfigException(keepException.getMessage(),keepException);
            }
        }catch (Exception e){
            log.info("create config fail.");
            throw  new ConfigException(e.getMessage(),e);
        }
    }

    @Override
    public DeleteConfigResponse deleteConfig(String appgroup, String app, String key) {
        log.info(String.format("delete config %s,%s,%s",appgroup,app,key));
        DeleteConfigResponse response = new DeleteConfigResponse();
        if(appgroup==null || appgroup.indexOf("/")>=0|| appgroup.equals("meta")){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_APP_GROUP_NAME);
            return  response;
        }
        if(app==null || app.indexOf("/")>=0){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_APP_NAME);
            return  response;
        }
        if(key==null || key.indexOf("/")>=0){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_KEY_NAME);
            return  response;
        }
        StringBuilder sb = new StringBuilder("/").append(appgroup);
        sb.append("/").append(app);
        sb.append("/").append(key);
        try{
            String path = sb.toString();

            zooKeeperUtil.getClient().delete().guaranteed().forPath(path);
            response.setIsSuccess(true);
            log.info("delete config success.");
            return  response;
        }catch (KeeperException keepException){
            if(keepException.code().equals(KeeperException.Code.NONODE)){
                response.setIsSuccess(false);
                response.setErrorCode(ErrorCode.CONFIG_NOT_EXISIT);
                return response;
            }else{
                log.info("delete config fail.");
                throw  new ConfigException(keepException.getMessage(),keepException);
            }
        }catch (Exception e){
            log.info("delete config fail.");
            throw  new ConfigException(e.getMessage(),e);
        }
    }

    @Override
    public UpdateConfigResponse updateConfig(String appgroup, String app, String key, String value) {
        log.info(String.format("update config for %s,%s,%s,%s",appgroup,app,key,value));
        UpdateConfigResponse response = new UpdateConfigResponse();
        if(appgroup==null || appgroup.indexOf("/")>=0|| appgroup.equals("meta")){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_APP_GROUP_NAME);
            return  response;
        }
        if(app==null || app.indexOf("/")>=0){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_APP_NAME);
            return  response;
        }
        if(key==null || key.indexOf("/")>=0){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_KEY_NAME);
            return  response;
        }

        if(StringUtils.isEmpty(value)){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_CONFIG_VALUE);
            return  response;
        }

        StringBuilder sb = new StringBuilder("/").append(appgroup);
        sb.append("/").append(app);
        sb.append("/").append(key);
        try{
            String path = sb.toString();
            zooKeeperUtil.getClient().setData().forPath(path,value.getBytes());
            response.setIsSuccess(true);
            log.info("update config success.");
            return  response;
        }catch (KeeperException keepException){
            if(keepException.code().equals(KeeperException.Code.NONODE)){
                response.setIsSuccess(false);
                response.setErrorCode(ErrorCode.CONFIG_NOT_EXISIT);
                return response;
            }else{
                log.info("update config fail.");
                throw  new ConfigException(keepException.getMessage(),keepException);
            }
        }catch (Exception e){
            log.info("update config fail.");
            throw  new ConfigException(e.getMessage(),e);
        }
    }

    @Override
    public GetConfigResponse getConfig(String appgroup, String app, String key) {
        log.info(String.format("get config for %s,%s,%s",appgroup,app,key));
        GetConfigResponse response = new GetConfigResponse();
        if(appgroup==null || appgroup.indexOf("/")>=0|| appgroup.equals("meta")){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_APP_GROUP_NAME);
            return  response;
        }
        if(app==null || app.indexOf("/")>=0){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_APP_NAME);
            return  response;
        }
        if(key==null || key.indexOf("/")>=0){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_KEY_NAME);
            return  response;
        }

        StringBuilder sb = new StringBuilder("/").append(appgroup);
        sb.append("/").append(app);
        sb.append("/").append(key);
        try{
            String path = sb.toString();
            String config = new String(zooKeeperUtil.getClient().getData().forPath(path));
            response.setValue(config);
            response.setIsSuccess(true);
            log.info("get config success.");
            return  response;
        }catch (KeeperException keepException){
            if(keepException.code().equals(KeeperException.Code.NONODE)){
                response.setIsSuccess(false);
                response.setErrorCode(ErrorCode.CONFIG_NOT_EXISIT);
                return response;
            }else{
                log.info("get config fail.");
                throw  new ConfigException(keepException.getMessage(),keepException);
            }
        }catch (Exception e){
            log.info("get config fail.");
            throw  new ConfigException(e.getMessage(),e);
        }
    }

    public ListenConfigResponse listenConfig(ListenConfigRequest request){
        String appgroup = request.getAppgroup();
        String app = request.getApp();
        String key = request.getKey();

        ListenConfigResponse response = new ListenConfigResponse();

        if(appgroup==null || appgroup.indexOf("/")>=0|| appgroup.equals("meta")){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_APP_GROUP_NAME);
            return  response;
        }
        if(app==null || app.indexOf("/")>=0){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_APP_NAME);
            return  response;
        }
        if(key==null || key.indexOf("/")>=0){
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.INVALID_KEY_NAME);
            return  response;
        }

        StringBuilder sb = new StringBuilder("/").append(appgroup);
        sb.append("/").append(app);
        sb.append("/").append(key);

        try{
            String path = sb.toString();
            watcher.addListener(path,request.getUrl());
           /* EnsurePath ensurePath = new EnsurePath(path);
            ensurePath.ensure(zooKeeperUtil.getClient().getZookeeperClient());*/
            zooKeeperUtil.getClient().getData().usingWatcher(watcher).forPath(path);
            if(request.getId()!=null){
                response.setId(request.getId());
            }
            response.setIsSuccess(true);
            log.info("get config success.");
            return  response;
        }catch (KeeperException keepException){
            if(keepException.code().equals(KeeperException.Code.NONODE)){
                if(request.getId()!=null){
                    response.setId(request.getId());
                }
                response.setIsSuccess(false);
                response.setErrorCode(ErrorCode.CONFIG_NOT_EXISIT);
                return response;
            }else{
                log.info("listen config fail.");
                throw  new ConfigException(keepException.getMessage(),keepException);
            }
        }catch (Exception e){
            log.info("listen config fail.");
            throw  new ConfigException(e.getMessage(),e);
        }
    }
}
