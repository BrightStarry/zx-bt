package com.zx.bt.web.config;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Date;

/**
 * author:ZhengXing
 * datetime:2018-03-11 0:48
 * 配置类
 */
@Component
@Accessors(chain = true)
@ConfigurationProperties(prefix = "zx-bt")
@Data
@Validated
public class Config {



    /**
     * web 相关
     */
    private Web web = new Web();

    /**
     * es 相关
     */
    private Elasticsearch es = new Elasticsearch();

    //磁力前缀
    public static final String MAGNET_LINK_PRE = "magnet:?xt=urn:btih:";

    /**
     * web 相关设置
     */
    @Data
    public static class Web{
        /**
         * 默认分页每页长度
         */
        private Integer pageSize = 10;

    }

    /**
     * elasticsearch
     */
    @Data
    public static class Elasticsearch{
        /**
         * ip
         */
        @NotBlank
        private String ip;

        /**
         * 端口
         */
        private Integer port = 9200;

        /**
         * 集群名
         */
        private String clusterName;

    }
}
