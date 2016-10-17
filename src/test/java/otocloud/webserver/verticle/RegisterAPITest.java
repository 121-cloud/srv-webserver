package otocloud.webserver.verticle;

import io.vertx.core.DeploymentOptions;
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
import org.junit.*;
import org.junit.runners.MethodSorters;
import otocloud.webserver.WebServerVerticle;
import otocloud.webserver.handler.DispatchOptions;
import otocloud.webserver.protocal.BasicBridgeProtocal;
import otocloud.webserver.util.tuple.ThreeTuple;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by better/zhangye on 15/9/23.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RegisterAPITest {
    protected static Logger logger = LoggerFactory.getLogger(RegisterAPITest.class);

    private static CountDownLatch deployLatch; //控制Vertx的同步

    private static int port = 8080;
    private static Vertx vertx;
    private static HttpClient client;
    private static EventBus bus;
    private static String WEB_SERVER_NAME = "121webserver-N01";

    private static DispatchOptions dispatchOptions = new DispatchOptions();

    //((addr, url, method), registerId)
    private static HashMap<ThreeTuple<String,String,String>, String> registerInfo;

    @BeforeClass
    public static void setUp() {
        vertx = Vertx.vertx();
        client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(port).setDefaultHost("localhost"));

        registerInfo = new HashMap<>();

        deployWebServer();

        initEventBus();

        initApp("my.application");
    }

    @AfterClass
    public static void tearDown() {
        try {
            vertx.close();
        } catch (VertxException e) {
            logger.info("Vertx关闭时发生异常。");
        }
    }

    @Test
    public void test01_it_should_register_api_and_GET_a_response_with_params(){
        String addr = "my.application";
        String url = "/my/app/:id";
        String method = "get";
        registerAPI(addr, url, method);

        get("/api/my/app/1?token1=abcde123");
    }

    @Test
    public void test02_it_should_register_api_and_GET_a_response(){
        registerAPI("my.application", "/my/apps", "get");

        get("/api/my/apps");
    }

    @Test
    public void test03_it_should_get_a_response_after_POST(){
        registerAPI("my.application", "/my/app/:id", "post");

        post("/api/my/app/1");
    }

    @Test
    public void test04_it_should_get_a_response_after_PUT(){
        registerAPI("my.application", "/my/app/:id", "put");

        put("/api/my/app/1?para=0");
    }

    @Test
    public void test05_it_should_get_a_response_after_DELETE(){
        registerAPI("my.application", "/my/app/:id", "delete");

        delete("/api/my/app/1");
    }

    @Test
    public void test06_it_should_get_err_page_after_DELETE_when_unregister_delete(){
        unRegisterAPI("my.application", "/my/app/:id", "delete");

        deleteWithError("/api/my/app/1");
        post("/api/my/app/1");
    }

    @Test
    public void test07_it_should_register_dynamic_eventbus_address(){
        registerAPIWithDynamicAddress("id.application", "/my/app/dynamic/:id", "get", "my.decoratingAddress");

        get("/api/my/app/dynamic/1?token1=abcde123", "my.application");
    }

    @Test
    public void test08_it_should_register_many(){
        JsonObject registerMsg = new JsonObject();
        registerMsg.put("address", "addr1");
        registerMsg.put("uri", "/app1");
        registerMsg.put("method", "get");

        JsonObject registerMsg2 = new JsonObject();
        registerMsg2.put("address", "addr2");
        registerMsg2.put("uri", "/app2");
        registerMsg2.put("method", "get");


        sendMessageToBus(dispatchOptions.getRegisterAddress(), new JsonArray().add(registerMsg).add(registerMsg2));
    }



    public static void deployWebServer() {
        deployLatch = new CountDownLatch(1);

        JsonObject config = new JsonObject();
        config.put("http.port", port);
        config.put(WebServerVerticle.CONFIG_WEBSERVER_NAME_KEY, WEB_SERVER_NAME);

        dispatchOptions.setWebServerName(WEB_SERVER_NAME);

        config.put("auth.enabled", false);

        DeploymentOptions options = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle(WebServerVerticle.class.getName(), options, result -> {
            if (result.failed()) {
                logger.debug(result.cause().toString());
                Assert.fail();

            } else if (result.succeeded()) {
                deployLatch.countDown();
            }
        });
    }

    public static void initEventBus() {
        bus = vertx.eventBus();
    }

    /**
     *
     * @param appAddr 应用响应的地址
     */
    public static void initApp(String appAddr) {
        try {
            deployLatch.await();

            String addr = appAddr;
            //模拟具体应用
            bus.consumer(addr, message -> {
                String method = message.headers().get("method");


                if (method.equals("GET")) {
                    log(method);
                    log(message.body());
                    handleGetInApp(message);
                }

                if (method.equals("POST")) {
                    log(method);
                    log(message.body());
                    handlePostInApp(message);
                }

                if (method.equals("PUT")) {
                    log(method);
                    log(message.body());
                    handlePutInApp(message);
                }

                if (method.equals("DELETE")) {
                    log(method);
                    log(message.body());
                    handleDeleteInApp(message);
                }


            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    private synchronized static void log(Object log) {
        System.out.println(log);
    }

    private static void replySuccessArray(Message message){
        JsonArray array = new JsonArray();

        JsonObject o1 = new JsonObject().put("name", "name1");
        JsonObject o2 = new JsonObject().put("name", "name2");
        array.add(o1).add(o2);

        message.reply(array);
    }
    private static void replySuccessInfo(Message message) {
        //向客户端返回信息
        JsonObject reply = new JsonObject();
        reply.put("result", "successfully");

        message.reply(reply);
    }

    private static void handleGetInApp(Message<Object> message) {
        JsonObject body = new JsonObject(message.body().toString());

        JsonObject params = body.getJsonObject(BasicBridgeProtocal.QUERY_PARAMS);
        if(params == null || params.isEmpty()){
            //处理list情况
            replySuccessArray(message);
            return;
        }

        Assert.assertNotNull(params.getString("id"));
        Assert.assertNotNull(params.getString("token1"));

        log("get object with id " + params.getString("id"));
        replySuccessInfo(message);
    }

    private static void handlePostInApp(Message<Object> message) {
        //提取请求的form参数
        JsonObject formData = new JsonObject(message.body().toString());

        JsonObject params = formData.getJsonObject(BasicBridgeProtocal.QUERY_PARAMS);
        Assert.assertEquals("0", params.getString("orgLocCity"));
        Assert.assertEquals("ncscm5", params.getString("orgOmpName"));

        replySuccessInfo(message);
    }

    private static void handlePutInApp(Message<Object> message) {
        JsonObject body = new JsonObject(message.body().toString());
        JsonObject params = body.getJsonObject(BasicBridgeProtocal.QUERY_PARAMS);
        JsonObject content = body.getJsonObject(BasicBridgeProtocal.REQUEST_CONTENT);

        Assert.assertNotNull(params.getString("id"));
        Assert.assertNotNull(content.getString("name"));

        log("update object with id " + params.getString("id"));

        replySuccessInfo(message);
    }

    private static void handleDeleteInApp(Message<Object> message) {
        JsonObject body = new JsonObject(message.body().toString());

        JsonObject params = body.getJsonObject(BasicBridgeProtocal.QUERY_PARAMS);

        Assert.assertNotNull(params.getString("id"));

        log("delete object with id " + params.getString("id"));

        replySuccessInfo(message);
    }

    public void registerAPI( String addr, String url,  String method) {
        JsonObject registerMsg = new JsonObject();
        registerMsg.put("address", addr);
        registerMsg.put("uri", url);
        registerMsg.put("method", method);


        sendMessageToBus(dispatchOptions.getRegisterAddress(), registerMsg);
    }


    public void registerAPIWithDynamicAddress(String addr, String url,
        String method, String decoratingAddress) {

        JsonObject registerMsg = new JsonObject();
        registerMsg.put("address", addr);
        registerMsg.put("uri", url);
        registerMsg.put("method", method);
        registerMsg.put("decoratingAddress", decoratingAddress);

        //解析真实地址
        bus.<JsonObject>consumer(decoratingAddress, event -> {
            JsonObject body = event.body();
            String pattern = body.getString("addressPattern");
            event.reply(new JsonObject().put("realAddress", "my.application"));
        });

        sendMessageToBus(dispatchOptions.getRegisterAddress(), registerMsg);
    }


    private void sendMessageToBus(String address, JsonArray message){
        CountDownLatch latch = new CountDownLatch(1);

        //某个应用向事件总线注册API和消息地址
        bus.send(address, message, event -> {
            if(event.failed()){
                Assert.fail("消息响应失败: " + event.cause().getMessage());
            }

            Message<JsonArray> result = (Message) event.result();
            JsonArray reply = result.body();

            Assert.assertTrue(reply.size() > 0);

            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void sendMessageToBus(String address, JsonObject message) {

        CountDownLatch latch = new CountDownLatch(1);

        //某个应用向事件总线注册API和消息地址
        bus.send(address, message, event -> {
            if(event.failed()){
                Assert.fail("消息响应失败: " + event.cause().getMessage());
            }

            Message<JsonObject> result = (Message) event.result();
            JsonObject reply = result.body();
            //验证注册结果
            String registerId = reply.getString("result");
            Assert.assertNotNull(registerId, "success");

            //添加注册消息
            ThreeTuple<String, String, String> info = convertTuple(message);

            registerInfo.put(info, registerId);

            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ThreeTuple<String, String, String> convertTuple(JsonObject message) {
        ThreeTuple<String, String, String> info = new ThreeTuple<>();
        info.setOne(message.getString("address"));
        info.setTwo(message.getString("uri"));
        info.setThree(message.getString("method"));
        return info;
    }

    public void post(String url) {
        CountDownLatch latch = new CountDownLatch(1);

        //注册成功后，发送post请求
        String postData =
                "orgName=345gt&orgLocProvince=%E5%AE%89%E5%BE%BD%E7%9C%81" +
                        "&orgLocCity=0&industry=0&orgOmpName=ncscm5" +
                        "&orgOmpPwd=a123456A&orgContact=qww" +
                        "&mobile=18101286680&email=";

        HttpClientRequest post = client.post(url, response -> {
            response.bodyHandler(body -> {
                //验证来自应用的body数据。
                JsonObject someData = new JsonObject(body.toString());
                Assert.assertEquals("successfully", someData.getString("result"));

                latch.countDown();
            });
        });

        post.putHeader("Content-Type", "application/x-www-form-urlencoded"); //模拟表单
        post.end(postData);

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void get(String url) {
        CountDownLatch latch = new CountDownLatch(1);

        HttpClientRequest get = client.get(url, response -> {
            response.bodyHandler(body -> {
                //验证来自应用的body数据。
                try {
                    JsonObject someData = new JsonObject(body.toString());
                    Assert.assertEquals("successfully", someData.getString("result"));
                }catch (Exception e){
                    JsonArray someArray = new JsonArray(body.toString());
                    Assert.assertTrue(someArray.isEmpty() == false);
                }

                latch.countDown();
            });
        });

        get.end();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void put(String url) {
        CountDownLatch latch = new CountDownLatch(1);

        JsonObject anotherData = new JsonObject();
        anotherData.put("name", "anotherName");

        HttpClientRequest put = client.put(url, response -> {
            response.bodyHandler(body -> {
                //验证来自应用的body数据。
                JsonObject someData = new JsonObject(body.toString());
                Assert.assertEquals("successfully", someData.getString("result"));

                latch.countDown();
            });
        });

        put.end(anotherData.toString());

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void delete(String url) {
        CountDownLatch latch = new CountDownLatch(1);

        HttpClientRequest delete = client.delete(url, response -> {
            response.bodyHandler(body -> {
                //验证来自应用的body数据。
                JsonObject someData = new JsonObject(body.toString());
                Assert.assertEquals("successfully", someData.getString("result"));

                latch.countDown();
            });
        });

        delete.end();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void unRegisterAPI(String addr, String url, String method) {
        JsonObject registerMsg = new JsonObject();
        registerMsg.put("address", addr);
        registerMsg.put("uri", url);
        registerMsg.put("method", method);

        ThreeTuple<String, String, String> tuple = convertTuple(registerMsg);
        String regisId = registerInfo.get(tuple);

        JsonObject unRegisterMsg = new JsonObject();
        unRegisterMsg.put("registerId", regisId);

        sendMessageToBus(dispatchOptions.getUnRegisterAddress(), unRegisterMsg);
    }

    public void deleteWithError( String url) {

        CountDownLatch latch = new CountDownLatch(1);

        HttpClientRequest delete = client.delete(url, response -> {
            response.bodyHandler(body -> {
                //验证来自应用的body数据。
                Assert.assertTrue("Should get an error page", body.toString().contains("Resource not found"));

                latch.countDown();
            });
        });

        delete.end();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void get(String url, String address) {
        CountDownLatch latch = new CountDownLatch(1);

        HttpClientRequest get = client.get(url, response -> {
            response.bodyHandler(body -> {
                //验证来自应用的body数据。
                try {
                    JsonObject someData = new JsonObject(body.toString());
                    Assert.assertEquals("successfully", someData.getString("result"));
                }catch (Exception e){
                    JsonArray someArray = new JsonArray(body.toString());
                    Assert.assertTrue(someArray.isEmpty() == false);
                }

                latch.countDown();
            });
        });

        get.end();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
