package otocloud.webserver.protocal;

import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;
import otocloud.webserver.base.WebServerTestBase;

/**
 * Created by better on 15/9/18.
 */
public class BridgeProtocalTest extends WebServerTestBase {

    private BridgeProtocal protocal;

    @Override
    public void setUp(TestContext context) {
        super.setUp(context);
        protocal = new BasicBridgeProtocal();
    }

    @Test
    public void it_should_build_message_headers(TestContext context) {
        final Async async = context.async();

        router.route().handler(routingContext -> {
            MultiMap headers = protocal.buildMessageHeaders(routingContext);

            context.assertTrue(headers.contains("method"));

            routingContext.response().setStatusCode(200).end();
        });

        vertx.setTimer(200, tid -> client.get(port, host, "/", event -> async.complete()).end());

    }

    @Test
    public void it_should_build_a_array_request_body(TestContext context) {
        final Async async = context.async();

        router.route().handler(routingContext -> {
            JsonObject body = protocal.buildMessageBody(routingContext);
            context.assertTrue(body.containsKey(BasicBridgeProtocal.REQUEST_CONTENT));
            context.assertTrue(body.containsKey(BasicBridgeProtocal.QUERY_PARAMS));
            context.assertTrue(body.getValue(BasicBridgeProtocal.REQUEST_CONTENT) instanceof JsonArray);

            routingContext.response().end();
        });

        vertx.setTimer(200, tid -> {
            //发送请求，请求体为json对象
            JsonArray postBody = makeArrayData();

            client.post(port, host, "/api/app/fun?para1=0", event -> event.bodyHandler(body -> async.complete()))
                    .end(postBody.toString());
        });

    }

    private JsonArray makeArrayData() {
        JsonArray requestBody = new JsonArray();

        requestBody.add(makeData()).add(makeData().put("newKey", "newValue"));

        return requestBody;
    }

    @Test
    public void it_should_build_a_message_body(TestContext context) {

        final Async async = context.async();

        router.route().handler(routingContext -> {
            JsonObject body = protocal.buildMessageBody(routingContext);
            context.assertTrue(body.containsKey(BasicBridgeProtocal.REQUEST_CONTENT));
            context.assertTrue(body.containsKey(BasicBridgeProtocal.QUERY_PARAMS));

            routingContext.response().end();
        });

        vertx.setTimer(200, tid -> {
            //发送请求，请求体为json对象
            JsonObject postBody = makeData();

            client.post(port, host, "/api/app/fun?para1=0", event -> event.bodyHandler(body -> async.complete()))
                    .end(postBody.toString());
        });


    }

    @Test
    public void it_should_build_a_response_body(TestContext context) throws Exception {
        final Async async = context.async();

        String address = "address";
        EventBus bus = vertx.eventBus();
        bus.consumer(address, message -> {
            Object object = message.body();
            if (object instanceof JsonObject) {
                JsonObject replyObject = (JsonObject) object;
                replyObject.put("reply", "replyContent");

                message.reply(replyObject);
            }
        });

        bus.<JsonObject>send(address, makeData(), result -> {
            Message<JsonObject> content = result.result();

            JsonObject replyContent = protocal.buildResponseBody(content);

            context.assertTrue(replyContent.containsKey("reply"));

            async.complete();
        });
    }

    private JsonObject makeData() {
        JsonObject json = new JsonObject();
        json.put("para1", "value1");
        json.put("para2", true);
        json.put("int", 1);

        return json;
    }
}
