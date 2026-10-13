import { TAG_DISPLAY_LIMIT } from '@/config'

/**
 * 标签云的公共取数规则：标签页和侧边栏共用同一份，保证两处顺序、数量一致。
 *
 * 1. 丢掉没有关联文章的标签（articleNum 为 0）——点进去是空列表，没有展示价值；
 * 2. 按关联文章数从多到少排序，让用得最多的标签排在最前；
 * 3. 最多取 limit 个，limit <= 0 表示不限制。
 *
 * 不会修改传入的数组。
 */
export function pickDisplayTags(tags, limit = TAG_DISPLAY_LIMIT) {
  const sorted = (tags || [])
    .filter(tag => Number(tag.articleNum) > 0)
    .sort((a, b) => Number(b.articleNum) - Number(a.articleNum))

  return limit > 0 ? sorted.slice(0, limit) : sorted
}
