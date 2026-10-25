package com.yunying.music;
public class SignerVectors {
    public static void main(String[] args)throws Exception {
        String secret="0000000000000000000000000000000000000000000000000000000000000001";
        System.out.println(AliyunDevice.publicKey(secret));
        System.out.println(AliyunDevice.signature(secret,"fixture-device","fixture-user"));
    }
}
