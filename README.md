
# Simple-Socks5 #

## Introduction ##

`simple-socks5` is a simple implementation of socks5 proxy server from scratch, based on `netty`.

You can test it using `cURL` and browser.


## 说明 ##

`simple-socks5`是一个基于`netty`的，从零开始实现的socks5协议的代理服务器。

可以使用`cURL`和浏览器来进行测试。


## Start proxy server ##


    mvn clean compile exec:java -Dport=1086
    
## Test proxy server ##


    curl --socks5 127.0.0.1:1086 http://threelambda.com 