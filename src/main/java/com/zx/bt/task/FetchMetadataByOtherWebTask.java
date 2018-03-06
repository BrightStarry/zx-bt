package com.zx.bt.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.config.Config;
import com.zx.bt.entity.Metadata;
import com.zx.bt.enums.InfoHashTypeEnum;
import com.zx.bt.enums.LengthUnitEnum;
import com.zx.bt.enums.MetadataTypeEnum;
import com.zx.bt.exception.BTException;
import com.zx.bt.util.EnumUtil;
import com.zx.bt.util.HtmlResolver;
import com.zx.bt.util.HttpClientUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

/**
 * author:ZhengXing
 * datetime:2018-03-06 20:16
 * 尝试从其他web网站获取已有的metadata信息
 */
@Component
@Slf4j
public class FetchMetadataByOtherWebTask {
    private static final String LOG = "[FetchMetadataByOtherWebTask]";

    /**
     * 等待尝试获取队列
     * 存储16进制的infoHash
     */
    private final BlockingQueue<String> queue;

    /**
     * 需要尝试的函数列表
     */
    private final List<Function<String, Metadata>> functions;

    /**
     * 种子搜网站前缀
     * 该前缀 + infoHash 即可进入详情页
     */
    private final String zhongzisouUrl = "https://www.zhongzidi.com/info-";
    private final String btsoUrl = "https://btso.pw/torrent/detail/hash/";

    private final HttpClientUtil httpClientUtil;
    private final Config config;
    private final ObjectMapper objectMapper;

    public FetchMetadataByOtherWebTask(HttpClientUtil httpClientUtil, Config config, ObjectMapper objectMapper) {
        this.httpClientUtil = httpClientUtil;
        this.config = config;
        this.queue = new LinkedBlockingQueue<>(config.getPerformance().getFetchMetadataByOtherWebTaskQueueNum());
        this.objectMapper = objectMapper;
        this.functions = Arrays.asList(this::fetchMetadataByZhongZiSou);
    }

    /**
     * 入队
     */
    public void put(String infoHashHexStr) {
        //去重
        if (infoHashRepository.countByInfoHashAndType(infoHashHexStr, InfoHashTypeEnum.ANNOUNCE_PEER.getCode()) > 0 ||
                queue.parallelStream().filter(item -> item.equals(infoHashHexStr)).count() > 0 ||
                getPeersCache.isExist(values -> values.parallelStream().filter(v -> v.getInfoHash().equals(infoHashHexStr)).count() > 0))
            return;
        queue.offer(infoHashHexStr);
    }

    /**
     * 取出一个infoHash依次请求
     */
    private Metadata run(String infoHashHexStr) {
        Metadata metadata = null;
        for (Function<String, Metadata> function : functions) {
            try {
                metadata = function.apply(infoHashHexStr);
            } catch (Exception e) {
            }
            if (metadata != null) {
                return metadata;
            }
        }
        return null;
    }


    /**
     * 种子搜网站
     * https://www.zhongzidi.com/
     */
    @SneakyThrows
    public Metadata fetchMetadataByZhongZiSou(String infoHashHexStr) {
        //整个页面信息
        String htmlStr = httpClientUtil.doGetForBasicBrowser(zhongzisouUrl + infoHashHexStr);
        //获取解析用的document
        Document document = HtmlResolver.getDocument(htmlStr);
        //获取种子名字
        String name = HtmlResolver.getElementText(document,
                "#wrapp > div.jumbotron > div > div > div.panel.panel-primary > div.panel-heading > h3 > div");
        //种子长度
        String lengthStr = HtmlResolver.getElementText(document,
                "#wrapp > div.jumbotron > div > div > div:nth-child(4) > div.col-md-9 > div:nth-child(1) > div.panel-body > dl > dd:nth-child(6)");
        //长度
        long length = lengthStr2ByteLength(lengthStr, true);
        //文件列表选择框
        Element infosSelector = HtmlResolver.getElement(document,
                "#wrapp > div.jumbotron > div > div > div:nth-child(4) > div.col-md-9 > div:nth-child(2) > div.panel-body > select");
        List<Metadata.Info> infos = new LinkedList<>();
        //循环每个<option>
        for (Element element : infosSelector.children()) {
            String infoStr = element.html();
            String[] infoStrArr = infoStr.split("&nbsp;&nbsp;&nbsp;&nbsp;");
            infos.add(new Metadata.Info(infoStrArr[0], lengthStr2ByteLength(infoStrArr[1], false)));
        }

        return new Metadata(infoHashHexStr, objectMapper.writeValueAsString(infos), name, length,
                MetadataTypeEnum.ZHONGZISOU.getCode(), infos);
    }

    /**
     * btso网站
     * https://btso.pw/torrent/
     */
    @SneakyThrows
    public Metadata fetchMetadataByBtso(String infoHashHexStr) {
        //整个页面信息
        String htmlStr = httpClientUtil.doGetForBasicBrowser(btsoUrl + infoHashHexStr);
        //获取解析用的document
        Document document = HtmlResolver.getDocument(htmlStr);
        //名字
        String name = HtmlResolver.getElementText(document, "body > div.container > h3");
        //总长度
        String lengthStr = HtmlResolver.getElementText(document, "body > div.container > div:nth-child(7) > div:nth-child(3) > div.col-md-10.col-sm-9.value");
        //长度
        long length = lengthStr2ByteLength(lengthStr, true);
        //文件列表div
        Element infosDiv = HtmlResolver.getElement(document, "body > div.container > div:nth-child(11)");
        Elements infosDivChildren = infosDiv.children();
        for (int i = 0; i < infosDivChildren.size(); i++) {
            if (i == 0) continue;
            String infoName = HtmlResolver.getElementByElementIndex(infosDivChildren.get(i), "div:nth-child(1)").text();


        }
        return null;
    }

    /**
     * 长度字符串 转 字节长度
     * 例如 368.62 MB  1.67 GB   3 B
     *
     * @param isHasSpace 数字和单位间是否有空格
     */
    private long lengthStr2ByteLength(String lengthStr, boolean isHasSpace) {
        String length = null;
        String lengthUnit = null;
        if (isHasSpace) {
            String[] arr = lengthStr.split(" ");
            lengthUnit = arr[1];
            length = arr[0];
        } else {
            char[] chars = lengthStr.toCharArray();
            int i;
            for (i = chars.length - 1; i >= 0; i--) {
                if (Character.isDigit(chars[i])) {
                    i++;
                    break;
                }
            }
            length = lengthStr.substring(0, i);
            lengthUnit = lengthStr.substring(i);
        }
        Optional<LengthUnitEnum> lengthUnitEnumOptional = EnumUtil.getByCode(lengthUnit, LengthUnitEnum.class);
        if (!lengthUnitEnumOptional.isPresent()) {
            log.error("{}[fetchMetadataByZhongZiSou]" + "长度单位不存在.当前单位:{}", LOG, lengthUnit);
            throw new BTException(LOG + "[fetchMetadataByZhongZiSou]" + "长度单位不存在.当前单位:" + lengthUnit);
        }
        return Double.valueOf(length).longValue() * lengthUnitEnumOptional.get().getValue();
    }

    public static void main(String[] args) {
        FetchMetadataByOtherWebTask fetchMetadataByOtherWebTask = new FetchMetadataByOtherWebTask(new HttpClientUtil(), null, new ObjectMapper());
        Metadata a = fetchMetadataByOtherWebTask.fetchMetadataByBtso("64db0fcc53cdb04a2e225d05387489179449b0ce");

    }


}
