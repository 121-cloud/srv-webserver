package otocloud.webserver.protocal;

import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import otocloud.webserver.util.JsonFactory;

import java.util.Map;

/**
 * Created by better on 15/9/18.
 */
public class BasicBridgeProtocal implements BridgeProtocal {
    public static final String QUERY_PARAMS = "query_params";
    public static final String REQUEST_CONTENT = "content";
    public static final String REQUEST_URI = "uri";
    public static final String REQUEST_ABS_URI = "abs_uri";
    public static final String REQUEST_Path = "path";

    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public JsonObject buildMessageBody(RoutingContext context) {
        if (logger.isInfoEnabled()) {
            logger.info("WebServer 正在构造总线消息体(body).");
        }

        HttpServerRequest request = context.request();
        MultiMap params = request.params();
        String body = context.getBodyAsString();

        Object jsonBody = null; //构造空请求体
        if (body != null) {
            jsonBody = JsonFactory.makeBodyContent(body);
        }

        String uri = request.uri();
        String path = request.path();
        String absUri = request.absoluteURI();

        JsonObject messageBody = new JsonObject();
        messageBody.put(QUERY_PARAMS, JsonFactory.fromMultiMap(params));
        messageBody.put(REQUEST_CONTENT, jsonBody);
        messageBody.put(REQUEST_URI, uri);
        messageBody.put(REQUEST_ABS_URI, absUri);
        messageBody.put(REQUEST_Path, path);

        if (logger.isInfoEnabled()) {
            logger.info("WebServer 成功构造总线消息体(body).");
        }

        return messageBody;
    }

    /**
     * 提取请求中的RequestMethod放到事件消息的头部.
     *
     * @param context
     * @return
     */
    @Override
    public MultiMap buildMessageHeaders(RoutingContext context) {
        if (logger.isInfoEnabled()) {
            logger.info("WebServer 正在构造总线消息头.");
        }
        HttpServerRequest request = context.request();
        String methodName = request.method().name(); //TODO 修改为小写单词

        DeliveryOptions options = new DeliveryOptions();
        
        //向API消息头添加token
        boolean hasToken = request.params().contains("token");
        if (hasToken) {        	
        	options.addHeader("token", request.params().get("token"));            
        }
        
        options.addHeader("method", methodName);

        //添加HTTP请求头
        MultiMap map = request.headers();
        for (Map.Entry<String, String> entry : map){
            options.addHeader(entry.getKey(), entry.getValue());
        }

        if (logger.isInfoEnabled()) {
            logger.info("WebServer 成功构造总线消息头(headers).");
        }
        return options.getHeaders();
    }

    @Override
    public <T> T buildResponseBody(Message<T> message) {
        return message.body();
    }

    @Override
    public <T> MultiMap buildResponseHeaders(Message<T> message) {
        return message.headers();
    }


}
