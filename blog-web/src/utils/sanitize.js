import DOMPurify from 'dompurify'

// 所有 v-html / innerHTML 渲染的内容必须经过这里
// DOMPurify 默认白名单已剔除 script、iframe、object、embed 与 on* 事件属性，
// 但仍保留 form/style/template/input/select/textarea 和整套 SVG/MathML：
// 表单控件可承载凭据钓鱼，style 可承载 CSS 注入（覆盖层钓鱼、background:url() 外带），故显式禁用
export function sanitizeHtml(html) {
  if (html === null || html === undefined) return ''
  return DOMPurify.sanitize(String(html), {
    // 聊天室的 @提醒 使用自定义 mention 标签
    ADD_TAGS: ['mention'],
    // style/template 同时在 DOMPurify 默认 FORBID_CONTENTS 中，内容会被整体丢弃而不是以文本形式漏出
    FORBID_TAGS: ['form', 'input', 'select', 'textarea', 'button', 'style', 'template']
  })
}

// 外部链接协议白名单：后端透传的第三方数据可能带 javascript: 等伪协议，校验通过才交给 href / window.open
export function safeExternalUrl(url) {
  if (!url) return ''
  try {
    const parsed = new URL(String(url))
    return parsed.protocol === 'http:' || parsed.protocol === 'https:' ? parsed.href : ''
  } catch (error) {
    return ''
  }
}

// 纯文本转 HTML 安全文本（用于必须以字符串拼接进 v-html 的场景）
export function escapeHtml(string) {
  return String(string)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
