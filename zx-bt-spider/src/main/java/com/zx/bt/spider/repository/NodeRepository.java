package com.zx.bt.spider.repository;

import com.zx.bt.spider.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-02-15 19:45
 */
@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {
	/**
	 * 查询 记录最多的node 前x个
	 */
	@Query(nativeQuery = true,value = "SELECT *,count(1) as count FROM node GROUP BY node_id ORDER BY count DESC LIMIT ?1")
	List<Node> findTopXNode(int size);

}
