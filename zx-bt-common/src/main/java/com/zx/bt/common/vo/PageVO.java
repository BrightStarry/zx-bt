package com.zx.bt.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-03-11 0:14
 * 分页视图
 */
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class PageVO<T> {
    /**当前页*/
    private Integer pageNo;
    /**每页条数*/
    private Integer pageSize;
    /**总记录数*/
    private Long totalElement;
    /**总页数*/
    private Integer totalPage;
    /**数据*/
    private List<T> list;
    /**当前id*/
    private Long id;
    /**当前搜索关键字*/
    private String keyword;

    public PageVO(Integer pageNo, Integer pageSize, Long totalElement, Integer totalPage, List<T> list,String keyword) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalElement = totalElement;
        this.totalPage = totalPage;
        this.list = list;
        this.keyword = keyword;
    }

    public PageVO(Integer pageNo, Integer pageSize, Long totalElement, Integer totalPage, List<T> list) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalElement = totalElement;
        this.totalPage = totalPage;
        this.list = list;
    }
}
