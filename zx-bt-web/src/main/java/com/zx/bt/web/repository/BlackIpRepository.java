package com.zx.bt.web.repository;

import com.zx.bt.web.entity.BlackIp;
import com.zx.bt.web.entity.KeywordRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * author:ZhengXing
 * datetime:2018-03-12 1:08
 * 黑名单ip
 */
@Repository
public interface BlackIpRepository extends JpaRepository<BlackIp, Long> {
}
