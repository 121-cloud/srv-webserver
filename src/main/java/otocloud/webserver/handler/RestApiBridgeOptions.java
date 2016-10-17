package otocloud.webserver.handler;

import otocloud.webserver.protocal.BridgeProtocal;
import otocloud.webserver.dispatch.EventBusTraveller;

/**
 * 设置桥接协议({@link BridgeProtocal})和派发策略({@link EventBusTraveller})。
 * <p/>
 * Created by better/zhangye on 15/9/22.
 */
public class RestApiBridgeOptions {
    private BridgeProtocal protocal;
    private EventBusTraveller traveller;

    public RestApiBridgeOptions() {

    }

    public BridgeProtocal getProtocal() {
        return protocal;
    }

    public void setProtocal(BridgeProtocal protocal) {
        this.protocal = protocal;
    }

    public EventBusTraveller getTraveller() {
        return traveller;
    }

    public void setTraveller(EventBusTraveller traveller) {
        this.traveller = traveller;
    }
}
