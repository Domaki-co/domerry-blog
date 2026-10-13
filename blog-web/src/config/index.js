/**
 * 前端可调参数集中放这里，改一处即可全局生效。
 * 每个值都可以用同名 VITE_ 环境变量覆盖（改 .env.* 即可，不用动代码）。
 */

/**
 * 标签云最多展示多少个标签（排序与过滤规则见 src/utils/tags.js）。
 * 设为 0 表示不限制数量，有多少展示多少。
 *
 * 想调整（改完重新构建 / 重启 dev 生效）：
 *   1. 直接改下面的默认值；或
 *   2. 在 .env.development / .env.production 里加 VITE_TAG_DISPLAY_LIMIT。
 */
const rawTagLimit = import.meta.env.VITE_TAG_DISPLAY_LIMIT
const parsedTagLimit = rawTagLimit ? Number(rawTagLimit) : NaN

export const TAG_DISPLAY_LIMIT = Number.isFinite(parsedTagLimit) ? parsedTagLimit : 20
