package com.zx.bt.web.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * author:ZhengXing
 * datetime:2018/3/12 0012 12:33
 * 排序相关视图
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderVO {

	/**
	 * 排序规则
	 */
	private Integer orderType;

	/**
	 * 是否必须包含关键字
	 */
	private Boolean isMustContain;
}
