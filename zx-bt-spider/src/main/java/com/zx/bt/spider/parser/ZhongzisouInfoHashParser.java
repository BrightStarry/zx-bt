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
 * https://www.zhongzidi.com
 */
@Component
@Slf4j
public class ZhongzisouInfoHashParser extends AbstractInfoHashParser {

    public ZhongzisouInfoHashParser() {
        super(MetadataTypeEnum.ZHONGZISOU,
                "https://www.zhongzidi.com/info-",
                "div.panel.panel-primary > div.panel-heading > h3 > div",
                "div:nth-child(4) > div.col-md-9 > div:nth-child(1) > div.panel-body > dl > dd:nth-child(6)",
                "div:nth-child(4) > div.col-md-9 > div:nth-child(2) > div.panel-body > select");
    }

    @Override
    protected String getUrlByInfoHash(String infoHash) {
        return url.concat(infoHash);
    }

    @Override
    protected Element getDocumentByPath(String url) {
        return HtmlResolver.getElement(super.getDocumentByPath(url), "#wrapp > div.jumbotron > div > div");
    }

    @Override
    protected List<MetadataVO.Info> getInfos(Element infosElement) {
        List<MetadataVO.Info> infos = super.getInfos(infosElement);
        //循环每个<option>
        for (Element element : infosElement.children()) {
            String[] infoStrArr = element.html().split("&nbsp;&nbsp;&nbsp;&nbsp;");
            infos.add(new MetadataVO.Info(infoStrArr[0], lengthStr2ByteLength(infoStrArr[1], false)));
        }
        return infos;
    }
}
