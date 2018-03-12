package com.zx.bt.web.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * author:ZhengXing
 * datetime:2018/3/12 0012 16:37
 * ip黑名单
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class BlackIp {
	@Id
	@GeneratedValue
	private Long id;

	private String ip;
}
