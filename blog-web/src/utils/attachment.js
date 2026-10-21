// 附件与图片共用一个上传接口（/file/upload 只回 URL），因此"插入什么"必须由前端按文件类型决定。
// 编辑器是 Vditor 的 IR 模式，insertValue 会经 Lute 把 HTML 转回 markdown：直接插入
// <a class="article-attachment">…</a> 会被压成 [文本](链接) 并丢掉 class 与 span
// （实测 HTML2VditorIRDOM 的结果就是 "[HTML文件名14.6 KB](…)"，再叠加文件名里的空格
// 还会让链接彻底失效，整段变成纯文本）。所以这里只产出 markdown 链接，
// 卡片样式由详情页的 upgradeAttachmentLinks() 在渲染后补回来。

const IMAGE_EXT = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp', 'avif', 'ico']

// 附件链接的识别标记。markdown 只有 title 能携带自定义信息，故用 title 前缀区分
// "附件"与普通外链，详情页据此把 <a> 升级成下载卡片
export const ATTACHMENT_TITLE_PREFIX = '附件：'

const extOf = (name) => (name.split('.').pop() || '').toLowerCase()

// 拖拽/粘贴来的文件可能没有 MIME（部分系统下 type 为空），退回按扩展名判断
export const isImageFile = (file) => {
  if (file.type) return file.type.startsWith('image/')
  return IMAGE_EXT.includes(extOf(file.name))
}

export const formatFileSize = (bytes) => {
  if (!bytes || bytes <= 0) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

// 后端保存名是 两位随机数_原名（见 FileController.upload），展示时去掉前缀
export const fileNameFromUrl = (url) => {
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
export const encodeHref = (url) => String(url)
  .replace(/[\r\n\t]/g, '')
  .replace(/ /g, '%20')
  .replace(/\(/g, '%28')
  .replace(/\)/g, '%29')
  .replace(/"/g, '%22')
  .replace(/</g, '%3C')
  .replace(/>/g, '%3E')

// 文件名进 markdown 文本/标题前同样要清掉会破坏语法的字符
const safeLabel = (value) => String(value).replace(/[\[\]"\r\n]/g, '').trim()

/**
 * 生成附件 markdown 链接：[文件名 · 大小](地址 "附件：文件名")
 * 大小未知（手填地址）时省略 "· 大小"
 */
export const buildAttachmentMarkdown = (url, name, size) => {
  const fileName = safeLabel(name || fileNameFromUrl(url)) || '附件'
  const sizeText = formatFileSize(size)
  const label = sizeText ? fileName + ' · ' + sizeText : fileName
  return '[' + label + '](' + encodeHref(url) + ' "' + ATTACHMENT_TITLE_PREFIX + fileName + '")'
}

/**
 * 从链接文本 "文件名 · 大小" 里拆出文件名与大小（大小缺省时 size 为空串）
 */
export const parseAttachmentLabel = (label, fileName) => {
  const text = String(label || '').trim()
  const cut = text.lastIndexOf(' · ')
  if (cut > 0) return { name: text.slice(0, cut), size: text.slice(cut + 3) }
  return { name: text || fileName || '附件', size: '' }
}

// MinIO 会照原样回写响应头（已用 curl 实测：匿名 GET 加 ?response-content-disposition
// = attachment; filename*=UTF-8''xxx 即返回 content-disposition: attachment），
// 于是"强制下载"不需要后端代理，只要在同一个对象 URL 上挂这个查询参数
const encodeQueryValue = (value) => encodeURIComponent(value).replace(/%25([0-9A-Fa-f]{2})/g, '%$1')

/**
 * 把对象 URL 变成"一定触发下载"的 URL。
 * filename* 内部已自身百分号编码，故外层编码时保留 %XX，避免二次编码成 %25XX
 */
export const buildDownloadHref = (url, fileName) => {
  const disposition = "attachment; filename*=UTF-8''" + encodeURIComponent(String(fileName || '附件'))
  const sep = String(url).indexOf('?') === -1 ? '?' : '&'
  return String(url) + sep + 'response-content-disposition=' + encodeQueryValue(disposition)
}

/**
 * 把带附件标记的 <a> 升级成下载卡片：卡体（badge + 文件名 + 大小）点开原文件，
 * 右侧「下载」按钮走内容协商强制存盘——html/图片这类会被浏览器直接预览的类型也能下载。
 * 必须在 $sanitize 之后调用：这里只重建结构，文本一律走 textContent。
 */
export const upgradeAttachmentLinks = (html) => {
  if (!html || typeof document === 'undefined') return html
  const tpl = document.createElement('template')
  tpl.innerHTML = html
  tpl.content.querySelectorAll('a[href]').forEach((a) => {
    if (a.closest('.article-attachment')) return
    const title = a.getAttribute('title') || ''
    if (!title.startsWith(ATTACHMENT_TITLE_PREFIX)) return
    const fileName = title.slice(ATTACHMENT_TITLE_PREFIX.length).trim() || '附件'
    const { name, size } = parseAttachmentLabel(a.textContent, fileName)
    const ext = extOf(fileName).toUpperCase().slice(0, 6) || 'FILE'
    const href = a.getAttribute('href') || ''

    const card = document.createElement('span')
    card.className = 'article-attachment'

    const link = document.createElement('a')
    link.className = 'article-attachment__link'
    link.setAttribute('href', href)
    link.setAttribute('title', title)

    const badge = document.createElement('span')
    badge.className = 'article-attachment__badge'
    badge.textContent = ext
    const nameEl = document.createElement('span')
    nameEl.className = 'article-attachment__name'
    nameEl.textContent = name
    link.appendChild(badge)
    link.appendChild(nameEl)
    if (size) {
      const sizeEl = document.createElement('span')
      sizeEl.className = 'article-attachment__size'
      sizeEl.textContent = size
      link.appendChild(sizeEl)
    }

    const download = document.createElement('a')
    download.className = 'article-attachment__download'
    download.setAttribute('href', buildDownloadHref(href, fileName))
    download.setAttribute('download', fileName)
    download.textContent = '下载'

    card.appendChild(link)
    card.appendChild(download)
    a.replaceWith(card)
  })
  return tpl.innerHTML
}
