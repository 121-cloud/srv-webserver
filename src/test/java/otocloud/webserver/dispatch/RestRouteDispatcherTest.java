/*package otocloud.webserver.dispatch;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
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
import otocloud.common.Command;
import otocloud.webserver.WebServerVerticle;
import otocloud.webserver.protocal.BasicBridgeProtocal;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertTrue;

*//**
 * RestRouteTable Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>九月 21, 2015</pre>
 *//*
@RunWith(VertxUnitRunner.class)
public class RestRouteDispatcherTest {
    public static final String API_PREFIX = "/api";
    *//**
     * 远程Mongo的IP地址.
     *//*
    private final String MONGO_HOST = "10.10.23.112";
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private int port = 8080;
    private Vertx vertx;
    private HttpClient client;

    private String registerAddress;


    @Before
    public void setUp(TestContext context) throws Exception {

        vertx = Vertx.vertx();
        client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(port).setDefaultHost("localhost"));

        registerAddress = "my.register";

        JsonObject config = new JsonObject();
        config.put("http.port", port);
        config.put("address.register", registerAddress);
        config.put("mongo_client", new JsonObject().put("host", MONGO_HOST).put("port", 27017));

        DeploymentOptions options = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle(WebServerVerticle.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        try {
            vertx.close(context.asyncAssertSuccess());
        } catch (VertxException e) {
            logger.info("Vertx关闭时发生异常。");
        }

    }

    *//**
     * 测试事件总线地址的动态解析功能.
     *//*
    @Test
    public void it_should_decorate_address(TestContext testContext) {
        final Async async = testContext.async();
        EventBus bus = vertx.eventBus();

        JsonObject registerMsg = new JsonObject();
        registerMsg.put("address", "my.application.");
        registerMsg.put("decoratingAddress", "my.resolver");
        registerMsg.put("uri", "/app/decorating");
        registerMsg.put("method", "get");
        registerMsg.put("messageFormat", "command");

        bus.<JsonObject>consumer("my.resolver", msg -> {
            JsonObject req = msg.body();
            String addressPattern = req.getString("addressPattern");

            JsonObject reply = new JsonObject();
            reply.put("realAddress", addressPattern + "1");
            msg.reply(reply);
        });

        //模拟一个具体应用
        Command.consumer(vertx, "my.application.1", cmd -> {

            assertTrue(cmd.isValid());
            cmd.succeed(vertx, new JsonObject().put("result", "ok"));

        });

        //某个应用向事件总线注册API和消息地址
        bus.send(registerAddress, registerMsg, event -> {
            client.getNow(API_PREFIX + "/app/decorating", response -> {
                testContext.assertEquals(200, response.statusCode());

                response.bodyHandler(body -> {
                    testContext.assertNotNull(body.toString());
                    JsonObject bodyObj = new JsonObject(body.toString());
                    testContext.assertEquals(0, bodyObj.getInteger("statusCode"));
                    async.complete();
                });

            });
        });
    }

    @Test
    public void it_should_fail_by_command(TestContext testContext) {
        final Async async = testContext.async();

        EventBus bus = vertx.eventBus();

        JsonObject registerMsg = new JsonObject();
        registerMsg.put("address", "my.application");
        registerMsg.put("uri", "/app/status");
        registerMsg.put("method", "get");
        registerMsg.put("messageFormat", "command");

        //模拟一个具体应用
        Command.consumer(vertx, "my.application", cmd -> {

            assertTrue(cmd.isValid());
            cmd.fail(vertx, 12, "无法完成请求.");
        });

        //某个应用向事件总线注册API和消息地址
        bus.send(registerAddress, registerMsg, event -> {
            client.getNow(API_PREFIX + "/app/status", response -> {
                testContext.assertEquals(200, response.statusCode());

                response.bodyHandler(body -> {
                    testContext.assertNotNull(body.toString());
                    JsonObject bodyObj = new JsonObject(body.toString());
                    testContext.assertEquals(12, bodyObj.getInteger("statusCode"));
                    testContext.assertEquals("无法完成请求.", bodyObj.getString("statusMessage"));
                    async.complete();
                });

            });
        });
    }

    @Test
    public void it_should_communicate_by_command(TestContext testContext) {
        final Async async = testContext.async();

        EventBus bus = vertx.eventBus();

        JsonObject registerMsg = new JsonObject();
        registerMsg.put("address", "my.application");
        registerMsg.put("uri", "/app/status");
        registerMsg.put("method", "post");
        registerMsg.put("messageFormat", "command");


        //模拟一个具体应用
        Command.consumer(vertx, "my.application", cmd -> {
            assertTrue(cmd.getContent() != null);
            assertTrue(cmd.isValid());
            cmd.succeed(vertx, new JsonObject().put("status", "alive"));
        });

        //某个应用向事件总线注册API和消息地址
        bus.send(registerAddress, registerMsg, event -> {
            Message<JsonObject> result = (Message) event.result();

            JsonObject po = new JsonObject();
            po.put("name", "yezhang");

            client.post(API_PREFIX + "/app/status", response -> {
                testContext.assertEquals(200, response.statusCode());
                response.bodyHandler(body -> {
                    System.out.println(body.toString());
                    testContext.assertNotNull(body.toString());
                    async.complete();
                });

            }).end(po.toString());
        });
    }

    @Test
    public void it_should_return_error_code(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        EventBus bus = vertx.eventBus();

        JsonObject registerMsg = new JsonObject();
        registerMsg.put("address", "my.application");
        registerMsg.put("uri", "/app/login");
        registerMsg.put("method", "post");

        //模拟一个具体应用，处理业务失败
        bus.consumer("my.application", message -> {
            message.fail(40001, "some error");
        });

        //某个应用向事件总线注册API和消息地址
        bus.send(registerAddress, registerMsg, event -> {
            Message<JsonObject> result = (Message) event.result();
            JsonObject reply = result.body();
            //验证注册结果
            testContext.assertNotNull(reply.getString("result"), "success");

            //注册成功后，发送post请求
            JsonObject po = new JsonObject();
            po.put("name", "yezhang");

            HttpClientRequest post = client.post(API_PREFIX + "/app/login", response -> {
                testContext.assertEquals(200, response.statusCode());
                response.bodyHandler(body -> {
                    System.out.println(body.toString());
                    testContext.assertNotNull(body.toString());
                    async.complete();
                });

            });

            post.end(po.toString());
        });
    }

    @Test
    public void it_should_register_apis_partially(TestContext testContext) {
        final Async async = testContext.async();

        EventBus bus = vertx.eventBus();
        JsonObject registerMsg1 = new JsonObject();
        registerMsg1.put("address", "my.application");
        registerMsg1.put("uri", "/app/login");
        registerMsg1.put("method", "post");

        JsonObject registerMsg2 = new JsonObject();
        registerMsg2.put("address", "my.application");
        registerMsg2.put("uri", "/app/login");
        registerMsg2.put("method", "post");

        JsonArray registerMsgs = new JsonArray().add(registerMsg1).add(registerMsg2);

        //某个应用向事件总线注册API和消息地址
        bus.send(registerAddress, registerMsgs, event -> {
            Message<JsonArray> result = (Message) event.result();
            JsonArray reply = result.body();
            //验证注册结果
            testContext.assertNotNull(reply.getJsonObject(0).getString("result"), "success");

            async.complete();

        });
    }

    @Test
    public void it_should_cause_duplicated_api_registration(TestContext testContext) {
        final Async async = testContext.async();

        EventBus bus = vertx.eventBus();
        JsonObject registerMsg = new JsonObject();
        registerMsg.put("address", "my.application");
        registerMsg.put("uri", "/app/login");
        registerMsg.put("method", "post");

        //某个应用向事件总线注册API和消息地址
        bus.send(registerAddress, registerMsg, event -> {
            Message<JsonObject> result = (Message) event.result();
            JsonObject reply = result.body();
            //验证注册结果
            testContext.assertNotNull(reply.getString("result"), "success");

            bus.send(registerAddress, registerMsg, innerEvent -> {
                Message<JsonObject> innResult = (Message) innerEvent.result();
                JsonObject innerReply = innResult.body();
                //验证注册结果
                testContext.assertNotNull(innerReply.getString("errMsg"), "success");

                async.complete();
            });

        });
    }

    @Test
    public void it_should_register_an_form_api(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        EventBus bus = vertx.eventBus();
        JsonObject registerMsg = new JsonObject();
        registerMsg.put("address", "my.application");
        registerMsg.put("uri", "/app/login");
        registerMsg.put("method", "post");

        //模拟具体应用
        bus.consumer("my.application", message -> {
            System.out.println(message.headers());
            System.out.println(message.body());
            //提取请求的form参数
            JsonObject formData = new JsonObject(message.body().toString());

            JsonObject params = formData.getJsonObject(BasicBridgeProtocal.QUERY_PARAMS);
            testContext.assertEquals("0", params.getString("orgLocCity"));
            testContext.assertEquals("ncscm5", params.getString("orgOmpName"));

            //向客户端返回信息
            JsonObject reply = new JsonObject();
            reply.put("result", "successfully");

            message.reply(reply);
        });

        //某个应用向事件总线注册API和消息地址
        bus.send(registerAddress, registerMsg, event -> {
            Message<JsonObject> result = (Message) event.result();
            JsonObject reply = result.body();
            //验证注册结果
            testContext.assertNotNull(reply.getString("result"), "success");

            //注册成功后，发送post请求
            String postData =
                    "orgName=345gt&orgLocProvince=%E5%AE%89%E5%BE%BD%E7%9C%81" +
                            "&orgLocCity=0&industry=0&orgOmpName=ncscm5" +
                            "&orgOmpPwd=a123456A&orgContact=qww" +
                            "&mobile=18101286680&email=";

            JsonObject po = new JsonObject();
            po.put("name", "yezhang");

            HttpClientRequest post = client.post(API_PREFIX + "/app/login", response -> {
                response.bodyHandler(body -> {
                    //验证来自应用的body数据。
                    JsonObject someData = new JsonObject(body.toString());

                    testContext.assertEquals("successfully", someData.getString("result"));
                    async.complete();
                });
            });

            post.putHeader("Content-Type", "application/x-www-form-urlencoded"); //模拟表单
            post.end(postData);
        });
    }

    @Test
    public void it_should_register_an_post_json_api(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        EventBus bus = vertx.eventBus();

        JsonObject registerMsg = new JsonObject();
        registerMsg.put("address", "my.application");
        registerMsg.put("uri", "/app/login/:id");
        registerMsg.put("method", "post");

        //模拟一个具体应用
        bus.consumer("my.application", message -> {
//            System.out.println(message.headers());
//            System.out.println(message.body());
            JsonObject poData = new JsonObject(message.body().toString());

            //收到的浏览器请求
            System.out.println(poData.toString());

            testContext.assertEquals("yezhang",
                    poData.getJsonObject(BasicBridgeProtocal.REQUEST_CONTENT).getString("name"));

            //向客户端返回信息
            JsonObject reply = new JsonObject();
            reply.put("result", "successfully");
            reply.put("data", poData.getJsonObject(BasicBridgeProtocal.REQUEST_CONTENT));

            message.reply(reply);
        });

        //某个应用向事件总线注册API和消息地址
        bus.send(registerAddress, registerMsg, event -> {
            Message<JsonObject> result = (Message) event.result();
            JsonObject reply = result.body();
            //验证注册结果
            testContext.assertNotNull(reply.getString("result"), "success");

            //注册成功后，发送post请求
            JsonObject po = new JsonObject();
            po.put("name", "yezhang");

            HttpClientRequest post = client.post(API_PREFIX + "/app/login/1", response -> {
                response.bodyHandler(body -> {
                    //打印body
                    System.out.println(body.toString());

                    //得到服务器响应，表示请求成功
                    async.complete();
                });
            });

            post.end(po.toString());
        });


    }


    *//**
     * 测试同时注册多个URL到不同地址
     *
     * @param testContext
     * @throws Exception
     *//*
    @Test
    public void it_should_register_many_get_json_api(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        EventBus bus = vertx.eventBus();

        JsonObject registerMsgApi1 = new JsonObject()
                .put("address", "my.application")
                .put("uri", "/app/login")
                .put("method", "post");

        JsonObject registerMsgApi2 = new JsonObject()
                .put("address", "my.application")
                .put("uri", "/app/logout")
                .put("method", "post");

        JsonArray registerMany = new JsonArray().add(registerMsgApi1).add(registerMsgApi2);

        //模拟一个具体应用
        bus.consumer("my.application", message -> {
            JsonObject poData = new JsonObject(message.body().toString());

            //收到的浏览器请求
            System.out.println(poData.toString());

            testContext.assertEquals("yezhang",
                    poData.getJsonObject(BasicBridgeProtocal.REQUEST_CONTENT).getString("name"));

            //向客户端返回信息
            JsonObject reply = new JsonObject();
            reply.put("result", "successfully");
            reply.put("data", poData.getJsonObject(BasicBridgeProtocal.REQUEST_CONTENT));

            message.reply(reply);
        });

        //某个应用向事件总线注册API和消息地址
        bus.send(registerAddress, registerMany, event -> {
            Message<JsonArray> result = (Message) event.result();
            JsonArray reply = result.body();
            //验证注册结果
            testContext.assertTrue(reply.isEmpty() == false);

            //注册成功后，发送post请求
            JsonObject po = new JsonObject();
            po.put("name", "yezhang");

            ExecutorService exec = Executors.newCachedThreadPool();
            CyclicBarrier barrier = new CyclicBarrier(2, () -> {

                async.complete(); //结束测试
                exec.shutdown();
            });
            exec.execute(() -> post(API_PREFIX + "/app/login", po.toString(), complete -> {
                System.out.println("/app/login, complete");
                try {
                    barrier.await();//完成请求
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }));

            exec.execute(() -> post(API_PREFIX + "/app/logout", po.toString(), complete -> {
                System.out.println("/app/logout, complete");
                try {
                    barrier.await();//完成请求
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }));


        });
    }

    private void post(String url, String content, Handler<String> complete) {
        HttpClientRequest post = client.post(url, response -> {
            response.bodyHandler(body -> {
                //打印body
                System.out.println(body.toString());

                //得到服务器响应，表示请求成功

                complete.handle(body.toString());

            });
        });

        post.end(content);
    }

}
*/