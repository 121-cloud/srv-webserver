package otocloud.webserver.dispatch;

import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.junit.Test;
import otocloud.webserver.base.WebServerTestBase;
import otocloud.webserver.handler.SessionHandler;
import otocloud.webserver.protocal.BasicBridgeProtocal;
import otocloud.webserver.protocal.BridgeProtocal;
import otocloud.webserver.util.AuthConfig;
import otocloud.webserver.util.AuthSwitch;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * DefaultTravellerImpl Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>九月 22, 2015</pre>
 */
public class DefaultTravellerImplTest extends WebServerTestBase {

//    @Test
    public void it_should_keep_sessionID_in_header(TestContext context) {
        final Async async = context.async();
        SessionStore sessionStore = startWebServer();

        final String[] sessions = new String[1];

        do1GetAndGetSessionID(sessions);

        do2GetAndGetSessionID(context, async, sessions[0], sessionStore);
//        do3Get(context, async, sessionId[0], sessionStore, cookies[0]);
    }

    private SessionStore startWebServer() {
        String addrInBus = "address";

        BridgeProtocal protocal = new BasicBridgeProtocal();

        EventBus bus = vertx.eventBus();

        EventBusTraveller traveller = EventBusTraveller.traveller(vertx);

        String[] sessionId = new String[1];

        bus.consumer(addrInBus, message -> {
            System.out.println(message.body().toString());

            JsonObject msgBody = new JsonObject(message.body().toString());

            //修改Session
            JsonObject session = msgBody.getJsonObject("session");

            if (session != null) {
                //update value
                Integer num = session.getInteger("num");

                //对客户端访问进行计数
                session.put("num", num == null ? 1 : num + 1);
            }

            JsonObject reply = new JsonObject();
            reply.put("result", "success");

            reply.put("session", session);
            sessionId[0] = session.getString("id");

            message.reply(reply); //解决io.vertx.core.VertxException: Connection was closed
        });

        SessionStore sessionStore = LocalSessionStore.create(vertx);
        AuthSwitch authSwitch = new AuthSwitch(false);
        AuthConfig authConfig  = new AuthConfig();
        SessionHandler sessionHandler = SessionHandler.create(sessionStore, authSwitch, authConfig);
        router.route().handler(sessionHandler);

        router.route("/myapp/session").handler(routingContext -> {

            JsonObject msgBody = protocal.buildMessageBody(routingContext);
            MultiMap headers = protocal.buildMessageHeaders(routingContext);

            traveller.carryHeaders(headers).carryBody(msgBody).from(routingContext).to(addrInBus);
        });
        return sessionStore;
    }

    @Test
    public void testSession(TestContext context) throws Exception {
        final Async async = context.async();
        String addrInBus = "address";

        BridgeProtocal protocal = new BasicBridgeProtocal();

        EventBus bus = vertx.eventBus();

        EventBusTraveller traveller = EventBusTraveller.traveller(vertx);

        String[] sessionId = new String[1];

        bus.consumer(addrInBus, message -> {
            System.out.println(message.body().toString());

            JsonObject msgBody = new JsonObject(message.body().toString());

            //修改Session
            JsonObject session = msgBody.getJsonObject("session");

            if (session != null) {
                //update value
                Integer num = session.getInteger("num");

                //对客户端访问进行计数
                session.put("num", num == null ? 1 : num + 1);
            }

            JsonObject reply = new JsonObject();
            reply.put("result", "success");

            reply.put("session", session);
            sessionId[0] = session.getString("id");

            message.reply(reply); //解决io.vertx.core.VertxException: Connection was closed
        });

        router.route().handler(CookieHandler.create()); //添加cookie

        SessionStore sessionStore = LocalSessionStore.create(vertx);
        io.vertx.ext.web.handler.SessionHandler sessionHandler = io.vertx.ext.web.handler.SessionHandler.create(sessionStore);
        router.route().handler(sessionHandler);

        router.route("/myapp/session").handler(routingContext -> {

            JsonObject msgBody = protocal.buildMessageBody(routingContext);
            MultiMap headers = protocal.buildMessageHeaders(routingContext);

            traveller.carryHeaders(headers).carryBody(msgBody).from(routingContext).to(addrInBus);
        });

        final String[] cookies = new String[1];

        do1Get(cookies);

        do2Get(cookies);

        do3Get(context, async, sessionId[0], sessionStore, cookies[0]);
    }

    private void do1Get(String[] cookies) throws InterruptedException {
        CountDownLatch latchStep1 = new CountDownLatch(1);

        client.getNow("/myapp/session", response -> {
            List<String> inCookies = response.cookies();
            if (inCookies.size() > 0) {
                cookies[0] = inCookies.get(0);
            }

            latchStep1.countDown();
        });

        latchStep1.await();
    }

    private void do2Get(String[] cookies) throws InterruptedException {
        CountDownLatch latchStep2 = new CountDownLatch(1);

        //第二次调用
        HttpClientRequest get = client.get("/myapp/session", response -> {
            List<String> inCookies = response.cookies();
            if (inCookies.size() > 0) {
                cookies[0] = inCookies.get(0);
            }
            latchStep2.countDown();
        });
        get.headers().add("Cookie", cookies[0]);
        get.end();

        latchStep2.await();
    }

    private void do3Get(TestContext context, Async async, String id, SessionStore sessionStore, String cookie) {
        HttpClientRequest get2 = client.get("/myapp/session", response -> {
            //验证num为3
            sessionStore.get(id, ret -> {
                if (ret.succeeded()) {
                    Session session = ret.result();
                    context.assertEquals(3, session.<Integer>get("num"));

                    async.complete();
                }
            });

        });
        get2.headers().add("Cookie", cookie);
        get2.end();
    }

    private void do1GetAndGetSessionID(String[] sessions) {
        CountDownLatch latchStep1 = new CountDownLatch(1);

        client.getNow("/myapp/session", response -> {

            String sessionId = response.headers().get("Set-Session");
            sessions[0] = sessionId;

            latchStep1.countDown();
        });

        try {
            latchStep1.await();
        } catch (InterruptedException ignore) {

        }
    }

    private void do2GetAndGetSessionID(TestContext context, Async async, String sessionId, SessionStore sessionStore){
        CountDownLatch latchStep1 = new CountDownLatch(1);

        //第二次调用
        HttpClientRequest get = client.get("/myapp/session", response -> {
            //验证num为2
            sessionStore.get(sessionId, ret -> {
                if (ret.succeeded()) {
                    Session session = ret.result();
                    context.assertEquals(2, session.<Integer>get("num"));

                    async.complete();
                }
            });
            latchStep1.countDown();
        });
        get.putHeader("Session", sessionId);
        get.end();

        try {
            latchStep1.await();
        } catch (InterruptedException ignore) {

        }
    }

    /**
     * Method: to(EventBusAddress address)
     */
    @Test
    public void testToAddress(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        String addrInBus = "address";

        BridgeProtocal protocal = new BasicBridgeProtocal();

        EventBus bus = vertx.eventBus();

        EventBusTraveller traveller = EventBusTraveller.traveller(vertx);

        bus.consumer(addrInBus, message -> {
            System.out.println(message.body().toString());

            JsonObject msgBody = new JsonObject(message.body().toString());

            //修改Session
            JsonObject session = msgBody.getJsonObject("session");

            if (session != null) {
                session.put("acctId", "123");
            }

            JsonObject reply = new JsonObject();
            reply.put("result", "success");

            reply.put("session", session);

            message.reply(reply); //解决io.vertx.core.VertxException: Connection was closed
        });

        router.route().handler(CookieHandler.create());
        SessionStore sessionStore = LocalSessionStore.create(vertx);
        io.vertx.ext.web.handler.SessionHandler sessionHandler = io.vertx.ext.web.handler.SessionHandler.create(sessionStore);
        router.route().handler(sessionHandler);

        router.route("/myapp/info").handler(routingContext -> {

            JsonObject msgBody = protocal.buildMessageBody(routingContext);
            MultiMap headers = protocal.buildMessageHeaders(routingContext);

            traveller.carryHeaders(headers).carryBody(msgBody).from(routingContext).to(addrInBus);
        });

        client.getNow("/myapp/info?para1=123&name=ye1", response -> {
            List<String> cookies = response.cookies();
            response.bodyHandler(body -> {
                System.out.println(body.toString());
                async.complete();
            });
        });
    }


    //测试应用没有响应的情况
//    @Test
    public void it_should_be_out_of_time(TestContext testContext) {
        final Async async = testContext.async();

        String addrInBus = "address";

        BridgeProtocal protocal = new BasicBridgeProtocal();

        EventBus bus = vertx.eventBus();

        EventBusTraveller traveller = EventBusTraveller.traveller(vertx);

        bus.consumer(addrInBus, message -> {
            System.out.println(message.body().toString());

            //message.reply(reply); 应用
        });

        router.route("/myapp/info").handler(routingContext -> {

            JsonObject msgBody = protocal.buildMessageBody(routingContext);
            MultiMap headers = protocal.buildMessageHeaders(routingContext);

            traveller.carryHeaders(headers).carryBody(msgBody).from(routingContext).to(addrInBus);
        });

        client.getNow("/myapp/info?para1=123&name=ye", response -> {
            response.bodyHandler(body -> {
                System.out.println(body.toString());
                async.complete();
            });
        });
    }
} 
