// 封面上传前的压缩：缩到长边上限并重新编码，只把压缩后的文件发给后端，原图不落盘。
// 与 blog-admin/src/utils/image-compress.ts 同一套策略，跨项目无共享包，故各留一份。

const MAX_EDGE = 1600
const QUALITY = 0.82
// 小于这个体积再编码一次也省不下多少，反而多一次解码开销
const SKIP_UNDER = 250 * 1024

function loadSource(file) {
  if (typeof createImageBitmap === 'function') {
    // from-image：按 EXIF 方向还原，避免竖拍手机照片被压成横向
    return createImageBitmap(file, { imageOrientation: 'from-image' })
  }
  return new Promise((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => { URL.revokeObjectURL(url); resolve(img) }
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('图片解码失败')) }
    img.src = url
  })
}

function sizeOf(source) {
  return source instanceof HTMLImageElement
    ? { width: source.naturalWidth, height: source.naturalHeight }
    : { width: source.width, height: source.height }
}

export async function compressImage(file, maxEdge = MAX_EDGE, quality = QUALITY) {
  // gif 重编码只会留下第一帧，与"保留原格式"的预期不符，直接放行
  if (file.type === 'image/gif' || file.size <= SKIP_UNDER) return file

  const source = await loadSource(file)
  const { width, height } = sizeOf(source)
  if (!width || !height) return file

  const scale = Math.min(1, maxEdge / Math.max(width, height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(width * scale)
  canvas.height = Math.round(height * scale)
  const ctx = canvas.getContext('2d')
  if (!ctx) return file
  ctx.drawImage(source, 0, 0, canvas.width, canvas.height)
  if (typeof source.close === 'function') source.close()

  const mime = file.type === 'image/png' ? 'image/png' : 'image/jpeg'
  const blob = await new Promise(resolve => canvas.toBlob(resolve, mime, quality))
  // 压完反而更大就别换（小图、已是高压缩率的图）
  if (!blob || blob.size >= file.size) return file

  const name = file.name.replace(/\.[^.]+$/, '') + (mime === 'image/png' ? '.png' : '.jpg')
  return new File([blob], name, { type: mime, lastModified: Date.now() })
}
