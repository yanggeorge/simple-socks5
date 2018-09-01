package com.threelambda.socks5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yangming 2018/8/30
 */
class Util {

    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    static String getAddr(byte[] addrBytes) {
        StringBuilder s = new StringBuilder();
        boolean first = true;
        for (byte addrByte : addrBytes) {
            String value = String.valueOf(addrByte & 0xFF);
            if (first) {
                s.append(value);
                first = false;
            } else {
                s.append(".").append(value);
            }
        }
        return s.toString();
    }

    static Integer getPort(byte[] portBytes) {
        Integer n = 0;
        for (byte portByte : portBytes) {
            n = n * 256 + (portByte & 0xFF);
        }
        return n;
    }
}
