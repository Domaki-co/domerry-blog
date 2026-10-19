<template>
  <div class="music-page">
    <div class="blob b1"></div><div class="blob b2"></div><div class="blob b3"></div>

    <section class="music-hero">
      <div class="shell hero-inner">
        <div class="logo">
          <div class="logo-mark brush">听</div>
          <div class="logo-text">
            <div class="top"><h1>墨澜</h1><small>水墨听音 · 星缘新风</small></div>
            <p>一曲清音 · 水墨成诗 · 行云流水 · 流如溪涧</p>
          </div>
        </div>
        <div class="hero-right"><span>● 星缘改版</span><span class="sep">|</span><span>古风 · 水墨 · 极简线条 · 音符律动</span></div>
      </div>
    </section>

    <div class="wrap shell">
      <div class="left">
        <div class="panel">
          <div class="s-head">
            <div class="s-title"><div class="line"></div><span>尋</span><div class="line line-rev"></div></div>
            <div class="search-row">
              <div class="search-box">
                <span class="i">🔍</span>
                <input v-model.trim="keyword" placeholder="搜歌名 / 歌手 / 专辑…" autocomplete="off" inputmode="search" enterkeyhint="search" @keydown.enter="handleSearch" />
                <button type="button" class="go" aria-label="搜索" @click="handleSearch($event)">▶</button>
              </div>
              <button type="button" class="import-btn" :class="{on:importOpen}" :aria-expanded="importOpen" title="粘贴歌单链接导入" aria-label="粘贴歌单链接导入" @click="toggleImport">≡</button>
            </div>
            <div v-if="importOpen" class="playlist-row">
              <input ref="playlistInput" v-model.trim="playlistInput" placeholder="粘贴网易歌单链接或ID" autocomplete="off" @keydown.enter="fetchPlaylist" />
              <button @click="fetchPlaylist">引入</button>
            </div>
          </div>
          <div class="tabs" role="tablist">
            <button :class="{on:activeTab==='search'}" role="tab" @click="activeTab='search'">搜索 {{ searchResults.length }}</button>
            <button :class="{on:activeTab==='queue'}" role="tab" @click="activeTab='queue'">队列 {{ queue.length }}</button>
          </div>
          <div ref="list" class="list">
            <div v-if="listLoading" class="empty"><div class="spinner"></div><div class="sm">{{ activeTab==='search' ? '墨正研开…' : '正引入歌单…' }}</div></div>
            <div v-else-if="visibleSongs.length===0" class="empty"><div class="big brush">无声处听惊雷</div><div class="sm">试试搜「周杰伦 / 陈奕迅 / 许嵩」<br/>或点右上角 ≡ 粘贴歌单链接</div></div>
            <div v-for="(song,index) in visibleSongs" v-else :key="`${song.id}-${index}`" class="song" :class="{on:isCurrent(song)}" tabindex="0" :aria-label="song.name" @click="playFromList(index)" @keydown.enter.prevent="playFromList(index)" @keydown.space.prevent="playFromList(index)">
              <span class="bar"></span>
              <div class="song-cover">
                <img v-if="song.cover" :src="song.cover" loading="lazy" alt="" />
                <div v-else class="cover-placeholder">♪</div>
              </div>
              <div class="song-meta"><div class="t">{{ song.name }}</div><div class="a">{{ song.artist }} · {{ song.album || '未知' }}</div></div>
              <div v-if="isCurrent(song)&&isPlaying" class="eq" aria-hidden="true"><span></span><span></span><span></span></div>
              <div class="song-acts">
                <button v-if="activeTab==='search'" type="button" class="song-act" :class="{added:inQueue(song)}" :title="inQueue(song)?'已在队列':'加入队列'" :aria-label="inQueue(song)?'已在队列':'加入队列'" @click.stop="addToQueue(song)" @keydown.enter.stop @keydown.space.stop>{{ inQueue(song)?'✓':'＋' }}</button>
                <button v-else type="button" class="song-act" title="从队列移除" aria-label="从队列移除" @click.stop="removeFromQueue(index)" @keydown.enter.stop @keydown.space.stop>✕</button>
                <button type="button" class="play-dot" title="播放" aria-label="播放" @click.stop="playFromList(index)" @keydown.enter.stop @keydown.space.stop><i class="icon-play"></i></button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="center">
        <div class="panel center-card">
          <div class="c-inner">
            <div class="disk-area">
              <div class="disk-wrap">
                <div class="ring"></div><div class="ring2"></div>
                <div ref="disk" class="disk" :class="{rolling:isPlaying}">
                  <div class="disk-inner"><div class="disk-cover">
                    <img v-if="currentSong&&currentSong.cover" :src="currentSong.cover" alt="" />
                    <div v-else class="cover-placeholder large">♪</div>
                    <div class="grain"></div><div class="spindle"></div>
                  </div></div>
                  <div class="seal brush" :class="{live:isPlaying}">澜</div>
                </div>
              </div>
            </div>
            <div class="meta-center">
              <h2>{{ currentSong ? currentSong.name : '未在播放' }}</h2>
              <div class="sub">{{ currentSong ? `${currentSong.artist} — ${currentSong.album || '单曲'}` : '寻一首心动，染半纸流云' }}</div>
            </div>
            <div class="quality-row">
              <label for="quality-select">音质</label>
              <select id="quality-select" v-model="selectedQuality" title="选择播放和下载音质" @change="reloadCurrentSong">
                <option value="standard">标准（128 kbps MP3）</option>
                <option value="higher">较高（192 kbps MP3）</option>
                <option value="exhigh">极高（320 kbps MP3）</option>
                <option value="lossless">无损（FLAC）</option>
                <option value="hires">Hi-Res（高清高解析度）</option>
                <option value="jymaster" selected>母带（Master）</option>
                <option value="sky">天空音效（Sky）</option>
                <option value="jyeffect">动态音效（Effect）</option>
              </select>
              <span class="q-status">{{ qualityStatus }}</span>
            </div>
            <div class="progress-zone">
              <div class="canvas-wrap" aria-label="进度条" @pointerdown="startSeek" @pointermove="moveSeek" @pointerup="endSeek">
                <canvas ref="wave"></canvas>
                <div class="track"></div><div class="fill" :style="{width:progressPercent+'%'}"></div>
                <div class="thumb" :style="{left:progressPercent+'%'}"><div class="thumb-box"><i></i></div></div>
              </div>
              <div class="time-row"><span>{{ formatTime(currentTime) }}</span><span>{{ formatTime(audioDuration) }}</span></div>
            </div>
            <div class="ctrls">
              <div class="ctrl-left">
                <button class="btn" :title="playModeTitle" :aria-label="playModeTitle" @click="switchPlayMode">{{ playModeIcon }}</button>
                <div class="vol"><span>🔊</span><input v-model.number="volume" type="range" min="0" max="1" step="0.01" aria-label="音量" /></div>
              </div>
              <div class="ctrl-mid">
                <button class="btn" aria-label="上一首" @click="playPrevious">⏮</button>
                <button class="btn btn-big" :aria-label="isPlaying?'暂停':'播放'" @click="togglePlay"><span class="ico" :class="{ play: !isPlaying }">{{ isPlaying?'⏸':'▶' }}</span></button>
                <button class="btn" aria-label="下一首" @click="playNext">⏭</button>
              </div>
              <div class="ctrl-right">
                <button class="btn" title="下载音频" aria-label="下载音频" @click="downloadAudio">⬇</button>
                <button class="btn" title="下载歌词" aria-label="下载歌词" @click="downloadLyric">♬</button>
              </div>
            </div>
            <div v-if="isLoadingSong" class="note"><i></i><span>正在取音·连缀古今…</span></div>
          </div>
        </div>
      </div>

      <div class="right">
        <div class="lyric-card panel">
          <div class="l-head">
            <div class="ll"><div class="dot">♪</div><div><div class="title">詞</div><div class="sub">句句如墨 · 行行成画</div></div></div>
            <div class="seal-mini brush">印</div>
          </div>
          <div ref="lyricBody" class="l-body" tabindex="0" @wheel="handleLyricWheel" @scroll="handleLyricScroll">
            <div v-if="lyricLines.length===0" class="l-empty">
              <div class="brush empty-title">静待一曲</div>
              <p>水墨未干，音未起时，留白最是撩人。<br/>播放后，歌词如卷轴展开。</p>
            </div>
            <div v-else class="l-list">
              <div v-for="(line,index) in lyricLines" :key="`${line.time}-${index}`" :data-index="index" class="l-line" :class="{on:index===activeLyric}" tabindex="0" @click="seekLyric(line.time)" @keydown.enter.prevent="seekLyric(line.time)">
                <div class="t">{{ line.text }}</div><div v-if="line.trans" class="tr">{{ line.trans }}</div>
              </div>
              <div class="lyric-end">— 曲终 · 墨未尽 —</div>
            </div>
          </div>
          <div class="l-foot"><span>水墨 · 留白 · 呼吸</span></div>
        </div>
      </div>
    </div>

    <footer>
      <div class="foot-line"><div class="l"></div><span class="brush">墨澜 · 星缘新风</span><div class="l"></div></div>
    </footer>
    <audio ref="audio" crossorigin="anonymous" preload="metadata" playsinline></audio>
  </div>
</template>

<script>
import { getLyricApi, getMusicApi, getPlaylistApi, searchSongsApi } from '@/api/music'

// 波形描边的兜底值：CSS 变量读不到时（SSR / 极老浏览器）仍能画出来
const WAVE_FALLBACK = { active: 'rgba(26,26,26,0.22)', idle: 'rgba(26,26,26,0.08)', verm: 'rgba(192,53,43,0.16)' }

const QUALITY_NAMES = {
  standard: '标准 128kbps', higher: '较高 192kbps', exhigh: '极高 320kbps',
  lossless: '无损 FLAC', hires: 'Hi-Res 高清高解析度', jymaster: '母带 Master',
  sky: '天空音效 Sky', jyeffect: '动态音效 Effect'
}

function extractArtists(track) {
  const artists = track.ar || track.artists || track.artist || ''
  if (Array.isArray(artists)) return artists.map(artist => typeof artist === 'object' ? artist.name : String(artist)).join(' / ')
  return String(artists || '')
}

function extractAlbum(track) {
  const album = track.al || track.album || ''
  if (typeof album === 'object' && album !== null) return album.name || ''
  return String(album || '')
}

function extractCover(track) {
  if (track.al && typeof track.al === 'object' && track.al.picUrl) return track.al.picUrl
  if (track.picUrl) return track.picUrl
  if (track.album && typeof track.album === 'object' && track.album.picUrl) return track.album.picUrl
  return ''
}

function normalizeSong(song) {
  return {
    id: song.id,
    name: song.name || '未知',
    artist: extractArtists(song),
    album: extractAlbum(song),
    cover: (extractCover(song) || '').replace('http://', 'https://'),
    duration: song.duration || song.dt || 0
  }
}

export default {
  name: 'ResourcesView',
  data() {
    return {
      keyword: '许嵩', playlistInput: '', importOpen: false, searchResults: [], queue: [], currentIndex: -1,
      searchAbortController: null,
      activeTab: 'search', listLoading: false, isLoadingSong: false, isPlaying: false,
      currentTime: 0, audioDuration: 0, volume: 0.85, selectedQuality: 'jymaster',
      qualityStatus: '', currentAudioUrl: '', lyricRaw: { lrc: '', tlrc: '' },
      lyricLines: [], activeLyric: 0, playMode: 'loop', isSeeking: false,
      lyricProgrammatic: false, userScrollingLyric: false, lyricPauseTimer: null,
      waveAnimationId: 0, waveTime: 0, waveRunning: true, songLoadToken: 0,
      waveColors: null, themeObserver: null
    }
  },
  computed: {
    visibleSongs() { return this.activeTab === 'search' ? this.searchResults : this.queue },
    currentSong() { return this.currentIndex >= 0 ? this.queue[this.currentIndex] : null },
    progressPercent() { return this.audioDuration ? Math.min(100, (this.currentTime / this.audioDuration) * 100) : 0 },
    playModeIcon() { return { loop: '↻', shuffle: '⇄', one: '①' }[this.playMode] },
    playModeTitle() { return { loop: '循环模式', shuffle: '随机播放', one: '单曲循环' }[this.playMode] },
    audio() { return this.$refs.audio }
  },
  watch: {
    volume(value) { if (this.audio) this.audio.volume = value }
  },
  mounted() {
    this.audio.volume = this.volume
    this.bindAudioEvents()
    this.syncWaveColors()
    this.startWaveAnimation()
    document.addEventListener('visibilitychange', this.handleVisibilityChange)
    window.addEventListener('resize', this.resizeWave)
    window.addEventListener('keydown', this.handleKeydown)
    // canvas 的颜色不在 CSS 里，主题切换时要自己重新取一次令牌再重绘。
    // 只监听 data-theme，别用通配：滚动时会有一堆高频属性变更打进来。
    this.themeObserver = new MutationObserver(() => {
      this.syncWaveColors()
      if (this.waveRunning) this.drawWave()
    })
    this.themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
    this.handleSearch('许嵩')
  },
  beforeUnmount() {
    if (this.themeObserver) { this.themeObserver.disconnect(); this.themeObserver = null }
    this.stopWaveAnimation()
    document.removeEventListener('visibilitychange', this.handleVisibilityChange)
    window.removeEventListener('resize', this.resizeWave)
    window.removeEventListener('keydown', this.handleKeydown)
    clearTimeout(this.lyricPauseTimer)
    this.songLoadToken += 1
    if (this.audio) { this.audio.pause(); this.audio.removeAttribute('src') }
  },
  methods: {
    isCurrent(song) { return Boolean(this.currentSong && String(this.currentSong.id) === String(song.id)) },
    formatTime(time) {
      if (!time || Number.isNaN(time)) return '0:00'
      const minutes = Math.floor(time / 60)
      const seconds = Math.floor(time % 60)
      return `${minutes}:${String(seconds).padStart(2, '0')}`
    },
    parseLRC(lrc, translatedLyric = '') {
      const map = new Map(); const translatedMap = new Map()
      const pattern = /\[(\d+):(\d+)(?:\.(\d+))?\](.*)/
      const addLines = (text, target) => {
        text.split('\n').forEach(line => {
          const match = line.match(pattern)
          if (!match) return
          const minute = parseInt(match[1], 10); const second = parseInt(match[2], 10)
          const millisecond = match[3] ? parseInt(match[3].padEnd(3, '0').slice(0, 3), 10) : 0
          const time = Math.floor((minute * 60 + second + millisecond / 1000) * 100) / 100
          const content = match[4].trim()
          if (content) target.set(time, content)
        })
      }
      addLines(lrc, map)
      if (translatedLyric) addLines(translatedLyric, translatedMap)
      const lines = []
      map.forEach((text, time) => {
        let translation = translatedMap.get(time)
        if (!translation) {
          for (const [translatedTime, translatedText] of translatedMap) {
            if (Math.abs(translatedTime - time) < 0.6) { translation = translatedText; break }
          }
        }
        lines.push({ time, text, trans: translation })
      })
      if (lines.length === 0 && lrc.trim()) {
        lrc.split('\n').filter(line => line.trim()).slice(0, 120).forEach((line, index) => {
          lines.push({ time: index * 3, text: line.trim(), trans: '' })
        })
      }
      return lines.sort((a, b) => a.time - b.time)
    },
    async handleSearch(keyword, event) {
      if (event && typeof event.preventDefault === 'function') event.preventDefault()
      const value = (typeof keyword === 'string' ? keyword : this.keyword || '').trim()
      if (!value) return
      if (this.searchAbortController) this.searchAbortController.abort()
      this.searchAbortController = new AbortController()
      this.keyword = value; this.activeTab = 'search'; this.listLoading = true
      try {
        const body = await searchSongsApi(value, this.searchAbortController.signal)
        let tracks = body.data
        if (tracks && typeof tracks === 'object' && !Array.isArray(tracks)) tracks = tracks.songs || []
        this.searchResults = (tracks || []).map(normalizeSong)
      } catch (error) {
        if (error?.name === 'AbortError') return
        this.searchResults = []
      } finally {
        this.listLoading = false
        this.$nextTick(() => { if (this.$refs.list) this.$refs.list.scrollTop = 0 })
      }
    },
    async fetchPlaylist() {
      const input = this.playlistInput.trim()
      if (!input) return
      const match = input.match(/(?:playlist\?id=|id=)(\d+)/) || input.match(/(\d{5,})/)
      const playlistId = match ? match[1] : input
      this.activeTab = 'search'; this.listLoading = true
      try {
        const body = await getPlaylistApi(playlistId)
        let tracks = []
        if (body?.data) {
          if (Array.isArray(body.data)) tracks = body.data
          else if (body.data.tracks) tracks = body.data.tracks
        }
        if (tracks.length === 0 && body.tracks) tracks = body.tracks
        const songs = tracks.map(normalizeSong)
        if (songs.length > 0) {
          this.queue = [...songs, ...this.queue]
          this.searchResults = songs
          this.playlistInput = ''
          this.importOpen = false
        }
      } catch (error) {} finally { this.listLoading = false }
    },
    playFromList(index) {
      if (this.activeTab === 'search') {
        const song = this.searchResults[index]
        if (!song) return
        const existingIndex = this.queue.findIndex(item => String(item.id) === String(song.id))
        if (existingIndex >= 0) this.currentIndex = existingIndex
        else { this.queue = [song, ...this.queue]; this.currentIndex = 0 }
        this.loadSong(song)
      } else { this.playAt(index) }
    },
    playAt(index) {
      if (index < 0 || index >= this.queue.length) return
      this.currentIndex = index
      this.loadSong(this.queue[index])
    },
    inQueue(song) { return this.queue.some(item => String(item.id) === String(song.id)) },
    addToQueue(song) {
      if (!song || this.inQueue(song)) return
      this.queue = [...this.queue, song]
    },
    removeFromQueue(index) {
      const removed = this.queue[index]
      if (!removed) return
      const wasCurrent = index === this.currentIndex
      this.queue.splice(index, 1)
      if (!this.queue.length) { this.currentIndex = -1; this.stopPlayback(); return }
      if (index < this.currentIndex) { this.currentIndex -= 1 }
      else if (wasCurrent) { this.playAt(Math.min(index, this.queue.length - 1)) }
    },
    stopPlayback() {
      this.audio.pause()
      this.audio.removeAttribute('src')
      this.isPlaying = false
      this.currentTime = 0; this.audioDuration = 0
      this.currentAudioUrl = ''; this.qualityStatus = ''
      this.lyricRaw = { lrc: '', tlrc: '' }
      this.lyricLines = []; this.activeLyric = 0
    },
    async loadSong(song) {
      const token = ++this.songLoadToken
      this.isLoadingSong = true; this.lyricLines = []; this.activeLyric = 0; this.qualityStatus = ''
      try {
        const [musicBody, lyricBody] = await Promise.all([
          getMusicApi(song.id, this.selectedQuality).catch(() => null),
          getLyricApi(song.id).catch(() => null)
        ])
        if (token !== this.songLoadToken) return
        let musicUrl = ''; let actualQuality = ''
        if (musicBody?.code === 200) {
          let data = musicBody.data
          if (Array.isArray(data)) data = data[0]
          musicUrl = data?.url || ''
          actualQuality = data?.level || ''
        }
        this.currentAudioUrl = musicUrl
        this.qualityStatus = actualQuality ? (QUALITY_NAMES[actualQuality] || actualQuality) : ''
        if (musicUrl) {
          this.audio.src = musicUrl
          await this.audio.play().catch(() => {})
        }
        let lyric = ''; let translatedLyric = ''
        if (lyricBody) {
          const data = lyricBody.code === 200 ? lyricBody.data : lyricBody
          lyric = data?.lrc || data?.lyric || ''
          translatedLyric = data?.tlyric || data?.tlrc || ''
          if (typeof lyric !== 'string') lyric = ''
          if (typeof translatedLyric !== 'string') translatedLyric = ''
        }
        this.lyricRaw = { lrc: lyric, tlrc: translatedLyric }
        this.lyricLines = this.parseLRC(lyric, translatedLyric)
        this.activeLyric = 0
        this.$nextTick(() => this.scrollLyricTo(0, false))
      } catch (error) {
        console.warn(error)
      } finally {
        if (token === this.songLoadToken) this.isLoadingSong = false
      }
    },
    reloadCurrentSong() { if (this.currentSong) this.loadSong(this.currentSong) },
    togglePlay() {
      if (!this.currentSong) return
      if (!this.audio.src) { this.loadSong(this.currentSong); return }
      if (this.audio.paused) this.audio.play().catch(() => {})
      else this.audio.pause()
    },
    playPrevious() { if (this.queue.length) this.playAt((this.currentIndex - 1 + this.queue.length) % this.queue.length) },
    playNext() { if (this.queue.length) this.playAt((this.currentIndex + 1) % this.queue.length) },
    switchPlayMode() { this.playMode = { loop: 'shuffle', shuffle: 'one', one: 'loop' }[this.playMode] },
    toggleImport() {
      this.importOpen = !this.importOpen
      if (this.importOpen) this.$nextTick(() => this.$refs.playlistInput?.focus())
    },
    // 空格播放/暂停，左右方向键前后 5 秒。输入框、按钮、歌曲行自己处理过的按键一律让路。
    handleKeydown(event) {
      if (event.defaultPrevented || event.metaKey || event.ctrlKey || event.altKey) return
      const target = event.target
      if (target?.isContentEditable) return
      if (target?.closest?.('input, select, textarea, button, a[href], [role="button"], [role="tab"]')) return
      if (event.key === ' ' || event.code === 'Space') { event.preventDefault(); this.togglePlay() }
      else if (event.key === 'ArrowRight') { event.preventDefault(); this.seekBy(5) }
      else if (event.key === 'ArrowLeft') { event.preventDefault(); this.seekBy(-5) }
    },
    seekBy(seconds) {
      if (!this.audio || !this.audioDuration) return
      this.audio.currentTime = Math.max(0, Math.min(this.audioDuration, (this.audio.currentTime || 0) + seconds))
    },
    bindAudioEvents() {
      this.audio.addEventListener('timeupdate', () => {
        this.currentTime = this.audio.currentTime || 0
        this.audioDuration = this.audio.duration || this.audioDuration
        if (!this.lyricLines.length) return
        let index = 0
        this.lyricLines.forEach((line, lineIndex) => { if (this.currentTime >= line.time) index = lineIndex })
        if (index !== this.activeLyric) {
          this.activeLyric = index
          if (!this.userScrollingLyric) this.$nextTick(() => this.scrollLyricTo(index, true))
        }
      })
      this.audio.addEventListener('loadedmetadata', () => { this.audioDuration = this.audio.duration })
      this.audio.addEventListener('play', () => { this.isPlaying = true })
      this.audio.addEventListener('pause', () => { this.isPlaying = false })
      this.audio.addEventListener('ended', () => {
        if (this.playMode === 'one') { this.audio.currentTime = 0; this.audio.play() }
        else if (this.playMode === 'shuffle') this.playAt(Math.floor(Math.random() * this.queue.length))
        else this.playNext()
      })
    },
    seekTo(clientX) {
      if (!this.audioDuration) return
      const rect = this.$el.querySelector('.canvas-wrap').getBoundingClientRect()
      const percent = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width))
      this.audio.currentTime = percent * this.audioDuration
      this.currentTime = this.audio.currentTime
    },
    startSeek(event) { this.isSeeking = true; event.currentTarget.setPointerCapture(event.pointerId); this.seekTo(event.clientX) },
    moveSeek(event) { if (this.isSeeking) this.seekTo(event.clientX) },
    endSeek() { this.isSeeking = false },
    seekLyric(time) { if (this.audio.duration) this.audio.currentTime = time },
    handleLyricWheel(event) {
      const container = this.$refs.lyricBody
      const atTop = container.scrollTop <= 0
      const atBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - 1
      if ((atTop && event.deltaY < 0) || (atBottom && event.deltaY > 0)) event.preventDefault()
      event.stopPropagation()
    },
    handleLyricScroll() {
      if (this.lyricProgrammatic) return
      this.userScrollingLyric = true
      clearTimeout(this.lyricPauseTimer)
      this.lyricPauseTimer = setTimeout(() => { this.userScrollingLyric = false }, 3500)
    },
    scrollLyricTo(index, smooth) {
      const container = this.$refs.lyricBody
      const target = this.$el.querySelector(`.l-line[data-index="${index}"]`)
      if (!container || !target) return
      const desired = target.offsetTop - container.clientHeight / 2 + target.offsetHeight / 2
      const max = container.scrollHeight - container.clientHeight
      this.lyricProgrammatic = true
      container.scrollTo({ top: Math.max(0, Math.min(desired, max)), behavior: smooth ? 'smooth' : 'auto' })
      setTimeout(() => { this.lyricProgrammatic = false }, smooth ? 600 : 50)
    },
    async downloadAudio() {
      if (!this.currentAudioUrl || !this.currentSong) return
      try {
        const response = await fetch(this.currentAudioUrl)
        const blob = await response.blob()
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        const extension = (this.currentAudioUrl.match(/\.([a-z0-9]+)(?:\?|$)/i) || [])[1] || ['standard', 'higher', 'exhigh'].includes(this.selectedQuality) ? 'mp3' : 'flac'
        link.href = url; link.download = `${this.currentSong.artist}-${this.currentSong.name}.${extension}`; link.click()
        setTimeout(() => URL.revokeObjectURL(url), 2000)
      } catch (error) { window.open(this.currentAudioUrl, '_blank') }
    },
    downloadLyric() {
      if (!this.currentSong) return
      const text = `${this.lyricRaw.lrc}\n\n[翻译]\n${this.lyricRaw.tlrc}`
      const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url; link.download = `${this.currentSong.name || 'lyric'}.lrc`; link.click()
      setTimeout(() => URL.revokeObjectURL(url), 1500)
    },
    /**
     * 读一次波形描边的颜色并缓存。逐帧 getComputedStyle 会强制样式重算，
     * 而播放时 canvas 是 60fps，所以只在挂载和 data-theme 变化时读。
     */
    syncWaveColors() {
      const style = this.$el ? getComputedStyle(this.$el) : null
      const read = (name, fallback) => (style && style.getPropertyValue(name).trim()) || fallback
      this.waveColors = {
        active: read('--wave-ink', WAVE_FALLBACK.active),
        idle: read('--wave-ink-idle', WAVE_FALLBACK.idle),
        verm: read('--wave-verm', WAVE_FALLBACK.verm)
      }
    },
    startWaveAnimation() { this.waveRunning = true; this.drawWave() },
    stopWaveAnimation() { this.waveRunning = false; cancelAnimationFrame(this.waveAnimationId) },
    handleVisibilityChange() {
      this.waveRunning = document.visibilityState === 'visible'
      if (this.waveRunning) this.drawWave()
    },
    resizeWave() { this.$nextTick(this.drawWave) },
    drawWave() {
      const canvas = this.$refs.wave
      if (!canvas) return
      const context = canvas.getContext('2d')
      const pixelRatio = Math.min(window.devicePixelRatio || 1, 1.6)
      const rect = canvas.getBoundingClientRect()
      canvas.width = rect.width * pixelRatio; canvas.height = rect.height * pixelRatio
      context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
      this.waveTime += 0.015
      const width = rect.width; const height = rect.height
      context.clearRect(0, 0, width, height)
      context.beginPath(); context.moveTo(0, height / 2)
      for (let x = 0; x < width; x += 2) {
        const amplitude = this.isPlaying ? (width < 400 ? 18 : 28) + Math.sin(this.waveTime * 2) * 6 : 4
        const y = height / 2 + Math.sin(x * 0.016 + this.waveTime * (this.isPlaying ? 2.6 : 0.25)) * amplitude + Math.sin(x * 0.008 - this.waveTime) * (this.isPlaying ? 10 : 1.2)
        context.lineTo(x, y)
      }
      const wave = this.waveColors || WAVE_FALLBACK
      context.strokeStyle = this.isPlaying ? wave.active : wave.idle; context.lineWidth = 1.6; context.stroke()
      context.beginPath(); context.moveTo(0, height / 2 + 10)
      for (let x = 0; x < width; x += 2) {
        const amplitude = this.isPlaying ? (width < 400 ? 10 : 16) : 2
        const y = height / 2 + 10 + Math.sin(x * 0.013 + this.waveTime * (this.isPlaying ? 2.2 : 0.2) + 1) * amplitude
        context.lineTo(x, y)
      }
      context.strokeStyle = wave.verm; context.lineWidth = 1.1; context.stroke()
      if (this.waveRunning) this.waveAnimationId = requestAnimationFrame(this.drawWave)
    }
  }
}
</script>

<style>
/* ============ 令牌 ============ */
.music-page{
  /* 底色 / 墨色。--ink-contrast 是 --ink 当背景色用时的前景色，--on-verm 是朱砂印上的字，
     --on-ink-rgb 是骑在 --ink 上的半透明白族。深色下这几个都要跟着翻。 */
  --paper:#fdf8ef;--paper-rgb:253,248,239;
  --ink:#1a1a1a;--ink-rgb:26,26,26;--ink-contrast:#fff;
  --verm:#c0352b;--verm-rgb:192,53,43;--on-verm:#fff;
  --on-ink-rgb:255,255,255;
  /* 灰阶：比旧版整体压暗一档，次级文字已经提到 12–13px，浅灰在这个字号上读不住 */
  --muted:#6f6f66;--muted-2:#54544c;--muted-3:#8a8a80;--faint:#45453f;
  /* 面：--surface 是不透明底（hover / 聚焦），--veil-* 是半透明白，深色下逐档换成暗值 */
  --card:rgba(255,255,255,0.8);--card-solid:#fffdf6;--surface:#fff;
  --veil-1:rgba(255,255,255,0.6);--veil-2:rgba(255,255,255,0.7);--veil-3:rgba(255,255,255,0.72);
  --cover-ph:#f0e8d8;--chip:#f3ebe0;--spin-track:rgba(0,0,0,0.1);
  /* 表单控件：UA 会给 input/button 写死 color:fieldtext，不继承 --ink */
  --field-text:#000;--field-ph:#6f6f66;
  --line:rgba(26,26,26,0.09);
  /* 歌词卷现在也是纸面，不是黑卡：两档弱化色跟着 --ink 走 */
  --lyric-bg:#fffdf6;--lyric-fg:#1a1a1a;
  --lyric-dim:rgba(26,26,26,0.62);--lyric-dim-2:rgba(26,26,26,0.45);
  /* 背景光斑与纸纹 */
  --blob-1:rgba(26,26,26,0.06);--blob-2:rgba(122,143,126,0.12);--blob-3:rgba(212,184,150,0.22);
  --grain-opacity:0.032;
  /* 进度波形描边（drawWave 用 getComputedStyle 读，见 syncWaveColors） */
  --wave-ink:rgba(26,26,26,0.22);--wave-ink-idle:rgba(26,26,26,0.08);--wave-verm:rgba(192,53,43,0.16);
  --safe-b:env(safe-area-inset-bottom,0px);--safe-t:env(safe-area-inset-top,0px);
}

/* === Reset === */
.music-page *,.music-page *::before,.music-page *::after{box-sizing:border-box;min-width:0;word-wrap:break-word;overflow-wrap:break-word}
.music-page{margin:0;width:100%;max-width:100vw;position:relative;overflow-x:clip;background:var(--paper);color:var(--ink);font:400 15px/1.55 "Noto Serif SC",serif;-webkit-text-size-adjust:100%;text-size-adjust:100%;-webkit-tap-highlight-color:transparent;touch-action:manipulation;min-height:100%}
.music-page img,.music-page canvas,.music-page svg{max-width:100%;display:block}
.music-page button,.music-page input,.music-page select{font:inherit;max-width:100%}
/* 只补给真正没写 color 的控件：通配写 color 会压掉按钮自己的自定义色 */
.music-page input:not([type='range']){color:var(--field-text)}
.music-page input::placeholder{color:var(--field-ph);opacity:1}
.music-page button{user-select:none;-webkit-user-select:none;cursor:pointer}
.music-page .brush{font-family:"Ma Shan Zheng",cursive}
.music-page ::selection{background:rgba(var(--verm-rgb),0.16)}

/* === 纸纹 / 光斑 === */
.music-page::before{content:"";position:fixed;inset:0;pointer-events:none;opacity:var(--grain-opacity);z-index:0;background-image:url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E")}
.blob{position:fixed;border-radius:50%;filter:blur(64px);pointer-events:none;z-index:0;will-change:transform}
.b1{width:min(520px,120vw);height:min(520px,120vw);top:-120px;right:-100px;background:radial-gradient(circle, var(--blob-1), transparent 70%)}
.b2{width:min(640px,130vw);height:min(640px,130vw);bottom:-160px;left:-120px;background:radial-gradient(circle, var(--blob-2), transparent 70%)}
.b3{width:min(320px,90vw);height:min(320px,90vw);top:40%;left:30%;background:radial-gradient(circle, var(--blob-3), transparent 70%)}

/* === 统一壳：标题行 / 三栏 / 页脚同一个 1600px 容器 === */
.shell{width:100%;max-width:1600px;margin:0 auto;padding-left:clamp(12px,2vw,28px);padding-right:clamp(12px,2vw,28px)}

/* === 标题行 === */
.music-hero{position:sticky;top:0;z-index:20;background:rgba(var(--paper-rgb),0.92);border-bottom:1px solid var(--line)}
@supports((backdrop-filter:blur(1px)) or (-webkit-backdrop-filter:blur(1px))){
  .music-hero{backdrop-filter:blur(12px) saturate(1.1);-webkit-backdrop-filter:blur(12px) saturate(1.1)}
}
.hero-inner{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:64px;padding-top:calc(10px + var(--safe-t));padding-bottom:10px}
.logo{display:flex;align-items:center;gap:12px;min-width:0;flex:1}
.logo-mark{width:38px;height:38px;flex-shrink:0;background:var(--verm);color:var(--on-verm);display:grid;place-items:center;font-size:20px;box-shadow:2px 2px 0 var(--ink);transform:rotate(-2deg);border-radius:2px}
.logo-text{min-width:0;flex:1}
.logo-text .top{display:flex;align-items:baseline;gap:8px;flex-wrap:wrap;min-width:0}
.logo-text h1{font-size:clamp(18px,2.4vw,22px);letter-spacing:0.18em;line-height:1;margin:0;white-space:nowrap}
.logo-text small{font-size:12px;letter-spacing:0.16em;color:var(--muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.logo-text p{font-size:12px;letter-spacing:0.1em;color:var(--muted);margin:3px 0 0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.hero-right{display:flex;align-items:center;gap:12px;font-size:12.5px;letter-spacing:0.14em;color:var(--muted-2);flex-shrink:0;white-space:nowrap}
.hero-right .sep{opacity:.25}
@media(max-width:900px){.hero-right{display:none}}
@media(max-width:600px){.hero-inner{padding-top:calc(8px + var(--safe-t));padding-bottom:8px}.logo-text p{display:none}}

/* === 三栏：等高靠 stretch + 列内 flex，只有歌单与歌词滚动 === */
.wrap{position:relative;z-index:1;display:grid;grid-template-columns:1fr;gap:clamp(12px,1.4vw,20px);align-items:stretch;padding-top:clamp(12px,1.6vw,22px);padding-bottom:clamp(12px,1.6vw,22px)}
.wrap > *{min-width:0}
/* 单列时播放器优先 */
.left{order:2}.center{order:1}.right{order:3}
.left,.center,.right{display:flex;min-width:0}
.left > .panel,.center > .center-card,.right > .lyric-card{flex:1;min-width:0;min-height:0}

.panel{background:var(--card);border:1px solid var(--line);border-radius:18px;box-shadow:0 8px 32px rgba(0,0,0,0.04);overflow:clip;display:flex;flex-direction:column;min-width:0;min-height:0}
@supports((backdrop-filter:blur(1px))){
  .panel{backdrop-filter:blur(18px);-webkit-backdrop-filter:blur(18px)}
}
@media(max-width:600px){.panel{border-radius:14px}}

@media(min-width:800px){
  .wrap{grid-template-columns:minmax(0,340px) minmax(0,1fr)}
  .right{grid-column:1 / -1}
  .left{order:1}.center{order:2}.right{order:3}
}
@media(min-width:1280px){
  .wrap{grid-template-columns:minmax(0,360px) minmax(0,1fr) minmax(0,400px)}
  .right{grid-column:auto}
}

/* === 左栏：搜索 + 歌单导入 === */
.s-head{padding:16px clamp(14px,1.6vw,20px) 10px;flex-shrink:0}
.s-title{display:flex;align-items:center;gap:10px;margin-bottom:12px}
.s-title .line{flex:1;height:1px;background:linear-gradient(to right,transparent, rgba(var(--ink-rgb),0.15))}
.s-title .line-rev{background:linear-gradient(to left,transparent, rgba(var(--ink-rgb),0.15))}
.s-title span{font-size:12px;letter-spacing:0.22em;color:var(--muted);white-space:nowrap}
.search-row{display:flex;align-items:center;gap:8px}
.search-box{position:relative;flex:1;min-width:0}
.search-box input{width:100%;height:44px;padding:0 44px 0 40px;border-radius:999px;border:1px solid rgba(var(--ink-rgb),0.12);background:var(--paper);outline:none;font-size:15px;letter-spacing:0.03em;transition:.2s}
.search-box input:focus{border-color:rgba(var(--ink-rgb),0.28);background:var(--surface);box-shadow:0 0 0 3px rgba(var(--ink-rgb),0.06)}
.search-box .i{position:absolute;left:14px;top:50%;transform:translateY(-50%);opacity:0.6;pointer-events:none}
.search-box .go{position:absolute;right:5px;top:50%;transform:translateY(-50%);width:34px;height:34px;border-radius:50%;background:var(--ink);color:var(--ink-contrast);border:0;display:grid;place-items:center;font-size:12px;line-height:1}
/* 更小的图标按钮：点开才露出歌单输入行 */
.import-btn{width:38px;height:38px;flex-shrink:0;border-radius:50%;border:1px dashed rgba(var(--ink-rgb),0.24);background:transparent;color:var(--muted-2);font-size:15px;line-height:1;display:grid;place-items:center;transition:.18s}
.import-btn:hover{border-color:rgba(var(--ink-rgb),0.4);color:var(--ink)}
.import-btn.on{border-style:solid;border-color:rgba(var(--ink-rgb),0.3);background:var(--surface);color:var(--ink)}
.playlist-row{margin-top:8px;display:flex;gap:8px;width:100%;min-width:0}
.playlist-row input{flex:1;min-width:0;height:36px;padding:0 14px;border-radius:999px;border:1px dashed rgba(var(--ink-rgb),0.18);background:transparent;font-size:13px;outline:none}
.playlist-row input:focus{border-style:solid;border-color:rgba(var(--ink-rgb),0.28);background:var(--veil-1)}
.playlist-row button{height:36px;padding:0 14px;border-radius:999px;border:1px solid var(--line);background:var(--chip);color:var(--field-text);font-size:13px;letter-spacing:0.1em;flex-shrink:0;white-space:nowrap}

.tabs{margin:0 clamp(12px,1.6vw,18px);display:flex;gap:4px;background:rgba(var(--ink-rgb),0.05);padding:4px;border-radius:999px;flex-shrink:0}
.tabs button{flex:1;min-width:0;height:32px;border-radius:999px;border:0;background:transparent;color:var(--muted-2);font-size:13px;letter-spacing:0.1em;transition:.2s;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.tabs button.on{background:var(--ink);color:var(--ink-contrast);box-shadow:0 2px 8px rgba(0,0,0,0.15)}

.list{flex:1 1 420px;min-height:0;overflow-y:auto;overflow-x:hidden;padding:10px 8px 14px;overscroll-behavior:contain;-webkit-overflow-scrolling:touch;scrollbar-width:thin}
.list::-webkit-scrollbar{width:4px;height:4px}
.list::-webkit-scrollbar-thumb{background:rgba(var(--ink-rgb),0.14);border-radius:999px}
@media(max-width:799px){.list{max-height:min(52vh,420px)}}
.empty{padding:40px 12px;text-align:center}
.empty .big{font-size:clamp(18px,5vw,24px);opacity:0.24;line-height:1.3}
.empty .sm{font-size:13px;letter-spacing:0.06em;color:var(--muted);margin-top:10px;line-height:1.7}

.song{position:relative;display:flex;align-items:center;gap:10px;padding:10px 10px 10px 13px;border-radius:12px;cursor:pointer;border:1px solid transparent;transition:.18s;overflow:hidden}
.song:hover{background:var(--surface);border-color:rgba(var(--ink-rgb),0.08)}
/* 播放中的一行：左侧朱砂竖条 + 淡朱砂底，歌词标题用朱砂 */
.song.on{background:rgba(var(--verm-rgb),0.06);border-color:rgba(var(--verm-rgb),0.16)}
.song.on:hover{background:rgba(var(--verm-rgb),0.09)}
.song .bar{position:absolute;left:0;top:50%;width:3px;height:62%;border-radius:0 3px 3px 0;background:var(--verm);opacity:0;transform:translateY(-50%) scaleY(.4);transition:.2s}
.song.on .bar{opacity:1;transform:translateY(-50%) scaleY(1)}
.song-cover{width:44px;height:44px;flex-shrink:0;border-radius:9px;overflow:hidden;background:var(--cover-ph);position:relative}
.song-cover img{width:100%;height:100%;object-fit:cover}
.cover-placeholder{display:grid;place-items:center;width:100%;height:100%;opacity:.3}
.cover-placeholder.large{font-size:36px}
.song-meta{flex:1;min-width:0;overflow:hidden}
.song-meta .t{font-size:14px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;letter-spacing:0.02em;line-height:1.3}
.song.on .t{color:var(--verm)}
.song-meta .a{font-size:12.5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:3px;letter-spacing:0.02em;color:var(--muted-2)}
/* 小律动器：只在「正在放」的那一行出现 */
.eq{display:flex;align-items:flex-end;gap:2px;height:14px;flex-shrink:0}
.eq span{width:2px;height:100%;background:var(--verm);border-radius:2px;transform-origin:bottom;animation:eqbar .9s ease-in-out infinite}
.eq span:nth-child(2){animation-delay:.25s}
.eq span:nth-child(3){animation-delay:.5s}
@keyframes eqbar{0%,100%{transform:scaleY(.28)}50%{transform:scaleY(1)}}
/* 悬浮才出现的加入队列 / 播放 */
.song-acts{display:flex;align-items:center;gap:6px;flex-shrink:0;opacity:0;transform:translateX(4px);transition:.18s}
.song:hover .song-acts,.song:focus-within .song-acts{opacity:1;transform:none}
@media(hover:none){.song-acts{opacity:1;transform:none}}
.song-act,.play-dot{width:28px;height:28px;flex-shrink:0;border-radius:50%;border:1px solid rgba(var(--ink-rgb),0.14);background:var(--paper);color:var(--ink);font-size:13px;line-height:1;display:grid;place-items:center;padding:0;transition:.16s}
.song-act:hover,.play-dot:hover{background:var(--ink);color:var(--ink-contrast);border-color:var(--ink)}
.song-act.added{opacity:.45}
.song-act.added:hover{background:var(--paper);color:var(--ink);border-color:rgba(var(--ink-rgb),0.14)}
.icon-play{width:0;height:0;border-left:7px solid currentColor;border-top:5px solid transparent;border-bottom:5px solid transparent;margin-left:2px}

/* === 中栏：唱片 + 进度 + 走带 === */
.center-card{border-radius:clamp(16px,2vw,26px);position:relative}
.c-inner{flex:1;display:flex;flex-direction:column;min-height:0;padding:clamp(16px,2.2vw,28px) clamp(14px,2vw,28px) 16px}
.disk-area{flex:1 1 auto;display:grid;place-items:center;min-height:0;width:100%;padding:2px 0}
/* 唱片吃掉可用空间：跟着视口高度伸缩，但不超过栏宽 */
.disk-wrap{position:relative;width:min(100%,clamp(200px,38vh,400px));aspect-ratio:1;margin:0 auto}
@media(max-height:820px) and (min-width:1280px){.disk-wrap{width:min(100%,clamp(180px,31vh,330px))}}
.ring{position:absolute;inset:0;border-radius:50%;border:1px solid rgba(var(--ink-rgb),0.07);pointer-events:none}
.ring2{position:absolute;inset:-6%;border-radius:50%;border:1px dashed rgba(var(--ink-rgb),0.09);pointer-events:none}
.disk{position:absolute;inset:0;border-radius:50%;overflow:hidden;box-shadow:0 12px 40px rgba(0,0,0,0.16), inset 0 0 0 1px rgba(0,0,0,0.1);background:var(--cover-ph);will-change:transform}
.disk-inner{position:absolute;inset:8%;border-radius:50%;background:var(--paper);padding:10%}
/* 封面即盘心标签 */
.disk-cover{width:100%;height:100%;border-radius:50%;overflow:hidden;position:relative;background:rgba(var(--ink-rgb),0.05)}
.disk-cover img{width:100%;height:100%;object-fit:cover}
.disk-cover .grain{position:absolute;inset:0;opacity:0.045;background-image:url("data:image/svg+xml,%3Csvg viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence baseFrequency='0.9'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");pointer-events:none}
.spindle{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:14%;aspect-ratio:1;border-radius:50%;background:var(--ink);border:3px solid var(--ink-contrast);box-shadow:0 2px 8px rgba(0,0,0,0.3);display:grid;place-items:center}
.spindle::after{content:"";width:8px;height:8px;border-radius:50%;background:var(--verm);display:block}
.seal{position:absolute;bottom:11%;right:11%;width:18%;aspect-ratio:1;background:var(--verm);color:var(--on-verm);display:grid;place-items:center;font-size:clamp(14px,3.5vw,18px);transform:rotate(3deg);box-shadow:2px 2px 0 var(--ink);mix-blend:multiply;pointer-events:none;border-radius:2px}
/* 「澜」印在播放时会动 */
.seal.live{animation:sealwobble 3.2s ease-in-out infinite}
@keyframes sealwobble{0%,100%{transform:rotate(3deg) translate(0,0)}50%{transform:rotate(-4deg) translate(-2px,-2px)}}
@keyframes rot{from{transform:rotate(0)}to{transform:rotate(360deg)}}
.disk.rolling{animation:rot 18s linear infinite}

.meta-center{text-align:center;margin-top:clamp(10px,1.4vw,18px);flex-shrink:0;overflow:hidden}
.meta-center h2{font-size:clamp(18px,1.8vw,24px);font-weight:700;letter-spacing:0.08em;line-height:1.3;margin:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.meta-center .sub{margin-top:6px;font-size:13px;letter-spacing:0.1em;color:var(--muted-2);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}

/* 音质选择：与进度条同一行节奏，状态就地显示 */
.quality-row{display:flex;align-items:center;justify-content:center;gap:8px;margin-top:10px;font-size:12.5px;color:var(--muted-2);flex-shrink:0}
.quality-row select{max-width:230px;padding:5px 10px;border:1px solid var(--line);border-radius:999px;background:var(--veil-3);color:var(--ink);font-size:12.5px;outline:none}
.q-status{font-size:12.5px;letter-spacing:0.04em;color:var(--muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}

/* 进度：加粗的朱砂条，整条可点可拖 */
.progress-zone{margin-top:clamp(10px,1.6vw,18px);width:100%;min-width:0;flex-shrink:0}
.canvas-wrap{position:relative;height:34px;cursor:pointer;width:100%;overflow:clip;border-radius:8px;touch-action:none}
.canvas-wrap canvas{position:absolute;inset:0;width:100%;height:100%}
.track{position:absolute;left:0;right:0;top:50%;transform:translateY(-50%);height:4px;border-radius:999px;background:rgba(var(--ink-rgb),0.1);pointer-events:none}
.fill{position:absolute;left:0;top:50%;transform:translateY(-50%);height:6px;border-radius:999px;background:var(--verm);pointer-events:none;max-width:100%}
.thumb{position:absolute;top:50%;transform:translate(-50%,-50%);pointer-events:none}
@media(prefers-reduced-motion:no-preference){.thumb{transition:left .12s linear}}
.thumb-box{width:14px;height:14px;border-radius:50%;background:var(--verm);box-shadow:0 0 0 3px rgba(var(--verm-rgb),0.16)}
.thumb-box i{display:none}
.time-row{display:flex;justify-content:space-between;font-size:12.5px;letter-spacing:0.1em;color:var(--muted);margin-top:4px;padding:0 2px}

/* 走带：防溢出 + 大触靶 */
/* 左右两列等宽（1fr），中间列按内容定宽：播放键永远落在行的正中心，不受两侧按钮数量影响 */
.ctrls{margin-top:14px;display:grid;grid-template-columns:1fr auto 1fr;align-items:center;column-gap:clamp(20px,1.8vw,28px);width:100%;min-width:0;flex-shrink:0}
/* 两侧列等宽（1fr）才能让播放键落在正中心；因此两侧组必须「撑满自己那一列再让内容收缩」，
   若按内容取宽（start/end 对齐），窄档里音量胶囊会溢出列宽、把 1fr 的列间距吃掉，上一曲就贴上音量 */
.ctrl-left,.ctrl-right{display:flex;align-items:center;gap:8px;min-width:0}
.ctrl-left{justify-self:stretch}
.ctrl-right{justify-self:stretch;justify-content:flex-end}
.ctrl-mid{display:flex;align-items:center;justify-content:center;gap:clamp(8px,1vw,12px);min-width:0}
.btn{width:40px;height:40px;flex-shrink:0;border-radius:50%;border:1px solid var(--line);background:var(--veil-2);display:grid;place-items:center;transition:.18s;font-size:13px;line-height:1;color:var(--ink)}
@media(max-width:600px){.btn{width:38px;height:38px}.ctrl-mid{gap:10px}}
.btn:hover{border-color:rgba(var(--ink-rgb),0.22);background:var(--surface)}
.btn:active{transform:scale(0.96)}
.btn-big{width:64px;height:64px;background:var(--ink);color:var(--ink-contrast);border:0;box-shadow:0 8px 24px rgba(0,0,0,0.24);font-size:22px}
.btn-big .ico{display:block;line-height:1;transform:translateY(0)}
/* ▶ 三角的重心偏左，只有播放态补 2px；⏸ 对称，严格居中 */
.btn-big .ico.play{margin-left:2px}
.btn-big:hover,.btn-big:focus,.btn-big:active{background:var(--ink);color:var(--ink-contrast)}
@media(max-width:600px){.btn-big{width:58px;height:58px;font-size:20px}}
@media(max-width:400px){.btn{width:34px;height:34px}.btn-big{width:54px;height:54px}.ctrl-mid{gap:8px}.ctrl-left,.ctrl-right{gap:6px}}
.vol{display:flex;align-items:center;gap:6px;border:1px solid var(--line);border-radius:999px;padding:0 10px;height:40px;background:var(--veil-1);min-width:0;flex:0 1 auto}
/* 滑杆可收缩（下限 34px）：列宽不足时音量胶囊自己变窄，而不是顶出去撞到上一曲 */
.vol input{width:64px;min-width:34px;flex:0 1 auto;accent-color:var(--ink)}
@media(max-width:760px){.vol{display:none}}
/* 两栏最窄档：中栏内宽 382–473px，音量胶囊塞不进侧列，让位 */
@media(min-width:800px) and (max-width:899px){.vol{display:none}}
/* 三栏最窄的一档：中栏内宽 380–480px，容不下「循环 + 音量 + 三个走带键 + 两个下载」，音量让位避免裁切 */
@media(min-width:1280px) and (max-width:1399px){.vol{display:none}}
.note{margin-top:10px;text-align:center;font-size:12.5px;letter-spacing:0.12em;color:var(--muted);display:flex;align-items:center;justify-content:center;gap:8px;flex-wrap:wrap;flex-shrink:0}
.note i{width:12px;height:12px;border:1.5px solid rgba(var(--ink-rgb),0.2);border-top-color:var(--ink);border-radius:50%;display:inline-block;animation:spin .7s linear infinite;flex-shrink:0}
@keyframes spin{to{transform:rotate(360deg)}}
.spinner{width:22px;height:22px;border:2px solid var(--spin-track);border-top-color:var(--ink);border-radius:50%;margin:0 auto;animation:spin 1s linear infinite}

/* === 右栏：纸上的词卷 === */
.lyric-card{background:var(--lyric-bg);color:var(--lyric-fg);border-radius:clamp(16px,2vw,24px);position:relative;box-shadow:0 8px 32px rgba(0,0,0,0.04)}
.l-head{padding:16px clamp(14px,1.6vw,20px) 8px;display:flex;align-items:center;justify-content:space-between;gap:10px;flex-shrink:0}
.l-head .ll{display:flex;align-items:center;gap:10px;min-width:0}
.l-head .ll > div:last-child{min-width:0}
.l-head .dot{width:28px;height:28px;flex-shrink:0;border-radius:50%;border:1px solid var(--line);color:var(--muted-2);display:grid;place-items:center;font-size:12px}
.l-head .title{font-size:13px;letter-spacing:0.2em;white-space:nowrap}
.l-head .sub{font-size:12px;color:var(--muted);letter-spacing:0.08em;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.seal-mini{width:26px;height:26px;flex-shrink:0;border-radius:3px;border:1px solid rgba(var(--verm-rgb),0.32);color:var(--verm);display:grid;place-items:center;font-size:12px;box-shadow:1px 1px 0 rgba(var(--verm-rgb),0.16)}
/* 词卷自身滚动；上下缘渐隐，滚动时不会硬切 */
.l-body{flex:1 1 520px;min-height:0;overflow-y:auto;overflow-x:hidden;padding:8px clamp(10px,1.4vw,16px);overscroll-behavior:contain;-webkit-overflow-scrolling:touch;scrollbar-width:thin;scrollbar-color:rgba(var(--ink-rgb),0.16) transparent;isolation:isolate;-webkit-mask-image:linear-gradient(to bottom,transparent 0,#000 12%,#000 88%,transparent 100%);mask-image:linear-gradient(to bottom,transparent 0,#000 12%,#000 88%,transparent 100%)}
.l-body::-webkit-scrollbar{width:4px}
.l-body::-webkit-scrollbar-track{background:transparent}
.l-body::-webkit-scrollbar-thumb{background:rgba(var(--ink-rgb),0.14);border-radius:999px}
@media(max-width:799px){.l-body{max-height:min(62vh,520px)}}
.l-empty{min-height:280px;height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:24px 12px;color:var(--muted-2)}
.empty-title{font-size:clamp(22px,6vw,28px);letter-spacing:0.15em;opacity:.55}
.empty-title + p{font-size:12.5px;margin-top:12px;letter-spacing:0.06em;line-height:1.9;opacity:.9;max-width:22em}
.l-list{padding:clamp(60px,18vh,140px) 0;width:100%}
/* 当前句放大居中，其余压成弱色 */
.l-line{position:relative;padding:9px 12px;border-radius:10px;transition:.22s;margin-bottom:4px;cursor:pointer;text-align:center}
.l-line:hover{background:rgba(var(--ink-rgb),0.04)}
.l-line.on{background:rgba(var(--verm-rgb),0.07)}
.l-line .t{font-size:14px;line-height:1.7;letter-spacing:0.02em;color:var(--lyric-dim);white-space:pre-wrap;word-break:break-word;overflow-wrap:break-word}
.l-line.on .t{font-size:clamp(17px,1.5vw,21px);font-weight:600;line-height:1.5;color:var(--lyric-fg)}
.l-line .tr{font-size:12.5px;margin-top:4px;line-height:1.6;color:var(--lyric-dim-2);white-space:pre-wrap;word-break:break-word}
.l-line.on .tr{color:var(--lyric-dim)}
.lyric-end{text-align:center;padding:18px 0 6px;font-size:12px;letter-spacing:0.24em;color:var(--lyric-dim-2)}
.l-foot{padding:10px clamp(12px,1.6vw,18px) calc(10px + var(--safe-b));border-top:1px solid var(--line);display:flex;justify-content:space-between;gap:10px;flex-wrap:wrap;font-size:12px;letter-spacing:0.1em;color:var(--muted);flex-shrink:0}

/* === 页脚 === */
.music-page footer{position:relative;z-index:1;max-width:1600px;margin:0 auto;width:100%;padding:14px clamp(12px,2vw,28px) calc(26px + var(--safe-b));text-align:center;overflow:clip}
.music-page footer .foot-line{display:flex;align-items:center;justify-content:center;gap:12px;opacity:0.4;flex-wrap:wrap}
.music-page footer .foot-line .l{width:40px;height:1px;background:var(--ink);flex-shrink:0}
.music-page footer .foot-line span{font-size:clamp(13px,3vw,15px);letter-spacing:0.05em;white-space:nowrap}

/* === 极窄屏 === */
@media(max-width:340px){
  .hero-inner{padding-top:calc(8px + var(--safe-t));padding-bottom:8px}
  .logo-mark{width:32px;height:32px;font-size:16px}
  .disk-wrap{width:min(100%,200px)}
  /* 极窄屏改单列堆叠：走带键独占一行居中，两侧键排到下面 */
  .ctrls{grid-template-columns:1fr;row-gap:10px;justify-items:center}
  .ctrl-mid{order:1;gap:12px}
  .ctrl-left{order:2;justify-self:center}
  .ctrl-right{order:3;justify-self:center}
}

/* === 减少动效 === */
@media(prefers-reduced-motion:reduce){
  .music-page *,.music-page *::before,.music-page *::after{animation:none !important;transition:none !important;scroll-behavior:auto !important}
}

/* === 夜墨 · 深色模式 ===

   音乐页自带一套「宣纸 + 墨 + 朱砂」的独立配色，不跟随全站的冷中性灰令牌
   （$dark 的 #101317 会把水墨调性洗掉）。深色下只翻极的两端：
     纸   — 宣纸 #fdf8ef  →  深墨纸 #14110e（暖调近黑，留一点棕，不是冷灰）
     墨   — 浓墨 #1a1a1a  →  月白   #e8e2d6
     朱砂 — #c0352b      →  #e0644a（原色在深底上只有 2.4:1，提亮后才撑得住）
   其余灰阶、面、线按同一比例推导。词卷现在是纸面，跟着同一条轴走。
   这里只写与浅色不同的部分。 */
:root[data-theme='dark'] .music-page{
  --paper:#14110e;--paper-rgb:20,17,14;
  --ink:#e8e2d6;--ink-rgb:232,226,214;--ink-contrast:#14110e;
  --verm:#e0644a;--verm-rgb:224,100,74;--on-verm:#fff;
  --on-ink-rgb:20,17,14;

  --muted:#a49e91;--muted-2:#c3bcac;--muted-3:#918b7e;--faint:#d6cfc0;

  --card:rgba(232,226,214,0.05);--card-solid:#1c1915;
  --surface:#26211b;
  --veil-1:rgba(232,226,214,0.07);--veil-2:rgba(232,226,214,0.08);--veil-3:rgba(232,226,214,0.09);
  --cover-ph:#2b2620;--chip:#26211c;--spin-track:rgba(232,226,214,0.14);
  --line:rgba(232,226,214,0.10);

  --lyric-bg:#1c1915;--lyric-fg:#e8e2d6;
  --lyric-dim:rgba(232,226,214,0.62);--lyric-dim-2:rgba(232,226,214,0.44);

  /* 光斑从墨色烟雾换成月晕 + 一层朱砂余温 */
  --blob-1:rgba(232,226,214,0.05);
  --blob-2:rgba(122,160,138,0.10);
  --blob-3:rgba(224,100,74,0.11);
  /* 纸纹在深底上原来那点 3.2% 完全看不见，提上来才有纸的质感 */
  --grain-opacity:0.055;

  --wave-ink:rgba(232,226,214,0.30);--wave-ink-idle:rgba(232,226,214,0.10);--wave-verm:rgba(224,100,74,0.26);

  --field-text:#e8e2d6;--field-ph:#a49e91;
}

/* 深底上 4% 的投影浮不起来，加重一档 */
:root[data-theme='dark'] .music-page .panel{
  box-shadow:0 8px 32px rgba(0,0,0,0.42);
}

/* sticky 顶栏的半透明壳：深色下换成深墨描边，别让后面的浅色纸透成一条亮边 */
:root[data-theme='dark'] .music-page .music-hero{
  border-bottom-color:rgba(232,226,214,0.08);
}
</style>
