package com.zx.bt.spider.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javafx.binding.StringFormatter;
import com.zx.bt.common.vo.MetadataVO;
import com.zx.bt.spider.config.Config;
import com.zx.bt.common.entity.Metadata;
import com.zx.bt.common.enums.LengthUnitEnum;
import com.zx.bt.spider.enums.MetadataTypeEnum;
import com.zx.bt.common.exception.BTException;
import com.zx.bt.common.service.MetadataService;
import com.zx.bt.spider.store.InfoHashFilter;
import com.zx.bt.common.util.EnumUtil;
import com.zx.bt.spider.util.HtmlResolver;
import com.zx.bt.spider.util.HttpClientUtil;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.util.*;
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
     * 各网站url规则
     */
    private final String zhongzisouUrl = "https://www.zhongzidi.com/info-";
    private final String btwhatUrl = "http://www.btwhat.info/wiki/%s.html";
    private final String cilibaUrl = "https://www.ciliba.org/detail/%s.html";

    /**
     * 其他功能类
     */
    private final HttpClientUtil httpClientUtil;
    private final Config config;
    private final ObjectMapper objectMapper;
    private final GetPeersTask getPeersTask;
    private final MetadataService metadataService;
    private final InfoHashFilter infoHashFilter;

    public FetchMetadataByOtherWebTask(HttpClientUtil httpClientUtil, Config config, ObjectMapper objectMapper,
                                       GetPeersTask getPeersTask, MetadataService metadataService,
                                       InfoHashFilter infoHashFilter) {
        this.httpClientUtil = httpClientUtil;
        this.config = config;
        this.queue = new LinkedBlockingQueue<>(config.getPerformance().getFetchMetadataByOtherWebTaskQueueNum());
        this.objectMapper = objectMapper;
        this.getPeersTask = getPeersTask;
        this.metadataService = metadataService;
        this.infoHashFilter = infoHashFilter;
        this.functions = Arrays.asList(this::fetchMetadataByZhongZiSou,this::fetchMetadataByBtwhat);
    }

    /**
     * 入队
     */
    public void put(String infoHashHexStr) {
        //如果处理过该infoHash,则不做任何操作
        if (infoHashFilter.contain(infoHashHexStr)) {
            return;
        }
        //否则将其加入布隆过滤器和队列
        //当队列已满.抛弃该任务, 也不加入过滤器
        if (queue.offer(infoHashHexStr)) {
            infoHashFilter.put(infoHashHexStr);
        }
    }

    /**
     * 是否存在
     */
    public boolean contain(String infoHashHexStr) {
        return queue.parallelStream().filter(item -> item.equals(infoHashHexStr)).count() > 0;
    }

    /**
     * 队列长度
     */
    public int size() {
        return queue.size();
    }

    /**
     * 开启若干线程,执行run()
     */
    public void start() {
        for (int i = 0; i < config.getPerformance().getFetchMetadataByOtherWebTaskThreadNum(); i++) {
            new Thread(()->{
                while (true) {
                    try {
                        String infoHashHexStr = queue.take();
                        Metadata metadata = run(infoHashHexStr);
                        //将种子信息入库
                        if (!Objects.isNull(metadata)) {
                            metadataService.insert(metadata);
                        } else {
                            //将任务加入get_peers队列
                            getPeersTask.put(infoHashHexStr);
                        }
                    } catch (Exception e) {
                        log.error("{}异常:{}",LOG,e.getMessage(),e);
                    }
                }
            }).start();
        }
    }

    /**
     * 取出一个infoHash依次请求
     */
    private Metadata run(String infoHashHexStr) {
        log.info("{}开始新任务.infoHash:{}", LOG, infoHashHexStr);
        Metadata metadata = null;
        for (Function<String, Metadata> function : functions) {
            try {
                metadata = function.apply(infoHashHexStr);
            } catch (Exception e) {
            }
            if (metadata != null) {
                log.info("{}成功.infoHash:{}", LOG, infoHashHexStr);
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
    private Metadata fetchMetadataByZhongZiSou(String infoHashHexStr) {
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
        List<MetadataVO.Info> infos = new LinkedList<>();
        //循环每个<option>
        for (Element element : infosSelector.children()) {
            String infoStr = element.html();
            String[] infoStrArr = infoStr.split("&nbsp;&nbsp;&nbsp;&nbsp;");
            infos.add(new MetadataVO.Info(infoStrArr[0], lengthStr2ByteLength(infoStrArr[1], false)));
        }

        return new Metadata(infoHashHexStr, objectMapper.writeValueAsString(infos), name, length,
                MetadataTypeEnum.ZHONGZISOU.getCode());
    }

    /**
     * btwhat网站
     * http://www.btwhat.info
     */
    @SneakyThrows
    private Metadata fetchMetadataByBtwhat(String infoHashHexStr) {
        //整个页面信息
        String htmlStr = httpClientUtil.doGetForBasicBrowser(StringFormatter.format(btwhatUrl,infoHashHexStr).getValue());
        //获取解析用的document
        Document document = HtmlResolver.getDocument(htmlStr);
        //名字 - 该网站需要 url解码
        String rawName = HtmlResolver.getElement(document, "#wall > h2").child(0).childNode(0).outerHtml();
        String name = decodeURIComponent(rawName);

        //总长度
        String lengthStr = HtmlResolver.getElementText(document, "#wall > div.fileDetail > table > tbody > tr:nth-child(2) > td:nth-child(4)");
        long length = lengthStr2ByteLength(lengthStr, true);
        //文件列表div
        Element infosDiv = HtmlResolver.getElement(document, "#wall > div.fileDetail > div:nth-child(4) > div.panel-body > ol");
        List<MetadataVO.Info> infos = new LinkedList<>();
        for (Element child : infosDiv.children()) {
            String infoName = decodeURIComponent(child.childNode(1).outerHtml());
            long infoLength = lengthStr2ByteLength(child.child(1).text(), true);
            infos.add(new MetadataVO.Info(infoName, infoLength));
        }
        return new Metadata(infoHashHexStr, objectMapper.writeValueAsString(infos), name, length,
                MetadataTypeEnum.BTWHAT.getCode());
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


    /**
     * 长度字符串 转 字节长度
     * 例如 368.62 MB  1.67 GB   3 B
     *
     * @param isHasSpace 数字和单位间是否有空格
     */
    private static long lengthStr2ByteLength(String lengthStr, boolean isHasSpace) {
        String length;String lengthUnit;
        if (isHasSpace) {
            String[] arr = lengthStr.split(" ");
            lengthUnit = arr[1];length = arr[0];
        } else {
            char[] chars = lengthStr.toCharArray();int i;
            for (i = chars.length - 1; i >= 0; i--) {
                if (Character.isDigit(chars[i])) {
                    i++;
                    break;
                }
            }
            length = lengthStr.substring(0, i);lengthUnit = lengthStr.substring(i).trim();
        }
        Optional<LengthUnitEnum> lengthUnitEnumOptional = EnumUtil.getByCodeString(lengthUnit, LengthUnitEnum.class);
        if (!lengthUnitEnumOptional.isPresent()) {
            log.error("{}[fetchMetadataByZhongZiSou]" + "长度单位不存在.当前单位:{}", LOG, lengthUnit);
            throw new BTException(LOG + "[fetchMetadataByZhongZiSou]" + "长度单位不存在.当前单位:" + lengthUnit);
        }
        return (long)(Double.valueOf(length) * lengthUnitEnumOptional.get().getValue());
    }



    /**
     * 磁力吧
     * https://www.ciliba.org
     */
    @SneakyThrows
    private Metadata fetchMetadataByCiliba(String infoHashHexStr) {
        //整个页面信息
        String htmlStr = httpClientUtil.doGetForBasicBrowser(StringFormatter.format(cilibaUrl, infoHashHexStr).getValue());
        //获取解析用的document
        Document document = HtmlResolver.getDocument(htmlStr);
        return null;
    }


}
