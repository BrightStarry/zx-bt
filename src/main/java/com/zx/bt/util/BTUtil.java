package com.zx.bt.util;

import com.zx.bt.config.Config;
import com.zx.bt.dto.MessageInfo;
import com.zx.bt.entity.Node;
import com.zx.bt.enums.MethodEnum;
import com.zx.bt.enums.YEnum;
import com.zx.bt.exception.BTException;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    //递增刷新阈值
    private static int maxMessageID = 1<<15;
    //用于生成20位随机字符,也就是byte[20]的nodeId
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
        byte[] r = new byte[20];
        for (int i = 0; i < 20; i++) {
            r[i] = (byte) RandomUtils.nextInt(0, 256);
        }
        return r;
    }

    /**
     * 生成一个随机的nodeID
     */
    public static String generateNodeIdString() {
        return new String(generateNodeId(), CharsetUtil.ISO_8859_1);
    }

    /**
     * 生成一个递增的t,相当于消息id
     */
    public static String generateMessageID() {
        int result;
        //当大于阈值时,重置为0
        if ((result = messageIDGenerator.getAndIncrement()) > maxMessageID) {
            messageIDGenerator.lazySet(1);
        }
        return new String(CodeUtil.int2TwoBytes(result), CharsetUtil.ISO_8859_1);

//        return randomStringGenerator.generate(2);
    }

    /**
     * 根据解析后的消息map,获取消息信息,例如 消息方法(ping/find_node等)/ 消息状态(请求/回复/异常)
     */
    public static MessageInfo getMessageInfo(Map<String, Object> map) throws Exception {
        MessageInfo messageInfo = new MessageInfo();

        /**
         * 状态 请求/回复/异常
         */
        Object yObj = map.get("y");
        if (yObj == null)
            throw new BTException("y属性不存在.map:" + map);
        String y = yObj.toString();
        Optional<YEnum> yEnumOptional = EnumUtil.getByCode(y, YEnum.class);
        if (!yEnumOptional.isPresent())
            throw new BTException("y属性值不正确.map:" + map);
        messageInfo.setStatus(yEnumOptional.get());

        /**
         * 消息id
         */
        Object tObj = map.get("t");
        if (tObj == null)
            throw new BTException("t属性不存在.map:" + map);
        String t = tObj.toString();
        messageInfo.setMessageId(t);

        /**
         * 获取方法 ping/find_node等
         */
        //如果是请求, 直接从请求主体获取其方法
        if (EnumUtil.equals(messageInfo.getStatus().getCode(), YEnum.QUERY)) {
            Object qObj = map.get("q");
            if (qObj == null)
                throw new BTException("q属性不存在.map:" + map);
            String q = qObj.toString();
            Optional<MethodEnum> qEnumOptional = EnumUtil.getByCode(q, MethodEnum.class);
            if (!qEnumOptional.isPresent())
                throw new BTException("q属性值不正确.map:" + map);
            messageInfo.setMethod(qEnumOptional.get());

        } else  if (EnumUtil.equals(messageInfo.getStatus().getCode(), YEnum.RECEIVE))  {
            Map<String, Object> rMap = BTUtil.getParamMap(map, "r", "r属性不存在.map:" + map);
            if(rMap.get("nodes") != null){
                messageInfo.setMethod(rMap.get("token") == null ? MethodEnum.FIND_NODE : MethodEnum.GET_PEERS);
            }else{
                //从缓存中读取其方法
                MessageInfo messageInfo1 = CacheUtil.getAndRemove(t);
                if(messageInfo1 == null){
                    throw new BTException("缓存不存在.map:" + map);
                }

                messageInfo.setMethod(messageInfo1.getMethod());
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
     * 从Map中获取Map属性
     */
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








}
