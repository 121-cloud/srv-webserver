package otocloud.webserver.util;

import io.vertx.core.Vertx;
import otocloud.webserver.dispatch.EventBusTraveller;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO 该类必须是线程安全的。当同时传来多个HTTP请求时，会有多个线程同时通过该类调用具体的分发器。
 * TODO 如果分发器不存在，需要生成它。此时便会出现线程安全问题。
 * Created by better/zhangye on 15/9/23.
 */
public class TravellerFactory {
    static ConcurrentHashMap<String, EventBusTraveller> travellers = new ConcurrentHashMap<>();

    /**
     * 创建一个新的分发器.
     *
     * @param vertx
     * @param name 派发器的类型
     * @return 新创建的EventBusTraveller.
     */
    public static EventBusTraveller create(Vertx vertx, String name) {
        return EventBusTraveller.traveller(vertx);
    }

    public static EventBusTraveller get(Vertx vertx, String name) {

        if (travellers.containsKey(name)) {
            return travellers.get(name);
        }

        if (name.equals(TravellerKind.Basic)) {

            EventBusTraveller traveller = EventBusTraveller.traveller(vertx);
            travellers.put(name, traveller);

            return traveller;
        }

        if (name.equals(TravellerKind.Dynamic)) {
            throw new NotImplementedException();
        }

        //默认返回基本类型
        return EventBusTraveller.traveller(vertx);
    }

    //TODO 需要对外提供文档/查询接口，说明派发策略的种类
    enum TravellerKind {
        Basic("static"),
        Dynamic("dynamic");

        private String name;

        TravellerKind(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
