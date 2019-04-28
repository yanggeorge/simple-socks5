package com.threelambda.socks5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yangming 2018/8/30
 */
class Util {

    static String getAddr(byte[] addr) {
        return String.format("%d.%d.%d.%d", addr[0] & 0xFF, addr[1] & 0xFF, addr[2] & 0xFF, addr[3] & 0xFF);
    }

    static Integer getPort(byte[] port) {
        return port[0] << 8 | (port[1] & 0xFF);
    }
}
