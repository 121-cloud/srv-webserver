package otocloud.webserver.dispatch;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * 消息派发策略的统一接口。
 * TODO 策略应作为单例。
 * <ol>
 * <li>根据路由上下文(RoutingContext)和注册的地址，自动生成“最终发往的事件总线地址”。</li>
 * <li>将消息发送到生成的地址上。</li>
 * </ol>
 * Created by better on 15/9/16.
 */
public interface EventBusTraveller {
    String BASIC = "basic";

    /**
     * 生成基本的派发策略。
     *
     * @param vertx
     * @return
     */
    static EventBusTraveller traveller(Vertx vertx) {
        return new EventBusTravellerImpl(vertx);
    }

    /**
     * 指定被派发的消息体。
     *
     * @param pack 具体的派发内容，如一个json对象(其类型是{@link io.vertx.core.json.JsonObject JsonObject})。
     * @return this
     */
    EventBusTraveller carryBody(Object pack);

    /**
     * 指定被派发的消息头。
     *
     * @param headers 消息头
     * @return this
     */
    EventBusTraveller carryHeaders(MultiMap headers);

    /**
     * 在消息体的附属消息.
     *
     * @param info
     * @return
     */
    EventBusTraveller carryInfo(JsonObject info);

    /**
     * 设置消息派发策略的数据来源。
     *
     * @param context 路由上下文
     * @return
     */
    EventBusTraveller from(RoutingContext context);


    /**
     * 将内容发送到具体地址。
     *
     * @param address 地址
     */
    void to(String address);

    /**
     * 将内容发送到具体地址。该具体地址通过解析参数而动态生成。
     *
     * @param addressPattern    地址模板
     * @param decoratingAddress 由应用提供的解析具体地址的地址
     */
    void to(String addressPattern, String decoratingAddress);

    /**
     * 将内容发送到具体地址，并带有回复响应事件。
     *
     * @param addressPattern
     * @param replyHandler
     * @param <T>
     */
    <T> void to(String addressPattern, Handler<AsyncResult<Message<T>>> replyHandler);
}
