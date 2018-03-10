package com.zx.bt.spider.service;

import com.zx.bt.spider.entity.InfoHash;
import com.zx.bt.spider.repository.InfoHashRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	/**
	 * 为空时新增, 不为空时追加peer
	 */
	@Transactional
	public void saveInfoHash(String infoHashHexStr,String peersStr) {
		InfoHash infoHash = infoHashRepository.findFirstByInfoHash(infoHashHexStr);
		if (infoHash == null) {
			//如果为空,则新建
			infoHash = new InfoHash(infoHashHexStr, peersStr);
		} else if(StringUtils.isEmpty(infoHash.getPeerAddress()) || infoHash.getPeerAddress().split(";").length <= 16){
			//如果不为空,并且长度小于一定值,则追加
			infoHash.setPeerAddress(infoHash.getPeerAddress()+ peersStr);
		}
		infoHashRepository.save(infoHash);
	}


}
