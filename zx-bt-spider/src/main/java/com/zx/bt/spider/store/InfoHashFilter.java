package com.zx.bt.spider.store;

/**
 * author:ZhengXing
 * datetime:2018-03-25 0:48
 * 过滤器接口
 */
public interface InfoHashFilter {

    /**
     * 加入
     */
   void put(String infoHash);

    /**
     * 判断是否存在
     * 当过滤器暂不可用, 此期间调用全部返回true,暂不接收任何info_hash
     */
    boolean contain(String infoHash);

    /**
     * 预期长度
     */
    long size();

    /**
     * 启动该过滤器
     */
    public void run();
}
