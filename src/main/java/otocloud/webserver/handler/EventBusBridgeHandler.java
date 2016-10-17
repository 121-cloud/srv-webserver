package otocloud.webserver.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;

/**
 * Created by better on 15/9/16.
 */
public class EventBusBridgeHandler implements Handler<BridgeEvent> {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private void logMessage(BridgeEventType type, JsonObject message) {
        if (logger.isInfoEnabled()) {
            logger.info("WebServer收到了Socket发来的数据[" + type + "], 收到的数据如下:");
            logger.info("————————————————————————————————————————————");
            logger.info("\t" + message.toString());
            logger.info("————————————————————————————————————————————");
        }


    }

    private void printSocketAddress(SocketAddress address){

    }

    @Override
    public void handle(BridgeEvent event) {
        BridgeEventType socketEventType = event.type();

        switch (socketEventType) {
            case SOCKET_CREATED:
                logger.info("建立了一个Socket连接," + event.type());
                break;
            case SOCKET_CLOSED:
                logger.info("关闭了一个Socket连接," + event.type());
                break;
            case SEND: {
                //客户端发来请求数据
//                JsonObject msg = event.rawMessage();
                break;
            }
            case PUBLISH:
                logMessage(socketEventType, event.getRawMessage());
                break;
            case RECEIVE:
//                logMessage(socketEventType, event.rawMessage());
                break;
            case REGISTER:
                logMessage(socketEventType, event.getRawMessage());
                break;
            case UNREGISTER:
                logMessage(socketEventType, event.getRawMessage());
                break;
            default:
                logMessage(socketEventType, event.getRawMessage());
                //默认拒绝消息
                event.complete(false);
                return;
        }

        // This signals that it's ok to process the event
        event.complete(true);
    }


    private void printMessage(JsonObject raw) {
        //消息总线上的地址
        String address = raw.getString("address");
        //消息的具体内容
        String content = raw.getString("body");

        System.out.println("[" + address + "] " + content);
    }
}
