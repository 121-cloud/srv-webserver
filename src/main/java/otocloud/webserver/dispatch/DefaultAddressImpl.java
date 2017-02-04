package otocloud.webserver.dispatch;

import io.vertx.ext.web.RoutingContext;

/**
 * Created by zhangye on 15/9/16.
 */
public class DefaultAddressImpl implements EventBusAddress {

    private String address;


    @Override
    public EventBusAddress at(String address) {
        this.address = address;
        return this;
    }

    @Override
    public EventBusAddress analyze(RoutingContext context) {
        return this;
    }


    @Override
    public EventBusAddress disable() {
        return null;
    }

    @Override
    public EventBusAddress enable() {
        return null;
    }

    @Override
    public EventBusAddress remove() {
        return this;
    }

    @Override
    public String getAddress() {
        return this.address;
    }


}
