import axios from '../utils/axios'

export interface SystemConfigItem {
  configId?: number
  configKey: string
  configValue: string
  description?: string
  configType?: string
  isEditable?: number
  createdAt?: string
  updatedAt?: string
}

export interface WebsiteConfig {
  websiteName?: string
  websiteDescription?: string
  websiteKeywords?: string
  websiteLogo?: string
  websiteFavicon?: string
  websiteIcp?: string
  websiteAnalytics?: string
  websiteStatus?: number
  closeMessage?: string
  pageSize?: number
  rssLimit?: number
  commentStatus?: number
  registerStatus?: number
}

export interface EmailConfig {
  smtpHost?: string
  smtpPort?: number
  smtpUsername?: string
  smtpPassword?: string
  enableSsl?: number
  fromEmail?: string
  fromName?: string
  emailEnabled?: number
}

export interface FileUploadConfig {
  maxFileSize?: number
  allowedImageTypes?: string
  allowedFileTypes?: string
  imageUploadPath?: string
  fileUploadPath?: string
  enableLocalStorage?: number
  enableOssStorage?: number
  ossAccessKey?: string
  ossSecretKey?: string
  ossBucketName?: string
  ossEndpoint?: string
}

export const systemConfigService = {
  getConfigByKey: (configKey: string) =>
    axios.get<SystemConfigItem>(`/system/config/${configKey}`),

  updateConfigByKey: (config: SystemConfigItem) =>
    axios.put('/system/config', config),

  getWebsiteConfig: () =>
    axios.get<WebsiteConfig>('/system/config/website'),

  updateWebsiteConfig: (config: WebsiteConfig) =>
    axios.put('/system/config/website', config),

  getEmailConfig: () =>
    axios.get<EmailConfig>('/system/config/email'),

  updateEmailConfig: (config: EmailConfig) =>
    axios.put('/system/config/email', config),

  getFileUploadConfig: () =>
    axios.get<FileUploadConfig>('/system/config/file-upload'),

  updateFileUploadConfig: (config: FileUploadConfig) =>
    axios.put('/system/config/file-upload', config)
}
