-- 添加同步记录状态和消息字段
-- 用于支持同步历史页的状态描述、分页、搜索、重试功能

-- 添加状态字段
ALTER TABLE sync_record
ADD COLUMN status INT DEFAULT 0 NOT NULL COMMENT '同步状态：0=成功, 1=同步中, 2=失败';

-- 添加消息字段
ALTER TABLE sync_record
ADD COLUMN message VARCHAR(500) DEFAULT NULL COMMENT '同步消息或错误描述';

-- 添加索引优化查询性能
CREATE INDEX idx_sync_record_user_status ON sync_record(user_id, status);
CREATE INDEX idx_sync_record_user_created ON sync_record(user_id, create_time DESC);

-- 验证查询
SELECT * FROM sync_record WHERE user_id = ? ORDER BY create_time DESC LIMIT 10;
SELECT * FROM sync_record WHERE user_id = ? AND status = ?;
SELECT COUNT(*) FROM sync_record WHERE user_id = ? AND DATE(create_time) >= ? AND DATE(create_time) <= ?;

-- 数据初始化：将现有记录状态设为成功（0）
UPDATE sync_record SET status = 0 WHERE status IS NULL;
