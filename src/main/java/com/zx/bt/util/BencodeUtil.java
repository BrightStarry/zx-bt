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




    }

}
