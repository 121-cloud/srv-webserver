package otocloud.webserver.dispatch;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import otocloud.webserver.protocal.BasicBridgeProtocal;
import otocloud.webserver.protocal.BridgeProtocal;

import java.util.Map;
import java.util.Set;

/**
 * Created by better/zhangye on 15/9/22.
 */
public class TravellerReplyHandler<T> implements Handler<AsyncResult<Message<T>>> {
    public static final String SESSION = "session";
    public static final String SESSION_ID = "id";

    private RoutingContext context;
    private HttpServerResponse response;
    private BridgeProtocal protocal;

    public TravellerReplyHandler(RoutingContext context) {
        this.context = context;
        this.response = context.response();
        this.protocal = new BasicBridgeProtocal();
    }

    @Override
    public void handle(AsyncResult<Message<T>> event) {
        Message<T> message = (Message) event.result();
        if (event.failed()) {
            Throwable cause = event.cause();
            // 用户处理消息失败(Message.fail)的情况
            if (cause instanceof ReplyException) {
                ReplyException replyException = (ReplyException) cause;
                int failureCode = replyException.failureCode();

                JsonObject errMessage = new JsonObject();
                errMessage.put("errCode", failureCode);
                errMessage.put("errMsg", replyException.getMessage());

                response.putHeader("content-type", "application/json; charset=utf-8");
                response.setStatusCode(200);
                response.end(errMessage.toString());
            }
        } else {

            //处理消息头
            MultiMap headers = protocal.buildResponseHeaders(message);
            for (Map.Entry<String, String> header: headers){
                response.putHeader(header.getKey(), header.getValue());
            }
            response.putHeader("content-type", "application/json; charset=utf-8");
            response.setStatusCode(200);
            T msg = protocal.buildResponseBody(message);

            //处理回复为空的情况。
            if (msg == null) {
                response.end();
                return;
            }

            //处理Session
            if (msg instanceof JsonObject) {
                handlerSessionObject((JsonObject) msg);
            }

            response.end(msg.toString());
        }
    }

    /**
     * 解析消息体中的Session对象，将其中的数据存储到上下文的Session中。
     * <p/>
     * 如果没有包含Session信息，直接返回。
     *
     * @param msg
     */
    private void handlerSessionObject(JsonObject msg) {
        JsonObject sessionInfo = msg.getJsonObject(SESSION);
        if (sessionInfo == null) {
            return;
        }

        Set<String> fields = sessionInfo.fieldNames();
        Session session = this.context.session();

        for (String f : fields) {
            if (f.equalsIgnoreCase(SESSION_ID)) {
                continue;
            }

            session.put(f, sessionInfo.getValue(f));
        }

        //将set-session放置到返回的消息体中
//        msg.put("set-session", session.id());

        //移除消息中的Session对象
        msg.remove(SESSION);
    }

    public BridgeProtocal getProtocal() {
        return protocal;
    }

    public void setProtocal(BridgeProtocal protocal) {
        this.protocal = protocal;
    }
}
