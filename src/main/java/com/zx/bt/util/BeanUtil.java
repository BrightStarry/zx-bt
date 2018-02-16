package com.zx.bt.util;

import com.zx.bt.entity.Node;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018-02-14 11:17
 * bean的操作
 */
public class BeanUtil {

    /**
     * 获取自己和父类中的所有字段
     */
    public static <T> Field[] getAllField(Class<T> tClass) {
        Field[] fields = tClass.getDeclaredFields();
        for (Class<? super T> fClass = tClass.getSuperclass(); fClass !=null;fClass = fClass.getSuperclass()){
            Field[] fFields = fClass.getDeclaredFields();
            fields = ArrayUtils.addAll(fields, fFields);
        }
        return fields;
    }

    /**
     * Bean 转 Map
     */
    @SneakyThrows
    public static <T> Map<Object, Object> beanToMap(T obj) {
        //实体类转map
        Map<Object, Object> map = new HashMap<>();

        Class<?> tClass = obj.getClass();
        //获取所有字段,包括父类字段
        Field[] fields = getAllField(tClass);

        for (Field field : fields) {
            field.setAccessible(true);
            Object temp = field.get(obj);
            //基本类型,则直接赋值
            if(temp.getClass().getName().contains("java"))
                map.put(field.getName(),temp);
            //其他自定义类型 TODO 可能还要扩展
            else{
                //递归
                Map<Object, Object> map1 = beanToMap(temp);
                map.put(field.getName(), map1);
            }
        }
        return map;
    }

    /**
     * map<String,Object> 转 bean
     */
    @Deprecated
    public static <T> T mapToBean(Map<String, Object> map,T obj) {
        Class<?> tClass = obj.getClass();
        Field[] fields = tClass.getDeclaredFields();
        //将每个字段的name提取为另一个数组
        String[] fieldNames = Arrays.stream(fields).map(Field::getName).toArray(String[]::new);
        for (Map.Entry<String, Object> item : map.entrySet()) {
            if(item.getValue().getClass().getName().contains("Map")){

            }
        }
        return null;
    }
    public static void main(String[] args) {
//        String nodeId = BTUtil.generateNodeIdString();
//        byte[] bytes = new byte[26];
//        for (int i = 0; i < 20; i++) {
//            bytes[i] = 111;
//        }
//        bytes[20] = 106;
//        bytes[21] = 14;
//        bytes[22] = 7;
//        bytes[23] = 29;
//
//        byte[] port = CodeUtil.int2TwoBytes(44451);
//        bytes[24] = port[0];
//        bytes[25] = port[1];
//
//        Node node = new Node(bytes);
//        System.out.println(node);

       //
       byte[] a = {19, 66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114,
               111, 116, 111, 99, 111, 108, 0, 0, 0, 0, 0, 16, 0, 1};
        String s = new String(a, CharsetUtil.US_ASCII);
        System.out.println(s);
    }

}
