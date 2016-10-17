package otocloud.webserver.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * 负责REST API的注册、HTTP 请求到事件总线的消息路由。
 * Created by better on 15/9/21.
 */
public interface EventMessageHandler extends Handler<RoutingContext> {
    static EventMessageHandler create(Vertx vertx) {
        return new EventMessageHandlerImpl(vertx);
    }

    static EventMessageHandler create(Vertx vertx, DispatchOptions options) {
        return new EventMessageHandlerImpl(vertx,options);
    }

    /**
     * 建立标准HTTP请求到事件总线消息的映射桥梁。完成从HTTP请求到事件总线消息，从事件总线消息到HTTP响应的双向映射。
     * 在桥接选项中，需要配置映射的协议以及请求到事件总线的派发策略。
     *
     * @param options 桥接选项，用于配置桥接协议和派发策略。
     * @return this
     */
    EventMessageHandler bridge(RestApiBridgeOptions options);
}
