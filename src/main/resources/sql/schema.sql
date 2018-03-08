CREATE TABLE IF NOT EXISTS info_hash (
  id           BIGINT        AUTO_INCREMENT
  COMMENT 'id',
  info_hash    CHAR(40) NOT NULL
  COMMENT 'info_hash',
  peer_address VARCHAR(4096) DEFAULT ''
  COMMENT 'peer地址, ip:port形式',
  create_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
  COMMENT '创建时间',
  update_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE INDEX (info_hash)
)
  AUTO_INCREMENT = 1000, COMMENT = 'info_hash';

CREATE TABLE IF NOT EXISTS node (
  id          BIGINT      AUTO_INCREMENT
  COMMENT 'id',
  node_id     CHAR(40)    DEFAULT ''
  COMMENT 'node_id',
  ip          VARCHAR(32) DEFAULT ''
  COMMENT 'ip',
  port        INT         DEFAULT 0
  COMMENT 'ports',
  create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
  COMMENT '创建时间',
  update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  COMMENT '修改时间',
  PRIMARY KEY (id)
)
  AUTO_INCREMENT = 1000, COMMENT = 'node';

CREATE TABLE IF NOT EXISTS metadata (
  id         BIGINT         AUTO_INCREMENT
  COMMENT 'id',
  info_hash  CHAR(40)     NOT NULL
  COMMENT 'info_hash',
  info_string VARCHAR(20000) DEFAULT ''
  COMMENT '文件信息,json, infos字段',
  name       VARCHAR(1024) DEFAULT ''
  COMMENT '名字',
  length     BIGINT UNSIGNED NOT NULL
  COMMENT '总长度(所有文件相加长度)',
  type       TINYINT      NOT NULL
  COMMENT '类型: 0:从peer处获取; 1:从www.zhongzidi.com获取;',
  create_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
  COMMENT '创建时间',
  update_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY (info_hash),
  KEY(length),
  KEY(create_time)
)
  AUTO_INCREMENT = 1000, COMMENT = 'Metadata';





