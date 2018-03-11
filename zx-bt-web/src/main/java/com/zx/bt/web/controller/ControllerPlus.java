package com.zx.bt.web.controller;

import com.zx.bt.common.enums.ErrorEnum;
import com.zx.bt.common.enums.OrderTypeEnum;
import com.zx.bt.common.exception.BTException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

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
}
