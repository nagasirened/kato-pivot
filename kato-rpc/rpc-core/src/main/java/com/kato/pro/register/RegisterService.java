package com.kato.pro.register;

import com.kato.pro.constant.ServiceInfo;

public interface RegisterService {

    /**
     * 服务注册
     */
    void register(ServiceInfo serviceInfo);

    /**
     *
     */
    void unregister(ServiceInfo serviceInfo);

}
