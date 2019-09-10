package com.alibaba.csp.sentinel.dashboard.sicily.config;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.client.config.filter.impl.ConfigFilterChainManager;
import com.alibaba.nacos.client.config.filter.impl.ConfigRequest;
import com.alibaba.nacos.client.config.http.HttpAgent;
import com.alibaba.nacos.client.config.impl.HttpSimpleClient.HttpResult;
import com.alibaba.nacos.client.config.utils.ContentUtils;
import com.alibaba.nacos.client.config.utils.ParamUtils;
import com.alibaba.nacos.client.utils.LogUtils;
import com.alibaba.nacos.client.utils.StringUtils;

/**
 * Bullshit 阿里.
 * @author zht
 *
 */
public class SicilyNacosConfigService extends NacosConfigService implements SicilyConfigService {
	private static final Logger LOGGER = LogUtils.logger(SicilyNacosConfigService.class);
	private final long POST_TIMEOUT = 3000L;
	private String namespace;
    private String encode;
    private ConfigFilterChainManager configFilterChainManager;
    private HttpAgent agent;
    
	public SicilyNacosConfigService(Properties properties) throws NacosException {
		super(properties);
	}
	
	@Override
	public boolean publishConfig(String dataId, String group, String content, String type) throws NacosException {
		//由于父类没有公开相应的私用属性和方法,这里反射获得
		if(StringUtils.isBlank(namespace)) {
			namespace = (String) getSuperFieldValue("namespace");
		}
		
		if(StringUtils.isBlank(encode)) {
			encode = (String) getSuperFieldValue("encode");
		}
		
		if(configFilterChainManager == null) {
			configFilterChainManager = (ConfigFilterChainManager) getSuperFieldValue("configFilterChainManager");
		}
		
		if(agent == null) {
			agent = (HttpAgent) getSuperFieldValue("agent");
		}
		return publishConfigInner(namespace, dataId, group, null, null, null, content, type);
	}

	private Object getSuperFieldValue(String fieldName) {
		Object result = null;
		Class<?> clazz = this.getClass().getSuperclass();
		try {
			Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			result = field.get(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private boolean publishConfigInner(String tenant, String dataId, String group, String tag, String appName,
			String betaIps, String content, String type) throws NacosException {
		group = (null == group) ? Constants.DEFAULT_GROUP : group.trim();
		ParamUtils.checkParam(dataId, group, content);

		ConfigRequest cr = new ConfigRequest();
		cr.setDataId(dataId);
		cr.setTenant(tenant);
		cr.setGroup(group);
		cr.setContent(content);
		configFilterChainManager.doFilter(cr, null);
		content = cr.getContent();

		String url = Constants.CONFIG_CONTROLLER_PATH;
		List<String> params = new ArrayList<String>();
		params.add("dataId");
		params.add(dataId);
		params.add("group");
		params.add(group);
		params.add("content");
		params.add(content);
		params.add("type");
		params.add(type);
		if (StringUtils.isNotEmpty(tenant)) {
			params.add("tenant");
			params.add(tenant);
		}
		if (StringUtils.isNotEmpty(appName)) {
			params.add("appName");
			params.add(appName);
		}
		if (StringUtils.isNotEmpty(tag)) {
			params.add("tag");
			params.add(tag);
		}

		List<String> headers = new ArrayList<String>();
		if (StringUtils.isNotEmpty(betaIps)) {
			headers.add("betaIps");
			headers.add(betaIps);
		}

		HttpResult result = null;
		try {
			result = agent.httpPost(url, headers, params, encode, POST_TIMEOUT);
		} catch (IOException ioe) {
			LOGGER.warn("[{}] [publish-single] exception, dataId={}, group={}, msg={}", agent.getName(), dataId, group,
					ioe.toString());
			return false;
		}

		if (HttpURLConnection.HTTP_OK == result.code) {
			LOGGER.info("[{}] [publish-single] ok, dataId={}, group={}, tenant={}, config={}", agent.getName(), dataId,
					group, tenant, ContentUtils.truncateContent(content));
			return true;
		} else if (HttpURLConnection.HTTP_FORBIDDEN == result.code) {
			LOGGER.warn("[{}] [publish-single] error, dataId={}, group={}, tenant={}, code={}, msg={}", agent.getName(),
					dataId, group, tenant, result.code, result.content);
			throw new NacosException(result.code, result.content);
		} else {
			LOGGER.warn("[{}] [publish-single] error, dataId={}, group={}, tenant={}, code={}, msg={}", agent.getName(),
					dataId, group, tenant, result.code, result.content);
			return false;
		}

	}
}
