package otocloud.webserver.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
//import io.vertx.ext.web.sstore.SessionStore;
import otocloud.webserver.util.AuthConfig;
//import otocloud.webserver.util.AuthSwitch;

/**
 * zhangyef@yonyou.com on 2015-11-05.
 */
public interface SessionHandler extends Handler<RoutingContext> {
    /**
     * Default time, in ms, that a session lasts for without being accessed before expiring.
     */
    //long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

/*    static SessionHandler create(SessionStore sessionStore, AuthSwitch authSwitch, AuthConfig authConfig){
        return new SessionHandlerImpl(sessionStore, DEFAULT_SESSION_TIMEOUT, authSwitch, authConfig);
    }*/
	
	static SessionHandler create(AuthConfig authConfig){
        return new SessionHandlerImpl(authConfig);
    }

}
