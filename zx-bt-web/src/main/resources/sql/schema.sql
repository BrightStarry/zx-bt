
/**
搜索关键词记录
 */
CREATE TABLE keyword_record(
  id  BIGINT   AUTO_INCREMENT COMMENT 'id',
  keyword VARCHAR(512) NOT NULL COMMENT '搜索关键词',
  ip CHAR(16) NOT NULL COMMENT '客户端ip',
  create_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
  COMMENT '创建时间',
  update_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  COMMENT '修改时间',
  PRIMARY KEY (id),
  INDEX (keyword),
  INDEX (ip)
) AUTO_INCREMENT = 1000, COMMENT = '记录用户keyword搜索记录';


