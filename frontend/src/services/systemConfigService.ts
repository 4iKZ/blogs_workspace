import axios from '../utils/axios'

export interface SystemConfig {
  siteName?: string
  siteDescription?: string
  siteKeywords?: string
  allowRegister?: boolean
  commentAudit?: boolean
  allowComment?: boolean
  maxUploadSize?: number
  smtpHost?: string
  smtpPort?: number
  fromEmail?: string
  password?: string
  allowedFormats?: string
}

export interface WebsiteConfig {
  siteName?: string
  siteDescription?: string
  siteKeywords?: string
}

export interface EmailConfig {
  smtpHost?: string
  smtpPort?: number
  fromEmail?: string
  password?: string
}

export interface FileUploadConfig {
  maxUploadSize?: number
  allowedFormats?: string
}

export const systemConfigService = {
  getSystemConfig: () =>
    axios.get<SystemConfig>('/system/config/all'),

  updateSystemConfig: (config: SystemConfig) =>
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
