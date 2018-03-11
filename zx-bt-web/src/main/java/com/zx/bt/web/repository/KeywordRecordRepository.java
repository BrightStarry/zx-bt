package com.zx.bt.web.repository;

import com.zx.bt.web.entity.KeywordReocrd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * author:ZhengXing
 * datetime:2018-03-12 1:08
 * 关键词搜索
 */
@Repository
public interface KeywordRecordRepository extends JpaRepository<KeywordReocrd, Long> {
}
