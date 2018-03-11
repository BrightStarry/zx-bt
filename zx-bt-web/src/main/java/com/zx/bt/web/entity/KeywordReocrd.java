package com.zx.bt.web.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;

/**
 * author:ZhengXing
 * datetime:2018-03-12 1:06
 * 关键词搜索记录
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@DynamicUpdate
public class KeywordReocrd {

    private Long id;

    private String keyword;

    private String ip;
}
