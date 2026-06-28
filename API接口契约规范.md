# 高中全科AI升学成长陪伴系统 - API接口契约规范

> 所有AI开发人员必须严格遵循此接口契约规范

---

## 一、接口基础规范

### 1.1 统一返回格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 1.2 错误码定义
| 错误码 | 含义 | HTTP状态码 |
|-------|------|-----------|
| 200 | 成功 | 200 |
| 400 | 请求参数错误 | 200 |
| 401 | 未认证（Token失效） | 401 |
| 403 | 无权限 | 403 |
| 404 | 资源不存在 | 200 |
| 500 | 服务器内部错误 | 500 |
| 503 | AI服务暂时不可用 | 200 |

### 1.3 接口版本控制
- 所有接口路径统一前缀：`/api/v1`

### 1.4 请求方法规范
| 操作 | HTTP方法 | 示例 |
|-----|---------|------|
| 查询列表 | GET | /api/v1/users |
| 查询单个 | GET | /api/v1/users/{id} |
| 新增 | POST | /api/v1/users |
| 更新 | PUT | /api/v1/users/{id} |
| 删除 | DELETE | /api/v1/users/{id} |

---

## 二、用户权限接口

### 2.1 登录接口
**路径**: `POST /api/v1/user/login`  
**描述**: 用户登录，返回JWT Token

**请求体**:
```json
{
  "username": "string",
  "password": "string",
  "captcha": "string"
}
```

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "string",
    "expireTime": "string",
    "user": {
      "id": "number",
      "username": "string",
      "role": "student|parent|admin",
      "studentId": "number"
    }
  }
}
```

### 2.2 获取当前用户信息
**路径**: `GET /api/v1/user/info`  
**描述**: 获取当前登录用户信息

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "number",
    "username": "string",
    "role": "student|parent|admin",
    "studentId": "number",
    "status": "number"
  }
}
```

### 2.3 退出登录
**路径**: `POST /api/v1/user/logout`  
**描述**: 用户退出登录，清除Token

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 三、学生档案接口

### 3.1 获取学生档案
**路径**: `GET /api/v1/student/profile/{studentId}`  
**描述**: 获取学生档案信息

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "number",
    "userId": "number",
    "grade": "string",
    "subjectCombination": "string",
    "gaokaoMode": "string",
    "targetScore": "number",
    "dreamCollege": "string",
    "dreamCollegeBatch": "string",
    "baselineScore": "number",
    "baselineRank": "number",
    "remainingDays": "number",
    "createdAt": "string",
    "updatedAt": "string"
  }
}
```

### 3.2 创建学生档案
**路径**: `POST /api/v1/student/profile`  
**描述**: 创建学生档案（仅可创建一次）

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
  "grade": "string",
  "subjectCombination": "string",
  "gaokaoMode": "string",
  "targetScore": "number",
  "dreamCollege": "string",
  "dreamCollegeBatch": "string"
}
```

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "number"
  }
}
```

### 3.3 更新学生档案
**路径**: `PUT /api/v1/student/profile/{studentId}`  
**描述**: 更新学生档案基础信息

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
  "grade": "string",
  "targetScore": "number",
  "dreamCollege": "string",
  "dreamCollegeBatch": "string"
}
```

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 四、模考位次接口

### 4.1 提交考试成绩
**路径**: `POST /api/v1/exam/submit`  
**描述**: 提交考试成绩，自动换算等效位次

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
  "examType": "weekly|monthly",
  "subjectScores": {
    "语文": "number",
    "数学": "number",
    "英语": "number",
    "历史": "number",
    "政治": "number",
    "地理": "number"
  },
  "examDate": "string"
}
```

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "number",
    "totalScore": "number",
    "equivalentGaokaoScore": "number",
    "equivalentRank": "number",
    "currentBatch": "string",
    "aiDiagnosisReport": "string"
  }
}
```

### 4.2 获取考试记录列表
**路径**: `GET /api/v1/exam/records/{studentId}`  
**描述**: 获取学生考试记录列表

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "number",
      "examType": "weekly|monthly",
      "totalScore": "number",
      "equivalentGaokaoScore": "number",
      "equivalentRank": "number",
      "currentBatch": "string",
      "examDate": "string",
      "aiDiagnosisReport": "string"
    }
  ]
}
```

### 4.3 获取考试详情
**路径**: `GET /api/v1/exam/detail/{examId}`  
**描述**: 获取单次考试详情

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "number",
    "studentId": "number",
    "examType": "weekly|monthly",
    "subjectScores": {},
    "totalScore": "number",
    "equivalentGaokaoScore": "number",
    "equivalentRank": "number",
    "currentBatch": "string",
    "examDate": "string",
    "aiDiagnosisReport": "string",
    "createdAt": "string"
  }
}
```

---

## 五、AI学习任务接口

### 5.1 获取今日任务
**路径**: `GET /api/v1/learning/today/{studentId}`  
**描述**: 获取AI生成的今日学习任务

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tasks": [
      {
        "id": "number",
        "type": "string",
        "content": "string",
        "subject": "string",
        "knowledgePoint": "string",
        "aiHint": "string",
        "completionRate": "number",
        "status": "pending|completed|partial"
      }
    ],
    "aiComment": "string",
    "completionRate": "number"
  }
}
```

### 5.2 完成任务
**路径**: `POST /api/v1/learning/complete`  
**描述**: 标记任务完成

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
  "taskId": "number",
  "completionRate": "number"
}
```

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 5.3 获取错题列表
**路径**: `GET /api/v1/learning/errors/{studentId}`  
**描述**: 获取学生错题列表

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "number",
      "subject": "string",
      "knowledgePoint": "string",
      "questionContent": "string",
      "wrongAnswer": "string",
      "aiAnalysis": "string",
      "reinforcementFlag": "number",
      "createdAt": "string"
    }
  ]
}
```

### 5.4 获取知识点掌握情况
**路径**: `GET /api/v1/learning/knowledge/{studentId}`  
**描述**: 获取学生知识点掌握情况

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "subjects": [
      {
        "subject": "string",
        "knowledgePoints": [
          {
            "name": "string",
            "masteryLevel": "number",
            "status": "strong|normal|weak"
          }
        ]
      }
    ]
  }
}
```

---

## 六、段位激励接口

### 6.1 获取段位卡片数据
**路径**: `GET /api/v1/growth/cards/{studentId}`  
**描述**: 获取三段式院校对标卡片数据

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "currentBatchCards": [
      {
        "id": "number",
        "name": "string",
        "logo": "string",
        "batch": "string"
      }
    ],
    "targetBatchCards": [
      {
        "id": "number",
        "name": "string",
        "logo": "string",
        "batch": "string"
      }
    ],
    "dreamCollege": {
      "name": "string",
      "logo": "string",
      "batch": "string",
      "scoreGap": "number",
      "subjectGaps": [
        {
          "subject": "string",
          "gap": "number"
        }
      ],
      "aiIncentive": "string"
    },
    "currentBatch": "string",
    "currentScore": "number"
  }
}
```

### 6.2 获取段位进度条数据
**路径**: `GET /api/v1/growth/progress/{studentId}`  
**描述**: 获取段位成长进度条数据

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "phases": [
      {
        "name": "二本阶段",
        "startScore": 400,
        "endScore": 480,
        "currentScore": 450,
        "progress": 62.5,
        "completed": false
      },
      {
        "name": "一本阶段",
        "startScore": 480,
        "endScore": 550,
        "currentScore": 450,
        "progress": 0,
        "completed": false
      },
      {
        "name": "211/双一流阶段",
        "startScore": 550,
        "endScore": 650,
        "currentScore": 450,
        "progress": 0,
        "completed": false
      }
    ],
    "totalProgress": "number",
    "targetScore": "number"
  }
}
```

### 6.3 获取段位升级记录
**路径**: `GET /api/v1/growth/history/{studentId}`  
**描述**: 获取段位升级历史记录

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "number",
      "previousBatch": "string",
      "currentBatch": "string",
      "scoreAtUpgrade": "number",
      "upgradeTime": "string",
      "aiIncentiveText": "string"
    }
  ]
}
```

### 6.4 获取成长数据
**路径**: `GET /api/v1/growth/data/{studentId}`  
**描述**: 获取学生成长数据（总分趋势、位次曲线、单科曲线）

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "scoreTrend": [
      {"date": "string", "score": "number"}
    ],
    "rankTrend": [
      {"date": "string", "rank": "number"}
    ],
    "subjectTrends": {
      "语文": [{"date": "string", "score": "number"}],
      "数学": [{"date": "string", "score": "number"}]
    },
    "monthlyReport": "string"
  }
}
```

---

## 七、题目接口

### 7.1 获取专项训练题目
**路径**: `GET /api/v1/question/training/{studentId}`  
**描述**: 获取AI生成的专项训练题目

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "number",
      "subject": "string",
      "knowledgePoint": "string",
      "difficulty": "basic|medium|hard",
      "questionContent": "string",
      "options": ["string"],
      "answer": "string"
    }
  ]
}
```

### 7.2 获取补强训练题目
**路径**: `GET /api/v1/question/reinforcement/{studentId}`  
**描述**: 获取AI生成的补强训练题目

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "number",
      "subject": "string",
      "knowledgePoint": "string",
      "difficulty": "basic|medium|hard",
      "questionContent": "string",
      "options": ["string"],
      "answer": "string",
      "relatedErrorId": "number"
    }
  ]
}
```

---

## 八、家长端接口

### 8.1 获取孩子学习概况
**路径**: `GET /api/v1/parent/overview/{studentId}`  
**描述**: 获取孩子学习概况（家长端）

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "currentBatch": "string",
    "dreamCollege": "string",
    "scoreGap": "number",
    "weeklyAiComment": "string",
    "weeklyCompletionRate": "number",
    "recentExamTrend": [
      {"date": "string", "score": "number", "rank": "number"}
    ]
  }
}
```

---

## 九、管理员接口

### 9.1 获取用户列表
**路径**: `GET /api/v1/admin/users`  
**描述**: 获取用户列表（管理员）

**请求头**: `Authorization: Bearer {token}`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| role | string | 否 | 角色筛选 |
| page | number | 否 | 页码 |
| size | number | 否 | 每页数量 |

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "number",
        "username": "string",
        "role": "string",
        "studentId": "number",
        "status": "number",
        "createdAt": "string"
      }
    ],
    "total": "number",
    "page": "number",
    "size": "number"
  }
}
```

### 9.2 创建用户
**路径**: `POST /api/v1/admin/users`  
**描述**: 创建用户（管理员）

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
  "username": "string",
  "password": "string",
  "role": "student|parent",
  "studentId": "number"
}
```

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "number"
  }
}
```

### 9.3 更新用户状态
**路径**: `PUT /api/v1/admin/users/{userId}/status`  
**描述**: 更新用户启用/禁用状态（管理员）

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
  "status": "number"
}
```

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 9.4 重置用户密码
**路径**: `POST /api/v1/admin/users/{userId}/reset-password`  
**描述**: 重置用户密码（管理员）

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "newPassword": "string"
  }
}
```

### 9.5 获取系统监控信息
**路径**: `GET /api/v1/admin/monitor`  
**描述**: 获取系统监控信息（管理员）

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "serverInfo": {
      "cpuUsage": "number",
      "memoryUsage": "number",
      "diskUsage": "number"
    },
    "databaseStatus": "string",
    "redisStatus": "string",
    "backupStatus": {
      "lastBackupTime": "string",
      "backupCount": "number",
      "status": "string"
    },
    "aiStatus": {
      "modelName": "string",
      "apiStatus": "string",
      "lastCallTime": "string"
    },
    "apiStats": [
      {"api": "string", "count": "number", "avgResponseTime": "number"}
    ]
  }
}
```

### 9.6 获取AI参数配置
**路径**: `GET /api/v1/admin/ai/config`  
**描述**: 获取AI参数配置（管理员）

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "scoreTrackThresholds": {
      "low": 500,
      "medium": 580
    },
    "safeCollegeRankDiff": 5000,
    "weakSubjectThreshold": 40,
    "incentiveStyle": "string"
  }
}
```

### 9.7 更新AI参数配置
**路径**: `PUT /api/v1/admin/ai/config`  
**描述**: 更新AI参数配置（管理员）

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
  "scoreTrackThresholds": {
    "low": "number",
    "medium": "number"
  },
  "safeCollegeRankDiff": "number",
  "weakSubjectThreshold": "number",
  "incentiveStyle": "string"
}
```

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 十、Mock数据策略

### 10.1 Mock模式说明
在后端接口未完成前，前端可使用Mock数据进行开发。Mock数据应存放在前端项目的mock目录中。

### 10.2 Mock数据文件结构
```
frontend/src/mock/
├── user.js
├── student.js
├── exam.js
├── learning.js
├── growth.js
├── question.js
├── parent.js
└── admin.js
```

### 10.3 Mock数据示例

**用户登录Mock**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "mock-jwt-token",
    "expireTime": "2026-07-28 10:00:00",
    "user": {
      "id": 1,
      "username": "student001",
      "role": "student",
      "studentId": 1
    }
  }
}
```

**段位卡片Mock**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "currentBatchCards": [
      {"id": 1, "name": "河南大学", "logo": "/logos/henan.png", "batch": "公办二本"},
      {"id": 2, "name": "河北师范大学", "logo": "/logos/hebei.png", "batch": "公办二本"},
      {"id": 3, "name": "山西大学", "logo": "/logos/shanxi.png", "batch": "公办二本"}
    ],
    "targetBatchCards": [
      {"id": 4, "name": "郑州大学", "logo": "/logos/zhengzhou.png", "batch": "211"},
      {"id": 5, "name": "陕西师范大学", "logo": "/logos/shaanxi.png", "batch": "211"},
      {"id": 6, "name": "西南大学", "logo": "/logos/xinan.png", "batch": "211"}
    ],
    "dreamCollege": {
      "name": "北京大学",
      "logo": "/logos/beida.png",
      "batch": "双一流",
      "scoreGap": 150,
      "subjectGaps": [
        {"subject": "数学", "gap": 60},
        {"subject": "英语", "gap": 30}
      ],
      "aiIncentive": "你的数学进步空间很大，加油！"
    },
    "currentBatch": "公办二本",
    "currentScore": 450
  }
}
```

### 10.4 Mock切换机制
前端应支持通过环境变量切换Mock模式和真实API模式：

```javascript
// vite.config.js
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

**环境变量配置**:
```env
# .env.development
VITE_API_URL=http://localhost:8080

# .env.mock
VITE_API_URL=/mock
```

---

**文档版本**: v1.0  
**最后更新**: 2026-06-28  
**适用范围**: 所有参与本项目开发的AI工具