package com.zx.bt.socket;

import com.zx.bt.util.BTUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018-02-13 13:50
 * 只用于记录日志的channel future 监听器
 * 记录数据发送成功或失败
 * @Deprecated udp通过应该都是发送成功,因为无法检测到底是否成功
 */
@Slf4j
@Component
@Deprecated
public class LogChannelFutureListener implements ChannelFutureListener {
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if(future.isSuccess())
            log.info("通道id:{},写入数据成功.", BTUtil.getChannelId(future.channel()));
        else
            log.info("通道id:{},写入数据失败.e:{}",BTUtil.getChannelId(future.channel()),future.cause().getMessage(),future.cause());
    }
}
