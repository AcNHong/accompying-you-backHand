package com.jun.usercenter.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StrProcess {
    public static String getMD5Hash(String input) throws NoSuchAlgorithmException {

            // 创建一个MD5加密算法的MessageDigest对象
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 将输入字符串转换为字节数组并进行加密
            byte[] messageDigest = md.digest(input.getBytes());

            // 使用 Formatter 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

    }
}
