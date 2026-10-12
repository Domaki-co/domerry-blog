const API = 'https://api.chksz.com/api'
const API_KEY = 'chksz_EzOgUj0rOUeIYpnGXZmZqCpc8tDwu8fbpmJE30Obg-g'

function createUrl(path, params = {}) {
  const url = new URL(`${API}/${path}`)
  Object.entries({ ...params, apikey: API_KEY }).forEach(([key, value]) => {
    url.searchParams.set(key, value)
  })
  return url.toString()
}

async function request(path, params, signal) {
  const response = await fetch(createUrl(path, params), { signal })
  return response.json()
}

export function searchSongsApi(keyword, signal) {
  return request('163_search', { keyword, limit: 30 }, signal)
}

export function getPlaylistApi(playlistId) {
  return request('163_playlist', { id: playlistId })
}

export function getMusicApi(songId, quality) {
  return request('163_music', { id: songId, level: quality })
}

export function getLyricApi(songId) {
  return request('163_lyric', { id: songId })
}
