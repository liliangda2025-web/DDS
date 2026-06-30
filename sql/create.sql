DROP TABLE IF EXISTS knowledge_entry;

CREATE TABLE knowledge_entry
(
    id              BIGINT PRIMARY KEY COMMENT '知识条目ID',

    entry_type      VARCHAR(50)  NOT NULL COMMENT '知识条目类型：DRAWING图纸库/PROGRAM_RULE程序生效准则/LAW法律法规/CASE历史案例',
    entry_code      VARCHAR(100) NOT NULL COMMENT '条目编码，图纸类对应图纸编码',
    title           VARCHAR(255) NOT NULL COMMENT '标题',

    keywords        VARCHAR(500) COMMENT '关键词，多个关键词可用空格分隔',
    version         VARCHAR(50)  NOT NULL COMMENT '版本',
    project_name    VARCHAR(255) COMMENT '所属项目',

    release_date    DATE COMMENT '发版日期',
    system_source   VARCHAR(255) COMMENT '系统来源',
    profession_code VARCHAR(100) COMMENT '专业代码',
    author_name     VARCHAR(100) COMMENT '编写人',
    secret_level    VARCHAR(50) COMMENT '密级',

    info_status     TINYINT  DEFAULT 0 COMMENT '信息完善状态：0待补充，1已完善',
    status          TINYINT  DEFAULT 1 COMMENT '状态：1正常，0删除',

    created_by      BIGINT COMMENT '创建人ID，当前阶段可为空或预留',
    updated_by      BIGINT COMMENT '修改人ID，当前阶段可为空或预留',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_entry_type_code_version (entry_type, entry_code, version),

    INDEX idx_entry_type (entry_type),
    INDEX idx_entry_code (entry_code),
    INDEX idx_title (title),
    INDEX idx_profession_code (profession_code),
    INDEX idx_release_date (release_date),
    INDEX idx_info_status (info_status),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='知识条目表';

DROP TABLE IF EXISTS knowledge_file;

CREATE TABLE knowledge_file
(
    id                 BIGINT PRIMARY KEY COMMENT '文件ID',

    entry_id           BIGINT       NOT NULL COMMENT '知识条目ID',

    original_file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_ext           VARCHAR(50) COMMENT '文件格式/扩展名',
    file_size          BIGINT COMMENT '文件大小，单位字节',

    file_url           VARCHAR(500) NOT NULL COMMENT '文件访问URL，用于预览',

    upload_status      VARCHAR(30) DEFAULT 'success' COMMENT '上传状态：success/failed',
    upload_error_msg   VARCHAR(500) COMMENT '上传失败原因',

    is_current         TINYINT     DEFAULT 1 COMMENT '是否当前文件：1是，0否',

    uploaded_by        BIGINT COMMENT '上传人ID，当前阶段可为空或预留',
    uploaded_at        DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',

    status             TINYINT     DEFAULT 1 COMMENT '状态：1正常，0删除',

    INDEX idx_entry_id (entry_id),
    INDEX idx_entry_current_status (entry_id, is_current, status),
    INDEX idx_upload_status (upload_status),
    INDEX idx_is_current (is_current),
    INDEX idx_uploaded_at (uploaded_at),
    INDEX idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='知识文件表';

INSERT INTO knowledge_entry (id, entry_type, entry_code, title,
                             keywords, version, project_name, release_date,
                             system_source, profession_code, author_name, secret_level,
                             info_status, status, created_by, updated_by, created_at, updated_at)
VALUES (2100000000000000001, 'DRAWING', 'DWG-HVAC-001', '总部办公楼暖通空调设计图',
        '总部 办公楼 暖通 空调 设计',
        'V1.0', '总部办公楼项目', '2026-06-26',
        '图纸库', 'HVAC', '张工', '内部',
        1, 1, NULL, NULL, NOW(), NOW()),
       (2100000000000000002, 'DRAWING', 'DWG-CIVIL-002', '高端小区地下车库人防工程施工图',
        '高端小区 地下车库 人防 工程 施工图',
        'V1.0', '高端小区项目', '2026-06-26',
        '图纸库', 'CIVIL', '李工', '内部',
        1, 1, NULL, NULL, NOW(), NOW()),
       (2100000000000000003, 'DRAWING', 'DWG-ARCH-003', '实验中学建筑竣工图',
        '实验中学 建筑 竣工图',
        'V1.0', '实验中学项目', '2026-06-25',
        '图纸库', 'ARCH', '王工', '普通',
        1, 1, NULL, NULL, NOW(), NOW()),
       (2100000000000000004, 'DRAWING', 'DWG-FIRE-004', '人民医院消防专项设计图',
        '人民医院 消防 专项 设计图',
        'V1.0', '人民医院项目', '2026-06-25',
        '图纸库', 'FIRE', '赵工', '内部',
        1, 1, NULL, NULL, NOW(), NOW()),
       (2100000000000000005, 'DRAWING', 'DWG-DRAIN-005', '商业综合体给排水系统图',
        '商业综合体 给排水 排水 系统图',
        'V1.0', '商业综合体项目', '2026-06-24',
        '图纸库', 'WATER', '陈工', '内部',
        1, 1, NULL, NULL, NOW(), NOW()),
       (2100000000000000006, 'PROGRAM_RULE', 'RULE-001', '图纸审查程序生效准则',
        '图纸 审查 程序 生效 准则',
        'V1.0', '审查规则库', '2026-06-20',
        '规则库', 'CHECK', '规则管理员', '内部',
        1, 1, NULL, NULL, NOW(), NOW()),
       (2100000000000000007, 'LAW', 'LAW-GB-001', '建筑设计防火规范',
        '建筑 设计 防火 规范 法律 法规',
        '2026版', '法律法规库', '2026-06-18',
        '法规库', 'FIRE', '法规管理员', '公开',
        1, 1, NULL, NULL, NOW(), NOW()),
       (2100000000000000008, 'CASE', 'CASE-CONFLICT-001', '暖通与消防管线冲突案例',
        '暖通 消防 管线 冲突 历史案例',
        'V1.0', '历史案例库', '2026-06-17',
        '案例库', 'HVAC', '案例管理员', '内部',
        1, 1, NULL, NULL, NOW(), NOW()),
       (2100000000000000009, 'DRAWING', 'TMP_2100000000000000009', '待补充图纸A',
        '待补充',
        'TEMP', NULL, NULL,
        '图纸库', NULL, NULL, NULL,
        0, 1, NULL, NULL, NOW(), NOW()),
       (2100000000000000010, 'DRAWING', 'TMP_2100000000000000010', '待补充图纸B',
        '待补充',
        'TEMP', NULL, NULL,
        '图纸库', NULL, NULL, NULL,
        0, 1, NULL, NULL, NOW(), NOW());


INSERT INTO knowledge_file (id, entry_id, original_file_name, file_ext, file_size,
                            file_url, upload_status, upload_error_msg,
                            is_current, uploaded_by, uploaded_at, status)
VALUES (2200000000000000001, 2100000000000000001,
        '总部办公楼暖通空调设计图1.pdf', 'pdf', 2048000,
        '/uploads/knowledge/2100000000000000001.pdf',
        'success', NULL, 1, NULL, NOW(), 1),
       (2200000000000000002, 2100000000000000002,
        '高端小区地下车库人防工程施工图2.pdf', 'pdf', 3145728,
        '/uploads/knowledge/2100000000000000002.pdf',
        'success', NULL, 1, NULL, NOW(), 1),
       (2200000000000000003, 2100000000000000003,
        '实验中学建筑竣工图3.pdf', 'pdf', 1572864,
        '/uploads/knowledge/2100000000000000003.pdf',
        'success', NULL, 1, NULL, NOW(), 1),
       (2200000000000000004, 2100000000000000004,
        '人民医院消防专项设计图4.pdf', 'pdf', 2621440,
        '/uploads/knowledge/2100000000000000004.pdf',
        'success', NULL, 1, NULL, NOW(), 1),
       (2200000000000000005, 2100000000000000005,
        '商业综合体给排水系统图5.pdf', 'pdf', 1992294,
        '/uploads/knowledge/2100000000000000005.pdf',
        'success', NULL, 1, NULL, NOW(), 1),
       (2200000000000000006, 2100000000000000006,
        '图纸审查程序生效准则.pdf', 'pdf', 1024000,
        '/uploads/knowledge/2100000000000000006.pdf',
        'success', NULL, 1, NULL, NOW(), 1),
       (2200000000000000007, 2100000000000000007,
        '建筑设计防火规范.pdf', 'pdf', 4096000,
        '/uploads/knowledge/2100000000000000007.pdf',
        'success', NULL, 1, NULL, NOW(), 1),
       (2200000000000000008, 2100000000000000008,
        '暖通与消防管线冲突案例.pdf', 'pdf', 1887436,
        '/uploads/knowledge/2100000000000000008.pdf',
        'success', NULL, 1, NULL, NOW(), 1),
       (2200000000000000009, 2100000000000000009,
        'f_2de134a04349.pdf', 'pdf', 1331200,
        '/uploads/knowledge/178244894958_SAP.pdf',
        'success', NULL, 1, NULL, NOW(), 1),
       (2200000000000000010, 2100000000000000010,
        'history11.pdf', 'pdf', 1459200,
        '/uploads/knowledge/history11.pdf',
        'success', NULL, 1, NULL, NOW(), 1);