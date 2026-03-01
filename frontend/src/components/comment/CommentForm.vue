<template>
  <div class="comment-form">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
      <el-form-item prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          :placeholder="parentId ? '写下你的回复...' : '写下你的评论...'"
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>
      
      <!-- Guest user fields (shown when not logged in) -->
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

      <el-form-item>
        <div class="comment-buttons">
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ parentId ? '回复' : '发表评论' }}
          </el-button>
          <el-button v-if="parentId" @click="handleCancel" class="cancel-btn">取消</el-button>
        </div>
      </el-form-item>
    </el-form>
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
  margin: 20px 0;
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

@media (max-width: 768px) {
  .guest-fields {
    grid-template-columns: 1fr;
  }
}
</style>
