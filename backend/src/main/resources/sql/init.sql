CREATE DATABASE IF NOT EXISTS superstudy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE superstudy;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) UNIQUE NOT NULL COMMENT '账号',
  password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
  role ENUM('student', 'parent', 'admin') NOT NULL COMMENT '角色',
  student_id BIGINT COMMENT '绑定学生ID',
  status TINYINT DEFAULT 1 COMMENT '启用状态：1-启用 0-禁用',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号表';

CREATE TABLE IF NOT EXISTS student_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '档案ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  grade VARCHAR(20) COMMENT '年级',
  subject_combination VARCHAR(50) COMMENT '选科组合',
  gaokao_mode VARCHAR(50) COMMENT '高考模式',
  target_score INT COMMENT '目标总分',
  dream_college VARCHAR(100) COMMENT '心仪院校名称',
  dream_college_batch VARCHAR(50) COMMENT '心仪院校批次',
  baseline_score INT COMMENT '基线总分',
  baseline_rank INT COMMENT '基线等效位次',
  remaining_days INT COMMENT '剩余备考天数（AI自动计算）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生档案表';

CREATE TABLE IF NOT EXISTS exam_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考试记录ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  exam_type ENUM('weekly', 'monthly') COMMENT '考试类型',
  subject_scores JSON COMMENT '各科分数（JSON格式）',
  total_score INT COMMENT '总分',
  equivalent_gaokao_score INT COMMENT '等效高考分',
  equivalent_rank INT COMMENT '等效三年位次',
  current_batch VARCHAR(50) COMMENT '当前稳妥批次',
  exam_date DATE COMMENT '考试日期',
  ai_diagnosis_report TEXT COMMENT 'AI诊断报告',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student_date (student_id, exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试记录表';

CREATE TABLE IF NOT EXISTS task_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  task_date DATE NOT NULL COMMENT '任务日期',
  task_content JSON COMMENT '任务内容（JSON格式）',
  completion_rate DECIMAL(5,2) COMMENT '完成率',
  ai_comment TEXT COMMENT 'AI点评',
  status ENUM('pending', 'completed', 'partial') DEFAULT 'pending' COMMENT '完成状态',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student_date (student_id, task_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI每日任务打卡表';

CREATE TABLE IF NOT EXISTS error_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '错题ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  subject VARCHAR(20) COMMENT '学科',
  knowledge_point VARCHAR(100) COMMENT '知识点',
  question_content TEXT COMMENT '题目内容',
  wrong_answer TEXT COMMENT '错误答案',
  ai_analysis TEXT COMMENT 'AI错因分析',
  reinforcement_flag TINYINT DEFAULT 0 COMMENT '补强训练标记',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student_subject (student_id, subject)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题归集表';

CREATE TABLE IF NOT EXISTS growth_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成长记录ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  previous_batch VARCHAR(50) COMMENT '升级前批次',
  current_batch VARCHAR(50) COMMENT '升级后批次',
  score_at_upgrade INT COMMENT '升级时总分',
  upgrade_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '升级时间',
  ai_incentive_text TEXT COMMENT 'AI激励文案',
  INDEX idx_student_time (student_id, upgrade_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='段位升级记录表';

CREATE TABLE IF NOT EXISTS college_basic (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '院校ID',
  college_name VARCHAR(100) NOT NULL COMMENT '院校名称',
  logo_path VARCHAR(255) COMMENT 'LOGO存储路径',
  admission_batch VARCHAR(50) COMMENT '录取批次',
  subject_type ENUM('physics', 'history', 'both') COMMENT '科类',
  min_rank INT COMMENT '稳妥位次下界',
  max_rank INT COMMENT '稳妥位次上界',
  province VARCHAR(50) DEFAULT '河南' COMMENT '适用省份',
  year INT DEFAULT 2025 COMMENT '参考年份',
  last_crawled TIMESTAMP COMMENT '抓取更新时间',
  UNIQUE KEY uk_name (college_name),
  INDEX idx_batch (admission_batch)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院校素材自治表';

CREATE TABLE IF NOT EXISTS score_rank (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  year INT NOT NULL COMMENT '年份',
  subject_type ENUM('physics', 'history') COMMENT '科类',
  score INT NOT NULL COMMENT '分数',
  rank_value INT NOT NULL COMMENT '对应位次',
  province VARCHAR(50) DEFAULT '未知' COMMENT '省份',
  UNIQUE KEY uk_year_subject_score (year, subject_type, score),
  INDEX idx_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='一分一段自治表';

CREATE TABLE IF NOT EXISTS ai_question_bank (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
  subject VARCHAR(20) NOT NULL COMMENT '学科',
  knowledge_point VARCHAR(100) COMMENT '知识点',
  difficulty ENUM('basic', 'medium', 'hard') COMMENT '难度',
  question_content TEXT NOT NULL COMMENT '题目内容',
  options_json JSON COMMENT '选项JSON',
  answer TEXT COMMENT '答案',
  analysis TEXT COMMENT '解析',
  score_range_tag VARCHAR(50) COMMENT '适配分数段标签',
  quality_score DECIMAL(4,2) DEFAULT 0.85 COMMENT '质量评分',
  usage_count INT DEFAULT 0 COMMENT '使用次数',
  source_type VARCHAR(20) DEFAULT 'AI' COMMENT '来源类型',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_subject_difficulty (subject, difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI动态题库表';

-- ========================
-- 初始用户数据
-- ========================
INSERT INTO sys_user (username, password, role, student_id, status) VALUES
('admin', '$2a$10$5zLGHK6AEmomhVeueoa0tOPfvc22w7sKBwH69eheOy0E/rXGD9H8i', 'admin', NULL, 1),
('student001', '$2a$10$5zLGHK6AEmomhVeueoa0tOPfvc22w7sKBwH69eheOy0E/rXGD9H8i', 'student', 1, 1),
('parent001', '$2a$10$5zLGHK6AEmomhVeueoa0tOPfvc22w7sKBwH69eheOy0E/rXGD9H8i', 'parent', 1, 1);

INSERT INTO student_profile (user_id, grade, subject_combination, gaokao_mode, target_score, dream_college, dream_college_batch, baseline_score, remaining_days) VALUES
(2, '高二', '历史+政治+地理', '新高考3+1+2', 600, '北京大学', '双一流', 450, 710);

-- ========================
-- 院校种子数据
-- ========================
INSERT INTO college_basic (college_name, logo_path, admission_batch, subject_type, min_rank, max_rank, province, `year`) VALUES
-- 985/双一流
('北京大学', '/logos/default.svg', '985', 'both', 500, 3000, '河南', 2025),
('清华大学', '/logos/default.svg', '985', 'both', 500, 3000, '河南', 2025),
('复旦大学', '/logos/default.svg', '985', 'both', 1500, 5000, '河南', 2025),
('上海交通大学', '/logos/default.svg', '985', 'both', 1500, 5000, '河南', 2025),
('浙江大学', '/logos/default.svg', '985', 'both', 2000, 6000, '河南', 2025),
('南京大学', '/logos/default.svg', '985', 'both', 2500, 6500, '河南', 2025),
('武汉大学', '/logos/default.svg', '985', 'both', 5000, 12000, '河南', 2025),
('中山大学', '/logos/default.svg', '985', 'both', 6000, 14000, '河南', 2025),
-- 211（物理类位次前50000，历史类前30000）
('郑州大学', '/logos/default.svg', '211', 'both', 10000, 35000, '河南', 2025),
('南昌大学', '/logos/default.svg', '211', 'both', 15000, 45000, '河南', 2025),
('云南大学', '/logos/default.svg', '211', 'both', 18000, 48000, '河南', 2025),
('广西大学', '/logos/default.svg', '211', 'both', 20000, 50000, '河南', 2025),
('贵州大学', '/logos/default.svg', '211', 'both', 20000, 50000, '河南', 2025),
('海南大学', '/logos/default.svg', '211', 'both', 25000, 55000, '河南', 2025),
('宁夏大学', '/logos/default.svg', '211', 'both', 28000, 58000, '河南', 2025),
-- 普通一本
('河南大学', '/logos/default.svg', 'first_class', 'both', 40000, 80000, '河南', 2025),
('河北师范大学', '/logos/default.svg', 'first_class', 'both', 45000, 85000, '河南', 2025),
('山西大学', '/logos/default.svg', 'first_class', 'both', 45000, 85000, '河南', 2025),
('安徽师范大学', '/logos/default.svg', 'first_class', 'both', 50000, 90000, '河南', 2025),
('福建师范大学', '/logos/default.svg', 'first_class', 'both', 50000, 90000, '河南', 2025),
('江西师范大学', '/logos/default.svg', 'first_class', 'both', 55000, 95000, '河南', 2025),
('湖北大学', '/logos/default.svg', 'first_class', 'both', 50000, 90000, '河南', 2025),
('湖南科技大学', '/logos/default.svg', 'first_class', 'both', 55000, 95000, '河南', 2025),
-- 公办二本
('洛阳师范学院', '/logos/default.svg', 'second_class', 'both', 100000, 160000, '河南', 2025),
('安阳师范学院', '/logos/default.svg', 'second_class', 'both', 110000, 170000, '河南', 2025),
('南阳理工学院', '/logos/default.svg', 'second_class', 'both', 110000, 170000, '河南', 2025),
('信阳师范大学', '/logos/default.svg', 'second_class', 'both', 120000, 180000, '河南', 2025),
('周口师范学院', '/logos/default.svg', 'second_class', 'both', 130000, 190000, '河南', 2025),
('黄淮学院', '/logos/default.svg', 'second_class', 'both', 130000, 190000, '河南', 2025),
('平顶山学院', '/logos/default.svg', 'second_class', 'both', 140000, 200000, '河南', 2025);

-- ========================
-- 一分一段种子数据（河南省 2025 历史类模拟）
-- ========================
INSERT INTO score_rank (year, subject_type, score, rank_value, province) VALUES
(2025, 'history', 650, 1200, '河南'),
(2025, 'history', 640, 1800, '河南'),
(2025, 'history', 630, 2600, '河南'),
(2025, 'history', 620, 3800, '河南'),
(2025, 'history', 610, 5200, '河南'),
(2025, 'history', 600, 7000, '河南'),
(2025, 'history', 590, 9200, '河南'),
(2025, 'history', 580, 12000, '河南'),
(2025, 'history', 570, 15500, '河南'),
(2025, 'history', 560, 19500, '河南'),
(2025, 'history', 550, 24000, '河南'),
(2025, 'history', 540, 29000, '河南'),
(2025, 'history', 530, 34500, '河南'),
(2025, 'history', 520, 40500, '河南'),
(2025, 'history', 510, 47000, '河南'),
(2025, 'history', 500, 54000, '河南'),
(2025, 'history', 490, 61500, '河南'),
(2025, 'history', 480, 69500, '河南'),
(2025, 'history', 470, 78000, '河南'),
(2025, 'history', 460, 87000, '河南'),
(2025, 'history', 450, 96500, '河南'),
(2025, 'history', 440, 106500, '河南'),
(2025, 'history', 430, 117000, '河南'),
(2025, 'history', 420, 128000, '河南'),
(2025, 'history', 410, 139500, '河南'),
(2025, 'history', 400, 151500, '河南'),
(2025, 'history', 390, 164000, '河南'),
(2025, 'history', 380, 177000, '河南'),
(2025, 'history', 370, 190500, '河南'),
(2025, 'history', 360, 204500, '河南'),
(2025, 'history', 350, 219000, '河南'),
(2025, 'history', 340, 234000, '河南'),
(2025, 'history', 330, 249500, '河南'),
(2025, 'history', 320, 265500, '河南'),
(2025, 'history', 310, 282000, '河南'),
(2025, 'history', 300, 299000, '河南');

INSERT INTO score_rank (year, subject_type, score, rank_value, province) VALUES
(2025, 'physics', 680, 800, '河南'),
(2025, 'physics', 670, 1400, '河南'),
(2025, 'physics', 660, 2400, '河南'),
(2025, 'physics', 650, 3800, '河南'),
(2025, 'physics', 640, 5600, '河南'),
(2025, 'physics', 630, 8000, '河南'),
(2025, 'physics', 620, 11000, '河南'),
(2025, 'physics', 610, 14500, '河南'),
(2025, 'physics', 600, 18500, '河南'),
(2025, 'physics', 590, 23000, '河南'),
(2025, 'physics', 580, 28000, '河南'),
(2025, 'physics', 570, 33500, '河南'),
(2025, 'physics', 560, 39500, '河南'),
(2025, 'physics', 550, 46000, '河南'),
(2025, 'physics', 540, 53000, '河南'),
(2025, 'physics', 530, 60500, '河南'),
(2025, 'physics', 520, 68500, '河南'),
(2025, 'physics', 510, 77000, '河南'),
(2025, 'physics', 500, 86000, '河南'),
(2025, 'physics', 490, 95500, '河南'),
(2025, 'physics', 480, 105500, '河南'),
(2025, 'physics', 470, 116000, '河南'),
(2025, 'physics', 460, 127000, '河南'),
(2025, 'physics', 450, 138500, '河南'),
(2025, 'physics', 440, 150500, '河南'),
(2025, 'physics', 430, 163000, '河南'),
(2025, 'physics', 420, 176000, '河南'),
(2025, 'physics', 410, 189500, '河南'),
(2025, 'physics', 400, 203500, '河南'),
(2025, 'physics', 390, 218000, '河南'),
(2025, 'physics', 380, 233000, '河南'),
(2025, 'physics', 370, 248500, '河南'),
(2025, 'physics', 360, 264500, '河南'),
(2025, 'physics', 350, 281000, '河南'),
(2025, 'physics', 340, 298000, '河南'),
(2025, 'physics', 330, 315500, '河南'),
(2025, 'physics', 320, 333500, '河南'),
(2025, 'physics', 310, 352000, '河南'),
(2025, 'physics', 300, 371000, '河南');

-- 2024年一分一段数据（近三年参考）
INSERT INTO score_rank (year, subject_type, score, rank_value, province) VALUES
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

COMMIT;
