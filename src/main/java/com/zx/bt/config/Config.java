package com.zx.bt.config;

import com.zx.bt.util.BTUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-02-13 12:39
 * 自定义配置类
 */
@Component
@ConfigurationProperties(prefix = "zx-bt")
@Data
public class Config {
    /**
     * 主要配置
     */
    private Main main = new Main();


    @Data
    public static class Main{
        /**
         * nodeId
         */
        private String nodeId = BTUtil.generateNodeIdString();

        /**UDP服务器端端口号*/
        private Integer port = 44444;

        /**UDP服务器主任务线程数*/
        private Integer udpServerMainThreadNum = 1;

        /**初始地址*/
        private List<String> initAddresses = new LinkedList<>();
    }
}
