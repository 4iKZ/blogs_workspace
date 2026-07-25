const safeFilename = (filename: string) => filename.replace(/[\\/]/g, '_')

export const parseDownloadFilename = (
  contentDisposition: string | undefined,
  fallback: string
) => {
  if (!contentDisposition) {
    return fallback
  }

  const utf8Match = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  if (utf8Match) {
    try {
      return safeFilename(decodeURIComponent(utf8Match[1].trim()))
    } catch {
      return fallback
    }
  }

  const filenameMatch = contentDisposition.match(/filename\s*=\s*(?:"([^"]+)"|([^;]+))/i)
  const filename = filenameMatch?.[1] ?? filenameMatch?.[2]?.trim()
  return filename ? safeFilename(filename) : fallback
}

export const saveBlob = (blob: Blob, filename: string) => {
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = filename
  link.style.display = 'none'
  document.body.appendChild(link)

  try {
    link.click()
  } finally {
    link.remove()
    URL.revokeObjectURL(objectUrl)
  }
}
