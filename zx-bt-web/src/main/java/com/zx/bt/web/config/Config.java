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

    /**
     * 业务相关
     */
    private Service service = new Service();

    //磁力前缀
    public static final String MAGNET_LINK_PRE = "magnet:?xt=urn:btih:";

    //默认起始页码
    public static final int DEFAULT_START_PAGE_NO = 1;

    /**
     * 业务相关
     */
    @Data
    public static class Service{
        /**
         * 同一种子,热度上涨1,至少需要x秒后
         */
        private Integer hotCacheExpireSecond = 20;

        /**
         * 热度缓存器, 总长度
         * 可以不用太大,因为满了之后, 应该是优先驱逐早的种子,符合业务逻辑
         */
        private Integer hotCacheSize = 102400;

        /**
         * webSocket连接过期时间(存取后自动刷新)
         * 正常情况下,退出或异常会进行清除, 这个属性只是为了防止bug.确保缓存会被清理
         */
        private Integer webSocketConnectExpireSecond = 60 * 30;


        /**
         * 最大支持webSocket连接数
         */
        private Integer webSocketMaxConnectNum = 10240;
    }

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
