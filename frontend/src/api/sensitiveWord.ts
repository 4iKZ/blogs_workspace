import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SensitiveWord, SensitiveWordCreateDTO, SensitiveCheckResultDTO } from '@/types/SensitiveWord'

export const getSensitiveWords = (params: { page?: number; size?: number; keyword?: string; category?: string }) => {
    return request.get<PageResult<SensitiveWord>>('/api/admin/sensitive-words', { params })
}

export const createSensitiveWord = (data: SensitiveWordCreateDTO) => {
    return request.post<number>('/api/admin/sensitive-words', data)
}

export const updateSensitiveWord = (id: number, data: SensitiveWordCreateDTO) => {
    return request.put<void>(`/api/admin/sensitive-words/${id}`, data)
}

export const deleteSensitiveWord = (id: number) => {
    return request.delete<void>(`/api/admin/sensitive-words/${id}`)
}

export const batchDeleteSensitiveWords = (ids: number[]) => {
    return request.delete<void>('/api/admin/sensitive-words/batch', { data: ids })
}

export const batchImportSensitiveWords = (data: { words: string[]; category?: string; level?: number }) => {
    return request.post<number>('/api/admin/sensitive-words/batch-import', data)
}

export const reloadSensitiveWordCache = () => {
    return request.post<void>('/api/admin/sensitive-words/reload-cache')
}

export const checkSensitiveWords = (content: string) => {
    return request.post<SensitiveCheckResultDTO>('/api/admin/sensitive-words/check', { content })
}
