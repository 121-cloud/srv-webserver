package otocloud.webserver.handler;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by better/zhangye on 15/9/22.
 */
public class DispatchOptions {
    public static final String REGISTER_ADDRESS = "platform.register.rest.to.webserver";
    public static final String UNREGISTER_ADDRESS = "platform.unregister.rest.to.webserver";

    private String registerAddress;
    private String unRegisterAddress;

    private String webServerName;

    public DispatchOptions() {
        this.registerAddress = REGISTER_ADDRESS;
        this.unRegisterAddress = UNREGISTER_ADDRESS;
    }

    public String getWebServerName() {
        return webServerName;
    }

    public void setWebServerName(String webServerName) {
        this.webServerName = webServerName;
    }

    public String getRegisterAddress() {
        if (StringUtils.isNoneBlank(webServerName)) {
            return webServerName + "." + registerAddress;
        }
        return registerAddress;
    }

    public void setRegisterAddress(String registerAddress) {
        this.registerAddress = registerAddress;
    }

    public String getUnRegisterAddress() {
        if (StringUtils.isNotBlank(webServerName)) {
            return webServerName + "." + unRegisterAddress;
        }
        return unRegisterAddress;
    }

    public void setUnRegisterAddress(String unRegisterAddress) {
        this.unRegisterAddress = unRegisterAddress;
    }
}
