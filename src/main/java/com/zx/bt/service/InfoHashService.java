package com.zx.bt.service;

import com.zx.bt.repository.InfoHashRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * author:ZhengXing
 * datetime:2018/3/6 0006 16:36
 * infoHash 服务类
 */
@Service
@Slf4j
public class InfoHashService {

	private final InfoHashRepository infoHashRepository;

	public InfoHashService(InfoHashRepository infoHashRepository) {
		this.infoHashRepository = infoHashRepository;
	}


}
