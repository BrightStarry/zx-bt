package com.zx.bt.common.dto;

import com.zx.bt.common.entity.Metadata;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-03-10 11:56
 */
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class MetadataDTO {

    private Metadata metadata;

    /**
     * 文件信息对象 List
     */
    private List<Info> infos;

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
