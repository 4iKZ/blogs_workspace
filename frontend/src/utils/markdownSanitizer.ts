import DOMPurify from 'dompurify'

const ALLOWED_TAGS = [
  'a', 'abbr', 'b', 'blockquote', 'br', 'code', 'del', 'details', 'div', 'em',
  'figcaption', 'figure', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hr', 'i', 'img',
  'input', 'kbd', 'li', 'mark', 'ol', 'p', 'pre', 's', 'small', 'span', 'strong',
  'sub', 'summary', 'sup', 'table', 'tbody', 'td', 'th', 'thead', 'tr', 'u', 'ul'
]

const ALLOWED_ATTR = [
  'alt', 'checked', 'colspan', 'disabled', 'href', 'rel', 'rowspan', 'src',
  'target', 'title', 'type'
]

const FORBIDDEN_TAGS = [
  'base', 'embed', 'form', 'iframe', 'math', 'object', 'script', 'style', 'svg',
  'template'
]

const FORBIDDEN_ATTR = ['style', 'srcset', 'xlink:href']

function decodeForUrlCheck(value: string): string {
  let decoded = value
  for (let attempt = 0; attempt < 3; attempt += 1) {
    try {
      const next = decodeURIComponent(decoded)
      if (next === decoded) break
      decoded = next
    } catch {
      break
    }
  }
  return decoded
}

function stripUrlControlsAndWhitespace(value: string): string {
  return Array.from(value)
    .filter((character) => {
      const code = character.charCodeAt(0)
      return character.trim() !== '' && code > 0x1f && (code < 0x7f || code > 0x9f)
    })
    .join('')
}

function isSafeUrl(value: string): boolean {
  const normalized = stripUrlControlsAndWhitespace(decodeForUrlCheck(value))

  if (!normalized || normalized.includes('\\') || normalized.startsWith('//')) {
    return false
  }

  if (/^https?:\/\//i.test(normalized)) {
    return true
  }

  if (/^(?:\/(?!\/)|\.\.?\/|\?|#)/.test(normalized)) {
    return true
  }

  return !/^[a-z][a-z\d+.-]*:/i.test(normalized)
}

/**
 * Sanitizes HTML generated from article Markdown before any browser render.
 */
export function sanitizeMarkdownHtml(html: string): string {
  const clean = DOMPurify.sanitize(html, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    FORBID_TAGS: FORBIDDEN_TAGS,
    FORBID_ATTR: FORBIDDEN_ATTR,
    ALLOW_UNKNOWN_PROTOCOLS: false,
    RETURN_TRUSTED_TYPE: false
  })

  const document = new DOMParser().parseFromString(clean, 'text/html')
  document.querySelectorAll<HTMLElement>('[href], [src]').forEach((element) => {
    const attribute = element.hasAttribute('href') ? 'href' : 'src'
    const value = element.getAttribute(attribute)
    if (!value || !isSafeUrl(value)) {
      element.removeAttribute(attribute)
    }
  })

  document.querySelectorAll<HTMLAnchorElement>('a[target="_blank"]').forEach((anchor) => {
    anchor.setAttribute('rel', 'noopener noreferrer')
  })

  return document.body.innerHTML
}
