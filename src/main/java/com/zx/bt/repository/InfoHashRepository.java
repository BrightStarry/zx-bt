package com.zx.bt.repository;

import com.zx.bt.entity.InfoHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-02-15 19:45
 */
@Repository
public interface InfoHashRepository extends JpaRepository<InfoHash, Long> {

    InfoHash findFirstByInfoHashAndType(String infoHash,int type);

    Integer countByInfoHashAndType(String infoHash, int type);

}
