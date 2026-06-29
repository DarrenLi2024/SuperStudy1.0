-- =============================================
-- SuperStudy 生产数据库迁移脚本 v1.0→v1.1
-- 日期: 2026-06-29
-- 说明: 将 v1.0 schema 升级至 v1.1
-- 用法: mysql -h <host> -u <user> -p < deploy/migration-2026-06-29.sql
-- =============================================

-- 1. ai_question_bank 补充列
ALTER TABLE ai_question_bank ADD COLUMN IF NOT EXISTS options_json JSON COMMENT '选项JSON' AFTER question_content;
ALTER TABLE ai_question_bank ADD COLUMN IF NOT EXISTS analysis TEXT COMMENT '解析' AFTER answer;
ALTER TABLE ai_question_bank ADD COLUMN IF NOT EXISTS quality_score DECIMAL(4,2) DEFAULT 0.85 COMMENT '质量评分' AFTER score_range_tag;
ALTER TABLE ai_question_bank ADD COLUMN IF NOT EXISTS usage_count INT DEFAULT 0 COMMENT '使用次数' AFTER quality_score;
ALTER TABLE ai_question_bank ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) DEFAULT 'AI' COMMENT '来源类型' AFTER usage_count;

-- 2. college_basic 补充列
ALTER TABLE college_basic ADD COLUMN IF NOT EXISTS min_rank INT COMMENT '稳妥位次下界' AFTER subject_type;
ALTER TABLE college_basic ADD COLUMN IF NOT EXISTS max_rank INT COMMENT '稳妥位次上界' AFTER min_rank;
ALTER TABLE college_basic ADD COLUMN IF NOT EXISTS province VARCHAR(50) DEFAULT '河南' COMMENT '适用省份' AFTER max_rank;
ALTER TABLE college_basic ADD COLUMN IF NOT EXISTS `year` INT DEFAULT 2025 COMMENT '参考年份' AFTER province;

-- 3. error_question 补充列
ALTER TABLE error_question ADD COLUMN IF NOT EXISTS correct_answer TEXT COMMENT '正确答案' AFTER wrong_answer;

-- 4. 修复 student_profile 关联
UPDATE student_profile SET user_id=2 WHERE user_id=1 LIMIT 1;
INSERT IGNORE INTO student_profile (user_id, grade, subject_combination, gaokao_mode, target_score, dream_college, dream_college_batch, baseline_score, baseline_rank, remaining_days)
SELECT 2, '高二', '历史+政治+地理', '新高考3+1+2', 600, '北京大学', '985', 450, 96500, 710
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id=2);

-- 5. 更新 BCrypt 密码哈希
UPDATE sys_user SET password='$2a$10$5zLGHK6AEmomhVeueoa0tOPfvc22w7sKBwH69eheOy0E/rXGD9H8i' WHERE username IN ('admin','student001','parent001');

-- 6. 院校位次范围
UPDATE college_basic SET province='河南' WHERE province IS NULL;
UPDATE college_basic SET `year`=2025 WHERE `year` IS NULL;
UPDATE college_basic SET min_rank=500, max_rank=14000 WHERE admission_batch='985' AND min_rank IS NULL;
UPDATE college_basic SET min_rank=10000, max_rank=58000 WHERE admission_batch='211' AND min_rank IS NULL;
UPDATE college_basic SET min_rank=40000, max_rank=95000 WHERE admission_batch='first_class' AND min_rank IS NULL;
UPDATE college_basic SET min_rank=100000, max_rank=200000 WHERE admission_batch='second_class' AND min_rank IS NULL;
UPDATE college_basic SET min_rank=500, max_rank=14000 WHERE admission_batch LIKE '%985%' AND min_rank IS NULL;
UPDATE college_basic SET min_rank=10000, max_rank=58000 WHERE admission_batch LIKE '%211%' AND min_rank IS NULL;
UPDATE college_basic SET min_rank=40000, max_rank=95000 WHERE (admission_batch LIKE '%一本%' OR admission_batch='first_class') AND min_rank IS NULL;
UPDATE college_basic SET min_rank=100000, max_rank=200000 WHERE (admission_batch LIKE '%二本%' OR admission_batch='second_class') AND min_rank IS NULL;

-- 7. 补充 2024 一分一段数据
INSERT IGNORE INTO score_rank (`year`, subject_type, score, rank_value, province) VALUES
(2024, 'history', 600, 7200, '河南'),
(2024, 'history', 550, 24800, '河南'),
(2024, 'history', 500, 55500, '河南'),
(2024, 'history', 450, 98500, '河南'),
(2024, 'history', 400, 154000, '河南'),
(2024, 'history', 350, 222000, '河南'),
(2024, 'history', 300, 301000, '河南'),
(2024, 'physics', 600, 19000, '河南'),
(2024, 'physics', 550, 47500, '河南'),
(2024, 'physics', 500, 88000, '河南'),
(2024, 'physics', 450, 141000, '河南'),
(2024, 'physics', 400, 206000, '河南'),
(2024, 'physics', 350, 284000, '河南'),
(2024, 'physics', 300, 374000, '河南');

SELECT '✅ Migration v1.0→v1.1 complete' AS status;
