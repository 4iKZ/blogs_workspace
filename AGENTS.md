# AGENTS.md - 博客网站全栈项目开发指南

## 项目概述

这是一个前后端分离的博客系统：
- **后端**：Spring Boot 3.5.6 + Java 21 + MyBatis Plus
- **前端**：Vue 3 + TypeScript + Vite + Element Plus
- **数据库**：MySQL 8.0+ / H2 (测试)
- **缓存**：Redis + Caffeine 二级缓存
- **认证**：Spring Security + JWT
- **文件存储**：火山云 TOS 对象存储

## 构建与测试命令

### 后端 (Spring Boot)

```bash
# 项目根目录下运行
# 启动开发服务器
mvn spring-boot:run

# 编译项目
mvn clean compile

# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=ArticleControllerTest

# 运行特定测试方法
mvn test -Dtest=ArticleControllerTest#testCompleteChunkedUpload

# 跳过测试构建
mvn clean package -DskipTests

# 运行单个测试类（推荐）
mvn test -Dtest=ArticleControllerTest
mvn test -Dtest=UserServiceTest

# 运行多个测试类
mvn test -Dtest="ArticleControllerTest,UserControllerTest"

# 运行集成测试
mvn test -Dtest="*IntegrationTest"

# 使用 Spring Boot 测试配置
mvn test -Dspring.profiles.active=test

# 生成测试覆盖率报告（需要 JaCoCo）
mvn clean test jacoco:report
```

### 前端 (Vue 3)

```bash
# 进入 frontend 目录
cd frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 生产环境构建
npm run build

# 预览构建结果
npm run preview

# 类型检查（TypeScript）
npm run type-check  # 或 npx vue-tsc --noEmit
```

### 数据库操作

```bash
# 初始化数据库（开发环境）
# 1. 先执行建表脚本
mysql -u root -p < database/schema.sql

# 2. 再执行数据脚本
mysql -u root -p < database/data.sql

# 测试环境使用 H2（自动内存数据库）
# 已在 test/resources/application.yml 中配置
```

### 常用 Git 命令

```bash
# 查看状态
git status

# 查看提交历史
git log --oneline -10

# 查看差异
git diff
git diff --cached

# 切换分支
git checkout feature-name

# 创建并切换到新分支
git checkout -b feature-name
```

## 项目结构

```
├── src/                    # 后端源代码
│   ├── main/java/com/blog/ # Java 源码
│   │   ├── controller/     # 控制器层
│   │   ├── service/        # 服务层（接口）
│   │   ├── service/impl/   # 服务层（实现）
│   │   ├── mapper/         # MyBatis Mapper
│   │   ├── entity/         # 实体类
│   │   ├── dto/           # 数据传输对象
│   │   ├── config/        # 配置类
│   │   ├── interceptor/   # 拦截器
│   │   ├── event/         # 事件定义
│   │   ├── exception/     # 异常处理
│   │   ├── security/      # 安全配置
│   │   ├── schedule/      # 定时任务
│   │   └── utils/         # 工具类
│   ├── main/resources/     # 资源配置
│   └── test/java/com/blog/ # 测试代码
│
├── frontend/               # 前端项目
│   ├── src/
│   │   ├── views/         # 页面组件
│   │   ├── components/    # 可复用组件
│   │   ├── store/         # Pinia 状态管理
│   │   ├── router/        # 路由配置
│   │   ├── api/           # API 接口定义
│   │   ├── services/      # 业务服务
│   │   ├── composables/   # 组合式函数
│   │   ├── utils/         # 工具函数
│   │   ├── types/         # TypeScript 类型定义
│   │   └── workers/       # Web Workers
│   └── package.json
│
├── database/               # 数据库脚本
├── docs/                   # 项目文档
└── nginx.conf              # Nginx 配置
```

## 代码风格指南

### Java 后端

#### 包结构与命名
- 使用**三级包名**：`com.blog.[子模块]`
- 控制器类：`XXXController`（如 `ArticleController`）
- 服务接口：`XXXService`
- 服务实现：`XXXServiceImpl`
- 数据实体：`XXX`（如 `Article`）
- 数据传输对象：`XXXDTO`（如 `ArticleCreateDTO`）
- 枚举类：`XXXEnum`（如 `ResultCode`）

#### 导入顺序
```java
// 1. 静态导入
import static org.junit.jupiter.api.Assertions.*;

// 2. Java 标准库
import java.util.*;
import java.time.*;

// 3. 第三方库
import org.springframework.*;
import lombok.*;

// 4. 项目内部
import com.blog.common.*;
import com.blog.dto.*;
```

#### 注解使用
- 控制器：`@RestController`、`@RequestMapping`、`@Tag`（Swagger）
- 服务层：`@Service`、`@Slf4j`（Lombok）
- 实体类：`@Data`（Lombok）、`@TableName`
- 字段：`@TableId`、`@TableField`、`@NotBlank`

#### 代码格式
- 使用 **4个空格**缩进，不使用 Tab
- 类和方法使用**驼峰命名法**，首字母大写
- 变量和方法使用**驼峰命名法**，首字母小写
- 常量使用**大写字母+下划线**
- 每行不超过 **120** 个字符
- 方法参数之间换行对齐

#### 错误处理
```java
// 统一使用 BusinessException
throw new BusinessException(ResultCode.PARAM_ERROR, "参数错误");

// 统一返回 Result<T>
@PostMapping
public Result<ArticleDTO> create(@Valid @RequestBody ArticleCreateDTO dto) {
    // 业务逻辑
    return Result.success(articleDTO);
}

// 参数校验使用 @Valid
@PostMapping("/register")
public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
    // ...
}
```

#### 日志记录
```java
@Slf4j
@Service
public class ArticleServiceImpl implements ArticleService {
    
    public void createArticle(ArticleCreateDTO dto) {
        log.info("开始创建文章: {}", dto.getTitle());
        try {
            // 业务逻辑
            log.debug("文章创建成功, ID: {}", articleId);
        } catch (Exception e) {
            log.error("创建文章失败: {}", dto.getTitle(), e);
            throw e;
        }
    }
}
```

### 前端 (Vue 3 + TypeScript)

#### 组件结构
```typescript
<template>
  <!-- 模板内容 -->
</template>

<script setup lang="ts">
// 导入顺序：Vue -> 外部库 -> 内部模块
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import type { Article } from '@/types/article'

// 组件逻辑
const props = defineProps<{ article: Article }>()
const emit = defineEmits<{ (e: 'update', value: boolean): void }>()
</script>

<style scoped>
/* 组件样式 */
</style>
```

#### 类型定义
- 接口使用 `I` 前缀或描述性名称
- 类型别名使用 `Type` 后缀
- 统一导入路径别名 `@/`
```typescript
// types/article.ts
export interface Article {
  id: number
  title: string
  content: string
  status: ArticleStatus
}

export type ArticleStatus = 'draft' | 'published' | 'deleted'

export interface ArticleCreateParams {
  title: string
  content: string
  categoryId?: number
}
```

#### 状态管理 (Pinia)
```typescript
// store/user.ts
import { defineStore } from 'pinia'
import type { User } from '@/types/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null as User | null,
  }),
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
    },
    // ...
  }
})
```

#### API 调用
```typescript
// services/articleService.ts
import service from '@/utils/axios'
import type { Article, ArticleCreateParams } from '@/types/article'

export const articleService = {
  async getArticles(page = 1, size = 10): Promise<Article[]> {
    const response = await service.get('/article/list', {
      params: { page, size }
    })
    return response.data
  },
  
  async createArticle(params: ArticleCreateParams): Promise<Article> {
    return service.post('/article', params)
  }
}
```

#### 响应式数据
- 使用 `ref()` 定义响应式基本类型
- 使用 `reactive()` 定义响应式对象
- 使用 `computed()` 定义计算属性
- 使用 `watch()` 进行副作用处理

### 数据库与 SQL

#### 命名规范
- 表名：**小写蛇形命名**，复数形式（`articles`、`users`）
- 字段名：**小写蛇形命名**（`created_at`、`updated_at`）
- 主键：`id`（bigint，自增）
- 外键：`xxx_id`（如 `article_id`、`user_id`）
- 索引：`idx_表名_字段`（如 `idx_articles_title`）

#### 常用字段
```sql
-- 每个表都应包含的字段
id BIGINT PRIMARY KEY AUTO_INCREMENT,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
deleted TINYINT DEFAULT 0,  -- 逻辑删除标记
version INT DEFAULT 1       -- 乐观锁版本号
```

### 测试规范

#### 单元测试命名
```java
// 测试类命名：被测试类名 + Test
@DisplayName("ArticleService 文章服务测试")
class ArticleServiceTest {
    
    // 测试方法命名：test[功能]_[场景]_[期望]
    @Test
    @DisplayName("创建文章 - 正常场景 - 成功")
    void testCreateArticle_NormalScenario_Success() {
        // 测试逻辑
    }
    
    @Test
    @DisplayName("创建文章 - 标题为空 - 抛出异常")
    void testCreateArticle_TitleEmpty_ThrowsException() {
        // 测试逻辑
    }
}
```

#### 集成测试
```java
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("文章控制器集成测试")
class ArticleControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("获取文章列表 - 成功")
    void testGetArticles_Success() throws Exception {
        mockMvc.perform(get("/api/article/list"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.code").value(200));
    }
}
```

#### 前端测试（待补充）
```typescript
// 组件测试示例
describe('ArticleCard.vue', () => {
  it('显示文章标题', async () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    })
    expect(wrapper.text()).toContain(mockArticle.title)
  })
})
```

## 开发工作流

### 1. 环境设置
```bash
# 克隆项目
git clone <repository-url>
cd blogs_workspace

# 后端依赖
mvn clean compile

# 前端依赖
cd frontend
npm install
cd ..
```

### 2. 数据库初始化
```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS blog DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入表结构
mysql -u root -p blog < database/schema.sql

# 导入示例数据
mysql -u root -p blog < database/data.sql
```

### 3. 配置修改
- 后端：`src/main/resources/application.yml`
- 前端：环境变量 `.env` 文件

### 4. 开发与测试
```bash
# 启动后端开发服务器
mvn spring-boot:run

# 启动前端开发服务器（新终端）
cd frontend
npm run dev

# 运行后端测试
mvn test -Dtest=ArticleServiceTest

# 运行前端类型检查
npm run type-check
```

### 5. 代码质量检查
```bash
# 检查代码风格（需配置检查工具）
mvn checkstyle:check

# 运行所有测试
mvn clean test

# 前端构建检查
cd frontend && npm run build
```

## 注意事项

### 后端注意事项
1. **事务管理**：Service 层方法添加 `@Transactional`
2. **缓存使用**：热点数据使用 Redis + Caffeine 二级缓存
3. **日志级别**：生产环境使用 INFO，开发环境使用 DEBUG
4. **异常处理**：统一使用 `BusinessException` 和全局异常处理器
5. **API 文档**：使用 OpenAPI 3.0，注解保持最新

### 前端注意事项
1. **API 调用**：使用 `@/utils/axios.ts` 封装的 service
2. **状态管理**：全局状态使用 Pinia store
3. **组件通信**：优先使用 props/emit，复杂场景使用 provide/inject
4. **路由守卫**：检查 `requiresAuth` 和 `requiresAdmin` 元数据
5. **文件上传**：使用分块上传和图片压缩

### 数据库注意事项
1. **索引使用**：查询频繁的字段添加索引
2. **事务隔离**：根据场景选择合适的事务隔离级别
3. **分页查询**：使用 `LIMIT` 避免全表扫描
4. **逻辑删除**：使用 `deleted` 字段而非物理删除

### 部署注意事项
1. **环境配置**：区分 `application-dev.yml`、`application-prod.yml`
2. **健康检查**：实现 `/actuator/health` 端点
3. **日志收集**：配置日志轮转和集中收集
4. **监控告警**：集成 Prometheus + Grafana

## 故障排除

### 常见问题

#### 后端启动失败
```bash
# 检查端口占用
netstat -ano | findstr :8080

# 检查数据库连接
# 查看 application.yml 配置是否正确
```

#### 前端构建失败
```bash
# 清除缓存重试
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run build
```

#### 测试失败
```bash
# 检查测试数据库配置
# test/resources/application.yml

# 运行单个测试定位问题
mvn test -Dtest=ArticleServiceTest#testMethodName
```

### 日志查看
```bash
# 后端日志
tail -f logs/application.log

# 前端控制台
# 浏览器开发者工具 Console 标签页
```

---

**最后更新**：2025-01-15  
**适用版本**：Java 21, Spring Boot 3.5.6, Vue 3.4+  
**维护者**：开发团队  
**文档版本**：1.0