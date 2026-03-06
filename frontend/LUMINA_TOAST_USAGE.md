# Lumina Toast 使用指南

## 组件位置
- 组件文件：`frontend/src/components/LuminaToast.vue`
- Composable：`frontend/src/composables/useLuminaToast.ts`

## 基本使用

### 方式一：使用 composable（推荐）

```vue
<script setup lang="ts">
import { useLuminaToast } from '@/composables/useLuminaToast'

const toast = useLuminaToast()

// 成功通知
const handleSuccess = () => {
  toast.success('操作成功！')
}

// 错误通知
const handleError = () => {
  toast.error('操作失败，请重试')
}

// 警告通知
const handleWarning = () => {
  toast.warning('请注意网络连接')
}

// 信息通知
const handleInfo = () => {
  toast.info('这是一条普通消息')
}

// 点赞通知（带爱心图标）
const handleLike = () => {
  toast.like('点赞成功！')
}

// 收藏通知（带星星图标）
const handleFavorite = () => {
  toast.favorite('收藏成功！')
}

// 带标题的通知
const handleWithTitle = () => {
  toast.success('保存成功', {
    title: '草稿已保存',
    duration: 3000
  })
}
</script>
```

### 方式二：直接导入（全局方法）

```vue
<script setup lang="ts">
import { toast } from '@/composables/useLuminaToast'

const handleSuccess = () => {
  toast.success('操作成功！')
}
</script>
```

## 替换现有 ElMessage

### 替换前
```typescript
import { ElMessage } from 'element-plus'

ElMessage.success('操作成功')
ElMessage.error('操作失败')
ElMessage.warning('请注意')
ElMessage.info('提示信息')
```

### 替换后
```typescript
import { toast } from '@/composables/useLuminaToast'

toast.success('操作成功')
toast.error('操作失败')
toast.warning('请注意')
toast.info('提示信息')
```

## 典型场景示例

### 点赞成功
```typescript
// 替换前
ElMessage.success('点赞成功')

// 替换后
import { toast } from '@/composables/useLuminaToast'
toast.like('点赞成功！')
```

### 收藏成功
```typescript
// 替换前
ElMessage.success('收藏成功')

// 替换后
import { toast } from '@/composables/useLuminaToast'
toast.favorite('收藏成功！')
```

### 评论发表成功
```typescript
// 替换前
ElMessage.success('评论发表成功')

// 替换后
import { toast } from '@/composables/useLuminaToast'
toast.success('评论发表成功')
```

### 评论删除成功
```typescript
// 替换前
ElMessage.success('评论删除成功')

// 替换后
import { toast } from '@/composables/useLuminaToast'
toast.success('评论删除成功')
```

### 敏感词警告
```typescript
// 替换前
ElMessage.warning('评论内容包含敏感词，请修改后重试')

// 替换后
import { toast } from '@/composables/useLuminaToast'
toast.warning('评论内容包含敏感词，请修改后重试')
```

### API 错误处理
```typescript
// 替换前
ElMessage.error(error.response?.data?.message || '操作失败')

// 替换后
import { toast } from '@/composables/useLuminaToast'
toast.error(error.response?.data?.message || '操作失败', {
  duration: 4000  // 错误信息显示时间稍长
})
```

## 高级选项

```typescript
import { toast } from '@/composables/useLuminaToast'

// 自定义显示时长（毫秒）
toast.success('保存成功', { duration: 2000 })

// 带标题的通知
toast.error('上传失败', {
  title: '文件过大',
  message: '请选择小于 10MB 的文件',
  duration: 5000
})

// 特殊类型：点赞（带爱心图标，默认2秒）
toast.like()
toast.like('已点赞')

// 特殊类型：收藏（带星星图标，默认2秒）
toast.favorite()
toast.favorite('已收藏到「技术」合集')
```

## 需要批量替换的文件

以下文件需要将 `ElMessage` 替换为 `toast`：

1. `frontend/src/components/Aside.vue`
2. `frontend/src/components/CommentSection.vue`
3. `frontend/src/components/article/PublishDrawer.vue`
4. 其他使用 ElMessage 的组件

## 批量替换命令

在项目根目录执行以下命令进行批量替换：

```bash
# 替换 ElMessage.success
find frontend/src -name "*.vue" -type f -exec sed -i 's/ElMessage\.success(/toast.success(/g' {} \;

# 替换 ElMessage.error
find frontend/src -name "*.vue" -type f -exec sed -i 's/ElMessage\.error(/toast.error(/g' {} \;

# 替换 ElMessage.warning
find frontend/src -name "*.vue" -type f -exec sed -i 's/ElMessage\.warning(/toast.warning(/g' {} \;

# 替换 ElMessage.info
find frontend/src -name "*.vue" -type f -exec sed -i 's/ElMessage\.info(/toast.info(/g' {} \;

# 删除 import 语句（需要手动检查）
# 在每个文件中删除：import { ElMessage } from "element-plus"
# 并添加：import { toast } from '@/composables/useLuminaToast'
```

**注意**：批量替换后需要手动检查每个文件，确保正确导入 toast 方法。
