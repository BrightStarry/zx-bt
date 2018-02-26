package com.zx.bt.controller;

import com.zx.bt.store.RoutingTable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * author:ZhengXing
 * datetime:2018/2/26 0026 17:25
 */
@RestController
@RequestMapping("/")
public class TestController {
	private final RoutingTable routingTable;

	public TestController(RoutingTable routingTable) {
		this.routingTable = routingTable;
	}

	@RequestMapping("/")
	public Long routingTableSize() {
		return routingTable.size();
	}
}
