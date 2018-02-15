package com.zx.bt.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * author:ZhengXing
 * datetime:2018-02-15 19:43
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class InfoHash {

    @Id
    @GeneratedValue()
    private Long id;

    private String content;

    public InfoHash(String content) {
        this.content = content;
    }
}
