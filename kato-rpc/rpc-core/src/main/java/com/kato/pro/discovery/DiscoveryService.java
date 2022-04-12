package com.kato.pro.discovery;

import com.kato.pro.constant.ServiceInfo;

public interface DiscoveryService {

    ServiceInfo discover(String serviceName) throws Exception;

}
