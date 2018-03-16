package com.zx.bt.spider.task.parser;

import com.zx.bt.spider.util.HtmlResolver;
import com.zx.bt.spider.util.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018/3/16 0016 17:44
 * 抽象的 infoHash 解析器
 * 将infoHash解析为 {@link com.zx.bt.common.entity.Metadata}
 */
@Slf4j
public abstract class AbstractInfoHashParser {

	protected final HttpClientUtil httpClientUtil;

	protected AbstractInfoHashParser(HttpClientUtil httpClientUtil) {
		this.httpClientUtil = httpClientUtil;
	}

	/**
	 * 网址
	 */
	protected String url;


	/**
	 * 设置url
	 */
	public AbstractInfoHashParser url(String url) {
		this.url = url;
		return this;
	}

	/**
	 * 获取到
	 */

	/**
	 * 根据网址发起请求,获取到{@link org.jsoup.nodes.Document}
	 */
	protected Document getDocumentByPath(String url) {
		return HtmlResolver.getDocument(httpClientUtil.doGetForBasicBrowser(url));
	}
}
