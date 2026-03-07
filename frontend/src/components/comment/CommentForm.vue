<template>
  <div class="comment-form" :class="{ 'is-reply-form': !!parentId }">
    <div class="form-shell">
      <div class="form-content">
        <div class="avatar-box">
          <el-avatar :size="parentId ? 34 : 40" :src="currentAvatar || ''">
            {{ avatarText }}
          </el-avatar>
        </div>

        <div class="form-box">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="0" class="editor-form">
            <el-form-item prop="content" class="content-item">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="parentId ? 3 : 4"
                :placeholder="parentId ? '平等表达，友善回复' : '平等表达，友善交流'"
                maxlength="1000"
                show-word-limit
                resize="none"
              />
            </el-form-item>

            <div v-if="!isLoggedIn" class="guest-fields">
              <el-form-item prop="nickname">
                <el-input
                  v-model="form.nickname"
                  placeholder="昵称（选填）"
                  maxlength="50"
                />
              </el-form-item>
              <el-form-item prop="email">
                <el-input
                  v-model="form.email"
                  placeholder="邮箱（选填）"
                  maxlength="100"
                />
              </el-form-item>
              <el-form-item prop="website">
                <el-input
                  v-model="form.website"
                  placeholder="网站（选填）"
                  maxlength="200"
                />
              </el-form-item>
            </div>

            <div class="form-footer">
              <div class="form-hint">
                {{ parentId ? '回复时请保持友善与克制' : '理性讨论，友善交流' }}
              </div>

              <div class="comment-buttons">
                <el-button v-if="parentId" @click="handleCancel" class="cancel-btn">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">
                  {{ parentId ? '回复' : '发送' }}
                </el-button>
              </div>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { toast } from '@/composables/useLuminaToast'
import { useUserStore } from '../../store/user'
import { commentService } from '../../services/commentService'
import type { CommentCreateRequest } from '../../types/comment'

interface Props {
  articleId: number | string
  parentId?: number
  replyToCommentId?: number
}

const props = withDefaults(defineProps<Props>(), {
  parentId: 0,
  replyToCommentId: undefined
})

const emit = defineEmits<{
  (e: 'submit'): void
  (e: 'cancel'): void
}>()

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const currentAvatar = computed(() => userStore.userInfo?.avatar || '')
const avatarText = computed(() => {
  if (isLoggedIn.value) {
    return userStore.userInfo?.nickname?.charAt(0) || userStore.userInfo?.username?.charAt(0) || '我'
  }
  return '游'
})

const formRef = ref()
const submitting = ref(false)

// 将 articleId 转换为 number
const toNumber = (id: number | string): number => {
  return typeof id === 'string' ? parseInt(id, 10) : id
}

const form = ref<CommentCreateRequest>({
  articleId: toNumber(props.articleId),
  parentId: props.parentId,
  content: '',
  nickname: '',
  email: '',
  website: ''
})

const rules = {
  content: [
    { required: true, message: '请输入评论内容', trigger: 'blur' },
    { min: 1, max: 1000, message: '评论内容长度在1-1000字符之间', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  website: [
    { type: 'url', message: '请输入正确的网址格式', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    submitting.value = true
    try {
      const data: CommentCreateRequest = {
        articleId: toNumber(props.articleId),
        content: form.value.content
      }
      
      if (props.parentId) {
        data.parentId = props.parentId
      }
      if (props.replyToCommentId) {
        data.replyToCommentId = props.replyToCommentId
      }
      
      if (!isLoggedIn.value) {
        if (form.value.nickname) data.nickname = form.value.nickname
        if (form.value.email) data.email = form.value.email
        if (form.value.website) data.website = form.value.website
      }
      
      await commentService.create(data)
      toast.success('评论发表成功')

      // Reset form
      form.value.content = ''
      form.value.nickname = ''
      form.value.email = ''
      form.value.website = ''
      
      emit('submit')
    } catch (error: any) {
      toast.error(error.response?.data?.message || '评论发表失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleCancel = () => {
  emit('cancel')
}
</script>

<style scoped>
.comment-form {
  margin: 0;
}

.form-shell {
  background: var(--bg-primary);
  border-radius: 14px;
}

.form-content {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.avatar-box {
  flex-shrink: 0;
  padding-top: 2px;
}

.form-box {
  flex: 1;
  min-width: 0;
}

.editor-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.content-item {
  margin-bottom: 0;
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.form-hint {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.comment-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.cancel-btn {
  border: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

.guest-fields {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.guest-fields :deep(.el-form-item) {
  margin-bottom: 0;
}

:deep(.content-item .el-form-item__content) {
  line-height: normal;
}

:deep(.el-textarea__inner) {
  border-radius: 14px !important;
  padding: 14px 16px !important;
  line-height: 1.65;
  box-shadow: none !important;
  border: 1px solid var(--border-color) !important;
  background: var(--bg-card) !important;
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--color-blue-500) !important;
}

:deep(.el-input__wrapper) {
  border-radius: 12px !important;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-blue-500) inset !important;
}

.comment-buttons :deep(.el-button) {
  min-height: 40px;
  padding: 0 18px;
  border-radius: 10px;
}

@media (max-width: 768px) {
  .form-content {
    gap: 10px;
  }

  .avatar-box {
    padding-top: 0;
  }

  .guest-fields {
    grid-template-columns: 1fr;
  }

  .form-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .comment-buttons {
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  .comment-buttons .el-button {
    flex: 1;
    min-height: 44px;
  }

  :deep(.el-textarea__inner) {
    font-size: 16px; /* 防止 iOS 自动缩放 */
    min-height: 88px !important;
    padding: 12px 14px !important;
  }

  .form-hint {
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .form-content {
    gap: 8px;
  }

  .comment-buttons {
    width: 100%;
  }

  .comment-buttons .el-button {
    width: 100%;
    flex: none;
  }
}
</style>
