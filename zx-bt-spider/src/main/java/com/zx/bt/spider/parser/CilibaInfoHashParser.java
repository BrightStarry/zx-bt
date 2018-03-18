package com.zx.bt.spider.parser;

import com.zx.bt.common.vo.MetadataVO;
import com.zx.bt.spider.enums.MetadataTypeEnum;
import com.zx.bt.spider.util.HtmlResolver;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-03-18 15:27
 * https://www.ciliba.org
 */
@Component
@Slf4j
public class CilibaInfoHashParser extends AbstractInfoHashParser {

    public CilibaInfoHashParser() {
        super(MetadataTypeEnum.CILIBA,
                "https://www.ciliba.org/detail/",
                "#wall > h1",
                "#wall > div.fileDetail > p:nth-child(3)",
                "#wall > ol");
    }


    @Override
    protected long getLength(Element element) {
        return lengthStr2ByteLength(HtmlResolver.getElementText(element, lengthSelector).substring(5), true);
    }

    @Override
    protected List<MetadataVO.Info> getInfos(Element infosElement) {
        List<MetadataVO.Info> infos = super.getInfos(infosElement);
        for (Element element : infosElement.children()) {
            infos.add(new MetadataVO.Info(element.ownText(), lengthStr2ByteLength(element.child(0).text(), true)));
        }
        return infos;
    }
}
