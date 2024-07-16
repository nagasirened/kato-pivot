package com.kato.pro.base.entity;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Preconditions;
import com.kato.pro.common.entity.ILock;
import com.kato.pro.common.entity.Locker;
import com.kato.pro.common.exception.IdempotencyException;
import org.springframework.security.authentication.LockedException;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class SuperServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements ISuperService<T> {

    @Override
    public boolean saveIdempotency(T entity, ILock<?> iLock, String lockKey, Wrapper<T> countWrapper) throws Exception {
        return saveIdempotency(entity, iLock, lockKey, countWrapper, null);
    }

    @Override
    public boolean saveIdempotency(T entity, ILock<?> iLock, String lockKey, Wrapper<T> countWrapper, String msg) throws Exception {
        Preconditions.checkNotNull(entity, "entity is null");
        Preconditions.checkNotNull(iLock, "iLock is null");
        Preconditions.checkNotNull(lockKey, "saveIdempotency lockKey is null");
        try (Locker<?> locker = iLock.tryLock(lockKey, 10, 60, TimeUnit.SECONDS)) {
            if (locker != null) {
                if (countWrapper != null) {
                    long count = super.count(countWrapper);
                    if (count > 0) {
                        msg = CharSequenceUtil.isBlank(msg) ? "exist already" : msg;
                        throw new IdempotencyException(msg);
                    }
                }
                return super.save(entity);
            } else {
                throw new LockedException("lock timeoutException");
            }
        }
    }

    @Override
    public boolean saveOrUpdateIdempotency(T entity, ILock<?> iLock, String lockKey, Wrapper<T> countWrapper) throws Exception {
        return saveOrUpdateIdempotency(entity, iLock, lockKey, countWrapper, null);
    }

    @Override
    public boolean saveOrUpdateIdempotency(T entity, ILock<?> iLock, String lockKey, Wrapper<T> countWrapper, String msg) throws Exception {
        Preconditions.checkNotNull(entity, "entity is null");
        Class<?> clazz = entity.getClass();
        TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
        if (tableInfo != null && CharSequenceUtil.isNotBlank(tableInfo.getKeyProperty())) {
            Object idValue = ReflectionKit.getFieldValue(entity, tableInfo.getKeyProperty());
            if (StringUtils.checkValNull(idValue) || super.getById((Serializable) idValue) == null ) {
                msg = "exist already";
                return this.saveIdempotency(entity, iLock, lockKey, countWrapper, msg);
            } else {
                return super.updateById(entity);
            }
        } else {
            throw ExceptionUtils.mpe("Error: Could not find @TableId.");
        }
    }


}
