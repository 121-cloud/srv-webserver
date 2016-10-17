package otocloud.webserver.util;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by better on 15/9/18.
 */
public class JsonFactory {
    protected static final Logger logger = LoggerFactory.getLogger(JsonFactory.class);

    public static JsonObject fromMultiMap(MultiMap map) {
        JsonObject json = new JsonObject();

        Iterator<Map.Entry<String, String>> itr = map.iterator();
        Map.Entry<String, String> entry;

        while (itr.hasNext()) {
            entry = itr.next();
            json.put(entry.getKey(), entry.getValue());
        }

        return json;
    }

    public static Object makeBodyContent(String bodyStr) {
        if(StringUtils.isBlank(bodyStr)){
            logger.warn("消息体内容为空, 将构建空的JSON.");
            return new JsonObject();
        }

        Object body;
        try {
            body = new JsonObject(bodyStr);
        } catch (Exception e) {
            try {
                body = new JsonArray(bodyStr);
            } catch (Exception innerE) {
                body = new JsonObject();
                logger.debug(innerE);
            }
        }
        return body;
    }


}
