package otocloud.webserver.register;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import otocloud.webserver.dao.MongoDAO;
import otocloud.webserver.dispatch.Dispatcher;
import otocloud.webserver.exception.DuplicatedApiException;
import otocloud.webserver.util.MessageUtil;
import otocloud.webserver.util.MultiFutureCollector;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by better on 15/9/16.
 */
public class RouteTable {
    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private Vertx vertx;
    private Router router;

    private ConcurrentHashMap<RegisterInfo, Route> registerInfos;
    private ConcurrentHashMap<String, RegisterInfo> registerIndex;

    private MongoDAO mongoDAO;

    public RouteTable(Vertx vertx, Router router) {
        this.vertx = vertx;
        this.router = router;
        this.registerInfos = new ConcurrentHashMap<>();
        this.registerIndex = new ConcurrentHashMap<>();

        mongoDAO = MongoDAO.create(vertx);
    }


    /**
     * 将注册信息保存在内容中，并进行持久化。
     *
     * @param info
     */
    private void storeInfo(String regId, RegisterInfo info, Route route) {
        registerInfos.put(info, route);
        registerIndex.put(regId, info);
    }

    public void hasBeenRegistered(RegisterInfo info) throws DuplicatedApiException {
        if (registerInfos.containsKey(info)) {
            throw new DuplicatedApiException().add(info.getMethod(), info.getUri());
        }
    }
    
    public void loadFromStore(Future<Void> next) {

        mongoDAO.findAll(ret -> {            
            if (ret.succeeded()) {
                List<JsonObject> apiRegInfos = ret.result();
                if(apiRegInfos != null && apiRegInfos.size() > 0){
                	for(JsonObject apiRegInfo: apiRegInfos){
                		try{
	                        RegisterInfo registerMessage = MessageUtil.convertMessage(apiRegInfo);
	                        if (!registerInfos.containsKey(registerMessage)) {
		                        String url = registerMessage.getUri();
		                        //如果没有前导斜线,则自动添加.
		                        url = url.startsWith("/") ? url : "/" + url;
		
		                        //核心注册代码
		                        String method = registerMessage.getMethod();
		                        Route route = addRoute(method, url);
		                        route.handler(createDispatcher(apiRegInfo));
		                        
		                        String registerId = apiRegInfo.getString("_id");
		                        storeInfo(registerId, registerMessage, route);
		                        
		                        System.out.println("API注册信息: " + apiRegInfo.toString());
	
	                        }
                		}catch(Exception ex){
                        	ex.printStackTrace();
                            logger.error(ex.getMessage()); 
                		}
                	}
                }                                
            } else {
            	Throwable err = ret.cause();
            	err.printStackTrace();
                logger.error("无法读取数据库API注册信息." + err.getMessage()); 
            }
            next.complete();
        });
    }

    /**
     * @param message 注册消息.
     * @return 注册ID，当注销时，传递注册ID完成注销。
     * @throws IllegalArgumentException
     */
    public void register(JsonObject message, Future<String> regInfo) {
        RegisterInfo registerMessage = MessageUtil.convertMessage(message);
        try {
            hasBeenRegistered(registerMessage);
        } catch (DuplicatedApiException dupEx) {
            logger.error(String.format("Rest API 进行了重复注册，[%s, %s]",
                    registerMessage.getMethod(), registerMessage.getUri()));

            regInfo.fail(dupEx);
            return;
        }

        String url = registerMessage.getUri();
        //如果没有前导斜线,则自动添加.
        url = url.startsWith("/") ? url : "/" + url;

        //核心注册代码
        String method = registerMessage.getMethod();
        Route route = addRoute(method, url);
        route.handler(createDispatcher(message));

        mongoDAO.insert(message.copy(), ret -> {
            String registerId;
            if (ret.succeeded()) {
                registerId = ret.result();
            } else {
                logger.warn("无法将API注册信息存储到数据库中, 注册信息将保留在内存.");
                registerId = UUID.randomUUID().toString();
            }

            storeInfo(registerId, registerMessage, route);

            regInfo.complete(registerId);
        });
    }

    /**
     * 将一个REST API注销。
     * <p/>
     * 移除注册信息，移除API的路由。
     *
     * @param message
     */
    public void unRegister(JsonObject message, Future<Void> unRegFuture) {
        String registerId = message.getString("registerId");

        if (registerIndex.containsKey(registerId)) {
            synchronized (this) {
                RegisterInfo info = registerIndex.remove(registerId);
                if (registerInfos.containsKey(info)) {
                    Route route = registerInfos.remove(info); //从注册信息中移除。
                    try {
                        route.remove();
                    } catch (Exception e) {
                        logger.warn("WebServer 移除路由时发生了异常: " + e.getMessage());
                    }
                }
            }
        }

        mongoDAO.deleteById(registerId, ret -> {
            if (ret.succeeded()) {
                logger.info("注销成功, 注册信息已经从数据库中删除.");
                unRegFuture.complete();
            } else {
                unRegFuture.fail(ret.cause());
            }
        });
    }

    private Handler<RoutingContext> createDispatcher(JsonObject message) {
        return Dispatcher.create(vertx, message);
    }

    private Route addRoute(String method, String path) {
        if (method == null || method.isEmpty()) {
            return router.route(path);
        }

        String methodName = method.toUpperCase();
        HttpMethod httpMethod = HttpMethod.valueOf(methodName);

        return router.route(httpMethod, path);
    }

    public void registerMany(JsonArray message, Future<List<String>> regFuture) {
        List<String> registerIds = new LinkedList<>();

        DuplicatedApiException duplicatedExcep = new DuplicatedApiException();

        MultiFutureCollector<String> collector = MultiFutureCollector.create();

        collector.setCount(message.size());

        collector.setHandler(ret -> {
            if (ret.succeeded()) {
                regFuture.complete(registerIds);
                return;
            }

            //出现了重复注册
            try {
                if (!duplicatedExcep.duplicatedItems().isEmpty()) {
                    //补充正确注册信息
                    for (String id : registerIds) {
                        RegisterInfo info = registerIndex.get(id);
                        duplicatedExcep.addCorrect(id, info.getMethod(), info.getUri());
                    }

                    throw duplicatedExcep;
                }
            } catch (DuplicatedApiException dup) {
                regFuture.fail(dup);
            }
        });

        message.forEach((item) -> {
            JsonObject jsonObject = (JsonObject) item;

            Future<String> regOneFuture = Future.future();

            register(jsonObject, regOneFuture);

            regOneFuture.setHandler(collector.collect(ret -> {
                String regId = null;

                if (ret.failed()) {
                    try {
                        throw ret.cause();
                    } catch (DuplicatedApiException ex) {
                        regId = null;
                        duplicatedExcep.add(ex.getMethod(), ex.getPath());
                    } catch (Throwable e) {
                        regId = null;
                        logger.error(e.getMessage());
                    }
                    return;
                }

                regId = ret.result();

                if (regId != null) {
                    registerIds.add(regId);
                }
            }));


        });


    }


    public void unRegisterMany(JsonArray message, Future<Void> unRegFuture) {
        int size = message.size();
        MultiFutureCollector<Void> collector = MultiFutureCollector.create();

        collector.setCount(message.size());

        collector.setHandler(ret -> {
            if (ret.succeeded()) {
                if (ret.succeeded()) {
                    logger.info("注销成功.");
                }
                unRegFuture.complete();
            } else {
                unRegFuture.fail(ret.cause());
            }
        });

        for (int i = 0; i < size; i++) {
            JsonObject unregister = message.getJsonObject(i);
            Future<Void> unRegOneFuture = Future.future();
            unRegister(unregister, unRegOneFuture);
            unRegOneFuture.setHandler(collector.collect(ret -> {
            }));
        }


    }
}
