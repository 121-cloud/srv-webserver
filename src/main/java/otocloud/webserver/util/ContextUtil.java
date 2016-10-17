package otocloud.webserver.util;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

import java.util.Map;

/**
 * zhangyef@yonyou.com on 2015-11-18.
 */
public class ContextUtil {
    protected static Logger logger = LoggerFactory.getLogger(ContextUtil.class);

    /**
     * 向转发的消息包中添加Session字段，其中存储了会话的信息。
     * <p/>
     * 注意, 参数将被修改.
     */
    public static void addSessionToPack(RoutingContext context, Object pack) {
        logger.info("将向数据包中添加Session内容.");
        if (pack != null && pack instanceof JsonObject) {
            JsonObject modifiedPack = (JsonObject) pack;

            Session session = context.session();
            if (session == null) {
                logger.info("Session为空, 将被忽略.");
                return;
            }
            filterParams(context, session);

            JsonObject sessionInfo = makeSession(session);

            modifiedPack.put("session", sessionInfo);

            logger.info("在数据包中添加了Session.");
        } else {
            logger.warn("数据包不是JsonObject格式, 不会添加Session内容.");
        }
    }

    /**
     * 过滤应用不需要的参数.
     *
     * @param context
     * @param session
     */
    public static void filterParams(RoutingContext context, Session session) {
        if (session != null) {
            //session已经构造完成,说明已经通过authServer进行了验证.
            //移除查询参数
            HttpServerRequest request = context.request();
            request.params().remove("session");
            request.params().remove("token");
        }
    }

    public static JsonObject makeSession(Session session) {
        JsonObject sessionInfo = new JsonObject();
        //没有Session，不处理。
        if (session == null) {
            return sessionInfo;
        }
        sessionInfo.put("id", session.id());

        Map<String, Object> data = session.data();
        data.forEach((key, value) -> {
            sessionInfo.put(key, value);
        });

        return sessionInfo;
    }
}
