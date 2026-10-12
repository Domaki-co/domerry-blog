const UTF8_LABEL = 'utf-8'
const GBK_LABEL = 'gbk'
const GB18030_LABEL = 'gb18030'
const BIG5_LABEL = 'big5'
const CANDIDATE_ENCODINGS = [
  UTF8_LABEL,
  GBK_LABEL,
  GB18030_LABEL,
  BIG5_LABEL
]

function decode(buffer, encoding) {
  try {
    return new TextDecoder(encoding, { fatal: true }).decode(buffer)
  } catch (error) {
    return null
  }
}

function hasChineseCharacters(text) {
  return /[\u4e00-\u9fff\u3400-\u4dbf]/.test(text)
}

function replacementRatio(text) {
  const matches = text.match(/\uFFFD/g)
  return matches ? matches.length / text.length : 0
}

export function decodeText(buffer, fallbackEncoding = UTF8_LABEL) {
  const preferred = new TextDecoder(fallbackEncoding, { fatal: false }).decode(buffer)
  if (replacementRatio(preferred) === 0) return preferred

  const candidates = [fallbackEncoding, ...CANDIDATE_ENCODINGS]
    .filter((encoding, index, list) => list.indexOf(encoding) === index)
  let best = { text: preferred, score: -1 }

  candidates.forEach(encoding => {
    const text = decode(buffer, encoding)
    if (text === null) return
    const invalidCount = replacementRatio(text) * 100
    const asciiRatio = (text.match(/[\x00-\x7f]/g) || []).length / Math.max(text.length, 1)
    const score = (hasChineseCharacters(text) ? 4 : 0) - invalidCount * 2 - asciiRatio
    if (score > best.score) best = { text, score }
  })

  return best.text
}
