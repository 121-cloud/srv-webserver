package otocloud.webserver.dispatch;

import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import otocloud.webserver.util.ContextUtil;

import java.util.Set;

/**
 * 基本的消息派发策略。
 * Created by better on 15/9/16.
 */
public class EventBusTravellerImpl implements EventBusTraveller {
    protected static Logger logger = LoggerFactory.getLogger(EventBusTravellerImpl.class);

    private Vertx vertx;
    private RoutingContext context;

    private Object pack;

    private MultiMap headers;

    private JsonObject info;

    public EventBusTravellerImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * 由应用解析真实的地址.
     *
     * @param context
     * @param addressPattern
     * @param decoratingAddress
     * @param future            返回真实地址.
     */
    public static void getDecoratedAddress(RoutingContext context,
                                           String addressPattern, String decoratingAddress,
                                           Future<String> future) {
        // 通过应用获取真实地址
        JsonObject addressObject = new JsonObject();
        addressObject.put("addressPattern", addressPattern);

        ContextUtil.addSessionToPack(context, addressObject);

        logger.info(">>>>>>向应用发起请求, 查询目标地址. 请求内容如下>>>>>>");
        logger.info("解析地址: " + decoratingAddress);
        logger.info(addressObject.encodePrettily());

        context.vertx().eventBus().send(decoratingAddress, addressObject, event -> {
            if (event.failed()) {
                future.fail(event.cause());
                return;
            }
            if (event.succeeded()) {
                JsonObject realAddressMsg = (JsonObject) event.result().body();
                String realAddress = realAddressMsg.getString("realAddress");
                logger.info("应用返回的目标地址是: " + realAddress);
                //返回真实地址
                future.complete(realAddress);
            }
        });
    }

    private EventBusAddress address(String address) {
        return new DefaultAddressImpl(this).at(address);
    }

    @Override
    public EventBusTraveller carryBody(Object pack) {
        this.pack = pack;
        return this;
    }

    @Override
    public EventBusTraveller carryHeaders(MultiMap headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public EventBusTraveller carryInfo(JsonObject info) {
        this.info = info;
        return this;
    }

    @Override
    public EventBusTraveller from(RoutingContext context) {
        this.context = context;
        return this;
    }

    @Override
    public void to(String address) {
        logger.info("WebServer正在向目标地址转发请求:" + address);
        to(address, new TravellerReplyHandler(context));
    }

    public void to(String addressPattern, String decoratingAddress) {
        Future<String> addrFuture = Future.future();
        getDecoratedAddress(context, addressPattern, decoratingAddress, addrFuture);

        addrFuture.setHandler(ret -> {
            if (ret.succeeded()) {
                String realAddress = ret.result();
                //向真实地址派发请求.
                to(realAddress);
            }
        });
    }

    @Override
    public <T> void to(String address, Handler<AsyncResult<Message<T>>> replyHandler) {
        if (this.context == null) {
            throw new IllegalArgumentException("消息派发策略的路由上下文没有设置.");
        }

        //为消息体添加额外信息
        addInfoToPack();

        ContextUtil.addSessionToPack(context, this.pack);

        EventBus bus = vertx.eventBus();
        DeliveryOptions options = new DeliveryOptions();
        options.setHeaders(headers);

        //动态解析地址
        EventBusAddress busAddress = address(address);

        if (replyHandler == null) {
            bus.send(busAddress.getAddress(), pack, options);
        } else {
            bus.send(busAddress.getAddress(), pack, options, replyHandler);
        }
    }

    private void addInfoToPack() {
        if (this.info != null && this.pack != null) {
            if (pack instanceof JsonObject == false) {
                return;
            }
            JsonObject modifiedPack = (JsonObject) pack;

            Set<String> fieldNames = info.fieldNames();
            fieldNames.forEach(name -> modifiedPack.put(name, info.getValue(name)));
        }
    }


}
