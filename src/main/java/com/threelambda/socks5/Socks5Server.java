package com.threelambda.socks5;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yangming 2018/8/29
 */
public class Socks5Server {

    private final static Logger logger = LoggerFactory.getLogger(Socks5Server.class);
    static final int PORT = Integer.parseInt(System.getProperty("port", "1086"));

    public static void main(String[] args) {
        try {
            new Socks5Server().start();
        } catch (Exception e) {
            logger.error("socks5server start fail.", e);
        }
    }

    private void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        try {
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)

                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        logger.debug("initChannel.");
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG), new Socks5ServerHandler());
                    }
                });

            logger.info("[127.0.0.1:{}] bind.", PORT);
            ChannelFuture future = b.bind(PORT).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }

}
