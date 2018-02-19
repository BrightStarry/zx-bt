package com.zx.bt.util;


import com.zx.bt.config.Config;
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
    public static String bytes2HexStr(byte[] bytes) {
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
     */
    public static byte[] bytesXorBytes(byte[] bytes1, byte[] bytes2) {
        if (bytes1.length != bytes2.length)
            throw new BTException("异或计算,两个byte[]长度不相同.byte1.len:+" + bytes1.length + "+,byte2.len:" + bytes2.length);
        int len = bytes1.length;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) (bytes1[i] ^ bytes2[i]);
        }
        return result;
    }

    /**
     * 将两个异或结果进行比较大小
     * 例如目标节点T, 两个比较节点A,B,分别将A/B和T进行异或计算,求出异或结果byteA[]/byteB[]
     * 再将这两个结果用该方法比较,判断哪个距离T更接近(结果更小)
     * <p>
     * 由于byte的第一位为符号位,所以,当00000000 ^ 11111111 = 11111111 = -127,这是差异最大的值
     * 当10000000 ^ 00000000 = 10000000 = -128,
     * 当11000000 ^ 00000000 = 11000000 = -64,
     * 所以当-127,差异最大. 其他值,如果为负数,越小差异越大; 如果为正数,越大差异越大; 负数的差异绝对大于正数和0
     *
     * @return 1: bytes1比bytes2差异更大. 0:等于. -1:bytes2比bytes1差异更大
     * 其值不能和xorResultCompare(bytes3,bytes4)的值进行差异度比较,只能作为bytes1和bytes2的差异度比较
     */
    public static int xorResultCompare(byte[] bytes1, byte[] bytes2) {
        if (bytes1.length != bytes2.length)
            throw new BTException("异或结果比较,两个byte[]长度不相同.byte1.len:+" + bytes1.length + "+,byte2.len:" + bytes2.length);
        //从最高8位(字节)开始,依次比较,只要高位小,就更接近(假设0111,低三位111值为7,最高位只要为1,值为8,所以,只要从高位开始比较即可)
        for (int i = 0; i < bytes1.length; i++) {
            byte a = bytes1[i];
            byte b = bytes2[i];
            //如果相等,跳过
            if (a == b)
                continue;
            //如果为正数,直接比较大小
            if (a >= 0 && b >= 0)
                return a > b ? 1 : 0;
                //如果为负数
            else if (a <= 0 && b <= 0)
                //如果有-127存在,直接返回,如果没有,判断大小
                return a == -127 || b == -127 ?
                        a == -127 ? 1 : 0 :
                        a < b ? 1 : 0;
                //如果一正一负(或为0),只要a比0小,则b一定>=0,直接判断大小
            else
                return a < 0 ? 1 : 0;
        }
        return 0;
    }

    /**
     * 根据异或结果分配索引
     * 该索引是自己的路由表中最大分类(分为长度为161的数组,最后一个多出的1,存储和自己nodeId相同的节点)的数组下标
     * 也就是说,当异或结果的第x位不为0时,即把这个节点存储在array[x]位置.
     */
    public static int getIndexByXorResult(byte[] bytes) {
        if (bytes.length != Config.BASIC_HASH_LEN)
            throw new BTException("根据异或结果分配索引,异或结果长度有误.xorResult.len:" + bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            //为0时退出本次循环
            if (bytes[i] == 0)
                continue;
            byte t = bytes[i];
            //如果为0,则进行下一个字节的循环
            //否则,返回对应位置的索引
            if (t < 0)
                return (i<< 3);
            if (t >= 64)
                return (i << 3) + 1;
            if (t >= 32)
                return (i << 3) + 2;
            if (t >= 16)
                return (i << 3) + 3;
            if (t >= 8)
                return (i << 3) + 4;
            if(t >= 4)
                return (i << 3) + 5;
            if(t >= 2)
                return (i << 3) + 6;
            if(t >= 1)
                return (i << 3) + 7;
        }
        //如果进行到此处,表示异或结果的byte[]全为0,也就是同一nodeId.直接返回160索引,
        return 160;
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
        byte[] a = new byte[]{0, 124, 123, -45, 65, -76, 123, 54, 43, -34, 99, -54, 32, 56, -35, 82, 73, 34, 112, 43};
        byte[] b = new byte[]{0, 124, 123, -34, 65, 43, 123, 54, 43, -34, 99, -54, 32, 56, -35, 82, 73, 34, 123, 123};
        byte[] bytes = bytesXorBytes(a, b);
        int indexByXorResult = getIndexByXorResult(bytes);

        System.out.println( 4*8);
        System.out.println(4 <<3 + 1);
    }


}
