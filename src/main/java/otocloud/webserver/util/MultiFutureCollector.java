package otocloud.webserver.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * zhangyef@yonyou.com on 2015-12-02.
 */
public class MultiFutureCollector<T> {
    private AtomicInteger successCount;

    private AtomicInteger count;

    private Handler<AsyncResult<Void>> completeHandler;

    private MultiFutureCollector() {
        count = new AtomicInteger();
        successCount = new AtomicInteger();
    }

    public static <T> MultiFutureCollector<T> create() {
        return new MultiFutureCollector<>();
    }

    public Handler<AsyncResult<T>> collect(Handler<AsyncResult<T>> handler) {
        Handler<AsyncResult<T>> innerHandler = (AsyncResult<T> result) -> {
            handler.handle(result);
            count.decrementAndGet();
            if (result.succeeded()) {
                successCount.decrementAndGet();
            }

            if (count.get() == 0) {
                if (completeHandler != null) {
                    completeHandler.handle(Future.succeededFuture());
                }
            }
        };

        return innerHandler;
    }

    public boolean isAllSucceeded() {
        return successCount.get() == 0;
    }


    public void setCount(int count){
        this.count.set(count);
        this.successCount.set(count);
    }
    public void setHandler(Handler<AsyncResult<Void>> completeHandler) {
        this.completeHandler = completeHandler;
    }

}
