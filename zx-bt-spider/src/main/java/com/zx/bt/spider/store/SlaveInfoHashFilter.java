package com.zx.bt.spider.store;

import com.zx.bt.spider.config.Config;
import com.zx.bt.spider.util.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018/3/26 0026 09:18
 * 从节点 {@link InfoHashFilter}
 */
@Slf4j
@ConditionalOnProperty(prefix = "zx-bt.main", name = "master", havingValue = "false")
@Component
public class SlaveInfoHashFilter implements InfoHashFilter {


	//true
	private static final String TRUE = "true";

	private final HttpClientUtil slaveHttpClientUtil;
	private final String masterUrl;
	private final String putUrl;


	public SlaveInfoHashFilter(HttpClientUtil slaveHttpClientUtil, Config config) {
		this.slaveHttpClientUtil = slaveHttpClientUtil;
		this.masterUrl = config.getMain().getMasterUrl();
		this.putUrl = this.masterUrl + PUT_METHOD;
		if (StringUtils.isBlank(masterUrl))
			throw new Error("从节点没有配置masterUrl属性");
	}

	@Override
	public boolean put(String infoHash) {
		return isTrue(slaveHttpClientUtil.doGet(putUrl.concat(infoHash)));
	}

	@Override
	public boolean contain(String infoHash) {
		return false;
	}

	@Override
	public long size() {
		return -1;
	}

	@Override
	public void run() {

	}

	/**
	 * 是否成功
	 */
	private boolean isTrue(String result) {
		return TRUE.equalsIgnoreCase(result);
	}
}
