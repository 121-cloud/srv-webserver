package otocloud.webserver.verticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import otocloud.webserver.WebServerVerticle;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by better on 15/9/11.
 */
@RunWith(VertxUnitRunner.class)
public class WebServerVerticleTest {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());


    protected static String websocketURI = "/eventbus/websocket"; //websocket后缀不能省略
    protected String address = "someaddress";

    private String WEB_SERVER_NAME = "121webserver-N01";

//    private String host = "10.1.200.234";
    private String host = "localhost";
    private int port = 8081;
    private Vertx vertx;
    private HttpClient client;

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
        JsonObject config = configWebServer();
        DeploymentOptions options = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle(WebServerVerticle.class.getName(), options, context.asyncAssertSuccess());

        client = vertx.createHttpClient(new HttpClientOptions().setDefaultHost(host).setDefaultPort(port));
    }

    private JsonObject configWebServer(){
        JsonObject config = new JsonObject();
        config.put("http.port", port);
        config.put(WebServerVerticle.CONFIG_WEBSERVER_NAME_KEY, WEB_SERVER_NAME);
        config.put("mongo_client", new JsonObject().put("host", "localhost").put("port", 27017));

        return config;
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }


    @Test
    public void getMethodTest(TestContext context) {
        final Async async = context.async();

        client.getNow(port, "localhost", "/api/status", response -> {
            response.bodyHandler(body -> {
                //打印body
                System.out.println(body.toString());

                context.assertTrue(body.toString().contains("alive"));
                async.complete();
            });
        });


    }

    @Test
    public void eventBusTest(TestContext context) {
        final Async async = context.async();

        client.websocket(websocketURI, ws -> {

            sendText(ws, "hello");

            async.complete();
        });
    }

    int msgCount = 0;

    @Test
    public void testReceiveInClient(TestContext context) {
        final Async async = context.async();
//        int msgCount = 0;
        client.websocket(websocketURI, ws -> {
            // Register
            JsonObject msg = new JsonObject().put("type", "register").put("address", address);
            ws.writeFrame(WebSocketFrame.textFrame(msg.encode(), true));

            // 接收服务器发来的消息
            ws.handler(buff -> {
                String str = buff.toString();
                JsonObject received = new JsonObject(str);
                context.assertEquals("rec", received.getString("type"));
                Object rec = received.getValue("body");

                System.out.println("收到服务端的消息");

                msgCount = msgCount + 1;

                if (msgCount == 2) {
                    //收到消息后，关闭连接
                    client.close();

                    async.complete();
                }
            });

            // Wait a bit to allow the handler to be setup on the server, then send message from eventbus

            // 若没有定时，将无法发送消息到消息总线
            vertx.setTimer(200, tid -> vertx.eventBus().send(address, "Sending from server1"));
            vertx.setTimer(200, tid -> vertx.eventBus().send(address, "Sending from server2"));

        });


    }

    //向服务器消息总线的某个地址发送消息
    private void sendText(WebSocket ws, String body) {
        JsonObject msg = new JsonObject().put("type", "send").put("address", address).put("body", body);

        ws.writeFrame(WebSocketFrame.textFrame(msg.encode(), true));
    }

    @Test
    public void it_should_get_the_config_of_webserver(TestContext context){
        final Async async = context.async();

//        String address = "DefaultWebServerName.configuration.get";
        String address = WEB_SERVER_NAME + ".configuration.get";
        vertx.setTimer(200, tid -> vertx.eventBus().<JsonObject>send(address, new JsonObject(), ret -> {
            if (ret.succeeded()) {
                Message<JsonObject> msg = ret.result();
                System.out.println(msg.body().toString());
                logger.info(msg.body().toString());
                async.complete();
            }else{
                context.fail();
            }
        }));

    }


}
