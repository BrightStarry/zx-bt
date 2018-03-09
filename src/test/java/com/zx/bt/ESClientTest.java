package com.zx.bt;

import lombok.SneakyThrows;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * author:ZhengXing
 * datetime:2018/3/9 0009 15:56
 * 测试es
 */
public class ESClientTest extends BtApplicationTests{

	@Autowired
	private TransportClient client;

	@Before
	public void init() {

	}

	@Test
	@SneakyThrows
	public void test() {
		XContentBuilder content = XContentFactory.jsonBuilder()
				.startObject()
				.field("info_hash", "xxx")
				.endObject();
		IndexResponse result = client.prepareIndex("bt", "metadata")
				.setSource(content)
				.get();
		System.out.println(result.getId());


	}
}
