package com.zx.bt.spider;

import com.zx.bt.spider.util.HttpClientUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpiderApplicationTests {

	@Test
	public void contextLoads() {
	}

	// 帮朋友刷票
//	public static void main(String[] args) throws Exception {
//		HttpClientUtil httpClientUtil = new HttpClientUtil(null);
//		// userId: 1331839
//		String url = "http://667128083.ax.nofollow.51wtp.com/index.php/toupiao/toupiao/ajax/getUserComments?user_id=%d&page=1";
//		int i = 1331839;
//		for (int j = 0; j < 20; j++) {
//			String s = httpClientUtil.doGet(String.format(url, i++));
//			System.out.println(s);
//			Thread.sleep(3000);
//			System.out.println("当前i: "+i);
//		}
//	}

}
