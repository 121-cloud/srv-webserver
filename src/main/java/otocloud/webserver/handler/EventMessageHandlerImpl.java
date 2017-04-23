package otocloud.webserver.handler;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import otocloud.webserver.exception.DuplicatedApiException;
import otocloud.webserver.register.RouteTable;

import java.util.Iterator;
import java.util.List;

/**
 * Created by better/zhangye on 15/9/22.
 */
public class EventMessageHandlerImpl implements EventMessageHandler {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private Vertx vertx;

    private Router router;

    private DispatchOptions dispatchOptions;
    
    private RouteTable routeTable;

	public EventMessageHandlerImpl(Vertx vertx) {
        this.vertx = vertx;
        this.router = Router.router(vertx);
        this.dispatchOptions = new DispatchOptions();
    }

    public EventMessageHandlerImpl(Vertx vertx, DispatchOptions options) {
        this.vertx = vertx;
        this.router = Router.router(vertx);
        this.dispatchOptions = options;
    }
    
    public RouteTable getRouteTable() {
		return routeTable;
	}

	public void setRouteTable(RouteTable routeTable) {
		this.routeTable = routeTable;
	}

    /**
     * 建立REST API请求到EventBus的桥。
     * 使得注册的REST API请求，可以经过转换变为事件总线上的消息，并将消息发送给应用。
     * 当应用处理完消息后，对消息进行回复。回复消息经过转化后，变为标准的HTTP响应，返回给发起REST API请求的客户端。
     *
     * @param defaultOptions 默认桥接选项({@linkplain RestApiBridgeOptions})。
     * @return this
     */
    @Override
    public EventMessageHandler bridge(RestApiBridgeOptions defaultOptions) {
        //建立对消息总线的监听：注册事件、注销事件
        EventBus eb = vertx.eventBus();
        routeTable = new RouteTable(vertx, router);

        String registerAddress = dispatchOptions.getRegisterAddress();
        String unregisterAddress = dispatchOptions.getUnRegisterAddress();

        addRegisterComsumer(eb, routeTable, registerAddress);

        addUnregisterConsumer(eb, routeTable, unregisterAddress);        
        
        return this;
    }

    private void addUnregisterConsumer(EventBus eb, RouteTable routeTable, String unregisterAddress) {
        eb.consumer(unregisterAddress, message -> {
            System.out.println("收到一个API注销消息: " + message.body());
            Object msgBody = message.body();
            if (msgBody instanceof JsonArray) {
                //批量注销
                unregister(routeTable, message, (JsonArray) msgBody);

            } else if (msgBody instanceof JsonObject) {
                //单独注销
                unregister(routeTable, message, (JsonObject) msgBody);

            } else {
                logger.warn("注册的消息格式必须为json或jsonarray");
            }

        }).completionHandler(res -> {
            if (res.succeeded()) {
                System.out.println("开始监听 REST API 注销地址[" + unregisterAddress + "]");
            } else {
                System.out.println("无法监听 REST API 注销地址");
            }
        });
    }

    private void addRegisterComsumer(EventBus eb, RouteTable routeTable, String registerAddress) {
        eb.consumer(registerAddress, message -> {
            System.out.println("收到一个API注册消息: " + message.body());

            Object msgBody = message.body();

            if (msgBody instanceof JsonArray) {
                //批量注册
                register(routeTable, message, (JsonArray) msgBody);

            } else if (msgBody instanceof JsonObject) {
                //单独注册
                register(routeTable, message, (JsonObject) msgBody);

            } else {
                logger.warn("注册的消息格式必须为json或jsonarray");
            }

        }).completionHandler(res -> {
            if (res.succeeded()) {
                System.out.println("开始监听 REST API 注册地址[" + registerAddress + "]");
            } else {
                System.out.println("无法监听 REST API 注册地址");
            }
        });
    }

    /**
     * 注销单独的API。
     *
     * @param routeTable
     * @param message
     * @param msgBody
     */
    private void unregister(RouteTable routeTable, Message<Object> message, JsonObject msgBody) {
        JsonObject regisResult = new JsonObject();

        Future<Void> unFuture = Future.future();

        routeTable.unRegister(msgBody, unFuture);

        unFuture.setHandler(ret -> {
            if(ret.succeeded()){
                regisResult.put("result", "success");
            }else{
                try {
                    throw ret.cause();
                } catch (IllegalArgumentException e) {
                    regisResult.put("result", e.getMessage());
                    message.reply(regisResult);
                } catch (Throwable ignore) {
                }
            }

            message.reply(regisResult);
        });
    }

    /**
     * 批量注销API。
     *
     * @param routeTable
     * @param message
     * @param msgBody
     */
    private void unregister(RouteTable routeTable, Message<Object> message, JsonArray msgBody) {
        JsonObject regisResult = new JsonObject();

        Future<Void> unFuture = Future.future();

        routeTable.unRegisterMany(msgBody, unFuture);

        unFuture.setHandler(ret -> {
            if(ret.succeeded()){
                regisResult.put("result", "success");
            }else{
                try {
                    throw ret.cause();
                } catch (IllegalArgumentException e) {
                    regisResult.put("result", e.getMessage());
                    message.reply(regisResult);
                    return;
                } catch (Throwable ignore) {
                }
            }

            message.reply(regisResult);
        });
    }

    /**
     * 单独注册一个API。
     *
     * @param routeTable Restful API的路由表。
     * @param message
     * @param msgBody
     */
    private void register(RouteTable routeTable, Message<Object> message, JsonObject msgBody) {
        JsonObject regisResult = new JsonObject();


        Future<String> regFuture = Future.future();
        routeTable.register(msgBody, regFuture);
        regFuture.setHandler(ret -> {
            if (ret.succeeded()) {
                String registerId = ret.result();
                regisResult.put("result", registerId);

            } else {
                try {
                    throw ret.cause();
                } catch (IllegalArgumentException e) {
                    regisResult.put("result", e.getMessage());

                } catch (DuplicatedApiException e) {
                    String log = String.format("注册的API已经存在[%s, %s]", e.getMethod(), e.getPath());
                    logger.warn(log);

                    regisResult.put("errMsg", log);
                } catch (Throwable e) {
                    regisResult.put("result", e.getMessage());
                }
            }
            message.reply(regisResult);
        });


    }

    /**
     * 批量注册API。
     *
     * @param routeTable
     * @param message
     * @param msgBody
     */
    private void register(RouteTable routeTable, Message<Object> message, JsonArray msgBody) {

        Future<List<String>> regFuture = Future.future();

        routeTable.registerMany(msgBody, regFuture);

        regFuture.setHandler(ret -> {
            JsonArray regisResult = new JsonArray();
            if (ret.succeeded()) {
                List<String> registerIds = ret.result();
                Iterator<String> registerIdItr = registerIds.iterator();

                while (registerIdItr.hasNext()) {
                    String id = registerIdItr.next();
                    //注册成功后，返回确认消息.
                    regisResult.add(new JsonObject().put("result", id));
                }
            } else {
                try {
                    throw ret.cause();
                } catch (DuplicatedApiException e) {
                    logger.warn("以下API重复注册, 已被自动忽略: " + e.duplicatedItems().toString());
                    Iterator<Object> itr = e.correctItems().iterator();
                    while (itr.hasNext()) {
                        JsonObject id = (JsonObject) itr.next();
                        regisResult.add(new JsonObject()
                                .put("result", id.getString(DuplicatedApiException.KEY_REGISTER_ID)));
                    }
                } catch (Throwable ignore) {
                }
            }

            message.reply(regisResult);
        });


    }

    @Override
    public void handle(RoutingContext event) {
        router.handleContext(event);
    }
}
