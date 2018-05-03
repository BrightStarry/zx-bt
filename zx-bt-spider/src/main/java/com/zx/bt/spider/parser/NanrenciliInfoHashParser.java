package com.zx.bt.spider.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.common.entity.Metadata;
import com.zx.bt.common.vo.MetadataVO;
import com.zx.bt.spider.enums.MetadataTypeEnum;
import com.zx.bt.spider.util.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-04-23 15:31
 * http://nanrencili.net
 * see {@link MetadataTypeEnum#NANRENCILI} 6
 *
 * 该网站的详情页是用id的,无法直接通过hash码跳转,
 * 需要发送Post请求(该网站开发者未做限制,可以直接用Get) /list/{hash}.html, 参数为kw={infoHash}, 然后再经过一个重定向到该hash的详情页
 *
 */
@Order(-1)
@Component
@Slf4j
public class NanrenciliInfoHashParser extends AbstractInfoHashParser {

    public NanrenciliInfoHashParser() {
        super(MetadataTypeEnum.NANRENCILI,
                "http://nanrencili.net/list/",
                "body > div.wrap > div:nth-child(1) > div > div.am-panel-bd.sidebar > h2",
                "body > div.wrap > div:nth-child(4) > div > div.am-u-md-8 > div:nth-child(1) > div.am-panel-bd > ul > li:nth-child(2) > b:nth-child(1)",
                "body > div.wrap > div:nth-child(4) > div > div.am-u-md-8 > div:nth-child(4) > div.am-panel-bd > ul");
    }

    @Override
    protected String getUrlByInfoHash(String infoHash) {
        return super.getUrlByInfoHash(infoHash).concat("?kw=" + infoHash);
    }


    @Override
    protected String getName(Element element) {
        return StringUtils.substringBeforeLast(super.getName(element),".");
    }

    @Override
    protected List<MetadataVO.Info> getInfos(Element infosElement) {
        List<MetadataVO.Info> infos = super.getInfos(infosElement);
        for (Element element : infosElement.children()) {
            infos.add(new MetadataVO.Info(element.ownText(), lengthStr2ByteLength(element.child(1).text(), true)));
        }
        return infos;
    }

    public static void main(String[] args) throws Exception {
        NanrenciliInfoHashParser nanrenciliInfoHashParser = new NanrenciliInfoHashParser();
        nanrenciliInfoHashParser.init(new HttpClientUtil(null), new ObjectMapper());
        Metadata a = nanrenciliInfoHashParser.parse("c51163433f69ff8a248f640105b481e97179236f");
        System.out.println(a);
    }


}
