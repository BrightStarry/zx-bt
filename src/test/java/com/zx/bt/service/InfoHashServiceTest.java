package com.zx.bt.service;

import com.zx.bt.BtApplicationTests;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * author:ZhengXing
 * datetime:2018/3/8 0008 12:00
 */
public class InfoHashServiceTest extends BtApplicationTests {

	@Autowired
	private InfoHashService infoHashService;

	@Test
	public void saveInfoHash() throws Exception {
		for (int i = 0; i < 10; i++) {
			new Thread(()->{
				infoHashService.saveInfoHash("xxx","xx");
			}).start();
		}
	}

}