package com.kato.pro.common.entity;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
public class Locker<T> implements AutoCloseable {

    @NonNull private ILock<T> iLock;
    @NonNull private T lock;

    @Override
    public void close() throws Exception {
        iLock.unlock(lock);
    }

}
