package com.zx.bt.web.service;

import com.zx.bt.common.exception.BTException;
import com.zx.bt.web.repository.KeywordRecordRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018/3/16 0016 09:21
 * 统计相关服务类
 */
@Service
public class StatService {
	private final KeywordRecordRepository keywordRecordRepository;

	public StatService(KeywordRecordRepository keywordRecordRepository) {
		this.keywordRecordRepository = keywordRecordRepository;
	}

	/**
	 * 获取到最新的x条ip不重复记录的城市信息
	 */
	public Map<String,Integer> getCityByTopXIp(int size) {
		if (size <= 0) {
			throw new BTException("[统计相关]获取最新城市记录,size<=0,当前size:" + size);
		}
		List<String> cityList = keywordRecordRepository.findDistinctIpTopX(size);

		/**
		 * 相当于单词统计
		 * 此处使用jdk8中Map新增的merge方法.
		 * 当key不存在,新增,存在,则根据旧值和新值计算结果 更新
		 */
		int increment = 1;
		HashMap<String, Integer> cityMap = new HashMap<>();
		cityList.forEach(city->cityMap.merge(city,increment,(oldV, newV)->oldV + newV));
		return cityMap;
	}




}
