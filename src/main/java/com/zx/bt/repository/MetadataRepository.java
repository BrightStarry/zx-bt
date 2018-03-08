package com.zx.bt.repository;

import com.zx.bt.entity.Metadata;
import com.zx.bt.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-02-15 19:45
 */
@Repository
public interface MetadataRepository extends JpaRepository<Metadata, Long> {

	/**
	 * 查询某infoHash是否存在
	 */
	int countByInfoHash(String infoHash);

	/**
	 * 最近x分钟内入库数量
	 */
	int countByCreateTimeGreaterThanEqual(Date date);

	/**
	 * 分页查询infoHash字段
	 */
	@Query(nativeQuery = true,value = "SELECT info_hash FROM metadata ORDER BY id LIMIT ?1,?2")
	List<String> findInfoHash(long start, int size);

}
