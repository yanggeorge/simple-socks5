package com.threelambda.socks5;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yangming 2018/8/29
 */
public class Socks5ServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(Socks5ServerHandler.class);

    /**
     * 是否增加用户密码验证
     */
    private boolean auth = false;
    private String user;
    private String pass;

    private Socks5State state = Socks5State.INIT;
    /**
     * 与dest建立的channel
     */
    private Channel back;

    Socks5ServerHandler(int auth, String user, String pass) {
        if (auth > 0) {
            this.auth = true;
            this.user = user;
            this.pass = pass;
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf)msg;
        logger.debug("state={}", state.name());
        if (buf.readerIndex() == buf.writerIndex()) {
            return;
        }
        switch (state) {
            case INIT: {
                handleInit(ctx, buf);
                break;
            }
            case AUTH: {
                handleAuth(ctx, buf);
                break;
            }
            case COMMAND: {
                handleCommand(ctx, buf);
                break;
            }
            case CONNECTING: {
                handleConnecting(ctx, buf);
                break;
            }
            default:
                break;
        }

    }

    private void handleConnecting(ChannelHandlerContext ctx, ByteBuf buf) {
        logger.debug("handleConnecting");
        if (back != null) {
            back.write(buf);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.debug("channelReadComplete");
        if (back != null) {
            back.flush();
        }
    }

    private void handleInit(ChannelHandlerContext ctx, ByteBuf buf) {
        int ver = buf.readUnsignedByte();
        ByteBuf resp = Unpooled.buffer();
        if (ver != 5) {
            //如果不是 socks5 怎么处理呢
            resp.writeBytes(new byte[] {0x05, (byte)0xFF});
            ctx.writeAndFlush(resp);
            return;
        }
        int nmethod = buf.readUnsignedByte();
        List<Byte> methods = new ArrayList<>();
        for (int i = 0; i < nmethod; i++) {
            methods.add(buf.readByte());
        }
        logger.debug("ver={}, nmethods={}", ver, methods);

        if (!auth) {
            //如果不需要认证，则直返回
            resp.writeBytes(new byte[] {0x05, 0x00});
            ctx.writeAndFlush(resp);
            state = Socks5State.COMMAND;
            return;
        }

        if (auth && methods.contains((byte)0x02)) {
            //如果需要用户名密码认证
            resp.writeBytes(new byte[] {0x05, 0x02});
            ctx.writeAndFlush(resp);
            state = Socks5State.AUTH;
        }
    }

    private void handleAuth(ChannelHandlerContext ctx, ByteBuf buf) {
        logger.debug("handleAuth");
        /*
        当前auth的自协议只有一种 https://tools.ietf.org/html/rfc1929
        +----+------+----------+------+----------+
        |VER | ULEN |  UNAME   | PLEN |  PASSWD  |
        +----+------+----------+------+----------+
        | 1  |  1   | 1 to 255 |  1   | 1 to 255 |
        +----+------+----------+------+----------+
        */
        int ver = buf.readUnsignedByte();
        ByteBuf resp = Unpooled.buffer();
        if (ver != 1) {
            logger.info("auth ver = {}", ver);
            resp.writeBytes(new byte[] {0x05, (byte)0xFF});
            ctx.writeAndFlush(resp);
            return;
        }

        int ulen = buf.readUnsignedByte();
        byte[] uname = new byte[ulen];
        buf.readBytes(uname);
        int plen = buf.readUnsignedByte();
        byte[] passwd = new byte[plen];
        buf.readBytes(passwd);
        String nameString = new String(uname);
        String passString = new String(passwd);
        logger.info("user={}, pass={}", nameString, passString);

        if (nameString.equals(this.user) && passString.equals(this.pass)) {
            resp.writeBytes(new byte[] {0x05, 0x00});
            ctx.writeAndFlush(resp);
            state = Socks5State.COMMAND;
        } else {
            logger.info("uname={}|auth fail", nameString);
            resp.writeBytes(new byte[] {0x05, (byte)0xFF});
            ctx.writeAndFlush(resp);
        }
    }

    private void handleCommand(ChannelHandlerContext ctx, ByteBuf buf) {
        int ver = buf.readUnsignedByte();
        int cmd = buf.readUnsignedByte();
        int rsv = buf.readUnsignedByte();
        int atyp = buf.readUnsignedByte();
        logger.info("ver={}, cmd={}, rsv={}, atyp={}", ver, cmd, rsv, atyp);
        String distAddr = null;
        Integer distPort = null;
        byte[] addrBytes = null;
        byte[] portBytes = new byte[2];
        if (atyp == 1) {
            //ipv4, 4 bytes
            addrBytes = new byte[4];
            buf.readBytes(addrBytes);
            buf.readBytes(portBytes);
            distAddr = Util.getAddr(addrBytes);
            distPort = Util.getPort(portBytes);
            logger.info("distAddr={}, distPort={}", distAddr, distPort);
        } else if (atyp == 3) {
            //domain
            int length = buf.readByte() & 0xFF;
            logger.debug("addr length = {}", length);
            addrBytes = new byte[length];
            buf.readBytes(addrBytes);
            buf.readBytes(portBytes);
            distAddr = new String(addrBytes);
            distPort = Util.getPort(portBytes);
            logger.info("distAddr={}, distPort={}", distAddr, distPort);
        }

        if (cmd == 1) {
            // CONNECT
            logger.debug("connect..");
            connect(ctx, distAddr, distPort, addrBytes, portBytes, atyp);
            this.state = Socks5State.CONNECTING;

        }

    }

    private void connect(ChannelHandlerContext ctx, String distAddr, Integer distPort, byte[] addrBytes,
                         byte[] portBytes, int atyp) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
            .channel(NioSocketChannel.class)
            .remoteAddress(new InetSocketAddress(distAddr, distPort))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                    ch.pipeline().addLast(new Socks5ConnectHandler(ctx.channel()));
                }
            });
        ChannelFuture future = bootstrap.connect();
        back = future.channel();
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.debug("connect complete.");
                if (future.isSuccess()) {
                    logger.debug("connect success.");
                    //连接成功则返回客户端应答
                    ByteBuf responseBuf = Unpooled.buffer();
                    byte VER = 0x05;
                    byte REP = 0x00;
                    byte RSV = 0x00;
                    byte ATYP = (byte)atyp;
                    responseBuf.writeByte(VER).writeByte(REP).writeByte(RSV).writeByte(ATYP);
                    if (atyp == 1) {
                        responseBuf.writeBytes(addrBytes).writeBytes(portBytes);
                    } else if (atyp == 3) {
                        responseBuf.writeByte(addrBytes.length).writeBytes(addrBytes).writeBytes(portBytes);
                    }
                    ctx.writeAndFlush(responseBuf);

                } else {
                    logger.warn("distAddr={}, distPort={}|connect fail.", distAddr, distPort);
                    future.channel().writeAndFlush("fail".getBytes());
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("error", cause);
    }
}
