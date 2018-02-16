package com.zx.bt.util;


import com.zx.bt.exception.BTException;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * author:ZhengXing
 * datetime:2018-02-13 18:32
 * 编码工具类
 */
public class CodeUtil {

    public static byte[] sha1(byte[] bytes) {
        return DigestUtils.sha1(bytes);
    }

    /**
     * byte[] 转 16进制字符串
     */
    public static String bytes2HexStr(byte[] bytes){
        return Hex.encodeHexString(bytes);
    }

    /**
     * 16进制Str 转 byte[] 大小写即可
     */
    @SneakyThrows
    public static byte[] hexStr2Bytes(String hexStr) {
        return Hex.decodeHex(hexStr.toCharArray());
    }


    /**
     * byte[] 和 byte[] 进行异或计算
     * 直接进行累加,得出值来作为异或的差异度
     */
    public static int bytesXorBytes(byte[] bytes1, byte[] bytes2) {
        if(bytes1.length != bytes2.length)
            throw new BTException("异或计算,两个byte[]长度相同.byte1.len:+" + bytes1.length +"+,byte2.len:" + bytes2.length);
//        byte[] result = new byte[len];
        int result = 0;
        for (int i = 0,len = bytes1.length; i < len; i++) {
//            result[i] = (byte) (bytes1[i] ^ bytes2[i]);
            result += Math.abs(bytes1[i] ^ bytes2[i]);
        }
        return result;
    }


    /**
     * int 转 2个字节的byte[]
     * 舍弃16位最高位,只保留16位,两个字节的低位.
     * 这个字节数组的顺序需要是这样的.. 目前我收到其他节点的信息,他们的字节数组大多是这样的/
     * 并且按照惯性思维,左边的(也就是byte[0]),的确应该是高位的.
     */
    public static byte[] int2TwoBytes(int value) {
        byte[] des = new byte[2];
        des[1] = (byte) (value & 0xff);
        des[0] = (byte) ((value >> 8) & 0xff);
        return des;
    }

    public static void main(String[] args) {
       byte[] a =  new byte[]{-32,124,123,-45,65,-76,123,54,43,-34,99,-54,32,56,-35,82,73,34,112,43};
       byte[] b =  new byte[]{-32,124,123,-34,65,43,123,54,43,-34,99,-54,32,56,-35,82,73,34,123,123};
//       byte[] b =  new byte[]{76,96,42,-20,74,-123,22,-34,31,23,-57,38,83,45,12,-35,53,35,32,63};
        int i = bytesXorBytes(a, b);
        System.out.println(i);
    }



}
