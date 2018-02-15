package com.zx.bt.entity;

import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * author:ZhengXing
 * datetime:2018-02-14 19:39
 * 一个节点信息
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Node {

    @Id
    @GeneratedValue
    private Long id;

    private String nodeId;

    private String ip;

    private Integer port;

    @Deprecated
    public String toByteString() {
//        byte[] bytes = new byte[26];
//        byte[] idBytes = id.getBytes(CharsetUtil.ISO_8859_1);
//        //将idBytes copy到 bytes
//        System.arraycopy(idBytes,0,bytes,0,20);
//
//        //String的ip 转 byte[4]
//        String[] ips = StringUtils.splitByWholeSeparator(ip, ".");
//        byte[] ipBytes = new byte[4];
//        for (int i = 0; i < 4; i++) {
//            ipBytes[i] = Integer.valueOf(ips[i]).byteValue();
//        }
//        //ipBytes copy到 bytes
//        System.arraycopy(ipBytes,0,bytes,20,4);
        return null;
    }

    public Node(String nodeId, String ip, Integer port) {
        this.nodeId = nodeId;
        this.ip = ip;
        this.port = port;
    }
}
