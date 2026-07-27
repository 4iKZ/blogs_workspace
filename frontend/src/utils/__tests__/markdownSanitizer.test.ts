import { describe, expect, it } from 'vitest'
import { sanitizeMarkdownHtml } from '../markdownSanitizer'

describe('sanitizeMarkdownHtml', () => {
  it('removes executable elements and event attributes', () => {
    const clean = sanitizeMarkdownHtml(
      '<p>safe</p><img src="/cover.png" onerror="alert(1)"><svg onload="alert(1)"><circle /></svg>'
    )

    expect(clean).toContain('<p>safe</p>')
    expect(clean).toContain('<img src="/cover.png">')
    expect(clean).not.toMatch(/onerror|onload|<svg/i)
  })

  it.each([
    'javascript:alert(1)',
    'java&#x0A;script:alert(1)',
    ' java\nscript:alert(1)',
    'data:text/html,<script>alert(1)</script>'
  ])('removes unsafe URL %s', (url) => {
    const clean = sanitizeMarkdownHtml(`<a href="${url}">link</a><img src="${url}" alt="image">`)

    expect(clean).not.toMatch(/href=|src=/i)
    expect(clean).toContain('link')
    expect(clean).toContain('<img alt="image">')
  })

  it('removes dangerous nested markup and styling', () => {
    const clean = sanitizeMarkdownHtml(
      '<blockquote><p><iframe src="https://evil.example"></iframe><a href="https://safe.example" style="color:red"><span onclick="alert(1)">safe</span></a></p></blockquote>'
    )

    expect(clean).toContain('<blockquote>')
    expect(clean).toContain('<a href="https://safe.example">')
    expect(clean).toContain('<span>safe</span>')
    expect(clean).not.toMatch(/iframe|style=|onclick/i)
  })

  it('preserves normal Markdown preview HTML', () => {
    const clean = sanitizeMarkdownHtml(
      '<h2>Heading</h2><p><strong>bold</strong> <a href="/guide#intro" title="Guide">guide</a></p><table><thead><tr><th>name</th></tr></thead><tbody><tr><td><code>value</code></td></tr></tbody></table><pre><code>const value = 1</code></pre>'
    )

    expect(clean).toContain('<h2>Heading</h2>')
    expect(clean).toContain('<strong>bold</strong>')
    expect(clean).toContain('<a href="/guide#intro" title="Guide">guide</a>')
    expect(clean).toContain('<table>')
    expect(clean).toContain('<pre><code>const value = 1</code></pre>')
  })
})
