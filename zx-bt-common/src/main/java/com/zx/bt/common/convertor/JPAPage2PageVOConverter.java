package com.zx.bt.common.convertor;

import com.zx.bt.common.vo.PageVO;
import org.springframework.data.domain.Page;

import java.util.ArrayList;

/**
 * author:ZhengXing
 * datetime:2018-03-11 0:37
 *  JPA的page对象转换为自定义的{@link com.zx.bt.common.vo.PageVO}对象
 */
public class JPAPage2PageVOConverter {

    /**
     * 一对一转换
     * @param page
     * @param <T>
     * @return
     */
    public static <T> PageVO<T> convert(Page<T> page) {
        int pageNo = page.getNumber();

        PageVO<T> pageVO = new PageVO<>(++pageNo, page.getSize(), page.getTotalElements(), page.getTotalPages(), page.getContent());
        //防止totalPage为0
        pageVO.setTotalPage(pageVO.getTotalPage() == 0 ? 1 : pageVO.getTotalPage());
        return pageVO;
    }
}
