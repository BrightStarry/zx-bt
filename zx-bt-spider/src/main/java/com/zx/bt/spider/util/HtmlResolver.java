package com.zx.bt.spider.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * author:ZhengXing
 * datetime:2018-03-06 20:38
 * html解析器
 */
public class HtmlResolver {

    /**
     * 获取document
     */
    public static Document getDocument(String html){
        return Jsoup.parse(html);
    }


    /**
     * 抽取html中指定索引的某元素
     */
    public static Element getElementByIndex(Element document, String selector, int index) {
        return document.select(selector).get(index);
    }

    /**
     *抽取html中单一的某个元素
     */
    public static Element getElement(String html, String selector){
        return getElementByIndex(html, selector, 0);
    }

    /**
     *抽取html中单一的某个元素
     */
    public static Element getElement(Element document, String selector){
        return getElementByIndex(document, selector, 0);
    }

    /**
     * 抽取html中指定索引的某元素
     */
    public static Element getElementByIndex(String html, String selector,int index) {
        return getElementByIndex(getDocument(html), selector, index);
    }

    /**
     * 抽取html中某单一元素的单一属性
     */
    public static String getElementAttr(String html, String selector,String attrName){
        return getElement(html, selector).attr(attrName);
    }

    /**
     * 抽取html中某单一元素的单一属性
     */
    public static String getElementAttr(Element document, String selector,String attrName){
        return getElement(document, selector).attr(attrName);
    }

    /**
     * 抽取指定索引的某元素的某属性
     */
    public static String getElementAttrByIndex(String html, String selector, int index, String attrName) {
        return getElementByIndex(html, selector, index).attr(attrName);
    }

    /**
     * 抽取指定索引的某元素的某属性
     */
    public static String getElementAttrByIndex(Element document, String selector, int index, String attrName) {
        return getElementByIndex(document, selector, index).attr(attrName);
    }

    /**
     * 抽取html中某单一元素的text
     */
    public static String getElementText(Element document, String selector){
        return getElement(document, selector).text();
    }

    /**
     * 抽取Element中的某一元素,根据索引
     */
    public static Element getElementByElementIndex(Element element, String selector, int index) {
        return element.select(selector).get(index);
    }

    /**
     * 抽取Element中的第一个元素
     */
    public static Element getElementByElementIndex(Element element, String selector) {
        return getElementByElementIndex(element, selector, 0);
    }
}
