# Lumina Toast 组件使用总结

## ✅ 已完成的修改

### 1. 新增文件
| 文件路径 | 说明 |
|----------|------|
| `frontend/src/components/LuminaToast.vue` | 主通知组件 |
| `frontend/src/composables/useLuminaToast.ts` | Composable 工具函数 |
| `frontend/LUMINA_TOAST_USAGE.md` | 使用指南文档 |

### 2. 已修改的文件（替换 ElMessage）
| 文件 | 修改内容 |
|------|----------|
| `frontend/src/App.vue` | 添加 LuminaToast 组件 |
| `frontend/src/components/CommentSection.vue` | 替换所有 ElMessage 调用 |
| `frontend/src/components/comment/CommentItem.vue` | 替换所有 ElMessage 调用 |
| `frontend/src/components/Aside.vue` | 替换所有 ElMessage 调用 |
| `frontend/src/components/article/PublishDrawer.vue` | 替换所有 ElMessage 调用 |

## 🎨 设计特色

- **光晕效果**：通知卡片带有微妙的光效背景
- **进度条**：显示剩余关闭时间的进度条
- **堆叠显示**：支持多条通知堆叠显示，最多4条
- **优雅动画**：流畅的进入/退出动画
- **悬停暂停**：鼠标悬停时暂停计时
- **类型图标**：不同类型带有专属图标

## 📦 API 使用

### 导入方式

```typescript
// 方式一：在组件中使用（推荐）
import { useLuminaToast } from '@/composables/useLuminaToast'

const toast = useLuminaToast()

// 方式二：全局使用
import { toast } from '@/composables/useLuminaToast'
```

### 基本方法

```typescript
// 成功通知
toast.success('操作成功！')

// 错误通知
toast.error('操作失败，请重试')

// 警告通知
toast.warning('请注意网络连接')

// 信息通知
toast.info('这是一条普通消息')

// 点赞通知（带爱心图标，2秒自动关闭）
toast.like('点赞成功！')

// 收藏通知（带星星图标，2秒自动关闭）
toast.favorite('收藏成功！')
```

### 高级选项

```typescript
// 自定义显示时长
toast.success('保存成功', { duration: 2000 })

// 带标题的通知
toast.error('上传失败', {
  title: '文件过大',
  message: '请选择小于 10MB 的文件'
})

// 指定类型
toast.success('已完成', { type: 'success' })
```

## 🔄 常见场景替换对照

| 原代码 | 新代码 |
|--------|--------|
| `ElMessage.success('点赞成功')` | `toast.like('点赞成功')` |
| `ElMessage.success('收藏成功')` | `toast.favorite('收藏成功')` |
| `ElMessage.success('评论发表成功')` | `toast.success('评论发表成功')` |
| `ElMessage.warning('评论内容包含敏感词')` | `toast.warning('评论内容包含敏感词')` |
| `ElMessage.error('操作失败')` | `toast.error('操作失败')` |
| `ElMessage.info('提示信息')` | `toast.info('提示信息')` |

## 🧪 测试建议

运行项目后，可以在浏览器控制台测试：

```javascript
// 在控制台测试（如果已全局挂载）
window.luminaToast.toast.success('测试成功')
```

或在组件中：
```vue
<script setup>
import { toast } from '@/composables/useLuminaToast'

// 测试各种类型
const testToasts = () => {
  toast.success('成功通知')
  toast.error('错误通知')
  toast.warning('警告通知')
  toast.info('信息通知')
  toast.like('点赞成功')
  toast.favorite('收藏成功')
}
</script>
```

## 🎨 样式变量

组件使用现有的 CSS 变量系统：
- `--color-primary`: 主色调 (#3b82f6)
- `--color-teal-500`: 成功色 (#14b8a6)
- `--color-amber-400`: 警告色 (#fbbf24)
- `--color-rose-500`: 错误色 (#f43f5e)
- `--text-primary`: 主文本色 (#0f172a)
- `--text-secondary`: 次要文本色 (#475569)
- `--radius-lg`: 圆角 (12px)
- `--shadow-md`: 阴影

## 📸 效果预览

通知显示在页面右上角，支持：
- 多条通知堆叠显示
- 进度条显示关闭倒计时
- 鼠标悬停暂停计时
- 手动关闭按钮
- 优雅的进入/退出动画
