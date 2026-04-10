-- 1. 用户表 sys_user
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(100) NOT NULL COMMENT '密码',
  real_name VARCHAR(64) COMMENT '真实姓名',
  phone VARCHAR(20) COMMENT '手机号',
  role VARCHAR(20) COMMENT '角色: owner/admin/worker/super_admin',
  community_id BIGINT COMMENT '归属社区ID',
  status INT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
  balance DECIMAL(10, 2) DEFAULT 0.00 COMMENT '账户余额',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_username (username),
  INDEX idx_phone (phone),
  INDEX idx_community (community_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 1.1 用户注册申请表 sys_user_register_request
CREATE TABLE IF NOT EXISTS sys_user_register_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(100) NOT NULL COMMENT '密码(加密后)',
  real_name VARCHAR(64) COMMENT '真实姓名',
  phone VARCHAR(20) COMMENT '手机号',
  role VARCHAR(20) COMMENT '申请角色: owner/admin/worker/super_admin',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/ACTIVE/REJECTED',
  community_id BIGINT COMMENT '归属社区ID',
  apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  approve_time DATETIME NULL DEFAULT NULL COMMENT '审核时间',
  approve_by BIGINT NULL DEFAULT NULL COMMENT '审核人ID',
  reject_reason VARCHAR(255) NULL DEFAULT NULL COMMENT '驳回原因',
  INDEX idx_status (status),
  INDEX idx_apply_time (apply_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户注册申请表';

-- 2. 社区表 sys_community
CREATE TABLE IF NOT EXISTS sys_community (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '社区名称',
    address VARCHAR(255) COMMENT '地址',
    linkman VARCHAR(50) COMMENT '联系人(注意：代码实体为linkman)',
    phone VARCHAR(20) COMMENT '联系电话',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区表';

-- 3. 房屋表 sys_house
CREATE TABLE IF NOT EXISTS sys_house (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    community_id BIGINT NOT NULL COMMENT '所属社区ID',
    community_name VARCHAR(100) COMMENT '社区名称(冗余)',
    building_no VARCHAR(32) NOT NULL COMMENT '楼栋号',
    house_no VARCHAR(32) NOT NULL COMMENT '房号',
    floor INT COMMENT '楼层',
    area DECIMAL(10, 2) COMMENT '面积',
    type VARCHAR(32) COMMENT '户型',
    bind_status INT DEFAULT 0 COMMENT '绑定状态: 0-未绑定, 1-已绑定',
    is_default INT DEFAULT 0 COMMENT '是否默认房屋: 0-否, 1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_community (community_id),
    INDEX idx_house_info (building_no, house_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房屋信息表';

-- 3.1 用户-房屋绑定关系表 user_house
CREATE TABLE IF NOT EXISTS user_house (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  house_id BIGINT NOT NULL COMMENT '房屋ID',
  status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/approved/rejected',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_user (user_id),
  INDEX idx_house (house_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户房屋绑定关系表';

-- 3.2 房屋绑定申请表 sys_house_bind_request
CREATE TABLE IF NOT EXISTS sys_house_bind_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '申请用户ID',
  username VARCHAR(64) NULL DEFAULT NULL COMMENT '用户名(冗余)',
  real_name VARCHAR(64) NULL DEFAULT NULL COMMENT '姓名(冗余)',
  phone VARCHAR(20) NULL DEFAULT NULL COMMENT '手机号(冗余)',
  house_id BIGINT NOT NULL COMMENT '房屋ID',
  community_id BIGINT NULL DEFAULT NULL COMMENT '社区ID',
  community_name VARCHAR(100) NULL DEFAULT NULL COMMENT '社区名称(冗余)',
  building_no VARCHAR(32) NULL DEFAULT NULL COMMENT '楼栋号(冗余)',
  house_no VARCHAR(32) NULL DEFAULT NULL COMMENT '房号(冗余)',
  identity_type VARCHAR(20) NULL DEFAULT NULL COMMENT '身份类型: OWNER/FAMILY/TENANT',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
  apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  approve_time DATETIME NULL DEFAULT NULL COMMENT '审核时间',
  approve_by BIGINT NULL DEFAULT NULL COMMENT '审核人ID',
  reject_reason VARCHAR(255) NULL DEFAULT NULL COMMENT '驳回原因',
  INDEX idx_house_status (house_id, status),
  INDEX idx_community_status (community_id, status),
  INDEX idx_apply_time (apply_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房屋绑定申请表';

-- 4. 报修表 biz_repair
CREATE TABLE IF NOT EXISTS biz_repair (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '报修人ID',
    community_id BIGINT COMMENT '归属社区ID',
    house_id BIGINT COMMENT '关联房屋ID',
    fault_type VARCHAR(64) COMMENT '故障类型',
    fault_desc TEXT COMMENT '故障描述',
    fault_imgs TEXT COMMENT '故障图片',
    contact_name VARCHAR(64) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    status VARCHAR(32) DEFAULT 'pending' COMMENT '状态: pending/processing/completed/cancelled',
    handle_remark VARCHAR(255) COMMENT '处理备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_community (community_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修记录表';

-- 5. 工单表 biz_work_order
CREATE TABLE IF NOT EXISTS biz_work_order (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  repair_id BIGINT(20) NOT NULL COMMENT '关联报修单ID',
  order_no VARCHAR(32) NOT NULL COMMENT '工单编号',
  community_id BIGINT(20) DEFAULT NULL COMMENT '所属社区ID',
  worker_id BIGINT(20) DEFAULT NULL COMMENT '维修员ID',
  worker_name VARCHAR(64) DEFAULT NULL COMMENT '维修员姓名',
  worker_phone VARCHAR(20) DEFAULT NULL COMMENT '维修员电话',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待指派, ASSIGNED-已指派, PROCESSING-处理中, COMPLETED-已完成, CANCELLED-已取消',
  priority TINYINT(4) DEFAULT '1' COMMENT '优先级: 1-普通, 2-紧急, 3-特急',
  plan_start_time DATETIME DEFAULT NULL COMMENT '计划开始时间',
  actual_start_time DATETIME DEFAULT NULL COMMENT '实际开始时间',
  actual_end_time DATETIME DEFAULT NULL COMMENT '实际结束时间',
  process_result TEXT COMMENT '处理结果',
  process_imgs TEXT COMMENT '处理后图片',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_repair_id (repair_id),
  KEY idx_worker_id (worker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- 6. 物业费表 sys_fee
CREATE TABLE IF NOT EXISTS sys_fee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    house_id BIGINT NOT NULL,
    community_id BIGINT,
    building_no VARCHAR(32),
    fee_cycle VARCHAR(20) COMMENT '费用周期(如2025-01)',
    fee_amount DECIMAL(10, 2) COMMENT '金额',
    fee_type VARCHAR(32) DEFAULT '物业费',
    status VARCHAR(20) DEFAULT 'UNPAID' COMMENT '状态: UNPAID/PAYING/PAID/OVERDUE',
    remind_count INT DEFAULT 0 COMMENT '催缴次数',
    due_date DATETIME COMMENT '截止日期',
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_house (house_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物业费表';

-- 6.1 物业缴费记录表 sys_fee_record
CREATE TABLE IF NOT EXISTS sys_fee_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fee_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    house_id BIGINT,
    pay_amount DECIMAL(10, 2) NOT NULL,
    pay_type VARCHAR(32),
    pay_time DATETIME,
    order_no VARCHAR(64) NOT NULL,
    trade_no VARCHAR(128),
    status VARCHAR(20) DEFAULT 'PENDING',
    remark VARCHAR(255),
    UNIQUE KEY uk_fee_record_order_no (order_no),
    UNIQUE KEY uk_fee_record_trade_no (trade_no),
    INDEX idx_fee_record_fee_id (fee_id),
    INDEX idx_fee_record_user_id (user_id),
    INDEX idx_fee_record_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物业缴费记录';

-- 7. 访客表 sys_visitor
CREATE TABLE IF NOT EXISTS sys_visitor (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  community_id BIGINT,
  visitor_name VARCHAR(50),
  visitor_phone VARCHAR(30),
  reason VARCHAR(255),
  visit_time DATETIME,
  car_no VARCHAR(20),
  status VARCHAR(20),
  audit_remark VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客预约';

-- 8. 投诉表 sys_complaint
CREATE TABLE IF NOT EXISTS sys_complaint (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  community_id BIGINT,
  type VARCHAR(50),
  content TEXT,
  images TEXT,
  status VARCHAR(20),
  result TEXT,
  create_time DATETIME,
  handle_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉建议';

-- 9. 活动表 sys_activity
CREATE TABLE IF NOT EXISTS sys_activity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  community_id BIGINT,
  title VARCHAR(100),
  content TEXT,
  start_time DATETIME,
  location VARCHAR(100),
  max_count INT,
  signup_count INT DEFAULT 0,
  status VARCHAR(20),
  cover_url VARCHAR(255),
  create_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区活动';

-- 10. 活动报名表 sys_activity_signup
CREATE TABLE IF NOT EXISTS sys_activity_signup (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  signup_time DATETIME,
  UNIQUE KEY uk_activity_user(activity_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动报名';

-- 11. 公告表 sys_notice
CREATE TABLE IF NOT EXISTS sys_notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    community_id BIGINT,
    community_name VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    content TEXT,
    publish_status VARCHAR(20) DEFAULT 'DRAFT',
    target_type VARCHAR(20) COMMENT 'ALL/COMMUNITY/BUILDING/USER',
    target_user_id BIGINT,
    target_building VARCHAR(32),
    publish_time DATETIME,
    expire_time DATETIME,
    top_flag TINYINT(1) DEFAULT 0,
    creator_id BIGINT,
    deleted TINYINT(1) DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- 12. 操作日志表 sys_oper_log
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(50) COMMENT '模块标题',
    business_type INT DEFAULT 0 COMMENT '业务类型',
    method VARCHAR(100) COMMENT '方法名称',
    request_method VARCHAR(10) COMMENT '请求方式',
    operator_type INT DEFAULT 0 COMMENT '操作类别',
    oper_name VARCHAR(50) COMMENT '操作人员',
    oper_url VARCHAR(255) COMMENT '请求URL',
    oper_ip VARCHAR(128) COMMENT '主机地址',
    oper_location VARCHAR(255) COMMENT '操作地点',
    oper_param TEXT COMMENT '请求参数',
    json_result TEXT COMMENT '返回参数',
    status INT DEFAULT 0 COMMENT '操作状态',
    error_msg VARCHAR(2000) COMMENT '错误消息',
    oper_time DATETIME COMMENT '操作时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录';

-- 13. 系统配置表 sys_config
CREATE TABLE IF NOT EXISTS sys_config (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(500),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 14. 停车相关表 (仅车位表示例，完整请参考 docs/sql/parking.sql)
CREATE TABLE IF NOT EXISTS biz_parking_space (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    community_id BIGINT NULL,
    community_name VARCHAR(128) NULL,
    space_no VARCHAR(64) NOT NULL COMMENT '车位编号',
    space_type VARCHAR(32) NOT NULL COMMENT '车位类型',
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    bind_user_id BIGINT NULL,
    bind_house_id BIGINT NULL,
    bind_time DATETIME NULL,
    lease_end_time DATETIME NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_community_id (community_id),
    INDEX idx_space_no (space_no)
) COMMENT '车位信息表';

CREATE TABLE IF NOT EXISTS biz_parking_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    community_id BIGINT NULL,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    space_id BIGINT NULL COMMENT '车位ID',
    plate_no VARCHAR(32) NULL COMMENT '车牌号',
    order_type VARCHAR(32) NOT NULL COMMENT '订单类型：TEMP/MONTHLY/YEARLY',
    amount DECIMAL(10,2) NOT NULL COMMENT '应付金额',
    paid_amount DECIMAL(10,2) NULL COMMENT '实付金额',
    status VARCHAR(32) NOT NULL DEFAULT 'UNPAID' COMMENT '状态：UNPAID/PAYING/PAID/CANCELLED',
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    pay_time DATETIME NULL,
    pay_channel VARCHAR(32) NULL,
    trade_no VARCHAR(128) NULL,
    pay_remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_parking_trade_no (trade_no),
    INDEX idx_parking_user (user_id),
    INDEX idx_parking_space (space_id),
    INDEX idx_parking_status (status),
    INDEX idx_parking_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='停车订单表';
