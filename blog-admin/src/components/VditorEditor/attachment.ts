// 附件与图片共用一个上传接口（/file/upload 只回 URL），因此"插入什么"必须由前端按文件类型决定。
// 编辑器是 Vditor 的 IR 模式，insertValue 会经 Lute 把 HTML 转回 markdown：直接插入
// <a class="article-attachment">…</a> 会被压成 [文本](链接) 并丢掉 class 与 span
// （实测 HTML2VditorIRDOM 的结果就是 "[HTML文件名14.6 KB](…)"，再叠加文件名里的空格
// 还会让链接彻底失效，整段变成纯文本）。所以这里只产出 markdown 链接，
// 卡片样式由门户详情页的 upgradeAttachmentLinks() 在渲染后补回来。

const IMAGE_EXT = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp', 'avif', 'ico']

// 附件链接的识别标记。markdown 只有 title 能携带自定义信息，故用 title 前缀区分
// "附件"与普通外链，门户详情页据此把 <a> 升级成下载卡片
export const ATTACHMENT_TITLE_PREFIX = '附件：'

const extOf = (name: string) => (name.split('.').pop() || '').toLowerCase()

// 拖拽/粘贴来的文件可能没有 MIME（部分系统下 type 为空），退回按扩展名判断
export const isImageFile = (file: File) => {
  if (file.type) return file.type.startsWith('image/')
  return IMAGE_EXT.includes(extOf(file.name))
}

export const formatFileSize = (bytes?: number) => {
  if (!bytes || bytes <= 0) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

// 后端保存名是 两位随机数_原名（见 FileController.upload），展示时去掉前缀
export const fileNameFromUrl = (url: string) => {
  try {
    const pathname = new URL(url, window.location.href).pathname
    const base = decodeURIComponent(pathname.split('/').filter(Boolean).pop() || '')
    return base.replace(/^\d{2}_/, '') || '附件'
  } catch (e) {
    return '附件'
  }
}

// markdown 的行内链接是 [文本](地址 "标题")，地址里的空格/括号/引号会截断解析
// （后端按原始文件名存对象，URL 里因此可能带空格和括号）
export const encodeHref = (url: string) => String(url)
  .replace(/[\r\n\t]/g, '')
  .replace(/ /g, '%20')
  .replace(/\(/g, '%28')
  .replace(/\)/g, '%29')
  .replace(/"/g, '%22')
  .replace(/</g, '%3C')
  .replace(/>/g, '%3E')

// 文件名进 markdown 文本/标题前同样要清掉会破坏语法的字符
const safeLabel = (value: string) => String(value).replace(/[\[\]"\r\n]/g, '').trim()

/**
 * 生成附件 markdown 链接：[文件名 · 大小](地址 "附件：文件名")
 * 大小未知（手填地址）时省略 "· 大小"
 */
export const buildAttachmentMarkdown = (url: string, name?: string, size?: number) => {
  const fileName = safeLabel(name || fileNameFromUrl(url)) || '附件'
  const sizeText = formatFileSize(size)
  const label = sizeText ? fileName + ' · ' + sizeText : fileName
  return '[' + label + '](' + encodeHref(url) + ' "' + ATTACHMENT_TITLE_PREFIX + fileName + '")'
}
