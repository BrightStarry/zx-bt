package com.zx.bt.web.service;

import com.zx.bt.web.entity.KeywordRecord;
import com.zx.bt.web.repository.KeywordRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * author:ZhengXing
 * datetime:2018/3/12 0012 09:56
 * 主要服务类
 */
@Service
@Slf4j
public class MainService {
	private final KeywordRecordRepository keywordRecordRepository;

	public MainService(KeywordRecordRepository keywordRecordRepository) {
		this.keywordRecordRepository = keywordRecordRepository;
	}

	/**
	 * 新增查询记录
	 */
	@Transactional
	public void insertKeywordRecord(String ip,String keyword) {
		keywordRecordRepository.save(new KeywordRecord(keyword,ip ));
	}
}
