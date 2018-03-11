package com.zx.bt.web.form;

import com.zx.bt.common.enums.OrderTypeEnum;
import com.zx.bt.web.controller.MetadataController;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;
import org.springframework.validation.BindingResult;

import javax.validation.Valid;

/**
 * author:ZhengXing
 * datetime:2018-03-11 5:44
 * see {@link MetadataController#listByKeyword(ListByKeywordForm, BindingResult)} ()}
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ListByKeywordForm extends PageForm {

    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索词不能为空")
    private String keyword;

    /**
     * 排序规则
     */
    @Range(min = 0,max = 4,message = "排序规则序号不正确(0-4)")
    private Integer orderType = OrderTypeEnum.NONE.getCode();


    /**
     * 是否必须包含关键词
     */
    private Boolean isMustContain = Boolean.FALSE;
}
