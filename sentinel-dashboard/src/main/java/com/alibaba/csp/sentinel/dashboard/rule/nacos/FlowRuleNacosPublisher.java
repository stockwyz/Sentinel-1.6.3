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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.dashboard.rule.nacosTest.NacosConfigUtil;
import com.alibaba.csp.sentinel.dashboard.sicily.config.SicilyConfigService;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
@Component("flowRuleNacosPublisher")
public class FlowRuleNacosPublisher implements DynamicRulePublisher<List<FlowRuleEntity>> {

    @Autowired
    //private ConfigService configService; modified by zht
    private SicilyConfigService configService;
    
    @Autowired
    private Converter<List<FlowRuleEntity>, String> converter;

    @Override
    public void publish(String app, List<FlowRuleEntity> rules) throws Exception {
        AssertUtil.notEmpty(app, "app name cannot be empty");
        if (rules == null) {
            return;
        }
        
        //add by zht.去掉FlowRuleEntity中."app"."gmtCreate"."gmtModified"."id","ip","port"
        //否则服务启动时从nacos加载限流规则报错.bullshit阿里.
        for(FlowRuleEntity rule : rules) {
        	rule.setApp(null);
        	rule.setGmtCreate(null);
        	rule.setGmtModified(null);
        	rule.setIp(null);
        	rule.setPort(null);
        	rule.setId(null);
        }
        //add by zht.格式化后同步再到nacos服务器持久层.在nacos配置管理中打开,不会一坨.bullshit阿里.
        String jsonFlowRule = "";
        if(!CollectionUtils.isEmpty(rules)) {
        	jsonFlowRule = JSON.toJSONString(rules, SerializerFeature.PrettyFormat);
        }
        configService.publishConfig(app + NacosConfigUtil.FLOW_DATA_ID_POSTFIX,
            NacosConfigUtil.GROUP_ID, jsonFlowRule, "json");
//        configService.publishConfig(app + NacosConfigUtil.FLOW_DATA_ID_POSTFIX,
//                NacosConfigUtil.GROUP_ID, converter.convert(rules));
    }
}
