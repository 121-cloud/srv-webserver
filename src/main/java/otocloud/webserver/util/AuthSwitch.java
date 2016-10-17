package otocloud.webserver.util;

/**
 * 授权开关.
 * 默认为关.
 * <p/>
 * zhangyef@yonyou.com on 2015-11-24.
 */
public class AuthSwitch {
    private boolean isEnabled = false;

    public AuthSwitch(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void turnOn() {
        isEnabled = true;
    }

    public void turnOff(){
        isEnabled = false;
    }

    public boolean isOn(){
        return isEnabled;
    }
}
