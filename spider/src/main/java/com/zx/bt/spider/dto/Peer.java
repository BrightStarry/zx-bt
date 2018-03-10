package com.zx.bt.spider.dto;

import com.zx.bt.spider.config.Config;
import com.zx.bt.common.exception.BTException;
import com.zx.bt.common.util.CodeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ArrayUtils;

/**
 * author:ZhengXing
 * datetime:2018-02-27 21:06
 * peer
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Peer {

    private String ip;

    private Integer port;

    /**
     * byte[6] 转 Node
     */
    public Peer(byte[] bytes) {
        if (bytes.length != Config.PEER_BYTES_LEN)
            throw new BTException("转换为Peer需要bytes长度为6,当前为:" + bytes.length);
        //ip
        ip = CodeUtil.bytes2Ip(ArrayUtils.subarray(bytes, 0, 4));

        //ports
        port = CodeUtil.bytes2Port(ArrayUtils.subarray(bytes, 4, 6));
    }
}
