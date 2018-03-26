package com.zx.bt.spider.parser;

import com.zx.bt.common.vo.MetadataVO;
import com.zx.bt.spider.enums.MetadataTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-03-18 15:27
 * http://www.btrabbit.net
 * see {@link MetadataTypeEnum#BTRABBIT} 5
 */
@Order(0)
@Component
@Slf4j
public class BtrabbitInfoHashParser extends AbstractInfoHashParser {

    public BtrabbitInfoHashParser() {
        super(MetadataTypeEnum.BTRABBIT,
                "http://www.btrabbit.net/wiki/",
                "#wall > h2",
                "#wall > div.fileDetail > div > table > tbody > tr:nth-child(2) > td:nth-child(5)",
                "#wall > div.fileDetail > div > div:nth-child(10) > div.panel-body > ol");
    }


    @Override
    protected List<MetadataVO.Info> getInfos(Element infosElement) {
        List<MetadataVO.Info> infos = super.getInfos(infosElement);
        for (Element element : infosElement.children()) {
            infos.add(new MetadataVO.Info(element.ownText(),lengthStr2ByteLength(element.child(0).text(),true)));
        }
        return infos;
    }
}
