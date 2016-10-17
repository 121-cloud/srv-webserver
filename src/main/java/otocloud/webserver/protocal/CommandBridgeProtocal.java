package otocloud.webserver.protocal;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import otocloud.common.Command;

/**
 * 构造 {@link otocloud.common.Command} 实例, 用于实现对消息的规范.
 * zhangyef@yonyou.com on 2015-11-16.
 */
public class CommandBridgeProtocal extends BasicBridgeProtocal {
    @Override
    public JsonObject buildMessageBody(RoutingContext context) {
        HttpServerRequest request = context.request();
        //检查是否含有token, 如果有token, 那么说明可能登陆了.
        String token = request.params().get("token");
        if (StringUtils.isBlank(token)) {
            //没有登录

        } else {
            //可能登录了
        }

        int accountID = -1;
        //事件总线地址
        String address = "";

        Command command = new Command(accountID, address);
//        command.execute();
        return super.buildMessageBody(context);
    }
}
