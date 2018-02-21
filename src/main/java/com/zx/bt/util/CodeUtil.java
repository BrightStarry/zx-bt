package com.zx.bt.util;


import com.zx.bt.config.Config;
import com.zx.bt.exception.BTException;
import io.netty.util.CharsetUtil;
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
     * 生成一个和指定info_hash(nodeId)异或值仅相差1的info_hash(nodeId)
     */
    public static byte[] generateSimilarInfoHash(byte[] hash) {
        byte[] result = new byte[hash.length];
        System.arraycopy(hash,0,result,0,hash.length);

        byte tmp = result[hash.length - 1];
        if (tmp > -128)
            result[hash.length - 1] = (byte) (tmp - 1);
        else
            result[hash.length - 1] = (byte) (tmp + 1);
        return result;
    }

    /**
     * 包装generateSimilarInfoHash()方法参数和返回值为string
     */
    public static String generateSimilarInfoHashString(String hash) {
        return new String(generateSimilarInfoHash(hash.getBytes(CharsetUtil.ISO_8859_1)),CharsetUtil.ISO_8859_1);
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
    @Deprecated
    public static int xorResultComparexxxx(byte[] bytes1, byte[] bytes2) {
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
     * 将两个异或结果进行比较大小
     * 例如目标节点T, 两个比较节点A,B,分别将A/B和T进行异或计算,求出异或结果byteA[]/byteB[]
     * 再将这两个结果用该方法比较,判断哪个距离T更接近(结果更小)
     *
     * 将之前的比较方法修改了下,直接&0xff 转为 正整数进行比较
     *
     * @return 1: bytes1比bytes2差异更大. 0:等于. -1:bytes2比bytes1差异更大
     * 其值不能和xorResultCompare(bytes3,bytes4)的值进行差异度比较,只能作为bytes1和bytes2的差异度比较
     */
    public static int xorResultCompare(byte[] bytes1, byte[] bytes2) {
        if (bytes1.length != bytes2.length)
            throw new BTException("异或结果比较,两个byte[]长度不相同.byte1.len:+" + bytes1.length + "+,byte2.len:" + bytes2.length);
        //从最高8位(字节)开始,依次比较,只要高位小,就更接近(假设0111,低三位111值为7,最高位只要为1,值为8,所以,只要从高位开始比较即可)
        for (int i = 0; i < bytes1.length; i++) {
            int a = bytes1[i] & 0xff;
            int b = bytes2[i] & 0xff;
            if (a != b)
                return a > b ? 1 : -1;
        }
        return 0;
    }

    /**
     * 获取一个字节数组的前x位为0
     * 返回x
     * 用于比较某NodeId和自己的nodeId相同的前缀
     */
    public static int getTopNZeroByBytes(byte[] bytes) {
        int r = 0;
        while( r < bytes.length && bytes[r] == 0)
            r++;
        return r;
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

    /**
     * byte 转 byte[8](8个bit)
     */
    public static byte[] byte2Bit(byte b) {
        byte[] r = new byte[8];
        //每次循环,获取当前最后一个bit,并将该二进制右移一位(让下个循环可以直接获取到新的最后一位).
        //假设10101010, 其数组为 {1,0,1,0,1,0,1,0}
        for (int i = 7; i >=0; i--) {
            r[i] = (byte)(b & 0x1);
            b = (byte)(b >> 1);
        }

        return r;
    }

    /**
     * 获取byte[]的第x位bit
     * x 从0开始
     */
    public static byte getBitByBytes(byte[] bytes,int index) {
        byte b = bytes[index / 8];
        return byte2Bit(b)[index % 8];
    }

    public static void main(String[] args) {
        byte[] a = new byte[]{0, 124, 123, -45, 65, -76, 123, 54, 43, -34, 99, -54, 32, 56, -35, 82, 73, 34, 112, -128};
        byte[] b = new byte[]{0, 124, 123, -34, 65, 43, 123, 54, 43, -34, 99, -54, 32, 56, -35, 82, 73, 34, 123, 123};
//        byte[] bytes = bytesXorBytes(a, b);
//        int indexByXorResult = getIndexByXorResult(bytes);
//
//        System.out.println(4 * 8);
//        System.out.println(4 << 3 + 1);
//        byte[] bytes = generateSimilarInfoHash(a);

        byte[] bytes = {-1, -11, 4, 12};
        byte[] bytes2 = {-1, -12, 5, 12};

        int i = xorResultCompare(bytes, bytes2);

        byte[] bytes1 = byte2Bit((byte) 127);

    }



}
