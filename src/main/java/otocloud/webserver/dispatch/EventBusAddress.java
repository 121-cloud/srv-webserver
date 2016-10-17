package otocloud.webserver.dispatch;

import io.vertx.ext.web.RoutingContext;

/**
 * 事件总线地址类。
 *
 * 用于表达一个事件总线地址，它既可以表示一个具体地址（即，即一个字符串），也可以表示一个带有参数的动态地址。
 * Created by better on 15/9/16.
 */
public interface EventBusAddress {

    /**
     * 使用指定地址，更改当前地址。
     * @param address 新的地址
     * @return this
     */
    EventBusAddress at(String address);

    /**
     * 根据路由上下文，解析地址。
     * @param context
     * @return
     */
    EventBusAddress analyze(RoutingContext context);

    EventBusAddress disable();

    EventBusAddress enable();

    EventBusAddress remove();

    /**
     * 得到当前所在的地址。
     * @return 当前地址，事件总线可以使用的合法地址。
     */
    String getAddress();
}
