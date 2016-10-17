package otocloud.webserver.protocal;

import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Created by better on 15/9/18.
 */
public interface BridgeProtocal {

    static BridgeProtocal basicProtocal() {
        return new BasicBridgeProtocal();
    }

    /**
     * 从标准的HTTP请求构建事件总线的消息实体.
     *
     * @param context 标准的HTTP请求
     * @return 事件总线上的消息实体.
     */
    JsonObject buildMessageBody(RoutingContext context);

    /**
     * 从标准的HTTP请求构建事件总线的消息头.
     *
     * @param context
     * @return 标准的消息头.
     */
    MultiMap buildMessageHeaders(RoutingContext context);

    <T> T buildResponseBody(Message<T> message);

    /**
     * 从事件总线消息构建HTTP响应头部。
     *
     * @param message 事件总线消息.
     * @param <T>     消息体的类型.
     * @return HTTP响应头.
     */
    <T> MultiMap buildResponseHeaders(Message<T> message);


}
