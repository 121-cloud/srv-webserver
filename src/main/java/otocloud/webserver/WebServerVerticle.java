package otocloud.webserver;

import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import otocloud.webserver.handler.*;
import otocloud.webserver.handler.SessionHandler;
import otocloud.webserver.util.AuthConfig;
import otocloud.webserver.util.AuthSwitch;
import otocloud.webserver.util.GlobalDataPool;
import otocloud.webserver.util.ShareableUtil;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 * 默认端口为8081.
 */
public class WebServerVerticle extends AbstractVerticle {

    public static final String CONFIG_WEBSERVER_NAME_KEY = "webserver_name";
    private static final String CONFIG_REGISTER_ADDRESS_KEY = "address.register";
    private static final String CONFIG_UN_REGISTER_ADDRESS_KEY = "address.unregister";
    private static final String CONFIG_EVENTBUS_URL_KEY = "eventbus.url";
    private static final String CONFIG_HTTP_PORT_KEY = "http.port";
    private static final String CONFIG_AUTHENTICATION = "auth.enabled";
    private static final String eventBusRouteURL = "/eventbus/*";

    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private Router mainRouter;
    private HttpServer server;
    private JsonObject config;

    //默认开启授权.
    private AuthSwitch authSwitch;
    private AuthConfig authConfig;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        int port = 8081;
        JsonObject deployConfig = new JsonObject();
        deployConfig.put(CONFIG_HTTP_PORT_KEY, port);
        deployConfig.put("static.directory", "webroot");
        DeploymentOptions options = new DeploymentOptions().setConfig(deployConfig);
        vertx.deployVerticle(WebServerVerticle.class.getName(), options);
    }

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        config = context.config();

        //如果有mongo_client配置,放入上下文当中.
        if (config.containsKey("mongo_client")) {
            JsonObject mongo_client = config.getJsonObject("mongo_client");
            GlobalDataPool.INSTANCE.put("mongo_client_at_webserver", mongo_client);
        }

        if (config.containsKey(CONFIG_WEBSERVER_NAME_KEY)) {
            GlobalDataPool.INSTANCE.put(CONFIG_WEBSERVER_NAME_KEY, config.getString(CONFIG_WEBSERVER_NAME_KEY));
        }

        //For Debug - 默认关闭授权, 允许所有请求通过.
        boolean authEnabled = config.getBoolean(CONFIG_AUTHENTICATION, false);

        authSwitch = new AuthSwitch(authEnabled);
        authConfig = new AuthConfig();

        mainRouter = createMainRouter(vertx);
    }

    private Router createMainRouter(Vertx vertx) {
        return ShareableUtil.getMainRouter(vertx);
    }

    @Override
    public void start(Future<Void> future) {

        HttpServerOptions options = createOptions();
        server = vertx.createHttpServer(options);
        server.requestHandler(configMainRouter()::accept);

        server.listen(result -> {
            if (result.succeeded()) {

                startEventBusAPI();

                logger.info("WebServer状态查询地址：" + options.getHost() + ":" + options.getPort() + "/api/status");
                logger.info("WebServer启动成功，监听[" + options.getHost() + ":" + options.getPort() + "]");
                future.complete();
            } else {
                future.fail(result.cause());
            }
        });
    }

    /**
     * 提供事件总线接口。
     * - 查询WebServer配置信息。
     */
    private void startEventBusAPI() {
        String webServerName = config.getString(CONFIG_WEBSERVER_NAME_KEY);
        String configAddress = null;
        if (webServerName != null) {
            configAddress = webServerName + ".configuration.get";
        } else {
            configAddress = "DefaultWebServerName.configuration.get";
        }

        logger.info("WebServer配置的查询地址: " + configAddress);
        vertx.eventBus().<JsonObject>consumer(configAddress, msg -> {
            msg.reply(config);
        });

        String updateConfigAddress = webServerName + ".configuration.put";
        logger.info("WebServer配置的修改地址: " + updateConfigAddress);

        vertx.eventBus().<JsonObject>consumer(updateConfigAddress, msg -> {

            JsonObject config = msg.body();

            if (logger.isInfoEnabled()) {
                logger.info("WebServer配置的修改请求: " + config);
            }

            Boolean enabled = config.getBoolean("auth.enabled");
            if (enabled != null) {
                if (enabled) {
                    if (logger.isInfoEnabled()) {
                        logger.info("WebServer配置的已经修改: " + "开启授权.");
                    }
                    authSwitch.turnOn();
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("WebServer配置的已经修改: " + "关闭授权.");
                    }
                    authSwitch.turnOff();
                }
            }

            String sessionQueryAddress = config.getString("auth.session_query_address");
            if (sessionQueryAddress != null) {
                authConfig.setSessionQueryAddress(sessionQueryAddress);
            }

            String loginUrl = config.getString("auth.login_url");
            if (loginUrl != null) {
                authConfig.setLoginUrl(loginUrl);
            }

            JsonArray securityUrls = config.getJsonArray("auth.security_urls");
            if (securityUrls != null) {
                authConfig.setSecurityUrls(securityUrls);
            }

            msg.reply(new JsonObject().put("errCode", 0).put("errMsg", "Success"));
        });
    }

    @Override
    public void stop(Future<Void> future) {
        if (server == null) {
            future.complete();
            logger.info("WebServer停止成功.");
            return;
        }
        server.close(result -> {
            if (result.failed()) {
                logger.info("WebServer停止失败。" + result.cause().getMessage());
                future.fail(result.cause());
            } else {
                future.complete();
                logger.info("WebServer停止成功.");
            }
        });
    }

    private HttpServerOptions createOptions() {
        HttpServerOptions options = new HttpServerOptions();
        // 从配置中读取端口号，如果未配置端口号，则默认为8081.
//        options.setHost("localhost");
        options.setPort(config().getInteger(CONFIG_HTTP_PORT_KEY, 8081));
        return options;
    }

    private Router configMainRouter() {

        // 解决跨域问题
        handlerCORS();

        //配置错误处理
        mainRouter.route().failureHandler(ErrorHandler.create(true));

        //处理消息体, BodyHandler需要放在SessionHandler之前.
        mainRouter.route().handler(BodyHandler.create());

		/* Session / cookies for users */
        mainRouter.route().handler(CookieHandler.create());

        /* SockJS / EventBus */
        // Socket需要放在SessionHandler之前, 以防
        String eventBusUrl = eventBusRouteURL;
        if (config.containsKey(CONFIG_EVENTBUS_URL_KEY)) {
            eventBusUrl = config.getString(CONFIG_EVENTBUS_URL_KEY);
        }
        mainRouter.route(eventBusUrl).handler(eventBusHandler());

        SessionStore sessionStore = LocalSessionStore.create(vertx);
        SessionHandler sessionHandler = SessionHandler.create(sessionStore, authSwitch, authConfig);
        mainRouter.route().handler(sessionHandler);

        //添加静态资源支持
        StaticHandler staticHandler = StaticHandler.create();
        if (config.containsKey("static.directory")) {
            staticHandler.setWebRoot(config.getString("static.directory"));
        }
        mainRouter.route("/static/*").handler(staticHandler);

        //处理API注册
        mainRouter.route("/api/*").handler(apiMessageHandler());

		/* API */
        mainRouter.mountSubRouter("/api", apiRouter());


        return mainRouter;
    }

    private void handlerCORS() {
        mainRouter.route().handler(CorsHandler.create("*")
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedHeader("X-PINGARUNER")
                .allowedHeader("Content-Type"));

        mainRouter.route().handler(new AllowAllCorsHandlerImpl());
    }


    private EventMessageHandler apiMessageHandler() {
        String regAddr = config.getString(CONFIG_REGISTER_ADDRESS_KEY);
        String unregAddr = config.getString(CONFIG_UN_REGISTER_ADDRESS_KEY);

        DispatchOptions dispatchOptions = new DispatchOptions();
        if (regAddr != null) {
            dispatchOptions.setRegisterAddress(regAddr);
        }

        if (unregAddr != null) {
            dispatchOptions.setUnRegisterAddress(unregAddr);
        }

        String webServerName = config.getString(CONFIG_WEBSERVER_NAME_KEY);
        //添加注册地址和注销地址的前缀
        dispatchOptions.setWebServerName(webServerName);

        EventMessageHandler handler = EventMessageHandler.create(vertx, dispatchOptions);
        handler.bridge(new RestApiBridgeOptions());

        return handler;
    }

    private SockJSHandler eventBusHandler() {
        SockJSHandlerOptions sockJSHandlerOptions = new SockJSHandlerOptions();
        SockJSHandler handler = SockJSHandler.create(vertx, sockJSHandlerOptions);

        BridgeOptions options = new BridgeOptions();

        PermittedOptions permitted = new PermittedOptions(); /* allow everything, we don't care for the demo */
        options.addOutboundPermitted(permitted);
        options.addInboundPermitted(new PermittedOptions());

        //查询配置参数
        if (config.containsKey("sockJS_bridge_options")) {
            try {
                JsonObject bridgeOption = config.getJsonObject("sockJS_bridge_options");
                long timeout = bridgeOption.getInteger("ping_timeout");

                logger.info("设置 SockJS的 PintTimeout : " + timeout);

                options.setPingTimeout(timeout);
            } catch (Exception ignore) {
                logger.warn("配置项格式错误: sockJS_bridge_options");
            }
        }

        handler.bridge(options, new EventBusBridgeHandler());

        return handler;
    }

    private Router apiRouter() {

        Router router = Router.router(vertx);
        router.route().consumes("application/json");
        router.route().produces("application/json");

        router.route().handler(context -> {
            context.response().headers().add(CONTENT_TYPE, "application/json");
            context.next();
        });

        /* status / application status : no token needed */
        router.get("/status").handler(this::status);

        return router;
    }

    private void status(RoutingContext context) {
        HttpServerResponse response = context.response();
        String host = context.request().getHeader("Host");
        response.putHeader("content-type", "application/json; charset=utf-8");

        response.setChunked(true);
        response.setStatusCode(200);
        response.write("Server Host\t: " + host + "\n");
        response.end("Server Status\t: alive");
    }
}
