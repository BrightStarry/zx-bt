package com.zx.bt.web;

import com.zx.bt.common.entity.Metadata;
import com.zx.bt.common.enums.OrderTypeEnum;
import com.zx.bt.common.service.MetadataService;
import com.zx.bt.common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class WebApplicationTests {

	@Autowired
	private MetadataService metadataService;

	@Test

	public void contextLoads() {
//		PageVO<Metadata> pageVO = metadataService.listByKeyword("黑絲", OrderTypeEnum.NONE, false, 0, 500);
//		log.info("总页数:{}",pageVO.getTotalPage());
//		log.info("总记录数:{}",pageVO.getTotalElement());
//		log.info("当前页:{}",pageVO.getPageNo());
//		log.info("每页条数:{}",pageVO.getPageSize());
//		log.info("当前页条数:{}",pageVO.getList().size());
//		for (Metadata metadata : pageVO.getList()) {
//			log.info("数据:{}",metadata);
//		}


	}

}
