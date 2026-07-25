import { describe, expect, it, vi } from 'vitest'
import { parseDownloadFilename, saveBlob } from '../download'

describe('download helpers', () => {
  it('decodes an RFC 5987 UTF-8 filename', () => {
    expect(
      parseDownloadFilename(
        "attachment; filename*=UTF-8''%E5%8D%9A%E5%AE%A2%E5%A4%87%E4%BB%BD.sql",
        'fallback.bin'
      )
    ).toBe('博客备份.sql')
  })

  it('supports a quoted filename and falls back when the header is missing', () => {
    expect(parseDownloadFilename('attachment; filename="backup.sql"', 'fallback.bin'))
      .toBe('backup.sql')
    expect(parseDownloadFilename(undefined, 'fallback.bin')).toBe('fallback.bin')
  })

  it('clicks a hidden link and releases the object URL', () => {
    const objectUrl = 'blob:test-download'
    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue(objectUrl)
    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
    const blob = new Blob(['data'], { type: 'application/octet-stream' })

    saveBlob(blob, '备份.sql')

    expect(createObjectURL).toHaveBeenCalledWith(blob)
    expect(click).toHaveBeenCalledTimes(1)
    expect(revokeObjectURL).toHaveBeenCalledWith(objectUrl)
    expect(document.querySelector('a[download="备份.sql"]')).toBeNull()
  })
})
