package otocloud.webserver.handler;


import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import otocloud.webserver.util.AuthConfig;


/**
 * zhangyef@yonyou.com on 2015-11-05.
 */
public class SessionHandlerImpl implements SessionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SessionHandlerImpl.class);

    private AuthConfig authConfig;

    
    public SessionHandlerImpl(AuthConfig authConfig) {
		this.authConfig = authConfig;
    }


    /**
     * 通过请求URL, 判断请求是否为登录请求.
     * 登录地址的URL通过AuthService注册时提供.
     * 登录地址格式如下: /api/.../login.
     *
     * @param request
     * @return
     */
    private boolean isLoginRequest(HttpServerRequest request) {
        String loginUrl = this.authConfig.getLoginUrl();
        if (loginUrl == null) {
            logger.warn("登录URL没有注册,无法完成登录.");
            return false;
        }
        return request.path().equals(this.authConfig.getLoginUrl());
    }

    /**
     * 判断当前URL是否为不用验证直接通过的URL.
     * 验证标准通过AuthService注册.
     *
     * @param request
     * @return
     */
    private boolean isSecurityUrls(HttpServerRequest request) {
        JsonArray urls = this.authConfig.getSecurityUrls();
        if (urls != null) {
            return urls.contains(request.path());
        }

        return false;
    }


    private void rejectRequest(RoutingContext context) {
        HttpServerResponse response = context.response();
        response.end(new JsonObject()
                .put("errCode", 4005)
                .put("errMsg", "您访问了未经授权的内容.[" + context.request().path() + "]").toString());
    }

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();

        boolean hasToken = request.params().contains("token");
        if (hasToken) {
        	context.next();
            return;
        }
        //没有token,并且是登录请求.
        if (isLoginRequest(request) || isSecurityUrls(request)) {
        	context.next();
            return;
        }

        rejectRequest(context);
    }
    

}
