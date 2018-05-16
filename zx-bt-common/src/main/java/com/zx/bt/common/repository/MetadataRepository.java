package com.zx.bt.common.repository;

import com.zx.bt.common.entity.Metadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Repository;

/**
 * author:ZhengXing
 * datetime:2018-05-13 14:29
 * dao，用于mysql数据库
 * extends JpaRepository<Metadata,Long>
 */
@Deprecated
public interface MetadataRepository {
}
