package com.zx.bt.spider.util;

import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
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
    public static <T> Map<String, Object> beanToMap(T obj) {
        //实体类转map
        Map<String, Object> map = new LinkedHashMap<>();

        Class<?> tClass = obj.getClass();
        //获取所有字段,包括父类字段
        Field[] fields = getAllField(tClass);

        for (Field field : fields) {
            field.setAccessible(true);
            Object temp = field.get(obj);

            //基本类型,则直接赋值
            if(temp.getClass().getName().contains("java"))
                map.put(field.getName(),temp);
            //其他自定义类型
            else{
                //递归
                Map<String, Object> map1 = beanToMap(temp);
                map.put(field.getName(), map1);
            }
        }
        return map;
    }


}
