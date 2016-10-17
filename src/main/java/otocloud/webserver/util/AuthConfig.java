package otocloud.webserver.util;

import io.vertx.core.json.JsonArray;

/**
 * 存储Auth服务器(授权服务器)的配置信息.
 * zhangyef@yonyou.com on 2015-11-24.
 */
public class AuthConfig {
    /**
     * Auth服务器注册的登录URL地址.
     */
    private String loginUrl;

    private JsonArray securityUrls;

    private String sessionQueryAddress;

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
