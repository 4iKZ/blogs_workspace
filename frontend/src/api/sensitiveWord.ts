import axios from '../utils/axios'

interface PageResult<T> {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}

export interface SensitiveWord {
  id: number
  word: string
  category: string
  level: number
  createTime: string
  updateTime: string
}

export interface SensitiveWordCreateDTO {
  word: string
  category?: string
  level?: number
}

export interface SensitiveCheckResultDTO {
  hasSensitive: boolean
  words: string[]
  positions: { start: number; end: number; word: string }[]
}

export const getSensitiveWords = (params: { page?: number; size?: number; keyword?: string; category?: string }) => {
    return axios.get<PageResult<SensitiveWord>>('/admin/sensitive-words', { params })
}

export const createSensitiveWord = (data: SensitiveWordCreateDTO) => {
    return axios.post<number>('/admin/sensitive-words', data)
}

export const updateSensitiveWord = (id: number, data: SensitiveWordCreateDTO) => {
    return axios.put<void>(`/admin/sensitive-words/${id}`, data)
}

export const deleteSensitiveWord = (id: number) => {
    return axios.delete<void>(`/admin/sensitive-words/${id}`)
}

export const batchDeleteSensitiveWords = (ids: number[]) => {
    return axios.delete<void>('/admin/sensitive-words/batch', { data: ids })
}

export const batchImportSensitiveWords = (data: { words: string[]; category?: string; level?: number }) => {
    return axios.post<number>('/admin/sensitive-words/batch-import', data)
}

export const reloadSensitiveWordCache = () => {
    return axios.post<void>('/admin/sensitive-words/reload-cache')
}

export const checkSensitiveWords = (content: string) => {
    return axios.post<SensitiveCheckResultDTO>('/admin/sensitive-words/check', { content })
}
