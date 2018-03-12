package com.zx.bt.web.form;

import com.zx.bt.web.config.Config;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018-03-11 0:54
 * 分页请求 form
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class PageForm {


    private static int defaultPageSize;

    @Autowired
    public void init(Config config) {
        PageForm.defaultPageSize = config.getWeb().getPageSize();
    }

    @Range(min = 1,max = Integer.MAX_VALUE,message = "当前页码范围不正确(1-2147483647)")
    protected Integer pageNo = 1;

    @Range(min = 5,max = 50,message = "每页记录数范围不正确(5-50)")
    protected Integer pageSize = defaultPageSize;
}
