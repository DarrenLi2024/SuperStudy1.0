# Claude Code 专属开发指南

> 后端架构师角色 - 负责后端核心服务开发

---

## 一、角色定位

**角色**：后端架构师

**擅长领域**：架构设计、代码生成、文档编写

**负责模块**：后端业务服务、数据库设计

---

## 二、负责任务

| 任务ID | 任务名称 | 优先级 | 依赖任务 |
|--------|---------|-------|---------|
| TASK-INIT | 项目初始化 | high | 无 |
| TASK-BE01 | 用户权限服务 | high | TASK-INIT |
| TASK-BE02 | 学生档案服务 | high | TASK-BE01 |
| TASK-BE03 | 模考位次换算服务 | high | TASK-BE01, TASK-BE02, TASK-AI01, TASK-AI02 |
| TASK-BE04 | AI自适应学习服务 | high | TASK-BE01, TASK-BE02, TASK-AI03, TASK-AI04 |
| TASK-BE05 | 段位激励服务 | high | TASK-BE02, TASK-AI02, TASK-AI05 |
| TASK-BE06 | 定时任务调度服务 | medium | TASK-BE04 |

---

## 三、技术栈规范

### 3.1 后端技术栈

| 技术 | 版本 | 说明 |
|-----|------|-----|
| SpringBoot | 2.7.18 | 主框架 |
| MyBatis-Plus | 3.5.3 | ORM框架 |
| SpringSecurity | 6.x | 权限框架 |
| JWT | - | 认证令牌 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.2+ | 缓存 |

### 3.2 代码结构

```
backend/src/main/java/com/example/
├── SuperStudyApplication.java    # 启动类
├── config/                       # 配置类
│   ├── SecurityConfig.java       # 安全配置
│   ├── JwtConfig.java            # JWT配置
│   └── CorsConfig.java           # 跨域配置
├── util/                         # 工具类
│   ├── JwtUtil.java              # JWT工具
│   └── ResponseUtil.java         # 响应工具
├── dto/                          # 数据传输对象
│   └── PageResult.java           # 分页结果
├── entity/                       # 实体类
├── sys/                          # 用户权限
│   ├── UserController.java
│   ├── UserService.java
│   ├── UserServiceImpl.java
│   ├── entity/SysUser.java
│   └── mapper/SysUserMapper.java
├── student/                      # 学生档案
├── exam/                         # 模考位次
├── learning/                     # AI自适应学习
└── growth/                       # 段位激励
```

---

## 四、开发规范

### 4.1 命名规范

| 类型 | 规范 | 示例 |
|-----|------|-----|
| 类名 | PascalCase | `UserController` |
| 方法名 | camelCase | `getUserById` |
| 变量名 | camelCase | `userId` |
| 常量名 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| 包名 | lowercase | `com.example.sys` |

### 4.2 代码模板

#### Controller模板

```java
@RestController
@RequestMapping("/api/v1/sys")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseResult login(@RequestBody LoginDTO dto) {
        return ResponseResult.success(userService.login(dto));
    }

    @GetMapping("/info")
    @PreAuthorize("hasAnyRole('STUDENT', 'PARENT', 'ADMIN')")
    public ResponseResult getUserInfo() {
        return ResponseResult.success(userService.getUserInfo());
    }
}
```

#### Service模板

```java
public interface UserService {
    LoginVO login(LoginDTO dto);
    UserInfoVO getUserInfo();
}

@Service
public class UserServiceImpl implements UserService {
    // 实现逻辑
}
```

#### Entity模板

```java
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String password;
    private String role;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
```

#### Mapper模板

```java
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // 自定义查询方法
}
```

### 4.3 统一响应格式

```java
{
    "code": 200,
    "message": "success",
    "data": {...},
    "timestamp": 1687900800000
}
```

### 4.4 错误码定义

| 错误码 | 含义 |
|-------|-----|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

## 五、最佳实践

### 5.1 安全规范

- ✅ 密码使用BCrypt加密存储
- ✅ JWT令牌过期时间设置合理
- ✅ 使用SpringSecurity权限注解
- ✅ 输入参数校验使用@Valid
- ✅ 防止SQL注入（MyBatis-Plus自动处理）

### 5.2 性能规范

- ✅ 使用Redis缓存高频访问数据
- ✅ 数据库查询使用索引
- ✅ 分页查询限制每页最大条数
- ✅ 避免N+1查询问题

### 5.3 代码质量

- ✅ 方法职责单一
- ✅ 注释清晰（类、方法）
- ✅ 异常处理统一
- ✅ 日志记录完整

---

## 六、验收标准

### 6.1 功能验收

- [ ] API接口正常响应
- [ ] 数据库CRUD操作正确
- [ ] 权限控制生效
- [ ] 数据校验完善

### 6.2 技术验收

- [ ] 代码结构规范
- [ ] 命名规范符合要求
- [ ] 统一响应格式
- [ ] 无硬编码

---

## 七、参考文档

| 文档 | 用途 |
|-----|------|
| [AI快捷执行手册.md](AI快捷执行手册.md) | 任务卡片和执行指南 |
| [项目目录结构规范.md](项目目录结构规范.md) | 目录结构和命名规范 |
| [API接口契约规范.md](API接口契约规范.md) | 接口定义和Mock数据策略 |
| [init.sql](backend/src/main/resources/sql/init.sql) | 数据库初始化脚本 |