package otocloud.webserver.dispatch;

//import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
//import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
//import org.apache.commons.lang3.StringUtils;
//import otocloud.common.Command;
//import otocloud.common.CommandDeliveryOptions;
import otocloud.webserver.protocal.BridgeProtocal;
import otocloud.webserver.register.RegisterInfo;
//import otocloud.webserver.util.CommandBuilder;
import otocloud.webserver.util.MessageUtil;
import otocloud.webserver.util.ProtocalFactory;
import otocloud.webserver.util.TravellerFactory;

/**
 * 针对每个注册请求,新建一个Dispatcher.
 * <p/>
 * zhangyef@yonyou.com on 2015-12-02.
 */
public class Dispatcher implements Handler<RoutingContext> {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private Vertx vertx;

    /**
     * Rest API的注册信息.
     */
    private JsonObject restApiRegInfo;

    private Dispatcher(Vertx vertx, JsonObject regInfo) {
        this.vertx = vertx;
        this.restApiRegInfo = regInfo;
    }

    public static Dispatcher create(Vertx vertx, JsonObject regInfo) {
        return new Dispatcher(vertx, regInfo);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logWhenReceivingRequest(routingContext);

        RegisterInfo registerInfo = MessageUtil.convertMessage(restApiRegInfo);
        
        //传递API调用
        travel(registerInfo, routingContext);

    }


    private void logWhenReceivingRequest(RoutingContext routingContext) {
        if (!logger.isInfoEnabled()) {
            return;
        }

        logger.info("WebServer收到了来自客户端的 HTTP 请求.");
        logger.info("-------- HTTP 请求内容 --------");
        HttpServerRequest request = routingContext.request();

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            logger.info("请求客户端信息: " + userAgent);
        }

        String methodName = request.method().name();
        String absUri = request.absoluteURI();
        logger.info(methodName + " " + absUri);
        try {
            String body = routingContext.getBodyAsJson().encodePrettily();
            logger.info(body);
        } catch (Exception e) {
            logger.info(routingContext.getBody().toString());
        }
        logger.info("-------------------------------");
    }


    /**
     * 根据注册信息, 将请求路由到指定位置.
     *
     * @param registerMessage RestAPI的注册信息.
     * @param routingContext  路由上下文.
     */
    private void travel(RegisterInfo registerMessage, RoutingContext routingContext) {

        String addressPattern = registerMessage.getAddress();

        //动态生成真实的地址
        String decoratingAddress = registerMessage.getDecoratingAddress();

        BridgeProtocal protocal = ProtocalFactory.get("basic"); //TODO 根据参数读取桥接协议
        EventBusTraveller traveller = TravellerFactory.get(vertx, "static"); //TODO 根据参数读取派发策略

        JsonObject msgBody = protocal.buildMessageBody(routingContext);
        MultiMap headers = protocal.buildMessageHeaders(routingContext);        

        traveller.carryHeaders(headers).carryBody(msgBody).from(routingContext);

        if (decoratingAddress != null) {
            traveller.to(addressPattern, decoratingAddress);
        } else {
            traveller.to(addressPattern);
        }
    }
}
