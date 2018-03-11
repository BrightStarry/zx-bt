package com.zx.bt.web.vo;

import com.zx.bt.common.enums.ErrorEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * author:ZhengXing
 * datetime:2018-03-11 6:46
 * 标准返回对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ResultVO<T> {

    private String code;

    private String message;

    private T data;



    public static ResultVO<?> success() {
        return new ResultVO<>(ErrorEnum.SUCCESS.getCode(), ErrorEnum.SUCCESS.getMessage());
    }

    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<T>(ErrorEnum.SUCCESS.getCode(), ErrorEnum.SUCCESS.getMessage()).setData(data);
    }

    public static ResultVO<?> error(ErrorEnum errorEnum) {
        return new ResultVO<>(errorEnum.getCode(), errorEnum.getMessage());
    }

    public static ResultVO<?> error(String message) {
        return new ResultVO<>(ErrorEnum.COMMON_ERROR.getCode(),message);
    }

    public static ResultVO<?> error(String code,String message) {
        return new ResultVO<>(code,message);
    }

    public ResultVO(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
