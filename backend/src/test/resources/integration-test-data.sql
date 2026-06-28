-- 集成测试数据初始化（H2兼容）
-- 创建用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  student_id BIGINT,
  status INT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建学生档案表
CREATE TABLE IF NOT EXISTS student_profile (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  grade VARCHAR(50),
  subject_combination VARCHAR(100),
  gaokao_mode VARCHAR(50),
  target_score INT,
  dream_college VARCHAR(100),
  dream_college_batch VARCHAR(50),
  baseline_score INT,
  baseline_rank INT,
  remaining_days INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建考试记录表
CREATE TABLE IF NOT EXISTS exam_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  exam_type VARCHAR(20),
  subject_scores TEXT,
  total_score INT,
  equivalent_gaokao_score INT,
  equivalent_rank INT,
  current_batch VARCHAR(50),
  ai_diagnosis_report TEXT,
  exam_date DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建任务记录表
CREATE TABLE IF NOT EXISTS task_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  task_content TEXT,
  completion_rate DECIMAL(5,2),
  status VARCHAR(20),
  ai_comment TEXT,
  task_date DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建错题表
CREATE TABLE IF NOT EXISTS error_question (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  subject VARCHAR(50),
  knowledge_point VARCHAR(100),
  question_content TEXT,
  wrong_answer TEXT,
  correct_answer TEXT,
  ai_analysis TEXT,
  reinforcement_flag INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建成长记录表
CREATE TABLE IF NOT EXISTS growth_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  previous_batch VARCHAR(50),
  current_batch VARCHAR(50),
  score_at_upgrade INT,
  ai_incentive_text TEXT,
  upgrade_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建院校表
CREATE TABLE IF NOT EXISTS college_basic (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  college_name VARCHAR(100) NOT NULL,
  logo_path VARCHAR(255),
  admission_batch VARCHAR(50),
  subject_type VARCHAR(20),
  min_rank INT,
  max_rank INT,
  province VARCHAR(50) DEFAULT '河南',
  `year` INT DEFAULT 2025,
  last_crawled TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建一分一段表
CREATE TABLE IF NOT EXISTS score_rank (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  `year` INT NOT NULL,
  subject_type VARCHAR(20) NOT NULL,
  score INT NOT NULL,
  rank_value INT NOT NULL,
  province VARCHAR(50) DEFAULT '河南',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建题库表
CREATE TABLE IF NOT EXISTS ai_question_bank (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  subject VARCHAR(50),
  knowledge_point VARCHAR(100),
  difficulty VARCHAR(20),
  question_content TEXT NOT NULL,
  options_json TEXT,
  answer TEXT,
  analysis TEXT,
  score_range_tag VARCHAR(50),
  quality_score DECIMAL(4,2) DEFAULT 0.85,
  usage_count INT DEFAULT 0,
  source_type VARCHAR(20) DEFAULT 'AI',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 先清空已有数据（支持重复执行）
DELETE FROM sys_user;
DELETE FROM student_profile;
DELETE DELETE FROM college_basic;
DELETE FROM score_rank;

-- 密码：password123（BCrypt加密）
INSERT INTO sys_user (id, username, password, role, student_id, status) VALUES
(1, 'admin', '$2a$10$5zLGHK6AEmomhVeueoa0tOPfvc22w7sKBwH69eheOy0E/rXGD9H8i', 'admin', NULL, 1),
(2, 'student001', '$2a$10$5zLGHK6AEmomhVeueoa0tOPfvc22w7sKBwH69eheOy0E/rXGD9H8i', 'student', 1, 1),
(3, 'parent001', '$2a$10$5zLGHK6AEmomhVeueoa0tOPfvc22w7sKBwH69eheOy0E/rXGD9H8i', 'parent', 1, 1);

INSERT INTO student_profile (id, user_id, grade, subject_combination, gaokao_mode, target_score, dream_college, dream_college_batch, baseline_score, remaining_days) VALUES
(1, 2, '高二', '历史+政治+地理', '新高考3+1+2', 600, '北京大学', '985', 450, 710);

INSERT INTO college_basic (college_name, logo_path, admission_batch, subject_type, min_rank, max_rank, province, `year`) VALUES
('北京大学', '/logos/default.svg', '985', 'both', 500, 3000, '河南', 2025),
('清华大学', '/logos/default.svg', '985', 'both', 500, 3000, '河南', 2025),
('郑州大学', '/logos/default.svg', '211', 'both', 30000, 50000, '河南', 2025),
('河南大学', '/logos/default.svg', 'first_class', 'both', 60000, 90000, '河南', 2025),
('洛阳师范学院', '/logos/default.svg', 'second_class', 'both', 150000, 200000, '河南', 2025);

INSERT INTO score_rank (`year`, subject_type, score, rank_value, province) VALUES
(2025, 'history', 400, 299000, '河南'),
(2025, 'history', 450, 259000, '河南'),
(2025, 'history', 500, 215000, '河南'),
(2025, 'history', 550, 165000, '河南'),
(2025, 'history', 600, 110000, '河南'),
(2025, 'physics', 400, 371000, '河南'),
(2025, 'physics', 450, 321000, '河南'),
(2025, 'physics', 500, 265000, '河南'),
(2025, 'physics', 550, 195000, '河南'),
(2025, 'physics', 600, 120000, '河南');
