package io.netty.example.http2;

public class Http2Consts {

    public static void setSystemProperties() {
        System.setProperty("ssl", "");
        System.setProperty("host", "push-api20.yy.com");
        System.setProperty("port", "443");
        System.setProperty("url", "/get_text");
    }


}
