package vertx;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 测试RoutingContext.
 * 当接收到HTTP请求时发送总线消息,会导致Request失效.
 * zhangyef@yonyou.com on 2015-11-25.
 */
@RunWith(VertxUnitRunner.class)
public class RoutingContextTest {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private Vertx vertx;
    private HttpServer server;

    private Router router;

    int port = 8088;

    @Before
    public void setUp(TestContext testContext) {
        final Async async = testContext.async();

        vertx = Vertx.vertx();

        router = makeRouter(vertx);



        HttpServerOptions options = new HttpServerOptions();
        options.setPort(port);
        server = vertx.createHttpServer(options);
        server.requestHandler(router::accept);

        server.listen(ret -> {
            if (ret.succeeded()) {
                logger.info("开始监听:" + port);
                async.complete();
            }
        });

        vertx.eventBus().consumer("info", msg -> {
            msg.reply("1");
        });

    }

    private Router makeRouter(Vertx vertx) {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.route().handler(context -> {
            //发送总线请求
            context.vertx().eventBus().<String>send("info", "get", ret -> {
                if (ret.succeeded()) {
                    Message<String> msg = ret.result();
                    System.out.println(msg.body());
                    context.next();
                }
            });


        });



        router.route(HttpMethod.PUT, "/info").handler(context -> {
            HttpServerRequest request = context.request();
            MultiMap map = request.headers();

            logger.info("put /info");
            context.response().end("ok");
        });

        return router;
    }

    @After
    public void tearDown(TestContext testContext) {

    }

    /**
     * 在接到HTTP请求后,首先发送事件总线消息,然后继续处理.
     */
    @Test
    public void it_should_send_message_in_http_request(TestContext testContext) {
        final Async async = testContext.async();

        HttpClient client = vertx.createHttpClient();
        client.put(port, "localhost", "/info", response -> {
            response.bodyHandler(body -> {
                System.out.println(body.toString());
                async.complete();
            });
        }).end("put");
    }
}
