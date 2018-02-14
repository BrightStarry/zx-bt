package com.zx.bt.util;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.zx.bt.dto.Ping;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;

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
        byte a = (byte)1;
        byte b = (byte)1;
        System.out.println(Integer.toString(a & 0xFF));
        System.out.println(Integer.toString(a & 0xFF | (b & 0xff) << 8));
    }
}
