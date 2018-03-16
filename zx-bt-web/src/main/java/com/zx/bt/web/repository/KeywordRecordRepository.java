package com.zx.bt.web.repository;

import com.zx.bt.web.entity.KeywordRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-03-12 1:08
 * 关键词搜索
 */
@Repository
public interface KeywordRecordRepository extends JpaRepository<KeywordRecord, Long> {

	/**
	 * 查询最新的x条ip不重复的记录的城市字段
	 * 此处使用group by (ip) 去重
	 */
	@Query(value = "SELECT city FROM keyword_record GROUP BY ip  ORDER BY id DESC LIMIT 0,?1",nativeQuery = true)
	List<String> findDistinctIpTopX(int size);
}
