// 缓存滚动条宽度
let cachedScrollbarWidth

// 检查页面是否有滚动
const hasScroll = () => {
  return document.documentElement.scrollHeight > document.documentElement.clientHeight
}

// 获取滚动条宽度
const getScrollbarWidth = () => {
  if (cachedScrollbarWidth !== undefined) {
    return cachedScrollbarWidth
  }
  
  const outer = document.createElement('div')
  outer.style.visibility = 'hidden'
  outer.style.overflow = 'scroll'
  document.body.appendChild(outer)

  const inner = document.createElement('div')
  outer.appendChild(inner)

  cachedScrollbarWidth = outer.offsetWidth - inner.offsetWidth
  outer.parentNode.removeChild(outer)

  return cachedScrollbarWidth
}

// 首页首屏「窗帘」露出的一条边（px），漏读 CSS 变量时的兜底值，与 global.scss 的
// --sheet-peek 保持一致（当前 0：大图占满一屏，面板静止时不露边）
const SHEET_PEEK_FALLBACK = 0

/**
 * 读首页窗帘那条边的高度。
 *
 * 首屏大图是 sticky 钉在视口顶上的，文章面板从底部盖上它，所以大图实际高度是
 * `100svh - --sheet-peek`：判「大图是否还压在顶栏 / 悬浮按钮底下」不能再用
 * `innerHeight`，得减掉这一条。CSS 和 JS 共用一个来源（global.scss 的 :root），
 * 避免像 $header-offset 那样在三处各写一份常量。
 *
 * 只在挂载和 resize 时读一次就好，滚动回调里不要再读（每次都会强制算一遍样式）。
 */
export const sheetPeek = () => {
  if (typeof window === 'undefined') return SHEET_PEEK_FALLBACK
  const raw = getComputedStyle(document.documentElement).getPropertyValue('--sheet-peek')
  const value = parseFloat(raw)
  return Number.isFinite(value) ? value : SHEET_PEEK_FALLBACK
}

// 禁用页面滚动
export const disableScroll = () => {
   document.body.classList.add('scroll-disabled')
}

// 启用页面滚动
export const enableScroll = () => {
  document.body.classList.remove('scroll-disabled')
} 