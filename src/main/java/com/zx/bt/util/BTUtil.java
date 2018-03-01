package com.zx.bt.util;

import com.zx.bt.config.Config;
import com.zx.bt.dto.MessageInfo;
import com.zx.bt.dto.method.CommonRequest;
import com.zx.bt.entity.Node;
import com.zx.bt.enums.MethodEnum;
import com.zx.bt.enums.YEnum;
import com.zx.bt.exception.BTException;
import com.zx.bt.store.CommonCache;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author:ZhengXing
 * datetime:2018-02-13 13:42
 * 通用工具类
 */
@Slf4j
@Component
public class BTUtil {

    //用于递增消息ID
    private static AtomicInteger messageIDGenerator = new AtomicInteger(1);
    private static AtomicInteger getPeersMessageIDGenerator = new AtomicInteger(1);
    //递增刷新阈值
    private static int maxMessageID = 1<<15;
    //用于生成20位随机字符,也就是byte[20]的nodeId
    @Deprecated
    private static RandomStringGenerator randomStringGenerator = new RandomStringGenerator.Builder()
            .withinRange('0', 'z').build();






    /**
     * 从channel中获取到当前通道的id
     */
    public static String getChannelId(Channel channel) {
        return channel.id().asShortText();
    }

    /**
     * 生成一个随机的nodeId
     */
    public static byte[] generateNodeId() {
        return RandomUtils.nextBytes(20);
    }

    /**
     * 生成一个随机的nodeID
     */
    public static String generateNodeIdString() {
        return new String(generateNodeId(), CharsetUtil.ISO_8859_1);
    }


    /**
     * 生成一个递增的t,相当于消息id
     * 使用指定生成器
     */
    private static String generateMessageID(AtomicInteger generator) {
        int result;
        //当大于阈值时,重置为0
        if ((result = generator.getAndIncrement()) > maxMessageID) {
            generator.lazySet(1);
        }
        return new String(CodeUtil.int2TwoBytes(result), CharsetUtil.ISO_8859_1);
    }

    /**
     * 生成一个递增的t,相当于消息id
     */
    public static String generateMessageID() {
        return generateMessageID(messageIDGenerator);
    }

    /**
     * 生成一个递增的t,相当于消息id
     * 用于get_peers请求
     */
    public static String generateMessageIDOfGetPeers() {
        return generateMessageID(getPeersMessageIDGenerator);
    }


    /**
     * 根据解析后的消息map,获取消息信息,例如 消息方法(ping/find_node等)/ 消息状态(请求/回复/异常)
     */
    public static MessageInfo getMessageInfo(Map<String, Object> map) throws Exception {
        MessageInfo messageInfo = new MessageInfo();

        /**
         * 状态 请求/回复/异常
         */
        String y = getParamString(map, "y", "y属性不存在.map:" + map);
        Optional<YEnum> yEnumOptional = EnumUtil.getByCode(y, YEnum.class);
        yEnumOptional.orElseThrow(() -> new BTException("y属性值不正确.map:" + map));
        messageInfo.setStatus(yEnumOptional.get());

        /**
         * 消息id
         */
        String t = getParamString(map, "t", "t属性不存在.map:" + map);
        messageInfo.setMessageId(t);

        /**
         * 获取方法 ping/find_node等
         */
        //如果是请求, 直接从请求主体获取其方法
        if (EnumUtil.equals(messageInfo.getStatus().getCode(), YEnum.QUERY)) {
            String q = getParamString(map, "q", "q属性不存在.map:" + map);

            Optional<MethodEnum> qEnumOptional = EnumUtil.getByCode(q, MethodEnum.class);
            qEnumOptional.orElseThrow(() -> new BTException("q属性值不正确.map:" + map));
            messageInfo.setMethod(qEnumOptional.get());

        } else  if (EnumUtil.equals(messageInfo.getStatus().getCode(), YEnum.RECEIVE))  {
            Map<String, Object> rMap = BTUtil.getParamMap(map, "r", "r属性不存在.map:" + map);

            if(rMap.get("token") != null){
                messageInfo.setMethod(MethodEnum.GET_PEERS);
            }else if(rMap.get("nodes") != null){
                messageInfo.setMethod(rMap.get("token") == null ? MethodEnum.FIND_NODE : MethodEnum.GET_PEERS);
            }else{
                throw new BTException("未知类型的回复消息.消息:" + map);
            }
        }
        return messageInfo;
    }

    /**
     * 从Map中获取Object属性
     */
    public static Object getParam(Map<String, Object> map, String key, String log) {
        Object obj = map.get(key);
        if (obj == null)
            throw new BTException(log);
        return obj;
    }

    /**
     * 从Map中获取String属性
     */
    public static String getParamString(Map<String, Object> map, String key, String log) {
        Object obj = getParam(map, key, log);
        return (String) obj;
    }

    /**
     * 从Map中获取List属性
     */

    @SuppressWarnings("unchecked")
    public static List<String> getParamList(Map<String, Object> map, String key, String log) {
        Object obj = getParam(map, key, log);
        return (List<String>) obj;
    }

    /**
     * 从Map中获取Map属性
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getParamMap(Map<String, Object> map, String key, String log) {
        Object obj = getParam(map, key, log);
        return (Map<String, Object>) obj;
    }

    /**
     * 从udp返回的sender属性中,提取出ip
     */
    public static String getIpBySender(InetSocketAddress sender) {
        return sender.getAddress().toString().substring(1);
    }

    /**
     * 从回复的r对象中取出nodes
     */
    public static List<Node> getNodeListByRMap(Map<String, Object> rMap) {
        byte[] nodesBytes = BTUtil.getParamString(rMap, "nodes", "FIND_NODE,找不到nodes参数.rMap:" + rMap).getBytes(CharsetUtil.ISO_8859_1);
        List<Node> nodeList = new LinkedList<>();
        for (int i = 0; i + Config.NODE_BYTES_LEN < nodesBytes.length; i += Config.NODE_BYTES_LEN) {
            //byte[26] 转 Node
            Node node = new Node(ArrayUtils.subarray(nodesBytes, i, i + Config.NODE_BYTES_LEN));
            nodeList.add(node);
        }
        return nodeList;
    }








}
