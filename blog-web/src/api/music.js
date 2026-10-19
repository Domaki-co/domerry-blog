import request from '@/utils/request'

// 音乐接口统一走后端代理：第三方地址与 key 由后端从字典读取（dictType: music_api），前端不再持有密钥。
// 后端原样透传第三方响应体，所以调用方仍按 res.code / res.data 读取。

// axios 取消请求抛的是 CanceledError，调用方按 AbortError 判断，这里做一次归一化
function toAbortError(error) {
  if (error?.code !== 'ERR_CANCELED' && error?.name !== 'CanceledError') return error
  const abortError = new Error('canceled')
  abortError.name = 'AbortError'
  return abortError
}

export function searchSongsApi(keyword, signal) {
  return request({
    url: '/api/music/search',
    method: 'get',
    params: { keyword, limit: 30 },
    signal
  }).catch(error => Promise.reject(toAbortError(error)))
}

// signal 一律放在参数表末尾：既有调用方不传也能照常工作，取消能力按需追加
export function getPlaylistApi(playlistId, signal) {
  return request({
    url: '/api/music/playlist',
    method: 'get',
    params: { id: playlistId },
    signal
  }).catch(error => Promise.reject(toAbortError(error)))
}

export function getMusicApi(songId, quality, signal) {
  return request({
    url: '/api/music/song',
    method: 'get',
    params: { id: songId, level: quality },
    signal
  }).catch(error => Promise.reject(toAbortError(error)))
}

export function getLyricApi(songId, signal) {
  return request({
    url: '/api/music/lyric',
    method: 'get',
    params: { id: songId },
    signal
  }).catch(error => Promise.reject(toAbortError(error)))
}
