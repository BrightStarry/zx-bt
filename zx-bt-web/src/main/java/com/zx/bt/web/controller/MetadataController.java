package com.zx.bt.web.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.zx.bt.common.enums.OrderTypeEnum;
import com.zx.bt.common.exception.BTException;
import com.zx.bt.common.service.MetadataService;
import com.zx.bt.common.util.EnumUtil;
import com.zx.bt.common.vo.MetadataVO;
import com.zx.bt.common.vo.PageVO;
import com.zx.bt.web.config.Config;
import com.zx.bt.web.form.ListByKeywordForm;
import com.zx.bt.web.service.MainService;
import com.zx.bt.web.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * author:ZhengXing
 * datetime:2018-03-11 2:52
 * Metadata 控制器
 */
@Slf4j
@Controller
@RequestMapping("/")
public class MetadataController implements ControllerPlus {
    private static final String LOG = "[MetadataController]";


    private final MetadataService metadataService;
    private final MainService mainService;

    public MetadataController(MetadataService metadataService, MainService mainService) {
        this.metadataService = metadataService;
        this.mainService = mainService;
    }


    /**
     * 根据 搜索词 排序类型 是否必须包含 分页查询
     */
    @JsonView(MetadataVO.ListView.class)
    @GetMapping("/list/{pageNo}")
    public String listByKeyword(@Valid ListByKeywordForm form, BindingResult bindingResult, Model model, HttpServletRequest request) {
        isValid(bindingResult);
        //清除两侧空格
        String keyword = form.getKeyword().trim();

        //查询
        PageVO<MetadataVO> metadataPageVO = metadataService.listByKeyword(keyword,
                EnumUtil.getByCode(form.getOrderType(), OrderTypeEnum.class).orElse(OrderTypeEnum.NONE),
                form.getIsMustContain(), form.getPageNo(), form.getPageSize());
        //将结果加入视图
        model.addAttribute("metadataPageVO", metadataPageVO);
        //将当前排序规则加入视图
        model.addAttribute("orderVO", new OrderVO(form.getOrderType(), form.getIsMustContain()));
        //如果当前页为第一页记录入库
        if(form.getPageNo().equals(Config.DEFAULT_START_PAGE_NO))
            mainService.insertKeywordRecord(getIp(request),keyword,form.getPageNo());
        return "list";
    }

    /**
     * 详情页
     */
    @JsonView(MetadataVO.DetailView.class)
    @GetMapping("/detail/{esId}")
    public String metadataDetail(@PathVariable String esId,Model model) {
        if (StringUtils.isBlank(esId)) {
            throw new BTException("参数有误");
        }
        MetadataVO metadataVO = metadataService.findOneByEsId(esId);
        if (metadataVO == null) {
            throw new BTException("种子不见鸟");
        }
        model.addAttribute("metadataVO", metadataVO);
        return "detail";
    }

}
