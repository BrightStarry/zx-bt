package com.zx.bt.web.controller.system;

import com.zx.bt.common.enums.ErrorEnum;
import com.zx.bt.common.exception.BTException;
import com.zx.bt.web.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * author:ZhengXing
 * datetime:2018-03-11 6:36
 * 异常处理类
 */
@ControllerAdvice
@Slf4j
public class CustomerExceptionHandler {
    /**
     * 自定义异常处理
     */
    @ExceptionHandler(BTException.class)
    public String customExceptionHandle(BTException e, HttpServletRequest request) {
        setCodeAndMessage(request,e.getCode(),e.getMessage());
        return "forward:/error/";
    }

    /**
     * 未受检异常处理
     */
    @ExceptionHandler(Exception.class)
    public String exceptionHandler(Exception e, HttpServletRequest request) {
        log.error("[异常处理]未知异常,error={}",e);
        setCodeAndMessage(request, ErrorEnum.UNKNOWN_ERROR.getCode(),ErrorEnum.UNKNOWN_ERROR.getMessage());
        return "forward:/error/";
    }

    /**
     * 给request设置code和message属性
     */
    public void setCodeAndMessage(HttpServletRequest request, String code, String message) {
        request.setAttribute("code",code);
        request.setAttribute("message",message);
    }
}
