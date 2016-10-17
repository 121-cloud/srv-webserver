package otocloud.webserver.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import otocloud.webserver.register.RegisterInfo;

import java.io.IOException;

/**
 * 转换不同的消息格式.
 * zhangyef@yonyou.com on 2015-12-02.
 */
public final class MessageUtil {
    protected static Logger logger = LoggerFactory.getLogger(MessageUtil.class);

    private static ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally

    public static RegisterInfo convertMessage(JsonObject message) {
        String msgStr = message.toString();
        try {
            RegisterInfo registerMessage = mapper.readValue(msgStr, RegisterInfo.class);

            checkMessage(registerMessage);

            return registerMessage;

        } catch (IOException e) {
            logger.error("API注册消息格式错误，收到的格式为 " + msgStr);
        }

        return null;
    }


    /**
     * 检查消息的格式是否正确，如果错误，则抛出异常。
     * 检查消息是否
     *
     * @param registerMessage REST API 注册消息.
     */
    private static void checkMessage(RegisterInfo registerMessage) {

    }
}
