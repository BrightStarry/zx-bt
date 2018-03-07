package com.zx.bt.service;

import com.zx.bt.entity.Metadata;
import com.zx.bt.repository.MetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * author:ZhengXing
 * datetime:2018/3/7 0007 12:05
 */
@Service
@Slf4j
public class MetadataService {

	private final MetadataRepository metadataRepository;

	public MetadataService(MetadataRepository metadataRepository) {
		this.metadataRepository = metadataRepository;
	}

	/**
	 * 如果不存在则入库
	 */
	@Transactional
	public void saveMetadata(Metadata metadata) {
		if(metadataRepository.countByInfoHash(metadata.getInfoHash()) > 0){
			return;
		}
		metadataRepository.save(metadata);
	}

	/**
	 * x分钟内入库数量统计
	 */
	public int countByMinute(int minute) {
		return metadataRepository.countByCreateTimeGreaterThanEqual(DateUtils.addMinutes(new Date(), -minute));
	}

}
