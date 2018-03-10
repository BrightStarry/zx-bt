package com.zx.bt.spider.repository;

import com.zx.bt.spider.entity.InfoHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * author:ZhengXing
 * datetime:2018-02-15 19:45
 */
@Repository
public interface InfoHashRepository extends JpaRepository<InfoHash, Long> {

    InfoHash findFirstByInfoHash(String infoHash);

    Integer countByInfoHash(String infoHash);

    @Transactional
    void deleteByInfoHash(String infoHash);

}
