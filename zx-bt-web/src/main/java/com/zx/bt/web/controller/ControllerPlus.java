package com.zx.bt.web.controller;

import com.zx.bt.common.enums.ErrorEnum;
import com.zx.bt.common.enums.OrderTypeEnum;
import com.zx.bt.common.exception.BTException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * author:ZhengXing
 * datetime:2018-03-11 0:47
 * 控制层扩展
 */
@Component
public interface ControllerPlus {

    /**
     * 分页请求对象拼接
     * pageNo从0开始
     */
    default PageRequest getPageRequest(int pageNo, int pageSize, OrderTypeEnum orderTypeEnum) {
        return new PageRequest(--pageNo, pageSize, new Sort(Sort.Direction.DESC,orderTypeEnum.getFieldName()));
    }

    /**
     * 表单验证
     * @param bindingResult 检验返回对象
     */
    default void isValid(BindingResult bindingResult) {
        //如果校验不通过,记录日志，并抛出异常
        if (bindingResult.hasErrors()) {
            throw new BTException(ErrorEnum.FORM_ERROR.getCode(), bindingResult.getFieldError().getDefaultMessage());
        }
    }


    /**
     * 获取客户端IP地址
     */
    default  String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if (ip.equals("127.0.0.1")) {
                //根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    return "";
                }
                ip = inet.getHostAddress();
            }
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ip != null && ip.length() > 15) {
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }
        return ip;
    }


}
