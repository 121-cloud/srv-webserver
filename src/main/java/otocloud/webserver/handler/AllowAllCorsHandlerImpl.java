package otocloud.webserver.handler;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.vertx.core.http.HttpHeaders.ORIGIN;

/**
 * 允许所有的请求域通过。
 * zhangyef@yonyou.com on 2015-11-04.
 */
public class AllowAllCorsHandlerImpl implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String origin = request.headers().get(ORIGIN);

        if(origin != null) {
            response.putHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.putHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        }

        context.next();
    }
}
