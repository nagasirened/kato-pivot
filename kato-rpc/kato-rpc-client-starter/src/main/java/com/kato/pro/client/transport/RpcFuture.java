package com.kato.pro.client.transport;

import java.util.concurrent.*;

public class RpcFuture<T> implements Future<T> {

    private T response;
    private CountDownLatch counter = new CountDownLatch(1);


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return this.response != null;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        counter.await();
        return response;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (counter.await(timeout, unit)) {
            return response;
        }
        return null;
    }

    public void setResponse(T response) {
        this.response = response;
        counter.countDown();
    }
}
