package otocloud.webserver.util;


import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * 存储Auth服务器(授权服务器)的配置信息.
 * zhangyef@yonyou.com on 2015-11-24.
 */
public class AuthConfig {
    /**
     * Auth服务器注册的登录URL地址.
     */
    private String loginUrl = "/api/otocloud-auth/user-management/users/actions/authenticate";

    private JsonArray securityUrls;

    private String sessionQueryAddress = "otocloud-auth.user-management.query";
    
    public AuthConfig(JsonObject config){
    	reInit(config);
    }
    
    public void reInit(JsonObject config){
        String sessionQueryAddress = config.getString("auth.session_query_address");
        if (sessionQueryAddress != null) {
            setSessionQueryAddress(sessionQueryAddress);
        }

        String loginUrl = config.getString("auth.login_url");
        if (loginUrl != null) {
            setLoginUrl(loginUrl);
        }

        JsonArray securityUrls = config.getJsonArray("auth.security_urls");
        if (securityUrls != null) {
            setSecurityUrls(securityUrls);
        }
    }

    public String getSessionQueryAddress() {
        return sessionQueryAddress;
    }

    public void setSessionQueryAddress(String sessionQueryAddress) {
        this.sessionQueryAddress = sessionQueryAddress;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public JsonArray getSecurityUrls() {
        return securityUrls;
    }

    /**
     * 拷贝后设置.
     * @param securityUrls
     */
    public void setSecurityUrls(JsonArray securityUrls) {
        this.securityUrls =  securityUrls.copy();
    }
}
