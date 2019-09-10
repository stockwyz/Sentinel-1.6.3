/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
@Configuration
public class NacosConfig {

    @Bean
    public Converter<List<FlowRuleEntity>, String> flowRuleEntityEncoder() {
        return JSON::toJSONString;
    }

    @Bean
    public Converter<String, List<FlowRuleEntity>> flowRuleEntityDecoder() {
        return s -> JSON.parseArray(s, FlowRuleEntity.class);
    }
    
    @SuppressWarnings("rawtypes")
	@Bean
    public ConfigService nacosConfigService(@Value("${spring.cloud.nacos.config.server-addr}") String nacosServerAddr) throws Exception {
    	//modified by zht.在com.alibaba.csp.sentinel.dashboard.rule.nacos.FlowRuleNacosPublisher中
    	//依赖NacosConfigService发布配置变更,这个类的publishConfig方法没有指定type:json.yml,properties等.而nacos
    	//服务端的接口com.alibaba.nacos.config.server.controller中publishConfig有type参数.bullshit阿里.现自
    	//定义类继承NacosConfigService,覆写publishConfig和publishConfigInner方法,只为了把type参数加上.以达到在
    	//sentinel-dashboard中修入限流规则后,在nacos中配置管理查询时,可以显示出配置的type,从而在编辑器中高亮显示.
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosServerAddr);
        try {
            //Class<?> driverImplClass = Class.forName("com.alibaba.nacos.client.config.NacosConfigService");
        	Class<?> driverImplClass = Class.forName("com.alibaba.csp.sentinel.dashboard.sicily.config.SicilyNacosConfigService");
            Constructor constructor = driverImplClass.getConstructor(Properties.class);
            ConfigService vendorImpl = (ConfigService) constructor.newInstance(properties);
            return vendorImpl;
        } catch (Throwable e) {
            throw new NacosException(-400, e.getMessage());
        }
//        return ConfigFactory.createConfigService(nacosServerAddr);
    }
    
//    @Bean
//    public ConfigService nacosConfigService(@Value("${spring.cloud.nacos.config.server-addr}") String nacosServerAddr, @Value("${spring.cloud.nacos.config.server-addr}") String nacosServerAddr) throws Exception {
//    	Properties properties = new Properties();
//        properties.put(PropertyKeyConst.SERVER_ADDR, nacosServerAddr);
//        return ConfigFactory.createConfigService(properties);
//    }
}
