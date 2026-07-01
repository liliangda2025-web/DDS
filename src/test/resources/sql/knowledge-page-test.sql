DROP TABLE IF EXISTS knowledge_file;
DROP TABLE IF EXISTS knowledge_entry;

CREATE TABLE knowledge_entry (
    id BIGINT PRIMARY KEY,
    entry_type VARCHAR(50) NOT NULL,
    entry_code VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    keywords VARCHAR(500),
    version VARCHAR(50) NOT NULL,
    project_name VARCHAR(255),
    release_date DATE,
    system_source VARCHAR(255),
    profession_code VARCHAR(100),
    author_name VARCHAR(100),
    secret_level VARCHAR(50),
    info_status TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    delete_marker BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_entry_type_code_version_marker
        UNIQUE (entry_type, entry_code, version, delete_marker)
);

CREATE TABLE knowledge_file (
    id BIGINT PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_ext VARCHAR(50),
    file_size BIGINT,
    file_url VARCHAR(500) NOT NULL,
    upload_status VARCHAR(30) NOT NULL DEFAULT 'success',
    upload_error_msg VARCHAR(500),
    is_current TINYINT NOT NULL DEFAULT 1,
    uploaded_by BIGINT,
    uploaded_at TIMESTAMP NOT NULL,
    status TINYINT NOT NULL DEFAULT 1
);

-- 当前有效且带文件的图纸记录，供组合条件查询。
INSERT INTO knowledge_entry VALUES (
    2100000000000000001, 'DRAWING', 'DWG-HVAC-001', '总部办公楼暖通空调设计图1',
    '总部 暖通 空调', 'V1.0', '总部项目', DATE '2026-06-26', '图纸库', 'HVAC', '张工', '内部',
    1, 1, 0, NULL, NULL, TIMESTAMP '2026-06-30 10:00:00', TIMESTAMP '2026-06-30 10:00:00'
);
INSERT INTO knowledge_file VALUES (
    2200000000000000001, 2100000000000000001, '总部办公楼暖通空调设计图1.pdf', 'pdf', 2048000,
    'https://liliangda-oss-test.oss-cn-beijing.aliyuncs.com/knowledge/2100000000000000001/2200000000000000001.pdf',
    'success', NULL, 1, NULL,
    TIMESTAMP '2026-06-30 10:00:00', 1
);

-- 没有当前文件的正常记录必须被 LEFT JOIN 保留，并因创建时间较新排在第一位。
INSERT INTO knowledge_entry VALUES (
    2100000000000000002, 'CASE', 'CASE-001', '历史案例',
    '案例', 'V1.0', NULL, DATE '2026-06-27', '案例库', NULL, NULL, '内部',
    1, 1, 0, NULL, NULL, TIMESTAMP '2026-06-30 11:00:00', TIMESTAMP '2026-06-30 11:00:00'
);

-- 逻辑删除记录不应进入管理列表。
INSERT INTO knowledge_entry VALUES (
    2100000000000000003, 'LAW', 'LAW-001', '已删除法规',
    '法规', 'V1.0', NULL, DATE '2026-06-28', '法规库', NULL, NULL, '公开',
    1, 0, 2100000000000000003, NULL, NULL,
    TIMESTAMP '2026-06-30 12:00:00', TIMESTAMP '2026-06-30 12:00:00'
);
