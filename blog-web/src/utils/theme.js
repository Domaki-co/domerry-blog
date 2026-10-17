export function getThemeMode() {
  const savedMode = localStorage.getItem('theme-mode')
  if (savedMode) {
    return savedMode
  }

  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark'
  }

  return 'light'
}

export function setThemeMode(mode) {
  localStorage.setItem('theme-mode', mode)

  if (mode === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark')
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}

// 只在切换那一瞬间挂在 <html> 上的类名
const THEME_TRANSITION_CLASS = 'theme-transition' // 全站颜色交叉淡入淡出
const THEME_VEIL_CLASS = 'theme-veil' // 径向光晕遮罩（仅降级路径用）
const THEME_SNAPSHOT_CLASS = 'theme-snapshot' // 掐掉元素级过渡，只用于 View Transition 的快照窗口
let themeTransitionTimer = null

/**
 * 系统是否要求「减少动态效果」。
 * 组件里有自绘动画的地方也复用这个判断，避免两处各写一遍 matchMedia。
 */
export function prefersReducedMotion() {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  )
}

/**
 * 把时长和缓动写进 CSS 变量，让 JS 计时和 CSS 过渡用的是同一份数字。
 * 调动画只改下面这两个常量即可——global.scss 里的
 * `transition: ... var(--theme-duration) var(--theme-ease)` 会自己跟上，
 * 不会出现「JS 450ms 就摘了类、CSS 还在慢慢过渡」这种截断。
 */
const THEME_TRANSITION_MS = 560
const THEME_EASING = 'cubic-bezier(0.22, 1, 0.36, 1)'

function syncTransitionTokens(root) {
  root.style.setProperty('--theme-duration', `${THEME_TRANSITION_MS}ms`)
  root.style.setProperty('--theme-ease', THEME_EASING)
}

/** 以 (x, y) 为圆心，盖满整个视口需要多大的半径 */
function coverRadius(x, y) {
  return Math.hypot(
    Math.max(x, window.innerWidth - x),
    Math.max(y, window.innerHeight - y)
  )
}

/**
 * 把「扩散起点」归一化成视口坐标。
 * 接受元素、事件对象，或直接给 { x, y }。
 * 键盘 Enter / Space 触发时事件里没有指针位置，退到元素中心，再退到视口中心。
 */
function resolveOrigin(origin) {
  const center = { x: window.innerWidth / 2, y: window.innerHeight / 2 }

  if (origin && typeof origin.x === 'number' && typeof origin.y === 'number') {
    return { x: origin.x, y: origin.y }
  }

  const el =
    origin instanceof Element
      ? origin
      : (origin && (origin.currentTarget || origin.target)) || null

  if (!el || typeof el.getBoundingClientRect !== 'function') {
    return center
  }

  const rect = el.getBoundingClientRect()
  if (!rect.width && !rect.height) {
    return center
  }

  return { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 }
}

/**
 * 带过渡地切换主题。
 *
 * 两条路径：
 *   ① View Transitions API —— 浏览器把整页快照成 old / new 两层，新层用 clip-path
 *      从点击处扩散成一个盖住全屏的圆。整页一起合成，比给几千个元素挂颜色过渡便宜得多，
 *      而且「图标变形」和「整页扩散」发生在同一帧里，不会互相打架。
 *   ② 降级（浏览器不支持）—— 全站颜色交叉淡入淡出 + 一圈径向光晕从点击处扩散。
 *
 * 注意 ①：走的是整页快照，所以调用前必须确保 Vue 已经把新状态刷进 DOM，
 * 否则「新」快照里还是旧图标，动画收尾时会跳一下。调用方先 await nextTick()。
 *
 * @param {'dark'|'light'} mode
 * @param {Element|Event|{x:number,y:number}} [origin] 扩散起点，缺省用视口中心
 */
export function setThemeModeWithTransition(mode, origin) {
  const root = document.documentElement
  syncTransitionTokens(root)

  // 系统开了「减少动态效果」：不做任何过渡，直接换色
  if (prefersReducedMotion()) {
    clearTimeout(themeTransitionTimer)
    root.classList.remove(THEME_TRANSITION_CLASS, THEME_VEIL_CLASS)
    setThemeMode(mode)
    return
  }

  const { x, y } = resolveOrigin(origin)

  // ① View Transitions API
  if (typeof document.startViewTransition === 'function') {
    try {
      // 快照窗口内掐掉所有元素级过渡。
      // View Transition 抓的是静态图，而「新」快照是在改完 data-theme 的下一帧抓的——
      // 那一刻元素自己的颜色过渡才走了 3%，图里留下的还是旧颜色；等圆形扩散结束、
      // 真实 DOM 露出来才会补上剩下的变化，看上去就是「切完之后又抖了一下」。
      // 掐掉之后快照直接抓到终态，扩散到哪里、哪里就是最终样子。
      root.classList.add(THEME_SNAPSHOT_CLASS)

      const transition = document.startViewTransition(() => setThemeMode(mode))
      // 快照在动画开始前就抓完了，所以这里早一点摘掉类没有影响；
      // 用 finished 而不是定时器，跳过 / 中途取消时也能自愈。
      // 必须写成 then(cleanup, cleanup)：finished 在过渡被跳过时是 reject 的，
      // 用 finally 会留下一个没人处理的 unhandled rejection。
      const releaseSnapshot = () => root.classList.remove(THEME_SNAPSHOT_CLASS)
      transition.finished.then(releaseSnapshot, releaseSnapshot)

      transition.ready
        .then(() => {
          root.animate(
            {
              clipPath: [
                `circle(0px at ${x}px ${y}px)`,
                `circle(${coverRadius(x, y)}px at ${x}px ${y}px)`
              ]
            },
            {
              duration: THEME_TRANSITION_MS,
              easing: THEME_EASING,
              // 动画挂在「新」快照那一层上，它的层级 / 默认动画在 global.scss 的 ::view-transition-* 里关掉
              pseudoElement: '::view-transition-new(root)'
            }
          )
        })
        .catch(() => {
          // 快照被跳过（比如标签页在后台、或已有 transition 在跑）时不用管，颜色已经切过去了
        })
      return
    } catch (e) {
      // 同一时刻只允许一个 view transition，撞上就走降级路径
      root.classList.remove(THEME_SNAPSHOT_CLASS)
    }
  }

  // ② 降级：全站颜色交叉淡入淡出 + 径向光晕
  // 先摘掉光晕类，才让同一次会话里的连续切换能重放 CSS 动画
  root.classList.remove(THEME_VEIL_CLASS)
  root.classList.add(THEME_TRANSITION_CLASS)
  // 强制一次样式计算：既保证 .theme-transition 的过渡规则先落地，也让上面的摘除生效
  void root.offsetWidth
  // 光晕的圆心和放大倍数交给 JS 算，其余（颜色、节奏、淡出）都在 CSS 里
  root.style.setProperty('--theme-veil-x', `${x}px`)
  root.style.setProperty('--theme-veil-y', `${y}px`)
  root.style.setProperty('--theme-veil-scale', String(Math.ceil(coverRadius(x, y) / 24)))

  root.classList.add(THEME_VEIL_CLASS)

  setThemeMode(mode)

  clearTimeout(themeTransitionTimer)
  themeTransitionTimer = setTimeout(() => {
    root.classList.remove(THEME_TRANSITION_CLASS, THEME_VEIL_CLASS)
  }, THEME_TRANSITION_MS)
}

export function initTheme() {
  const mode = getThemeMode()
  setThemeMode(mode)
  return mode === 'dark'
}
