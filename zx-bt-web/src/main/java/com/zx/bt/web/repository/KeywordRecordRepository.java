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
	 * 去重的limit查询
	 */
	@Query("select KeywordRecord from KeywordRecord group by ip  order by id desc ")
	List<KeywordRecord> findDistinctIpTopX(int size);
}
