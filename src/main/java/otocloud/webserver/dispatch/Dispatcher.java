package otocloud.webserver.dispatch;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import otocloud.common.Command;
import otocloud.common.CommandDeliveryOptions;
import otocloud.webserver.protocal.BridgeProtocal;
import otocloud.webserver.register.RegisterInfo;
import otocloud.webserver.util.CommandBuilder;
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

        String messageType = registerInfo.getMessageFormat();

        //判断是否使用了Command模式
        if (StringUtils.isNotBlank(messageType) && messageType.equals(RegisterInfo.COMMAND)) {
            logger.info("正在使用 [Command] 消息结构传递HTTP请求.");

            //传递API调用
            travelWithCommand(registerInfo, routingContext);
            
        } else {
            logger.info("正在使用 [兼容] 消息结构传递HTTP请求.");
            
            //传递API调用
            travel(registerInfo, routingContext);
        }
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
     * 使用{@link otocloud.common.Command}传递消息, 从而规范消息格式.
     *
     * @param registerMessage RestAPI注册信息ID.
     * @param context         路由上下文.
     */
    private void travelWithCommand(RegisterInfo registerMessage, RoutingContext context) {
        logger.debug("正在使用 [Command] 消息结构传. 原始注册信息是: " + registerMessage);

        //动态生成真实的地址
        String decoratingAddress = registerMessage.getDecoratingAddress();

        RegisterInfo decoratedInfo = registerMessage;

        if (decoratingAddress != null) {
            logger.info("应用提供的解析地址是: " + decoratingAddress);
            Future<String> addrFuture = Future.future();
            EventBusTravellerImpl.getDecoratedAddress(context,
                    decoratedInfo.getAddress(), decoratedInfo.getDecoratingAddress(),
                    addrFuture);

            addrFuture.setHandler(ret -> {
                if (ret.succeeded()) {
                    String realAddr = ret.result();
                    logger.warn("WebServer确认目标地址: " + realAddr);
                    decoratedInfo.setAddress(realAddr);
                    decoratedInfo.setDecoratingAddress(null);

                    doTravelWithCommand(decoratedInfo, context);
                } else {
                    logger.warn("无法解析目标地址, HTTP请求无法转发, 将被忽略. 可能的原因: 应用解析目标地址失败.");
                    context.next();
                }
            });
        } else {
            logger.info("应用未提供解析地址, 使用的目标地址是: " + registerMessage.getAddress());
            doTravelWithCommand(registerMessage, context);
        }
    }

    private void doTravelWithCommand(RegisterInfo registerMessage, RoutingContext context) {

        logger.info("WebServer正在向目标地址转发请求:" + registerMessage.getAddress());

        Command cmd = CommandBuilder.create(registerMessage, context);
        CommandDeliveryOptions options = CommandBuilder.createDeliveryOptions(context);

        cmd.execute(context.vertx(), options, cmdRet -> {
            if (logger.isTraceEnabled()) {
                logger.trace("WebServer收到了应用的完整响应: " + cmdRet.toString());
                logger.trace("WebServer收到的应用响应数据(datas): " + cmdRet.getDatas());
            }

            HttpServerResponse response = context.response();
            response.putHeader("content-type", "application/json; charset=utf-8");

            response.setStatusCode(200);
            response.end(cmdRet.toString());
        });
    }


    /**
     * 根据注册信息, 将请求路由到指定位置.
     *
     * @param registerMessage RestAPI的注册信息.
     * @param routingContext  路由上下文.
     */
    private void travel(RegisterInfo registerMessage, RoutingContext routingContext) {
        String url = registerMessage.getUri();
        String method = registerMessage.getMethod();

        String addressPattern = registerMessage.getAddress();
        String action = registerMessage.getAction();

        //动态生成真实的地址
        String decoratingAddress = registerMessage.getDecoratingAddress();

        BridgeProtocal protocal = ProtocalFactory.get("basic"); //TODO 根据参数读取桥接协议
        EventBusTraveller traveller = TravellerFactory.get(vertx, "static"); //TODO 根据参数读取派发策略

        JsonObject info = new JsonObject().put("api", new JsonObject().put("uri", url).put("method", method));
        if (StringUtils.isNotBlank(action)) {
            info.put("action", action);
        }

        JsonObject msgBody = protocal.buildMessageBody(routingContext);
        MultiMap headers = protocal.buildMessageHeaders(routingContext);

        traveller.carryHeaders(headers).carryBody(msgBody).carryInfo(info).from(routingContext);

        if (decoratingAddress != null) {
            traveller.to(addressPattern, decoratingAddress);
        } else {
            traveller.to(addressPattern);
        }
    }
}
