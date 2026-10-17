/**
 * 大数字加千分位，避免侧栏那三格被一长串数字撑开。
 *
 * 0 / NaN 一律返回「—」而不是 0：接口没回来和真的是 0 是两件事，
 * 显示成 0 会让人以为站点没有数据。
 *
 * 侧栏作者卡的统计和筛选栏的篇数胶囊显示的是同一批数字。
 * 侧栏作者卡那三格统一走这条规则，免得同一批数字在不同位置各写一套格式。
 */
export function formatCount(value) {
  const n = Number(value)
  if (!n) return '—'
  return n.toLocaleString('en-US')
}
