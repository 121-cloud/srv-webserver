package otocloud.webserver.session;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.impl.SessionImpl;

/**
 * 将Session的增删改查代理到Auth服务器上。
 * 通过事件总线通信。
 * <p/>
 * Created by zhangye on 2015-10-20.
 */
public class MongoSessionStore implements SessionStore {

    @Override
    public Session createSession(long timeout) {
        return new SessionImpl(timeout);
    }

    @Override
    public void get(String id, Handler<AsyncResult<Session>> resultHandler) {

    }

    @Override
    public void delete(String id, Handler<AsyncResult<Boolean>> resultHandler) {

    }

    @Override
    public void put(Session session, Handler<AsyncResult<Boolean>> resultHandler) {

    }

    @Override
    public void clear(Handler<AsyncResult<Boolean>> resultHandler) {

    }

    @Override
    public void size(Handler<AsyncResult<Integer>> resultHandler) {

    }

    @Override
    public void close() {

    }

	@Override
	public long retryTimeout() {
		// TODO Auto-generated method stub
		return 10000;
	}
}
