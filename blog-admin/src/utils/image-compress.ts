// 封面压缩：上传前把图缩到长边上限并重新编码，原始大图不再进对象存储。
// 只在 article-cover 上传点调用，其他 source 不走这里。

const MAX_EDGE = 1600
const QUALITY = 0.82
// 小于这个体积再编码一次也省不下多少，反而多一次解码开销
const SKIP_UNDER = 250 * 1024

const loadSource = async (file: File): Promise<CanvasImageSource> => {
  if (typeof createImageBitmap === 'function') {
    // from-image：按 EXIF 方向还原，避免竖拍手机照片被压成横向
    return await createImageBitmap(file, { imageOrientation: 'from-image' } as ImageBitmapOptions)
  }
  return await new Promise<CanvasImageSource>((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => { URL.revokeObjectURL(url); resolve(img) }
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('图片解码失败')) }
    img.src = url
  })
}

const sizeOf = (bitmap: CanvasImageSource) => {
  const source = bitmap as ImageBitmap
  return 'naturalWidth' in bitmap
    ? { width: (bitmap as HTMLImageElement).naturalWidth, height: (bitmap as HTMLImageElement).naturalHeight }
    : { width: source.width, height: source.height }
}

export const compressImage = async (file: File, maxEdge = MAX_EDGE, quality = QUALITY): Promise<File> => {
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
  if (typeof ImageBitmap !== 'undefined' && source instanceof ImageBitmap) source.close()

  const mime = file.type === 'image/png' ? 'image/png' : 'image/jpeg'
  const blob = await new Promise<Blob | null>(resolve => canvas.toBlob(resolve, mime, quality))
  // 压完反而更大就别换（小图、已是高压缩率的图）
  if (!blob || blob.size >= file.size) return file

  const name = file.name.replace(/\.[^.]+$/, '') + (mime === 'image/png' ? '.png' : '.jpg')
  return new File([blob], name, { type: mime, lastModified: Date.now() })
}

export const formatSize = (bytes: number) =>
  bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)}MB` : `${Math.round(bytes / 1024)}KB`
