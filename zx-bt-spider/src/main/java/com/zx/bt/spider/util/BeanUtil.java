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
     * Bencode专用
     * 将bean中包含的子bean也转为map
     * @return {@link LinkedHashMap<String,Object>}
     */
    @SneakyThrows
    public static <T> Map<String, Object> beanToMap(T obj) {
        //实体类转map
        Map<String, Object> map = new LinkedHashMap<>();
        //获取所有字段,包括父类字段
        Field[] fields = getAllField(obj.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            Object temp = field.get(obj);
            //基本类型,则直接赋值;  其他自定义类型,则递归
            map.put(field.getName(),
                    temp.getClass().getName().contains("java") ?
                            temp : beanToMap(temp));
        }
        return map;
    }


}
