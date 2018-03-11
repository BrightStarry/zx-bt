package com.zx.bt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author:ZhengXing
 * datetime:2018-03-06 21:12
 */
@Getter
@AllArgsConstructor
public enum LengthUnitEnum implements CodeEnum<String> {

    BYTE("Byte", "Byte",1L,0),//因为需要解析的网站对字节表示不同
    B("B", "B",1L,0),
    KB("KB","KB",1024L,1),
    MB("MB", "MB",1024 * 1024L,2),
    GB("GB", "GB",1024 * 1024 * 1024L,3),
    ;
    private String code;
    private String message;
    //用于换算为字节(B)
    private Long value;
    //用于数据库字段的值
    private Integer index;

    /**
     * 根据字节长度匹配合适的单位
     */
    public static LengthUnitEnum matchLengthUnit(long byteLength) {
        if (byteLength > LengthUnitEnum.GB.getValue()) {
            return LengthUnitEnum.GB;
        } else if (byteLength > LengthUnitEnum.MB.getValue()) {
            return LengthUnitEnum.MB;
        } else if (byteLength > LengthUnitEnum.KB.getValue()) {
            return LengthUnitEnum.KB;
        } else {
            return LengthUnitEnum.B;
        }
    }

    /**
     * 将该单位的长度换算为指定单位长度
     */
    public double convert(long length, LengthUnitEnum unitEnum) {
        //大单位 转 小单位 乘
        if(this.getIndex() > unitEnum.getIndex())
            return (double) length * (this.getValue() / unitEnum.getValue());
        //否则, 除
        return (double)length / (unitEnum.getValue() / this.getValue());
    }

    /**
     * 输入一个 以字节为单位的long, 输出对应的合适的长度单位的字符
     */
    public static String convert(long byteLength) {
        //转换后的单位
        LengthUnitEnum afterUnit = matchLengthUnit(byteLength);
        //以转换后单位为单位的长度
        double length = B.convert(byteLength, afterUnit);
        return String.format("%.2f", length) + afterUnit.getCode();
    }



}
