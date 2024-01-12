package com.kato.pro.register;

import com.kato.pro.constant.ServiceInfo;

import java.io.IOException;
import java.util.List;

public interface RegisterService {

    /**
     * 服务注册
     */
    void register(ServiceInfo serviceInfo);

    /**
     * 服务解绑
     */
    void unregister(ServiceInfo serviceInfo);

    /**
     * 销毁
     */
    void destroy(List<String> serviceVersionList) throws IOException;
}
