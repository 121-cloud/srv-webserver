package otocloud.webserver.exception;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Created by zhangye on 2015-10-13.
 */
public class DuplicatedApiException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String KEY_REGISTER_ID = "registerId";

    private JsonArray duplicatedItems;
    private JsonArray correctItems;
    private String method;
    private String path;

    public DuplicatedApiException() {
        duplicatedItems = new JsonArray();
        correctItems = new JsonArray();
    }

    public DuplicatedApiException add(String method, String path) {
        this.method = method;
        this.path = path;

        duplicatedItems.add(new JsonObject().put("method", method).put("path", path));

        return this;
    }

    public DuplicatedApiException addCorrect(String registerId, String method, String path) {
        correctItems.add(new JsonObject()
                .put(KEY_REGISTER_ID, registerId)
                .put("method", method)
                .put("path", path));
        return this;
    }

    public JsonArray correctItems(){
        return this.correctItems;
    }

    public JsonArray duplicatedItems() {
        return duplicatedItems;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
