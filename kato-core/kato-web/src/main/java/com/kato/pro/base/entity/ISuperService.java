package com.kato.pro.base.entity;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kato.pro.common.entity.ILock;

/**
 * 写一些公共函数
 */
public interface ISuperService<T> extends IService<T> {

    /**
     * 幂等性新增记录
     * @param entity    实体对象
     * @param iLock     锁实例
     * @param lockKey   锁key
     * @param countWrapper  判断对象是否存在
     * @param msg       对象已经存在得提示信息
     * @return          是否保存成功
     */
    boolean saveIdempotency(T entity, ILock<?> iLock, String lockKey, Wrapper<T> countWrapper, String msg) throws Exception;

    boolean saveIdempotency(T entity, ILock<?> iLock, String lockKey, Wrapper<T> countWrapper) throws Exception;

    /**
     * 幂等性新增或更新记录
     * 例子如下：
     * String username = sysUser.getUsername();
     * boolean result = super.saveOrUpdateIdempotency(sysUser, lock
     *                 , LOCK_KEY_USERNAME+username
     *                 , new QueryWrapper<SysUser>().eq("username", username));
     *
     * @param entity       实体对象
     * @param iLock        锁实例
     * @param lockKey      锁的key
     * @param countWrapper 判断是否存在的条件
     * @param msg          对象已存在提示信息
     */
    boolean saveOrUpdateIdempotency(T entity, ILock<?> iLock, String lockKey, Wrapper<T> countWrapper, String msg) throws Exception;

    boolean saveOrUpdateIdempotency(T entity, ILock<?> iLock, String lockKey, Wrapper<T> countWrapper) throws Exception;

}
