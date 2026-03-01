<template>
  <Layout>
    <div class="admin-settings">
      <h2 class="page-title">
        <SvgIcon
          name="settings"
          size="24px"
          style="margin-right: 8px; vertical-align: middle"
        />
        系统设置
      </h2>

      <div class="admin-content">
        <!-- 网站基本信息 -->
        <el-card class="settings-card" v-loading="loading">
          <template #header>
            <h3>网站基本信息</h3>
          </template>
          <el-form :model="websiteForm" label-width="120px">
            <el-form-item label="网站名称">
              <el-input
                v-model="websiteForm.siteName"
                placeholder="请输入网站名称"
              ></el-input>
            </el-form-item>
            <el-form-item label="网站描述">
              <el-input
                v-model="websiteForm.siteDescription"
                type="textarea"
                :rows="3"
                placeholder="请输入网站描述"
              ></el-input>
            </el-form-item>
            <el-form-item label="网站关键词">
              <el-input
                v-model="websiteForm.siteKeywords"
                placeholder="请输入网站关键词，多个关键词用逗号分隔"
              ></el-input>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 系统开关 -->
        <el-card class="settings-card">
          <template #header>
            <h3>系统开关</h3>
          </template>
          <el-form label-width="120px">
            <el-form-item label="允许注册">
              <el-switch v-model="systemForm.allowRegister"></el-switch>
            </el-form-item>
            <el-form-item label="评论审核">
              <el-switch v-model="systemForm.commentAudit"></el-switch>
            </el-form-item>
            <el-form-item label="允许评论">
              <el-switch v-model="systemForm.allowComment"></el-switch>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 邮件配置 -->
        <el-card class="settings-card">
          <template #header>
            <h3>邮件配置</h3>
          </template>
          <el-form :model="emailForm" label-width="120px">
            <el-form-item label="SMTP服务器">
              <el-input
                v-model="emailForm.smtpHost"
                placeholder="请输入SMTP服务器地址"
              ></el-input>
            </el-form-item>
            <el-form-item label="SMTP端口">
              <el-input-number
                v-model="emailForm.smtpPort"
                :min="1"
                :max="65535"
              ></el-input-number>
            </el-form-item>
            <el-form-item label="发送邮箱">
              <el-input
                v-model="emailForm.fromEmail"
                placeholder="请输入发送邮箱"
              ></el-input>
            </el-form-item>
            <el-form-item label="邮箱密码">
              <el-input
                v-model="emailForm.password"
                type="password"
                placeholder="请输入邮箱密码"
                show-password
              ></el-input>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 文件上传配置 -->
        <el-card class="settings-card">
          <template #header>
            <h3>文件上传配置</h3>
          </template>
          <el-form :model="uploadForm" label-width="120px">
            <el-form-item label="最大文件大小">
              <el-input-number
                v-model="uploadForm.maxUploadSize"
                :min="1"
                :max="100"
              ></el-input-number>
              <span style="margin-left: 8px; color: #909399">MB</span>
            </el-form-item>
            <el-form-item label="允许的格式">
              <el-input
                v-model="uploadForm.allowedFormats"
                placeholder="请输入允许的文件格式，如：jpg,png,gif"
              ></el-input>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 操作按钮 -->
        <div class="actions">
          <el-button type="primary" @click="handleSave" :loading="saving">
            保存设置
          </el-button>
          <el-button @click="handleReset"> 重置 </el-button>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { toast } from "@/composables/useLuminaToast";
import Layout from "../../components/Layout.vue";
import SvgIcon from "../../components/SvgIcon.vue";
import {
  systemConfigService,
  type SystemConfig,
} from "../../services/systemConfigService";

const loading = ref(false);
const saving = ref(false);
const websiteForm = ref({
  siteName: "",
  siteDescription: "",
  siteKeywords: "",
});
const systemForm = ref({
  allowRegister: true,
  commentAudit: false,
  allowComment: true,
});
const emailForm = ref({
  smtpHost: "",
  smtpPort: 465,
  fromEmail: "",
  password: "",
});
const uploadForm = ref({
  maxUploadSize: 10,
  allowedFormats: "jpg,png,gif,webp",
});

const getSystemConfig = async () => {
  loading.value = true;
  try {
    const response = await systemConfigService.getSystemConfig();
    if (response.siteName) websiteForm.value.siteName = response.siteName;
    if (response.siteDescription)
      websiteForm.value.siteDescription = response.siteDescription;
    if (response.siteKeywords)
      websiteForm.value.siteKeywords = response.siteKeywords;
    if (response.allowRegister !== undefined)
      systemForm.value.allowRegister = response.allowRegister;
    if (response.commentAudit !== undefined)
      systemForm.value.commentAudit = response.commentAudit;
    if (response.allowComment !== undefined)
      systemForm.value.allowComment = response.allowComment;
    if (response.maxUploadSize !== undefined)
      uploadForm.value.maxUploadSize = response.maxUploadSize;
    if (response.smtpHost !== undefined)
      emailForm.value.smtpHost = response.smtpHost;
    if (response.smtpPort !== undefined)
      emailForm.value.smtpPort = response.smtpPort;
    if (response.fromEmail !== undefined)
      emailForm.value.fromEmail = response.fromEmail;
    if (response.password !== undefined)
      emailForm.value.password = response.password;
    if (response.allowedFormats !== undefined)
      uploadForm.value.allowedFormats = response.allowedFormats;
  } catch (error: any) {
    console.error("获取系统配置失败:", error);
    toast.error(
      error.response?.data?.message || error.message || "获取系统配置失败"
    );
  } finally {
    loading.value = false;
  }
};

const handleSave = async () => {
  saving.value = true;
  try {
    const config: SystemConfig = {
      ...websiteForm.value,
      ...systemForm.value,
      ...emailForm.value,
      ...uploadForm.value,
    };
    await systemConfigService.updateSystemConfig(config);
    toast.success("保存成功");
  } catch (error: any) {
    console.error("保存失败:", error);
    toast.error(
      error.response?.data?.message || error.message || "保存失败"
    );
  } finally {
    saving.value = false;
  }
};

const handleReset = () => {
  getSystemConfig();
  toast.info("已重置为上次保存的配置");
};

onMounted(() => {
  getSystemConfig();
});

onUnmounted(() => {
  // Note: toast.closeAll() can be used here if needed
});
</script>

<style scoped>
.admin-settings {
  padding: 20px 0;
}

.page-title {
  margin-bottom: 24px;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
}

.admin-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.settings-card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 20px;
}
</style>
