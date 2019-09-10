package com.alibaba.csp.sentinel.dashboard.sicily.config;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;

/**
 * bullshit阿里
 * @author zht
 *
 */
public interface SicilyConfigService extends ConfigService {
    /**
     * Publish config.
     *
     * @param dataId  dataId
     * @param group   group
     * @param content content
     * @param type json,yml,properties
     * @return Whether publish
     * @throws NacosException NacosException
     */
    boolean publishConfig(String dataId, String group, String content, String type) throws NacosException;
}
