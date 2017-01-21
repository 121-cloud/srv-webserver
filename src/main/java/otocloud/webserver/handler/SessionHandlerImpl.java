package otocloud.webserver.handler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.SessionStore;
import otocloud.webserver.util.AuthConfig;
import otocloud.webserver.util.AuthSwitch;

import java.util.Map;
import java.util.Objects;

/**
 * zhangyef@yonyou.com on 2015-11-05.
 */
public class SessionHandlerImpl implements SessionHandler {
    public static final String HTT_HEADERS_SESSION = "Session";

    private static final Logger logger = LoggerFactory.getLogger(SessionHandlerImpl.class);

    private static final long GET_SESSION_TIMEOUT = 5000;
    private static final String HTT_HEADERS_SET_SESSION = "Set-Session";

    private final SessionStore sessionStore;
    private long sessionTimeout;

    //是否开启授权的开关.
    private AuthSwitch authSwitch;

    private AuthConfig authConfig;


    public SessionHandlerImpl(SessionStore sessionStore, long sessionTimeout,
                              AuthSwitch authSwitch,
                              AuthConfig authConfig) {
        this.sessionStore = sessionStore;
        this.sessionTimeout = sessionTimeout;
        this.authConfig = authConfig;
        this.authSwitch = authSwitch;

    }

    /**
     * Auth提供的查询授权信息的地址.
     * "服务名".user-management.query
     *
     * @return
     */
    private String getAuthQueryInfoAddress() {
        //"otocloud-auth.user-management.query";
        return this.authConfig.getSessionQueryAddress();
    }

    /**
     * 通过请求URL, 判断请求是否为登录请求.
     * 登录地址的URL通过AuthService注册时提供.
     * 登录地址格式如下: /api/.../login.
     *
     * @param request
     * @return
     */
    private boolean isLoginRequest(HttpServerRequest request) {
        String loginUrl = this.authConfig.getLoginUrl();
        if (loginUrl == null) {
            logger.warn("登录URL没有注册,无法完成登录.");
            return false;
        }
        return request.path().equals(this.authConfig.getLoginUrl());
    }

    /**
     * 判断当前URL是否为不用验证直接通过的URL.
     * 验证标准通过AuthService注册.
     *
     * @param request
     * @return
     */
    private boolean isSecurityUrls(HttpServerRequest request) {
        JsonArray urls = this.authConfig.getSecurityUrls();
        if (urls != null) {
            return urls.contains(request.path());
        }

        return false;
    }

    private void checkTokenByAuthService(Vertx vertx,
                                         String token, Future<String> checkFuture) {
        //向Auth发送请求, 根据token查询sessionId
        String address = getAuthQueryInfoAddress();
        JsonObject query = new JsonObject();
        query.put("condition", new JsonObject().put("token", token));

        logger.info("向Auth服务发送请求, 根据token查询SessionID");

        vertx.eventBus().<JsonObject>send(address, query, reply -> {
            if (reply.failed()) {
                checkFuture.fail("无法查询到SessionID");
                return;
            }

            JsonObject body = reply.result().body();
            String session_id = body.getJsonObject("data").getString("session_id");

            if (session_id == null) {
                checkFuture.fail("无法查询到SessionID, 将创建新的Session.");
                return;
            }

            if (logger.isInfoEnabled()) {
                logger.info("查询到的SessionID是: " + session_id);
            }

            checkFuture.complete(session_id);

        });

    }

    private void dispatchRequestAfterLogin(RoutingContext context) {
        HttpServerRequest request = context.request();
        Future<String> sessionIdQueryFuture = Future.future();
        String token = request.getParam("token");

        if (!authSwitch.isOn()) {
            //没有开启授权,则直接通过请求
            context.next();
            return;
        }

        if (token == null) {
            //没有token,直接拒绝
            rejectRequest(context);
            return;
        }

        logger.info("收到的请求中含有token: " + token);

        checkTokenByAuthService(context.vertx(), token, sessionIdQueryFuture);

        RoutingContext succeedContext = context;
        sessionIdQueryFuture.setHandler(ret -> {
            if (ret.failed()) {
                rejectRequest(succeedContext);
                return;
            }

            String repliedSessionID = ret.result();
            getSessionOrCreateIt(succeedContext, repliedSessionID);
        });
    }

    /**
     * 直接转发请求.
     * 例如,登录请求/退出请求等.
     *
     * @param context
     */
    private void dispatchRequestWithoutAuth(RoutingContext context) {
        createNewSession(context);
        context.next();
    }

    private void rejectRequest(RoutingContext context) {
        HttpServerResponse response = context.response();
        response.end(new JsonObject()
                .put("errCode", 4005)
                .put("errMsg", "您访问了未经授权的内容.[" + context.request().path() + "]").toString());
    }

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        context.response().ended();

        //没有开启权限过滤,默认全部通过.
        if (!this.authSwitch.isOn()) {
            context.next();
            return;
        }

        boolean hasToken = request.params().contains("token");
        if (hasToken) {
            dispatchRequestAfterLogin(context);
            return;
        }
        //没有token,并且是登录请求.
        if (isLoginRequest(request) || isSecurityUrls(request)) {
            dispatchRequestWithoutAuth(context);
            return;
        }

        rejectRequest(context);
    }


    private void getSessionOrCreateIt(RoutingContext context, String repliedSessionID) {
        Objects.requireNonNull(repliedSessionID);

        getSession(context.vertx(), repliedSessionID, res -> {
            if(res.failed()){

            }
            if (res.succeeded()) {
                Session session = res.result();
                if (session != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("查找到Session, 内容如下.");
                        Map data = session.data();
                        data.forEach((k, v) -> {
                            logger.debug(k + ": " + v);
                        });
                    }
                    context.setSession(session);
                    session.setAccessed();
                    addStoreSessionHandler(context);
                } else {
                    // Cannot find session - either it timed out, or was explicitly destroyed at the server side on a
                    // previous request.
                    // Either way, we create a new one.
                    if (logger.isDebugEnabled()) {
                        logger.debug("无法根据SessionID查找到Session.");
                        logger.debug("Session已经超时, 或者被销毁. 正在创建新的Session.");

                        //登录已经超时,返回浏览器错误信息
                        HttpServerResponse response = context.response();
                        response.end(new JsonObject()
                                .put("errCode", 1)
                                .put("errMsg", "登录已经超时,请重新登录.").toString());
                        return;
                    }
//                    createNewSession(context);
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("无法根据SessionID查找到Session.");
                    logger.debug(res.cause());
                }
                context.fail(res.cause());
            }
            context.next();
        });
    }

    private void getSession(Vertx vertx, String sessionID, Handler<AsyncResult<Session>> resultHandler) {
        doGetSession(vertx, System.currentTimeMillis(), sessionID, resultHandler);
    }

    private void doGetSession(Vertx vertx, long startTime, String sessionID,
                              Handler<AsyncResult<Session>> resultHandler) {
        sessionStore.get(sessionID, res -> {
            if (res.succeeded()) {
                if (res.result() == null) {
                    // Can't find it so retry. This is necessary for clustered sessions as it can take sometime for the session
                    // to propagate across the cluster so if the next request for the session comes in quickly at a different
                    // node there is a possibility it isn't available yet.
                    if (System.currentTimeMillis() - startTime < GET_SESSION_TIMEOUT) {
                        logger.info("没有获得Session, 可能的原因是 \'WebServer重启后用户未重新登录\'. 正在重新尝试.");
                        vertx.setTimer(500, v -> doGetSession(vertx, startTime, sessionID, resultHandler));
                        return;
                    }
                }
            } else {
                logger.error("获取Session失败, SessionID是: " + sessionID + ".");
            }
            resultHandler.handle(res);
        });
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    private void addStoreSessionHandler(RoutingContext context) {
        context.addHeadersEndHandler(v -> {
            Session session = context.session();
            if (!session.isDestroyed()) {
                // Store the session
                session.setAccessed();
                sessionStore.put(session, res -> {
                    if (res.failed()) {
                        logger.error("Failed to store session", res.cause());
                    }
                });
            } else {
                sessionStore.delete(session.id(), res -> {
                    if (res.failed()) {
                        logger.error("Failed to delete session", res.cause());
                    }
                });
            }
        });
    }

    private void createNewSession(RoutingContext context) {
        Session session = sessionStore.createSession(sessionTimeout);
        context.setSession(session);

        //在响应头中添加Set-Session
        context.response().headers().add(HTT_HEADERS_SET_SESSION, session.id());

        addStoreSessionHandler(context);
    }

}
