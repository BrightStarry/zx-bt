package com.zx.bt.common.vo;

import com.fasterxml.jackson.annotation.JsonView;
import com.zx.bt.common.entity.Metadata;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-03-10 11:56
 * 种子详情页VO
 */
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class MetadataVO {



    @JsonView(ListView.class)
    private Metadata metadata;

    /**
     * 长度的字符串形式, 例如 1.7G 387.3MB 16KB 等
     */
    @JsonView(ListView.class)
    private String length;

    /**
     * 文件信息对象 List
     */
    @JsonView(DetailView.class)
    private List<Info> infos;


    /**
     * 列表视图
     */
    public interface ListView{}

    /**
     * 详情视图
     */
    public interface  DetailView extends ListView{}

    public MetadataVO(Metadata metadata, String length) {
        this.metadata = metadata;
        this.length = length;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Info {
        /**
         * 名字
         * 如果是单文件为名字, 如果为多文件为路径, 如果为多文件多级路径,"/"分割,也就是文件名.
         */
        private String name;

        /**
         * 长度
         */
        private Long length;
    }

}
