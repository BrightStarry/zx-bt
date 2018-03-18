package com.zx.bt.spider.parser;

import com.zx.bt.common.vo.MetadataVO;
import com.zx.bt.spider.enums.MetadataTypeEnum;
import com.zx.bt.spider.util.HtmlResolver;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-03-18 15:27
 * http://www.btcerise.me
 */
@Component
@Slf4j
public class BtceriseInfoHashParser extends AbstractInfoHashParser {

    public BtceriseInfoHashParser() {
        super(MetadataTypeEnum.BTCERISE,
                "http://www.btcerise.me/info/",
                "#content > div > h1",
                "#content > div > ul > li:nth-child(3) > span",
                "#filelist");
    }

    @Override
    protected String getUrlByInfoHash(String infoHash) {
        return url.concat(infoHash.toUpperCase());
    }

    @Override
    protected long getLength(Element element) {
        return lengthStr2ByteLength(HtmlResolver.getElementText(element, lengthSelector).substring(3), false);
    }

    @Override
    protected List<MetadataVO.Info> getInfos(Element infosElement) {
        List<MetadataVO.Info> infos = super.getInfos(infosElement);
        Elements infosDivChildrens = infosElement.children();
        //此处不获取最后一个文件,因为它是该网站的广告(防止其他爬虫的(目测是防止那种直接操作浏览器内核的爬虫的))
        for (int i = 0; i < infosDivChildrens.size() -1; i++) {
            Element element = infosDivChildrens.get(i);
            infos.add(new MetadataVO.Info(element.childNode(1).childNode(0).outerHtml(),  lengthStr2ByteLength(element.childNode(2).childNode(0).outerHtml(),false)));
        }
        return infos;
    }
}
