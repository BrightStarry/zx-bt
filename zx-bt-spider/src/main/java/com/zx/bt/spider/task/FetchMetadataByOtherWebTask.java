package com.zx.bt.spider.task;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
     * infoHash默认为40位16进制小写字符串
     */
    // url + infoHash
    private final String zhongzisouUrl = "https://www.zhongzidi.com/info-";
    // url + infoHash + .html
    private final String btwhatUrl = "http://www.btwhat.info/wiki/";
    // url + infoHash + .html
    private final String cilibaUrl = "https://www.ciliba.org/detail/";
    // url + 大写infoHash
    private final String btceriseUrl = "http://www.btcerise.me/info/";
    // url + infoHash + .html
    private final String btrabbitUrl = "http://www.btrabbit.net/wiki/";


    private final String commonSuf = ".html";

    /**
     * 线程池
     */
    private final ExecutorService threadPool;

    /**
     * 其他功能类
     */
    private final HttpClientUtil httpClientUtil;
    private final ObjectMapper objectMapper;
    private final GetPeersTask getPeersTask;
    private final MetadataService metadataService;
    private final InfoHashFilter infoHashFilter;

    public FetchMetadataByOtherWebTask(HttpClientUtil httpClientUtil, Config config, ObjectMapper objectMapper,
                                       GetPeersTask getPeersTask, MetadataService metadataService,
                                       InfoHashFilter infoHashFilter) {
        this.httpClientUtil = httpClientUtil;
        this.queue = new LinkedBlockingQueue<>(config.getPerformance().getFetchMetadataByOtherWebTaskQueueNum());
        this.objectMapper = objectMapper;
        this.getPeersTask = getPeersTask;
        this.metadataService = metadataService;
        this.infoHashFilter = infoHashFilter;
        this.functions = Arrays.asList(
                this::fetchMetadataByBtcerise,
                this::fetchMetadataByBtwhat,
                this::fetchMetadataByCiliba,
                this::fetchMetadataByBtrabbit,
                this::fetchMetadataByZhongZiSou);
        this.threadPool = Executors.newFixedThreadPool(config.getPerformance().getFetchMetadataByOtherWebTaskThreadNum());
    }

    /**
     * 入队
     * 下面的操作如果没有当队列满了,不加入过滤器的需求, 可以直接使用infoHashFilter.put()方法,
     * 并通过返回值判断. 为true,put成功,不重复; 为false,put失败,重复
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

    public static void main(String[] args) {
        System.out.println(new Config());
    }

    /**
     * 开启若干线程,执行run()
     * 从原先的直接启动x个线程,靠阻塞队列实现暂停, 修改为使用线程池,只控制最大线程数
     */
    public void start() {
        new Thread(()->{
            while (true) {
                try {
                    String infoHashHexStr = queue.take();
                    this.threadPool.execute(()->{
                        try {
                            Metadata metadata = run(infoHashHexStr);
                            //将种子信息入库
                            if (!Objects.isNull(metadata)) {
								metadataService.insert(metadata);
							} else {
								//将任务加入get_peers队列
								getPeersTask.put(infoHashHexStr);
							}
                        } catch (Exception e) {
                            log.error("{}子任务异常:{}",LOG,e.getMessage(),e);
                        }
                    });
                } catch (Exception e) {
                    log.error("{}主任务异常:{}",LOG,e.getMessage(),e);
                }
            }
        }).start();

//        for (int i = 0; i < config.getPerformance().getFetchMetadataByOtherWebTaskThreadNum(); i++) {
//            new Thread(()->{
//                while (true) {
//                    try {
//                        String infoHashHexStr = queue.take();
//                        Metadata metadata = run(infoHashHexStr);
//                        //将种子信息入库
//                        if (!Objects.isNull(metadata)) {
//                            metadataService.insert(metadata);
//                        } else {
//                            //将任务加入get_peers队列
//                            getPeersTask.put(infoHashHexStr);
//                        }
//                    } catch (Exception e) {
//                        log.error("{}异常:{}",LOG,e.getMessage(),e);
//                    }
//                }
//            }).start();
//        }
    }

    /**
     * 取出一个infoHash依次请求
     */
    private Metadata run(String infoHashHexStr) {
        log.info("{}开始新任务.infoHash:{}", LOG, infoHashHexStr);
        Metadata metadata= null;
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

        //主体
        Element body = HtmlResolver.getElement(document, "#wrapp > div.jumbotron > div > div");

        //获取种子名字
        String name = HtmlResolver.getElementText(body,
                "div.panel.panel-primary > div.panel-heading > h3 > div");
        //种子长度
        String lengthStr = HtmlResolver.getElementText(body,
                "div:nth-child(4) > div.col-md-9 > div:nth-child(1) > div.panel-body > dl > dd:nth-child(6)");
        //长度
        long length = lengthStr2ByteLength(lengthStr, true);
        //文件列表选择框
        Element infosDiv = HtmlResolver.getElement(body,
                "div:nth-child(4) > div.col-md-9 > div:nth-child(2) > div.panel-body > select");
        List<MetadataVO.Info> infos = new LinkedList<>();
        //循环每个<option>
        for (Element element : infosDiv.children()) {
            String infoStr = element.html();
            String[] infoStrArr = infoStr.split("&nbsp;&nbsp;&nbsp;&nbsp;");
            infos.add(new MetadataVO.Info(infoStrArr[0], lengthStr2ByteLength(infoStrArr[1], false)));
        }

        return new Metadata(infoHashHexStr, objectMapper.writeValueAsString(infos), name, length,
                MetadataTypeEnum.ZHONGZISOU.getCode());
    }

    /**
     * 磁力吧
     * https://www.ciliba.org
     * 2539eebf4f3db43eb1a680f3960926d20a253a2a
     */
    @SneakyThrows
    private Metadata fetchMetadataByCiliba(String infoHashHexStr) {
        // 整个页面信息
        String htmlStr = httpClientUtil.doGetForBasicBrowser(cilibaUrl +  infoHashHexStr + commonSuf);
        // 获取解析用的document
        Document document = HtmlResolver.getDocument(htmlStr);
        // 名字
        String name = HtmlResolver.getElementText(document, "#wall > h1");
        // 长度 它的格式是 "种子大小: 1.5 Gb"
        String lengthStr = HtmlResolver.getElementText(document, "#wall > div.fileDetail > p:nth-child(3)").substring(5);
        long length = lengthStr2ByteLength(lengthStr, true);
        //文件列表选择框
        Element infosDiv = HtmlResolver.getElement(document, "#wall > ol");
        List<MetadataVO.Info> infos = new LinkedList<>();
        for (Element element : infosDiv.children()) {
            infos.add(new MetadataVO.Info(element.ownText(), lengthStr2ByteLength(element.child(0).text(), true)));
        }
        return new Metadata(infoHashHexStr, objectMapper.writeValueAsString(infos), name, length,
                MetadataTypeEnum.CILIBA.getCode());
    }


    /**
     * btwhat网站
     * http://www.btwhat.info
     */
    @SneakyThrows
    private Metadata fetchMetadataByBtwhat(String infoHashHexStr) {
        //整个页面信息
        String htmlStr = httpClientUtil.doGetForBasicBrowser(btwhatUrl + infoHashHexStr + commonSuf);
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
            infos.add(new MetadataVO.Info(decodeURIComponent(child.childNode(1).outerHtml()),
                    lengthStr2ByteLength(child.child(1).text(), true)));
        }
        return new Metadata(infoHashHexStr, objectMapper.writeValueAsString(infos), name, length,
                MetadataTypeEnum.BTWHAT.getCode());
    }

    /**
     * btcerise
     * http://www.btcerise.me
     */
    @SneakyThrows
    Metadata fetchMetadataByBtcerise(String infoHashHexStr) {
        //整个页面信息
        String htmlStr = httpClientUtil.doGetForBasicBrowser(btceriseUrl + infoHashHexStr.toUpperCase());
        //获取解析用的document
        Document document = HtmlResolver.getDocument(htmlStr);

        //名字
        String name = HtmlResolver.getElementText(document, "#content > div > h1");
        //长度
        String lengthStr = HtmlResolver.getElementText(document, "#content > div > ul > li:nth-child(3) > span").substring(3);
        long length = lengthStr2ByteLength(lengthStr, false);
        //文件列表div
        Element infosDiv = HtmlResolver.getElement(document, "#filelist");
        List<MetadataVO.Info> infos = new LinkedList<>();
        Elements infosDivChildrens = infosDiv.children();
        //此处不获取最后一个文件,因为它是该网站的广告(防止其他爬虫的(目测是防止那种直接操作浏览器内核的爬虫的))
        for (int i = 0; i < infosDivChildrens.size() -1; i++) {
            Element element = infosDivChildrens.get(i);
            infos.add(new MetadataVO.Info(element.childNode(1).childNode(0).outerHtml(),  lengthStr2ByteLength(element.childNode(2).childNode(0).outerHtml(),false)));
        }

        return new Metadata(infoHashHexStr, objectMapper.writeValueAsString(infos), name, length,
                MetadataTypeEnum.BTCERISE.getCode());
    }

    /**
     * btrabbit
     * http://www.btrabbit.net
     */
    @SneakyThrows
    private Metadata fetchMetadataByBtrabbit(String infoHashHexStr) {
        //整个页面信息
        String htmlStr = httpClientUtil.doGetForBasicBrowser(btrabbitUrl + infoHashHexStr + commonSuf);
        //获取解析用的document
        Document document = HtmlResolver.getDocument(htmlStr);
        //名字
        String name = HtmlResolver.getElementText(document, "#wall > h2");
        //长度
        String lengthStr = HtmlResolver.getElementText(document, "#wall > div.fileDetail > div > table > tbody > tr:nth-child(2) > td:nth-child(5)");
        long length = lengthStr2ByteLength(lengthStr, true);
        //文件列表div
        Element infosDiv = HtmlResolver.getElement(document, "#wall > div.fileDetail > div > div:nth-child(10) > div.panel-body > ol");
        List<MetadataVO.Info> infos = new LinkedList<>();
        for (Element element : infosDiv.children()) {
            infos.add(new MetadataVO.Info(element.ownText(),lengthStr2ByteLength(element.child(0).text(),true)));
        }

        return new Metadata(infoHashHexStr, objectMapper.writeValueAsString(infos), name, length,
                MetadataTypeEnum.BTRABBIT.getCode());
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
            lengthUnit = arr[1].trim();length = arr[0].trim();
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





}
