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
    category: string
    level: number
}

export interface SensitiveCheckResultDTO {
    passed: boolean
    hitWords: string[]
}
