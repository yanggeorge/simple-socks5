package com.threelambda.socks5;

/**
 * @author yangming 2018/8/30
 */
public enum Socks5State {
    INIT, COMMAND, CONNECTING,
    AUTH;
}
