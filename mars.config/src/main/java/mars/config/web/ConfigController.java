package mars.config.web;

import lombok.extern.apachecommons.CommonsLog;
import mars.config.request.*;
import mars.config.request.response.*;
import mars.config.service.ConfigService;
import mars.config.util.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/config")
@CommonsLog
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @RequestMapping(value="/create/", method= RequestMethod.POST)
    public CreateConfigResponse createConfig(@RequestBody CreateConfigRequest createConfigRequest){
        String appgroup = createConfigRequest.getAppgroup();
        String app = createConfigRequest.getApp();
        String key = createConfigRequest.getKey();
        String value = createConfigRequest.getValue();
        try{
            return configService.createConfig(appgroup,app,key,value);
        }catch (Exception e){
            log.error(e.getMessage(),e);
            CreateConfigResponse response = new CreateConfigResponse();
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.UNEXPECT_ERROR);
            return response;
        }

    }

    @RequestMapping(value="/delete/", method= RequestMethod.POST)
    public DeleteConfigResponse deleteConfig(@RequestBody DeleteConfigRequest request){
        String appgroup = request.getAppgroup();
        String app = request.getApp();
        String key = request.getKey();
        try{
            return configService.deleteConfig(appgroup,app,key);
        }catch (Exception e){
            log.error(e.getMessage(),e);
            DeleteConfigResponse response = new DeleteConfigResponse();
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.UNEXPECT_ERROR);
            return response;
        }

    }

    @RequestMapping(value="/update/", method= RequestMethod.POST)
    public UpdateConfigResponse updateConfig(@RequestBody UpdateConfigRequest request){
        String appgroup = request.getAppgroup();
        String app = request.getApp();
        String key = request.getKey();
        String value = request.getValue();
        try{
            return configService.updateConfig(appgroup,app,key,value);
        }catch (Exception e){
            log.error(e.getMessage(),e);
            UpdateConfigResponse response = new UpdateConfigResponse();
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.UNEXPECT_ERROR);
            return response;
        }
    }

    @RequestMapping(value="/get/", method= RequestMethod.POST)
    public GetConfigResponse getConfig(@RequestBody GetConfigRequest request){
        String appgroup = request.getAppgroup();
        String app = request.getApp();
        String key = request.getKey();
        try{
            return configService.getConfig(appgroup,app,key);
        }catch (Exception e){
            log.error(e.getMessage(),e);
            GetConfigResponse response = new GetConfigResponse();
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.UNEXPECT_ERROR);
            return response;
        }
    }

    @RequestMapping(value="/listen/", method= RequestMethod.POST)
    public ListenConfigResponse listenConfig(@RequestBody ListenConfigRequest request){

        try{
            return configService.listenConfig(request);
        }catch (Exception e){
            log.error(e.getMessage(),e);
            ListenConfigResponse response = new ListenConfigResponse();
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.UNEXPECT_ERROR);
            return response;
        }
    }

}
