package com.zx.bt.web.controller.system;

import com.zx.bt.common.enums.ErrorEnum;
import com.zx.bt.web.config.Config;
import com.zx.bt.web.vo.ResultVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * author:ZhengXing
 * datetime:2018-03-11 6:39
 * 异常控制类
 */
@Controller
@RequestMapping("/error")
public class ErrorController {



    /**
     * ajax请求异常返回
     */
    @ResponseBody
    @RequestMapping(value = "/")
    public ResultVO<?> commonJson(HttpServletRequest request) {
        return ResultVO.error((String) request.getAttribute("code"), (String) request.getAttribute("message"));
    }

    /**
     * 普通请求异常返回
     */
    @RequestMapping(value = "/", produces = "text/html")
    public String commonHtml(HttpServletRequest request,Model model) {
        model.addAttribute("code", request.getAttribute("code"));
        model.addAttribute("message", request.getAttribute("message"));
        return "error/error";
    }

    /**
     * 404-页面
     */
    @RequestMapping(value = "/404",produces = "text/html")
    public String error404Html(Model model) {
        model.addAttribute("code", ErrorEnum.NOT_FOUND_ERROR.getCode());
        model.addAttribute("message", ErrorEnum.NOT_FOUND_ERROR.getMessage());
        return "error/error";
    }


    /**
     * 404-json
     */
    @ResponseBody
    @RequestMapping(value = "/404")
    public ResultVO<?> error404 () {
        return  ResultVO.error(ErrorEnum.NOT_FOUND_ERROR);
    }
}
