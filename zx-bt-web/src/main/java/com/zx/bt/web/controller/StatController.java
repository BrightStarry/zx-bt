package com.zx.bt.web.controller;

import com.zx.bt.common.enums.ErrorEnum;
import com.zx.bt.common.exception.BTException;
import com.zx.bt.web.service.StatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018/3/16 0016 14:49
 * 统计相关controller
 */
@Slf4j
@Controller
@RequestMapping("/stat")
public class StatController implements ControllerPlus{
	private final StatService statService;

	public StatController(StatService statService) {
		this.statService = statService;
	}

	/**
	 * 获取最新的topX个不重复ip对应的城市计数
	 */
	@PostMapping("/city/{size}")
	@ResponseBody
	public Map<String, Integer> getTopXCityByDistinct(@PathVariable int size) {
		if(size <= 0)
			throw new BTException(ErrorEnum.FORM_ERROR);
		return statService.getCityByTopXIp(size);
	}

}
