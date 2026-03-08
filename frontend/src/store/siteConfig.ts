import { defineStore } from 'pinia'
import { systemConfigService, type WebsiteConfig } from '../services/systemConfigService'

export const useSiteConfigStore = defineStore('siteConfig', {
  state: () => ({
    config: null as WebsiteConfig | null,
    loading: false,
    initialized: false
  }),

  getters: {
    websiteName: (state) => state.config?.websiteName || 'Lumina',
    websiteDescription: (state) => state.config?.websiteDescription || '',
    websiteKeywords: (state) => state.config?.websiteKeywords || '',
    websiteLogo: (state) => state.config?.websiteLogo || '',
    websiteFavicon: (state) => state.config?.websiteFavicon || '',
    websiteIcp: (state) => state.config?.websiteIcp || '',
    websiteStatus: (state) => state.config?.websiteStatus ?? 1,
    closeMessage: (state) => state.config?.closeMessage || '',
    pageSize: (state) => state.config?.pageSize || 10,
    rssLimit: (state) => state.config?.rssLimit || 20,
    commentStatus: (state) => state.config?.commentStatus ?? 1,
    registerStatus: (state) => state.config?.registerStatus ?? 1,
    isWebsiteOpen: (state) => state.config?.websiteStatus === 1,
    isCommentEnabled: (state) => state.config?.commentStatus === 1,
    isRegisterEnabled: (state) => state.config?.registerStatus === 1
  },

  actions: {
    async fetchConfig() {
      if (this.initialized && this.config) {
        return this.config
      }

      this.loading = true
      try {
        const config = await systemConfigService.getWebsiteConfig()
        this.config = config
        this.initialized = true
        return config
      } catch (error) {
        console.error('Failed to fetch website config:', error)
        return null
      } finally {
        this.loading = false
      }
    },

    updateConfig(config: Partial<WebsiteConfig>) {
      if (this.config) {
        this.config = { ...this.config, ...config }
      }
    },

    updateFavicon() {
      if (this.config?.websiteFavicon) {
        const links = document.querySelectorAll("link[rel*='icon']")
        links.forEach((link) => {
          (link as HTMLLinkElement).href = this.config!.websiteFavicon!
        })
      }
    },

    updateMetaTags() {
      if (this.config) {
        const description = document.querySelector('meta[name="description"]')
        if (description && this.config.websiteDescription) {
          description.setAttribute('content', this.config.websiteDescription)
        }

        const keywords = document.querySelector('meta[name="keywords"]')
        if (keywords && this.config.websiteKeywords) {
          keywords.setAttribute('content', this.config.websiteKeywords)
        }
      }
    }
  }
})