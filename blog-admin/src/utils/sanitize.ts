import DOMPurify from 'dompurify'

// 后台会渲染匿名可写的留言/评论内容，必须消毒后再交给 v-html，否则注入的脚本能读到管理员 token
export function sanitizeHtml(html: string): string {
  if (typeof html !== 'string') return ''
  return DOMPurify.sanitize(html)
}
