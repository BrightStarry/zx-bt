package com.zx.bt.web.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.zx.bt.common.entity.Metadata;
import com.zx.bt.common.enums.OrderTypeEnum;
import com.zx.bt.common.exception.BTException;
import com.zx.bt.common.service.MetadataService;
import com.zx.bt.common.util.EnumUtil;
import com.zx.bt.common.vo.MetadataVO;
import com.zx.bt.common.vo.PageVO;
import com.zx.bt.web.form.ListByKeywordForm;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

/**
 * author:ZhengXing
 * datetime:2018-03-11 2:52
 * Metadata 控制器
 */
@Controller
@RequestMapping("/")
public class MetadataController implements ControllerPlus {

    private final MetadataService metadataService;

    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }


    /**
     * 根据 搜索词 排序类型 是否必须包含 分页查询
     */
    @JsonView(MetadataVO.ListView.class)
    @GetMapping("/{keyword}/list/{pageNo}")
    public String listByKeyword(@Valid ListByKeywordForm form, BindingResult bindingResult,Model model) {
        isValid(bindingResult);
        PageVO<MetadataVO> metadataPageVO = metadataService.listByKeyword(form.getKeyword(),
                EnumUtil.getByCode(form.getOrderType(), OrderTypeEnum.class).orElse(OrderTypeEnum.NONE),
                form.getIsMustContain(), form.getPageNo(), form.getPageSize());
        model.addAttribute("metadataPageVO", metadataPageVO);

        return "list";
    }

    /**
     * 详情页
     */
    @GetMapping("/detail")
    public String list() {
        return "detail";
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
