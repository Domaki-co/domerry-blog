import DOMPurify from 'dompurify'

// 所有 v-html / innerHTML 渲染的内容必须经过这里
// 默认白名单保留 iframe、img、代码高亮 class 等正常内容，剔除 script、事件属性（onclick/onerror 等）与危险协议
export function sanitizeHtml(html) {
  if (html === null || html === undefined) return ''
  return DOMPurify.sanitize(String(html), {
    // 聊天室的 @提醒 使用自定义 mention 标签
    ADD_TAGS: ['mention']
  })
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
