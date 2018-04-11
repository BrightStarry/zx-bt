package com.zx.bt.web.util;

import com.ggstar.util.ip.IpHelper;

/**
 * author:ZhengXing
 * datetime:2018-04-11 19:51
 * ip工具类
 */
public class IpUtil {

    /**
     * 根据 ip 获取该ip所属城市
     * 基于 github：https://github.com/wzhe06/ipdatabase 项目
     */
    public static String getCity(String ip) {
        return IpHelper.findRegionByIp(ip);
    }
}
