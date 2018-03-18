package com.zx.bt.spider.parser;

import com.zx.bt.common.vo.MetadataVO;
import com.zx.bt.spider.enums.MetadataTypeEnum;
import com.zx.bt.spider.util.HtmlResolver;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-03-18 15:27
 * http://www.btwhat.info
 */
@Component
@Slf4j
public class BtwhatInfoHashParser extends AbstractInfoHashParser {

    public BtwhatInfoHashParser() {
        super(MetadataTypeEnum.BTWHAT,
                "http://www.btwhat.info/wiki/",
                "#wall > h2",
                "#wall > div.fileDetail > table > tbody > tr:nth-child(2) > td:nth-child(4)",
                "#wall > div.fileDetail > div:nth-child(4) > div.panel-body > ol");
    }

    @Override
    protected String getName(Element element) {
        return decodeURIComponent(HtmlResolver.getElement(element, nameSelector).child(0).childNode(0).outerHtml());
    }


    @Override
    protected List<MetadataVO.Info> getInfos(Element infosElement) {
        List<MetadataVO.Info> infos = super.getInfos(infosElement);
        for (Element child : infosElement.children()) {
            infos.add(new MetadataVO.Info(decodeURIComponent(child.childNode(1).outerHtml()),
                    lengthStr2ByteLength(child.child(1).text(), true)));
        }
        return infos;
    }

    /**
     * 如下字符 转  utf-8编码结果
     * document.write(decodeURIComponent("%e6%"+"8b%b"+"3%e8"+"%84%"+"9a%e"+"5%88"+"%91%"+"e8%a"+"d%a6"+"%ef%"+"bc%9"+"a%e5"+"%94%"+"90%e"+"4%ba"+"%ba%"+"e8%a"+"1%97"+".HD."+"720p"+".%e9"+"%9f%"+"a9%e"+"8%af"+"%ad%"+"e4%b"+"8%ad"+"%e5%"+"ad%9"+"7"));
     */
    @SneakyThrows
    private String decodeURIComponent(String rawStr) {
        String searchStr = "decodeURIComponent(";
        String substr = rawStr.substring(rawStr.indexOf(searchStr) + searchStr.length());
        String substr2 = substr.substring(0, substr.indexOf("));"));
        String[] subStr2Arr = substr2.split("\"\\+\"");
        subStr2Arr[0] = subStr2Arr[0].substring(1);
        subStr2Arr[subStr2Arr.length - 1] = StringUtils.substringBeforeLast(subStr2Arr[subStr2Arr.length - 1], "\"");
        return URLDecoder.decode(String.join("", subStr2Arr), CharsetUtil.UTF_8.name());
    }
}
