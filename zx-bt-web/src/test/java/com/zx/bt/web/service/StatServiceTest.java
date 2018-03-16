package com.zx.bt.web.service;

import com.zx.bt.web.WebApplicationTests;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * author:ZhengXing
 * datetime:2018/3/16 0016 14:43
 */
public class StatServiceTest extends WebApplicationTests {

	@Autowired
	private StatService statService;

	@Test
	public void getCityByTopXIp() throws Exception {
		Map<String, Integer> cityMap = statService.getCityByTopXIp(5);
		cityMap.forEach((k,v) -> System.out.println(k + " -- " + v));
	}

}