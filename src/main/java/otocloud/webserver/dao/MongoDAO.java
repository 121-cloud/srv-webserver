package otocloud.webserver.dao;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import otocloud.webserver.WebServerVerticle;
import otocloud.webserver.util.GlobalDataPool;

import java.util.List;
import java.util.Objects;

/**
 * zhangyef@yonyou.com on 2015-10-28.
 */
public class MongoDAO {
    private final String DB_NAME = "otocloud-webserver";

    private MongoClient client;

    private String RESTFUL_API_REG_INFO = "ApiRegInfo";

    private boolean isConfigured = false;

    private MongoDAO(Vertx vertx) {
        if (GlobalDataPool.INSTANCE.containsKey(WebServerVerticle.CONFIG_WEBSERVER_NAME_KEY)) {
            String webServerName = GlobalDataPool.INSTANCE.<String>get(WebServerVerticle.CONFIG_WEBSERVER_NAME_KEY);
            RESTFUL_API_REG_INFO = RESTFUL_API_REG_INFO + "_at_" + webServerName;
        }

        JsonObject mongo_client = GlobalDataPool.INSTANCE.<JsonObject>get("mongo_client_at_webserver");
        JsonObject config;

        if (mongo_client != null) {
            Objects.requireNonNull(mongo_client.getString("host"), "mongo_client配置中没有包含host字段.");
            Objects.requireNonNull(mongo_client.getInteger("port"), "mongo_client配置中没有包含port字段.");

            config = mongo_client;

            isConfigured = true;
        } else{
            isConfigured = false;
            client = null;
            return;
        }

        config.put("db_name", DB_NAME);

        try {
            client = MongoClient.createShared(vertx, config);
        }catch (Exception e){
            isConfigured = false;
        }
    }

    public static MongoDAO create(Vertx vertx) {
        return new MongoDAO(vertx);
    }

    public void insert(JsonObject data, Handler<AsyncResult<String>> resultHandler) {
        //如果没有配置mongoClient
        if (!isConfigured){
            if(resultHandler != null){
                resultHandler.handle(Future.failedFuture("没有配置MongoClient,无法插入数据."));
            }
            return;
        }
        client.insert(RESTFUL_API_REG_INFO, data, resultHandler);
    }


    public void findOne(JsonObject query, Handler<AsyncResult<JsonObject>> resultHandler) {
        if (!isConfigured){
            if(resultHandler != null){
                resultHandler.handle(Future.failedFuture("没有配置MongoClient,无法查询数据."));
            }
            return;
        }
        client.findOne(RESTFUL_API_REG_INFO, query, new JsonObject(), resultHandler);
    }
    
    public void findAll(Handler<AsyncResult<List<JsonObject>>> resultHandler) {
        if (!isConfigured){
            if(resultHandler != null){
                resultHandler.handle(Future.failedFuture("没有配置MongoClient,无法查询数据."));
            }
            return;
        }
        FindOptions findOptions = new FindOptions();        
        client.findWithOptions(RESTFUL_API_REG_INFO, new JsonObject(), findOptions, resultHandler);
    }

    /**
     * 移除符合条件的所有用户数据.
     *
     * @param condition
     * @param resultHandler
     */
    public void delete(JsonObject condition, Handler<AsyncResult<MongoClientDeleteResult>> resultHandler) {
        if (!isConfigured){
            if(resultHandler != null){
                resultHandler.handle(Future.failedFuture("没有配置MongoClient,无法删除数据."));
            }
            return;
        }
        Handler<AsyncResult<MongoClientDeleteResult>> removeHandler = (ret) -> resultHandler.handle(ret);
        client.removeDocument(RESTFUL_API_REG_INFO, condition, removeHandler);
    }

    public void deleteById(String id, Handler<AsyncResult<MongoClientDeleteResult>> resultHandler) {
        if (!isConfigured){
            if(resultHandler != null){
                resultHandler.handle(Future.failedFuture("没有配置MongoClient,无法根据\"_id\"删除数据."));
            }
            return;
        }
        delete(new JsonObject().put("_id", id), resultHandler);
    }
}
