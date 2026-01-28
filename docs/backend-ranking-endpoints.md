# 后端排行端点改动方案

## 目标
- 增加别名端点 `/api/articles/ranking` 与 `/api/authors/ranking`，对齐前端语义。
- 增强结构化日志与错误返回，保证状态码与 message 合理。

## 映射关系
- `/api/articles/ranking` → 现有热点文章服务（等价于 `/api/article/hot`）。
- `/api/authors/ranking` → 现有作者排行服务（等价于 `/api/user/top-authors`）。

## Controller 示例（Spring Boot）

```java
@RestController
@RequestMapping("/api")
@Slf4j
public class RankingController {
  private final ArticleService articleService;
  private final UserService userService;

  public RankingController(ArticleService articleService, UserService userService) {
    this.articleService = articleService;
    this.userService = userService;
  }

  @GetMapping("/articles/ranking")
  public ResponseEntity<?> articlesRanking(@RequestParam(defaultValue = "10") int limit) {
    long start = System.currentTimeMillis();
    try {
      var data = articleService.getHotArticles(limit);
      log.info("articles.ranking limit={} size={} cost={}ms", limit, data.size(), System.currentTimeMillis() - start);
      return ResponseEntity.ok(Map.of("code", 200, "data", data));
    } catch (Exception e) {
      log.error("articles.ranking error limit={} ex={}", limit, e.toString(), e);
      return ResponseEntity.status(500).body(Map.of("code", 500, "message", "internal error"));
    }
  }

  @GetMapping("/authors/ranking")
  public ResponseEntity<?> authorsRanking(@RequestParam(defaultValue = "10") int limit) {
    long start = System.currentTimeMillis();
    try {
      var data = userService.getTopAuthors(limit);
      log.info("authors.ranking limit={} size={} cost={}ms", limit, data.size(), System.currentTimeMillis() - start);
      return ResponseEntity.ok(Map.of("code", 200, "data", data));
    } catch (Exception e) {
      log.error("authors.ranking error limit={} ex={}", limit, e.toString(), e);
      return ResponseEntity.status(500).body(Map.of("code", 500, "message", "internal error"));
    }
  }
}
```

## 错误返回约定
- 成功：`200`，`{ code: 200, data: [...] }`
- 参数错误：`400`，`{ code: 400, message: "bad request" }`
- 服务异常：`500`，`{ code: 500, message: "internal error" }`
- 空数据：`200`，`data: []`（由前端控制是否重试）。

## 日志建议
- 记录 `traceId`（来自请求头或生成）。
- 打印关键参数（`limit`、分页与排序）、命中缓存与耗时、异常栈。
- 统一日志格式，便于检索与告警。

