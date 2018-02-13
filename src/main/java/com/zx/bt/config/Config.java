package com.zx.bt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018-02-13 12:39
 * 自定义配置类
 */
@Component
@ConfigurationProperties
@Data
public class Config {
    /**
     * 主要配置
     */
    private Main main = new Main();


    @Data
    public static class Main{
        /**UDP服务器端端口号*/
        private Integer port = 44444;

        /**UDP服务器主任务线程数*/
        private Integer udpServerMainThreadNum = 1;
    }
}
