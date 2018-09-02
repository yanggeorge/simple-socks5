package com.threelambda.socks5;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yangming 2018/8/30
 */
public class Socks5ConnectHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(Socks5ConnectHandler.class);

    /**
     * client与server建立channel
     */
    private Channel front;

    public Socks5ConnectHandler(Channel front) {
        this.front = front;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("connect handler channelRead");
        front.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.debug("connect handler readComplete");
        front.flush();
    }
}
