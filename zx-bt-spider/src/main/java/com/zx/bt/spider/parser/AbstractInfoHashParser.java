package com.zx.bt.spider.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.bt.common.entity.Metadata;
import com.zx.bt.common.enums.LengthUnitEnum;
import com.zx.bt.common.exception.BTException;
import com.zx.bt.common.util.EnumUtil;
import com.zx.bt.common.vo.MetadataVO;
import com.zx.bt.spider.enums.MetadataTypeEnum;
import com.zx.bt.spider.util.HtmlResolver;
import com.zx.bt.spider.util.HttpClientUtil;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * author:ZhengXing
 * datetime:2018/3/16 0016 17:44
 * 抽象的 infoHash 解析器
 *
 * 解析已有的其他磁力搜索网站
 * 将infoHash解析为 {@link com.zx.bt.common.entity.Metadata}
 */
@Slf4j
@Component
@Order(Integer.MIN_VALUE)
public abstract class AbstractInfoHashParser {
	protected static final String LOG = "[InfoHash解析器]";

	protected static HttpClientUtil httpClientUtil;
	protected static ObjectMapper objectMapper;

	@Autowired
	public void init(HttpClientUtil httpClientUtil,ObjectMapper objectMapper) {
		AbstractInfoHashParser.httpClientUtil = httpClientUtil;
		AbstractInfoHashParser.objectMapper = objectMapper;
	}



	/**
	 * 当前解析器类型
	 */
	protected MetadataTypeEnum metadataType;

	/**
	 * 通用后缀
	 */
	protected  String urlSuf = ".html";

	/**
	 * 网址
	 */
	protected String url;

	/**
	 * 种子名字的 selector
	 */
	protected String nameSelector;

	/**
	 * 种子长度的 selector
	 */
	protected String lengthSelector;

	/**
	 * 文件列表的 selector
	 */
	protected String infosElementSelector;

	/**
	 * @param metadataType 类型
	 * @param urlSuf 网址后缀
	 * @param url 网址
	 * @param nameSelector 名字元素的selector
	 * @param lengthSelector 长度元素的selector
	 * @param infosElementSelector 文件列表元素的selector
	 */
	public AbstractInfoHashParser(MetadataTypeEnum metadataType, String urlSuf, String url,
								  String nameSelector, String lengthSelector, String infosElementSelector) {
		this.metadataType = metadataType;
		this.urlSuf = urlSuf;
		this.url = url;
		this.nameSelector = nameSelector;
		this.lengthSelector = lengthSelector;
		this.infosElementSelector = infosElementSelector;
	}

	public AbstractInfoHashParser(MetadataTypeEnum metadataType,String url,
								  String nameSelector, String lengthSelector, String infosElementSelector) {
		this.metadataType = metadataType;
		this.url = url;
		this.nameSelector = nameSelector;
		this.lengthSelector = lengthSelector;
		this.infosElementSelector = infosElementSelector;
	}

	public MetadataTypeEnum getMetadataType() {
		return metadataType;
	}

	/**
	 * 根据infoHash获取到请求地址
	 * 默认是 {@link #url} + infoHash + {@link #urlSuf}
	 */
	protected  String getUrlByInfoHash(String infoHash){
		return url.concat(infoHash).concat(urlSuf);
	}

	/**
	 * 根据网址发起请求,获取到{@link org.jsoup.nodes.Element}
	 * 需要注意的是 {@link Document}也是Element的子类
	 */
	protected Element getDocumentByPath(String url) {
		return HtmlResolver.getDocument(httpClientUtil.doGetForBasicBrowser(url));
	}

	/**
	 * 获取种子名字
	 */
	protected String getName(Element element) {
		return HtmlResolver.getElementText(element, nameSelector);
	}

	/**
	 * 获取种子长度Element
	 */
	protected  long getLength(Element element){
		String lengthStr = HtmlResolver.getElementText(element, lengthSelector);
		return lengthStr2ByteLength(lengthStr, true);
	}

	/**
	 * 获取种子文件列表元素
	 */
	protected Element getInfosElement(Element element) {
		return HtmlResolver.getElement(element, infosElementSelector);
	}

	/**
	 * 从文件列表解析出每个元素,加入集合
	 */
	protected  List<MetadataVO.Info> getInfos(Element infosElement){
		return new LinkedList<>();
	}

	/**
	 * 根据属性构建出{@link com.zx.bt.common.entity.Metadata}
	 * @param infoHash 16进制小写哈希码
	 * @param infos 文件列表信息
	 * @param name 名字
	 * @param length 字节长度
	 */
	@SneakyThrows
	protected Metadata buildMetadata(String infoHash, List<MetadataVO.Info> infos, String name, long length) {
		return new Metadata(infoHash,objectMapper.writeValueAsString(infos), name, length,
				this.metadataType.getCode());
	}


	/**
	 * 解析
	 * 模版方法
	 */
	public Metadata parse(String infoHash) {
		// 主体元素,默认是整个html
		Element body = getDocumentByPath(getUrlByInfoHash(infoHash));
		// 种子名字
		String name = getName(body);
		// 种子长度
		long length = getLength(body);
		// 文件列表元素
		Element infosElement = getInfosElement(body);
		// 文件列表
		List<MetadataVO.Info> infos = getInfos(infosElement);
		return buildMetadata(infoHash, infos, name, length);
	}



	/**
	 * 长度字符串 转 字节长度
	 * 例如 368.62 MB  1.67 GB   3 B
	 *
	 * @param isHasSpace 数字和单位间是否有空格
	 */
	protected  long lengthStr2ByteLength(String lengthStr, boolean isHasSpace) {
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
			log.error("{}当前解析器类型:{}," + "长度单位不存在.当前单位:{}.", LOG,this.metadataType.getCode(), lengthUnit);
			throw new BTException(LOG + "长度单位不存在.");
		}
		return (long)(Double.valueOf(length) * lengthUnitEnumOptional.get().getValue());
	}
}
