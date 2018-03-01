package com.zx.bt.dto;

import com.zx.bt.config.Config;
import com.zx.bt.exception.BTException;
import com.zx.bt.util.CodeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ArrayUtils;

import javax.persistence.Entity;

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
        byte[] ipBytes = ArrayUtils.subarray(bytes, 0, 4);
        ip = String.join(".", Integer.toString(ipBytes[0] & 0xFF), Integer.toString(ipBytes[1] & 0xFF)
                , Integer.toString(ipBytes[2] & 0xFF), Integer.toString(ipBytes[3] & 0xFF));

        //ports
        byte[] portBytes = ArrayUtils.subarray(bytes, 4, 6);
        port = portBytes[1] & 0xFF | (portBytes[0] & 0xFF) << 8;
    }
}
