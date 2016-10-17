package otocloud.webserver.dispatch;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import otocloud.webserver.protocal.BasicBridgeProtocal;
import otocloud.webserver.protocal.BridgeProtocal;

import javax.annotation.PreDestroy;

/**
 * Created by zhangye on 2015-10-20.
 */
public class SessionTestHost {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    protected Vertx vertx;
    protected HttpServer server;
    protected HttpClient client;
    protected Router router;

    protected String host = "localhost";
    protected int port = 8080;

    public SessionTestHost(){
        vertx = Vertx.vertx();

        router = Router.router(vertx);
        server = vertx.createHttpServer(new HttpServerOptions().setPort(port).setHost(host));
        client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(port));

        server.requestHandler(router::accept).listen(result -> {
            if (result.succeeded()) {
                logger.info("服务器启动成功！");
            } else {
                logger.info("服务器启动失败！");
            }
        });

        router.route().handler(BodyHandler.create());
    }

    public void start(){
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
                //update value
                Integer num = session.getInteger("num");

                session.put("num", num == null ? 0 : num + 1);
            }

            JsonObject reply = new JsonObject();
            reply.put("result", "success");

            reply.put("session", session);

            message.reply(reply); //解决io.vertx.core.VertxException: Connection was closed
        });

        router.route().handler(CookieHandler.create());
        SessionStore sessionStore = LocalSessionStore.create(vertx);
        SessionHandler sessionHandler = SessionHandler.create(sessionStore);
        router.route().handler(sessionHandler);

        router.route("/myapp/session").handler(routingContext -> {

            JsonObject msgBody = protocal.buildMessageBody(routingContext);
            MultiMap headers = protocal.buildMessageHeaders(routingContext);

            traveller.carryHeaders(headers).carryBody(msgBody).from(routingContext).to(addrInBus);
        });

    }

    public static void main(String[] args){
        SessionTestHost host = new SessionTestHost();
        host.start();

    }
    @PreDestroy
    public void dispose(){
        vertx.close();

        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.close((asyncResult) -> {
                logger.info("服务器成功关闭.");
            });
        }
    }
}
