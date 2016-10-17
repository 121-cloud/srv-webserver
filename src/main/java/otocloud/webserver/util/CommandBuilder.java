package otocloud.webserver.util;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import org.apache.commons.lang3.StringUtils;
import otocloud.common.Command;
import otocloud.common.CommandDeliveryOptions;
import otocloud.webserver.protocal.BridgeProtocal;
import otocloud.webserver.register.RegisterInfo;

/**
 * zhangyef@yonyou.com on 2015-11-18.
 */
public class CommandBuilder {
    protected static final Logger logger = LoggerFactory.getLogger(CommandBuilder.class);

    public static CommandDeliveryOptions createDeliveryOptions(RoutingContext context) {
        CommandDeliveryOptions options = new CommandDeliveryOptions();

        BridgeProtocal protocal = ProtocalFactory.get("basic"); //TODO 根据参数读取桥接协议

        MultiMap headers = protocal.buildMessageHeaders(context);
        options.setHeaders(headers);

        return options;
    }

    public static Command create(RegisterInfo registerMessage, RoutingContext context) {
        logger.debug("构建Command");
        Session session = context.session();
        int accountID = -1;
        if (session != null) {
            accountID = getAcctID(session);
            //清理查询参数中的session
            ContextUtil.filterParams(context, session);
        }

        String addr = registerMessage.getAddress();

        Command command = new Command(accountID, addr);

        String url = registerMessage.getUri();
        String method = registerMessage.getMethod();
        JsonObject info = new JsonObject().put("uri", url).put("method", method);

        command.setRestAPIDef(info);

        String action = registerMessage.getAction();

        if (StringUtils.isNotBlank(action)) {
            command.put("action", action);
        }

        HttpServerRequest request = context.request();

        String uri = request.uri();
        String path = request.path();
        String absUri = request.absoluteURI();

        command.setRequestURI(absUri);
        command.put("uri", uri);
        command.put("path", path);

        MultiMap params = request.params();
        command.setParams(JsonFactory.fromMultiMap(params));

        String body = context.getBodyAsString();
        Object content = JsonFactory.makeBodyContent(body);

        logger.info("开始构建Command内容: " + content);

        if (content instanceof JsonObject) {
            command.addContent((JsonObject) content);
            logger.info("构建后的Command内容: " + command.getContent().toString());
        } else {
            command.addContent((JsonArray) content);
            logger.info("构建后的Command内容: " + command.getContents().toString());
        }

        //添加Session
        if (session != null) {
            logger.info("构建Command中的Session.");
            command.setSessionID(session.id());
            command.setSessions(ContextUtil.makeSession(session));
        }

        return command;
    }


    private static int getAcctID(Session session) {
        try {
            int acctID = session.get("acctId");
            return acctID;
        } catch (Exception e) {
            return -1;
        }
    }
}
