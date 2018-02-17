package com.zx.bt.config;

import com.dampcake.bencode.Bencode;
import io.netty.util.CharsetUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * author:ZhengXing
 * datetime:2018-02-17 13:57
 * 注入bean
 */
@Configuration
public class BeanConfig {
    /**
     * Bencode编解码工具类
     */
    @Bean
    public Bencode bencode() {
        return new Bencode(CharsetUtil.ISO_8859_1);
    }
}
