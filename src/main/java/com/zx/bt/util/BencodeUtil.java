package com.zx.bt.util;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018-02-13 14:57
 * BitTorrent专用的Bencode编码方式
 */
public class BencodeUtil {
    /**
     * github找的,看了下,是线程安全,并且默认为UTF-8编码
     */
    private static final Bencode bencode = new Bencode();









    public static void main(String[] args) {

//        //编码
//        byte[] encoded = bencode.encode(new HashMap<Object, Object>() {{
//            put("string", "value");
//            put("number", 123456);
//            put("list", new ArrayList<Object>() {{
//                add("list-item-1");
//                add("list-item-2");
//            }});
//            put("dict", new ConcurrentSkipListMap<Integer,Object>() {{
//                put(123, "test");
//                put(456, "thing");
//            }});
//        }});
//        String encodeStr = new String(encoded, bencode.getCharset());
//        System.out.println(encodeStr);
//
//        //解码
//        Bencode bencode = new Bencode();
//        Map<String, Object> dict = bencode.decode(encodeStr.getBytes(), Type.DICTIONARY);
//
//        System.out.println(dict);

        byte[] encoded = bencode.encode(new HashMap<Object, Object>() {{
            put("t", "aa");
            put("y", "q");
            put("q", "find_node");
            put("a", new HashMap<Object, Object>() {{
                put("id","abcdefghij0123456789");
                put("target","mnopqrstuvwxyz123456");
            }});
        }});
        String encodeStr = new String(encoded, bencode.getCharset());
        System.out.println(encodeStr);

        Map<String, Object> map = bencode.decode(encoded, Type.DICTIONARY);
        System.out.println(map);




    }

}
