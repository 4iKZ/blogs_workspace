<template>
  <Layout>
    <div class="moderation-page">
      <h2>文章审核队列</h2>
      <el-select
        v-model="status"
        clearable
        placeholder="审核状态"
        @change="load"
      >
        <el-option
          v-for="item in statuses"
          :key="item"
          :label="item"
          :value="item"
        />
      </el-select>
      <el-table
        v-loading="loading"
        :data="items"
        style="margin-top: 16px"
      >
        <el-table-column
          prop="title"
          label="标题"
          min-width="240"
        />
        <el-table-column
          prop="submissionType"
          label="类型"
          width="100"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="150"
        />
        <el-table-column
          prop="retryCount"
          label="重试"
          width="80"
        />
        <el-table-column
          label="操作"
          width="180"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              type="success"
              :disabled="terminal(row.status)"
              @click="decide(row, true)"
            >
              批准
            </el-button>
            <el-button
              size="small"
              type="danger"
              :disabled="terminal(row.status)"
              @click="decide(row, false)"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import Layout from '@/components/Layout.vue'
import { adminService, type ModerationSubmission, type ModerationSubmissionStatus } from '@/services/adminService'
import { toast } from '@/composables/useLuminaToast'

const statuses: ModerationSubmissionStatus[] = ['PENDING', 'PROCESSING', 'RETRY', 'PASSED', 'REJECTED', 'MANUAL_REVIEW']
const status = ref<ModerationSubmissionStatus>()
const items = ref<ModerationSubmission[]>([])
const loading = ref(false)
const terminal = (value: ModerationSubmissionStatus) => value === 'PASSED' || value === 'REJECTED'
const load = async () => {
  loading.value = true
  try { items.value = await adminService.getModerationSubmissions(status.value) }
  catch (error: any) { toast.error(error.message || '加载审核队列失败') }
  finally { loading.value = false }
}
const decide = async (row: ModerationSubmission, approve: boolean) => {
  try {
    const { value } = await ElMessageBox.prompt('请填写审核原因', approve ? '批准审核' : '拒绝审核', { inputPattern: /\S+/, inputErrorMessage: '审核原因不能为空' })
    if (approve) await adminService.approveModerationSubmission(row.submissionToken, value)
    else await adminService.rejectModerationSubmission(row.submissionToken, value)
    toast.success('审核决定已保存')
    await load()
  } catch { /* user cancellation or handled request failure */ }
}
onMounted(load)
</script>

<style scoped>.moderation-page { padding: 20px; }</style>
