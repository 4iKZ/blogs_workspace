/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_GITHUB_CLIENT_ID?: string
  readonly VITE_GITHUB_CALLBACK_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
