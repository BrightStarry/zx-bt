package com.zx.bt.web.repository;

import com.zx.bt.web.WebApplicationTests;
import com.zx.bt.web.entity.KeywordRecord;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * author:ZhengXing
 * datetime:2018/3/16 0016 14:01
 */
public class KeywordRecordRepositoryTest extends WebApplicationTests {

	@Autowired
	private KeywordRecordRepository keywordRecordRepository;
	@Test
	public void findDistinctIpTopX() throws Exception {
		List<String> result = keywordRecordRepository.findDistinctIpTopX(5);
		result.forEach(System.out::println);
	}

}