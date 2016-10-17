package otocloud.webserver.base;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by better on 15/9/18.
 */
@RunWith(VertxUnitRunner.class)
public class WebServerTestBase {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    protected Vertx vertx;
    protected HttpServer server;
    protected HttpClient client;
    protected Router router;

    protected String host = "localhost";
    protected int port = 8080;


    @Before
    public void setUp(TestContext context) {
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

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());

        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.close((asyncResult) -> {
                context.assertTrue(asyncResult.succeeded());
                logger.info("服务器成功关闭.");
            });
        }

    }

    @Test
    public void doNothing(TestContext context){

    }
}
