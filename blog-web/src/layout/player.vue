<template>
  <div v-show="audio.length > 0" class="mj-player" aria-hidden="false">
    <!-- 折叠态：圆形唱盘，可拖拽，松手吸附到左右边缘 -->
    <div
      v-show="!open"
      ref="orb"
      class="mj-orb"
      :class="{ 'is-dragging': dragging }"
      :style="{ left: orbX + 'px', top: orbY + 'px', '--orb-dx': orbDx, '--chip-dx': chipDx }"
      role="button"
      tabindex="0"
      aria-label="展开播放器"
      @mouseenter="hovered = true"
      @mouseleave="hovered = false"
      @pointerdown="onOrbPointerDown"
      @click="onOrbClick"
      @keydown.enter.prevent="openPanel"
      @keydown.space.prevent="openPanel"
    >
      <svg class="mj-orb__ring" viewBox="0 0 76 76" aria-hidden="true">
        <circle class="mj-orb__ring-track" cx="38" cy="38" r="35" />
        <circle
          class="mj-orb__ring-bar"
          cx="38"
          cy="38"
          r="35"
          :stroke-dasharray="RING_C"
          :stroke-dashoffset="ringOffset"
        />
      </svg>

      <span class="mj-orb__disc" :class="{ 'is-spinning': !paused }" :style="discStyle">
        <span class="mj-orb__grooves"></span>
        <span class="mj-orb__hole"></span>
      </span>

      <button
        type="button"
        class="mj-orb__chip"
        :class="{ 'is-hidden': paused && !hovered }"
        :aria-label="paused ? '播放' : '暂停'"
        @pointerdown.stop
        @click.stop="toggle"
      >
        <svg v-if="paused" viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5.2v13.6L19 12z" /></svg>
        <svg v-else viewBox="0 0 24 24" aria-hidden="true"><path d="M7 5h3.4v14H7zM13.6 5H17v14h-3.4z" /></svg>
      </button>
    </div>

    <!-- 展开态：唱片封套面板 -->
    <section
      v-show="open"
      ref="panel"
      class="mj-panel"
      role="region"
      aria-label="音乐播放器"
      v-click-outside="onClickOutside"
    >
      <button type="button" class="mj-panel__close" aria-label="收起播放器" @click="closePanel">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
      </button>

      <div class="mj-panel__disc" :class="{ 'is-spinning': !paused }" :style="discStyle">
        <span class="mj-orb__grooves"></span>
        <span class="mj-orb__hole"></span>
      </div>

      <p class="mj-panel__title" :title="currentSong && currentSong.name">{{ currentSong ? currentSong.name : '暂无曲目' }}</p>
      <p class="mj-panel__artist">{{ currentSong && currentSong.artist ? currentSong.artist : '—' }}</p>

      <div v-show="!showList" ref="lrcBox" class="mj-panel__lrc">
        <p
          v-for="(line, i) in lyricLines"
          :key="i"
          class="mj-panel__lrc-line"
          :class="{ 'is-active': i === activeLine }"
          @click="seekTo(line.time)"
        >{{ line.text }}</p>
        <p v-if="!lyricLines.length" class="mj-panel__lrc-empty">
          {{ lyricLoading ? '歌词载入中…' : '暂无歌词' }}
        </p>
      </div>

      <ol v-show="showList" ref="listBox" class="mj-panel__list">
        <li
          v-for="(song, i) in audio"
          :key="i"
          class="mj-panel__list-item"
          :class="{ 'is-active': i === index }"
          @click="selectTrack(i)"
        >
          <span class="mj-panel__list-index">{{ String(i + 1).padStart(2, '0') }}</span>
          <span class="mj-panel__list-meta">
            <span class="mj-panel__list-name">{{ song.name }}</span>
            <span class="mj-panel__list-artist">{{ song.artist || '—' }}</span>
          </span>
        </li>
      </ol>

      <div class="mj-panel__time">
        <span>{{ formatTime(currentTime) }}</span>
        <span>{{ formatTime(duration) }}</span>
      </div>

      <div
        ref="bar"
        class="mj-panel__bar"
        role="slider"
        aria-label="播放进度"
        aria-valuemin="0"
        :aria-valuemax="Math.round(duration)"
        :aria-valuenow="Math.round(currentTime)"
        @pointerdown="onBarPointerDown"
      >
        <span class="mj-panel__bar-track"></span>
        <span class="mj-panel__bar-loaded" :style="{ width: loadedPct }"></span>
        <span class="mj-panel__bar-played" :style="{ width: progressPct }"></span>
        <span class="mj-panel__bar-thumb" :style="{ left: progressPct }"></span>
      </div>

      <div class="mj-panel__controls">
        <button type="button" class="mj-ctl" :title="orderTitle" :aria-label="orderTitle" @click="cycleOrder">
          <svg v-if="order === 'list'" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 7h16M4 12h16M4 17h10" />
          </svg>
          <svg v-else viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 7h4l8 10h4M4 17h4l8-10h4" />
            <path d="M17 4l3 3-3 3M17 14l3 3-3 3" />
          </svg>
        </button>

        <button type="button" class="mj-ctl" aria-label="上一首" @click="prev">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 5v14M19 5v14L9 12z" /></svg>
        </button>

        <button type="button" class="mj-ctl mj-ctl--play" :aria-label="paused ? '播放' : '暂停'" @click="toggle">
          <svg v-if="paused" viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5.2v13.6L19 12z" /></svg>
          <svg v-else viewBox="0 0 24 24" aria-hidden="true"><path d="M7 5h3.4v14H7zM13.6 5H17v14h-3.4z" /></svg>
        </button>

        <button type="button" class="mj-ctl" aria-label="下一首" @click="next">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M17 5v14M5 5v14l10-7z" /></svg>
        </button>

        <button type="button" class="mj-ctl" :title="loopTitle" :aria-label="loopTitle" @click="cycleLoop">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 12a8 8 0 0 1 8-8h4" />
            <path d="M13 1l3 3-3 3" />
            <path d="M20 12a8 8 0 0 1-8 8H8" />
            <path d="M11 23l-3-3 3-3" />
            <circle v-if="loop === 'one'" cx="12" cy="12" r="1.7" class="mj-ctl__dot" />
            <path v-if="loop === 'none'" d="M5 19L19 5" />
          </svg>
        </button>

        <button
          type="button"
          class="mj-ctl"
          :class="{ 'is-on': showList }"
          title="歌曲列表"
          aria-label="歌曲列表"
          @click="toggleList"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 6h16M4 12h10M4 18h10" />
            <circle cx="18" cy="16" r="2.4" />
            <path d="M20.4 16V9l-3 1" />
          </svg>
        </button>
      </div>

      <div class="mj-panel__volume">
        <button type="button" class="mj-ctl mj-ctl--vol" :aria-label="muted ? '取消静音' : '静音'" @click="toggleMute">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 9h3l4-3.5v13L7 15H4z" />
            <path v-if="!muted" d="M14.5 9.5a4 4 0 0 1 0 5M17 7a7 7 0 0 1 0 10" />
            <path v-else d="M15 10l4 4M19 10l-4 4" />
          </svg>
        </button>
        <input
          class="mj-panel__range"
          type="range"
          min="0"
          max="1"
          step="0.01"
          :value="volume"
          aria-label="音量"
          @input="onVolume"
        />
      </div>
    </section>

    <!-- APlayer 仅作为音频引擎与歌词解析器，视觉全部隐藏 -->
    <div ref="aplayer" class="mj-player__engine" aria-hidden="true"></div>
  </div>
</template>

<script>
import { getSongList } from '../api/file'
import APlayer from 'aplayer'
import 'aplayer/dist/APlayer.min.css'
import axios from 'axios'
import { decodeText } from '@/utils/encoding'

const RING_R = 35
const RING_C = 2 * Math.PI * RING_R
const ORB_SIZE = 56
const ORB_SIZE_SM = 48
const EDGE_MARGIN = 18
const TOP_MARGIN = 80
const ORB_POS_KEY = 'player-orb-pos'

export default {
  name: 'Player',
  data() {
    return {
      audio: [],
      isLoading: true,
      player: null,
      errorHandler: null,
      // 播放状态镜像（APlayer 内部状态不是响应式的，这里手动同步）
      index: 0,
      paused: true,
      currentTime: 0,
      duration: 0,
      loadedPct: '0%',
      volume: 0.7,
      muted: false,
      prevVolume: 0.7,
      order: 'list',
      loop: 'all',
      lyricLines: [],
      activeLine: 0,
      lyricLoading: false,
      showList: false,
      // 折叠 / 展开
      open: false,
      dragging: false,
      hovered: false,
      orbX: 0,
      orbY: 0,
      orbSide: 'right'
    }
  },
  computed: {
    RING_C() {
      return RING_C
    },
    currentSong() {
      return this.audio[this.index] || null
    },
    progress() {
      return this.duration > 0 ? Math.min(1, this.currentTime / this.duration) : 0
    },
    progressPct() {
      return `${(this.progress * 100).toFixed(2)}%`
    },
    ringOffset() {
      return RING_C * (1 - this.progress)
    },
    discStyle() {
      const cover = this.currentSong && this.currentSong.cover
      return cover ? { backgroundImage: `url("${cover}")` } : {}
    },
    // 播放中始终吸附在边缘（悬浮也不滑出），只有暂停时悬浮才滑出完整圆盘；
    // 拖拽时不平移，让圆盘跟随指针。
    orbDx() {
      if (this.dragging) return '0px'
      if (this.paused && this.hovered) {
        const shift = this.orbSize() / 2
        return this.orbSide === 'left' ? `${shift}px` : `-${shift}px`
      }
      return '0px'
    },
    // 播放中圆盘只露一半，把暂停键挪到露出的那半，保证可点
    chipDx() {
      if (this.dragging || this.paused) return '0px'
      const shift = this.orbSize() / 4
      return this.orbSide === 'left' ? `${shift}px` : `-${shift}px`
    },
    orderTitle() {
      return this.order === 'list' ? '顺序播放' : '随机播放'
    },
    loopTitle() {
      return { all: '列表循环', one: '单曲循环', none: '不循环' }[this.loop]
    }
  },
  async created() {
    // 非响应式引用，用来判断歌词是否已重新解析
    this._lyricRef = undefined
    this._drag = null
    this._dragEndAt = 0

    try {
      const resp = await getSongList()
      if (!resp.data || resp.data.length === 0) {
        console.warn('No songs found in the list')
        return
      }
      // 歌词不随歌单一次性下载，只保留地址，真正播放到某首歌时才按需请求
      const processedData = resp.data.map((item) => {
        if (item.cover === null || item.cover === undefined || item.cover === '') {
          item.cover = 'https://avatars2.githubusercontent.com/u/20062482?s=270'
        }
        item.lrcUrl = item.lrc || ''
        item.lrc = ''
        item.lrcLoading = false
        return item
      })
      this.audio = processedData

      this.$nextTick(() => {
        this.initAPlayer()
      })
    } catch (error) {
      console.error('Error loading song list:', error)
    } finally {
      this.isLoading = false
    }
  },
  mounted() {
    this.placeOrb()
    window.addEventListener('resize', this.onResize)
  },
  methods: {
    initAPlayer() {
      if (this.audio.length === 0) return

      if (this.player) {
        this.player.destroy()
      }

      this.player = new APlayer({
        container: this.$refs.aplayer,
        lrcType: 1,
        autoplay: false,
        theme: '#a86a10',
        listFolded: true,
        audio: this.audio
      })

      this.order = this.player.options.order
      this.loop = this.player.options.loop
      this.volume = this.player.audio.volume
      this.prevVolume = this.volume || 0.7
      this.muted = this.volume === 0

      this.bindPlayerEvents()
      this.bindPlayerErrorHandler()
      this.refreshLyric()
    },
    bindPlayerEvents() {
      const player = this.player

      player.on('play', () => {
        this.paused = false
        this.refreshLyric()
        this.loadCurrentLyric()
      })
      player.on('pause', () => {
        this.paused = true
      })
      player.on('timeupdate', () => {
        this.currentTime = player.audio.currentTime || 0
        this.duration = player.duration || this.duration
        const buffered = player.audio.buffered
        if (buffered && buffered.length && this.duration) {
          this.loadedPct = `${Math.min(100, (buffered.end(buffered.length - 1) / this.duration) * 100).toFixed(2)}%`
        }
        this.refreshLyric()
        this.updateActiveLine()
      })
      player.on('listswitch', (data) => {
        if (data && typeof data.index === 'number') this.index = data.index
        this.currentTime = 0
        this.activeLine = 0
        this.lyricLines = []
        this._lyricRef = undefined
        this.$nextTick(() => {
          this.refreshLyric()
          if (this.showList) this.scrollListToCurrent()
        })
      })
      player.on('seeked', () => {
        this.updateActiveLine()
      })
    },
    // APlayer 把解析后的歌词放在 lrc.parsed[index]，这里同步到响应式数据
    refreshLyric() {
      const player = this.player
      if (!player || !player.lrc) return
      const parsed = player.lrc.parsed && player.lrc.parsed[player.list.index]
      if (parsed === this._lyricRef) return
      this._lyricRef = parsed
      const lines = Array.isArray(parsed)
        ? parsed
            .map(([time, text]) => ({ time, text: String(text || '').trim() }))
            .filter((line) => line.text && line.text !== 'Not available' && line.text !== 'Loading')
        : []
      this.lyricLines = lines
      if (this.activeLine >= lines.length) this.activeLine = 0
    },
    updateActiveLine() {
      const lines = this.lyricLines
      if (!lines.length) return
      const t = this.currentTime
      let idx = 0
      for (let i = 0; i < lines.length; i++) {
        if (t >= lines[i].time) idx = i
        else break
      }
      if (idx !== this.activeLine) {
        this.activeLine = idx
        this.$nextTick(() => this.scrollLyricIntoView())
      }
    },
    scrollLyricIntoView() {
      const box = this.$refs.lrcBox
      if (!box || !this.lyricLines.length) return
      const el = box.children[this.activeLine]
      if (!el) return
      box.scrollTo({
        top: el.offsetTop - box.clientHeight / 2 + el.clientHeight / 2,
        behavior: 'smooth'
      })
    },
    async loadCurrentLyric() {
      const player = this.player
      if (!player || !player.lrc) return

      const index = player.list.index
      const song = this.audio[index]
      if (!song || !song.lrcUrl || song.lrc || song.lrcLoading) return

      song.lrcLoading = true
      this.lyricLoading = true
      try {
        const response = await axios.get(song.lrcUrl, { responseType: 'arraybuffer' })
        const contentType = response.headers['content-type'] || ''
        const charsetMatch = contentType.match(/charset=["']?([^;"'\s]+)/i)
        song.lrc = decodeText(response.data, charsetMatch ? charsetMatch[1] : 'utf-8')
      } catch (error) {
        console.error(`Error loading lyrics for song: ${song.name}`, error)
      } finally {
        song.lrcLoading = false
        this.lyricLoading = false
      }

      if (!song.lrc) {
        song.lrcUrl = ''
        return
      }

      // APlayer 切歌时已把空歌词缓存进 lrc.parsed，这里清掉让它按新歌词重新解析
      if (player.lrc.parsed) {
        player.lrc.parsed[index] = undefined
      }
      if (player.list.index === index) {
        player.lrc.switch(index)
        player.lrc.update()
        this.refreshLyric()
        this.updateActiveLine()
      }
    },
    bindPlayerErrorHandler() {
      const audio = this.player && this.player.audio
      if (!audio) return
      if (this.errorHandler) return

      this.errorHandler = (event) => {
        try {
          event.stopImmediatePropagation()
        } catch (e) { /* ignore */ }
        if (this.player) {
          this.player.pause()
          this.player.seek(0)
        }
      }
      // 用捕获阶段提前拦截 error 事件，阻止 APlayer 内部自动切歌/重试
      audio.addEventListener('error', this.errorHandler, true)
    },

    // ── 播放控制 ──
    toggle() {
      if (!this.player) return
      this.player.toggle()
    },
    prev() {
      if (this.player) this.player.skipBack()
    },
    next() {
      if (this.player) this.player.skipForward()
    },
    seekTo(time) {
      if (!this.player) return
      this.player.seek(time)
      this.currentTime = time
      this.updateActiveLine()
    },
    onBarPointerDown(e) {
      const bar = this.$refs.bar
      if (!bar || !this.player || !this.duration) return
      bar.setPointerCapture(e.pointerId)
      this.applyScrub(e)
      const move = (ev) => this.applyScrub(ev)
      const up = () => {
        bar.removeEventListener('pointermove', move)
        bar.removeEventListener('pointerup', up)
        bar.removeEventListener('pointercancel', up)
      }
      bar.addEventListener('pointermove', move)
      bar.addEventListener('pointerup', up)
      bar.addEventListener('pointercancel', up)
    },
    applyScrub(e) {
      const bar = this.$refs.bar
      if (!bar || !this.duration) return
      const rect = bar.getBoundingClientRect()
      const ratio = Math.min(1, Math.max(0, (e.clientX - rect.left) / rect.width))
      const time = ratio * this.duration
      this.player.seek(time)
      this.currentTime = time
      this.updateActiveLine()
    },
    cycleOrder() {
      if (!this.player) return
      this.order = this.order === 'list' ? 'random' : 'list'
      this.player.options.order = this.order
    },
    cycleLoop() {
      if (!this.player) return
      this.loop = { all: 'one', one: 'none', none: 'all' }[this.loop]
      this.player.options.loop = this.loop
    },
    onVolume(e) {
      const value = Number(e.target.value)
      this.volume = value
      this.muted = value === 0
      if (this.player) this.player.volume(value)
    },
    toggleMute() {
      if (!this.player) return
      if (this.muted) {
        this.volume = this.prevVolume || 0.7
        this.muted = false
      } else {
        this.prevVolume = this.volume
        this.volume = 0
        this.muted = true
      }
      this.player.volume(this.volume)
    },
    formatTime(time) {
      if (!time || Number.isNaN(time)) return '0:00'
      const minutes = Math.floor(time / 60)
      const seconds = Math.floor(time % 60)
      return `${minutes}:${String(seconds).padStart(2, '0')}`
    },
    toggleList() {
      this.showList = !this.showList
      if (this.showList) this.$nextTick(() => this.scrollListToCurrent())
    },
    // 与 APlayer 列表点击一致：点当前曲目切播放/暂停，点其他曲目切歌并播放
    selectTrack(i) {
      if (!this.player) return
      if (i === this.index) {
        this.player.toggle()
        return
      }
      this.player.switchAudio(i)
      this.player.play()
    },
    scrollListToCurrent() {
      const box = this.$refs.listBox
      if (!box) return
      const el = box.children[this.index]
      if (!el) return
      box.scrollTo({
        top: el.offsetTop - box.clientHeight / 2 + el.clientHeight / 2,
        behavior: 'smooth'
      })
    },

    // ── 圆形唱盘的定位与吸附 ──
    orbSize() {
      return window.innerWidth <= 640 ? ORB_SIZE_SM : ORB_SIZE
    },
    placeOrb() {
      const size = this.orbSize()
      const half = size / 2
      // 吸附态：圆心正好落在视口边缘，只露出半个圆
      this.orbX = this.orbSide === 'right' ? window.innerWidth - half : -half
      if (!this.orbY) {
        // 首次进入：恢复上次保存的吸附位置，没有就用垂直居中
        const saved = this.readSavedOrbPos()
        if (saved) {
          this.orbSide = saved.side
          this.orbX = saved.side === 'right' ? window.innerWidth - half : -half
          this.orbY = Math.round(saved.ratio * window.innerHeight - size / 2)
        } else {
          this.orbY = Math.round(window.innerHeight / 2 - size / 2)
        }
      }
      this.clampOrb()
    },
    readSavedOrbPos() {
      try {
        const raw = localStorage.getItem(ORB_POS_KEY)
        if (!raw) return null
        const data = JSON.parse(raw)
        if (!data || (data.side !== 'left' && data.side !== 'right')) return null
        const ratio = Number(data.ratio)
        if (!Number.isFinite(ratio)) return null
        return { side: data.side, ratio: Math.min(Math.max(ratio, 0), 1) }
      } catch (error) {
        return null
      }
    },
    saveOrbPos() {
      try {
        const ratio = (this.orbY + this.orbSize() / 2) / window.innerHeight
        localStorage.setItem(ORB_POS_KEY, JSON.stringify({
          side: this.orbSide,
          ratio: Math.min(Math.max(ratio, 0), 1)
        }))
      } catch (error) { /* 隐私模式等场景下忽略写入失败 */ }
    },
    clampOrb() {
      const size = this.orbSize()
      const half = size / 2
      const maxY = window.innerHeight - size - EDGE_MARGIN
      this.orbY = Math.min(Math.max(this.orbY, TOP_MARGIN), Math.max(TOP_MARGIN, maxY))
      // 横向允许拖到一半出屏，松手再吸附
      const maxX = window.innerWidth - half
      this.orbX = Math.min(Math.max(this.orbX, -half), Math.max(-half, maxX))
    },
    onResize() {
      this.placeOrb()
    },
    onOrbPointerDown(e) {
      if (e.button !== undefined && e.button !== 0) return
      const orb = this.$refs.orb
      if (!orb) return
      this.dragging = true
      this._drag = {
        pointerId: e.pointerId,
        startX: e.clientX,
        startY: e.clientY,
        originX: this.orbX,
        originY: this.orbY,
        moved: false
      }
      orb.setPointerCapture(e.pointerId)
      orb.addEventListener('pointermove', this.onOrbMove)
      orb.addEventListener('pointerup', this.onOrbUp)
      orb.addEventListener('pointercancel', this.onOrbUp)
    },
    onOrbMove(e) {
      const drag = this._drag
      if (!drag || e.pointerId !== drag.pointerId) return
      const dx = e.clientX - drag.startX
      const dy = e.clientY - drag.startY
      if (!drag.moved && Math.hypot(dx, dy) > 5) drag.moved = true
      if (!drag.moved) return
      this.orbX = drag.originX + dx
      this.orbY = drag.originY + dy
      this.clampOrb()
    },
    onOrbUp(e) {
      const orb = this.$refs.orb
      if (orb) {
        orb.removeEventListener('pointermove', this.onOrbMove)
        orb.removeEventListener('pointerup', this.onOrbUp)
        orb.removeEventListener('pointercancel', this.onOrbUp)
      }
      const drag = this._drag
      this._drag = null
      this.dragging = false
      if (!drag) return
      if (drag.moved) {
        // 拖拽结束后浏览器会补发一次 click，记下时间让 onOrbClick 忽略
        this._dragEndAt = Date.now()
        // 吸附到最近的左右边缘，圆心压在边缘上，只露半个圆
        const size = this.orbSize()
        const half = size / 2
        const center = this.orbX + half
        this.orbSide = center < window.innerWidth / 2 ? 'left' : 'right'
        this.orbX = this.orbSide === 'right' ? window.innerWidth - half : -half
        this.clampOrb()
        this.saveOrbPos()
      }
    },
    onOrbClick() {
      // 拖拽结束时浏览器也会补发 click，短时间内直接忽略
      if (this.dragging || Date.now() - this._dragEndAt < 300) return
      this.openPanel()
    },
    openPanel() {
      this.open = true
      this.$nextTick(() => {
        if (this.showList) this.scrollListToCurrent()
        else this.scrollLyricIntoView()
      })
    },
    closePanel() {
      this.open = false
      // 收起后重置悬浮态，避免触屏上残留的 hover 让圆盘停在屏幕里
      this.hovered = false
    },
    onClickOutside(event) {
      if (!this.open) return
      // 点击圆形唱盘本身不算「外部」，否则展开后会被立刻收起
      const orb = this.$refs.orb
      if (orb && orb.contains(event.target)) return
      this.closePanel()
    }
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.onResize)
    if (this.player) {
      const audio = this.player.audio
      if (audio && this.errorHandler) {
        audio.removeEventListener('error', this.errorHandler, true)
      }
      this.player.destroy()
      this.player = null
      this.errorHandler = null
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  全站音乐播放器 · 圆形唱盘
//
//  折叠态：一枚吸附在视口边缘的圆盘，播放时唱片旋转，琥珀色进度环贴着
//  内缘走一圈。展开态：一张唱片封套面板，近直角、1px 实线、等宽时间码。
//  唯一的记忆点就是那枚旋转的圆盘，其余一律安静。
// ════════════════════════════════════════════════════════════════

.mj-player {
  position: fixed;
  inset: 0;
  z-index: 1040;
  pointer-events: none;
}

// ── 圆形唱盘 ──
.mj-orb {
  position: absolute;
  width: 56px;
  height: 56px;
  padding: 0;
  border: 1px solid var(--rule);
  border-radius: 50%;
  background: var(--surface);
  box-shadow: $shadow-lg;
  cursor: grab;
  pointer-events: auto;
  user-select: none;
  touch-action: none;
  transform: translateX(var(--orb-dx, 0px));
  transition: left 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    top 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover {
    transform: translateX(var(--orb-dx, 0px)) scale(1.05);
  }

  &.is-dragging {
    cursor: grabbing;
    transform: translateX(var(--orb-dx, 0px)) scale(1.06);
    transition: none;
  }

  &:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 3px;
  }
}

.mj-orb__ring {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
  pointer-events: none;

  circle {
    fill: none;
    stroke-width: 2;
  }
}

.mj-orb__ring-track {
  stroke: var(--rule);
}

.mj-orb__ring-bar {
  stroke: var(--accent);
  stroke-linecap: round;
  transition: stroke-dashoffset 0.25s linear;
}

.mj-orb__disc,
.mj-panel__disc {
  position: absolute;
  inset: 7px;
  border-radius: 50%;
  overflow: hidden;
  background-color: #1b1f24;
  background-size: cover;
  background-position: center;
  display: grid;
  place-items: center;
}

.mj-orb__disc.is-spinning,
.mj-panel__disc.is-spinning {
  animation: mj-spin 16s linear infinite;
}

// 无封面时的黑胶纹路；有封面时被图片盖住，只留一圈内环
.mj-orb__grooves {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: repeating-radial-gradient(
    circle at center,
    rgba(255, 255, 255, 0.06) 0 1px,
    transparent 1px 4px
  );
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
  pointer-events: none;
}

.mj-orb__hole {
  position: relative;
  width: 18%;
  aspect-ratio: 1;
  border-radius: 50%;
  background: var(--surface);
  box-shadow: 0 0 0 1px var(--rule);
}

.mj-orb__chip {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(calc(-50% + var(--chip-dx, 0px)), -50%);
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(16, 19, 23, 0.62);
  color: #fff;
  cursor: pointer;
  opacity: 1;
  transition: transform 0.18s ease, background 0.18s ease, opacity 0.2s ease;

  svg {
    width: 13px;
    height: 13px;
    fill: currentColor;
  }

  // 未播放且没人碰时，吸附态只露半个圆盘，不显示播放键
  &.is-hidden {
    opacity: 0;
    pointer-events: none;
  }

  &:hover {
    background: var(--accent);
    transform: translate(calc(-50% + var(--chip-dx, 0px)), -50%) scale(1.08);
  }
}

// ── 展开面板 ──
.mj-panel {
  position: absolute;
  right: 24px;
  bottom: 24px;
  width: 300px;
  padding: 20px 18px 16px;
  background: var(--surface);
  border: 1px solid var(--rule);
  border-radius: $border-radius-lg;
  box-shadow: $shadow-lg;
  pointer-events: auto;
  animation: mj-panel-in 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

.mj-panel__close {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: color 0.18s ease;

  svg {
    width: 14px;
    height: 14px;
    stroke: currentColor;
    stroke-width: 1.6;
    fill: none;
  }

  &:hover {
    color: var(--text-primary);
  }
}

.mj-panel__disc {
  position: relative;
  inset: auto;
  width: 116px;
  height: 116px;
  margin: 4px auto 14px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.18);
}

.mj-panel__title {
  font-family: $font-serif;
  font-size: 16px;
  line-height: 1.35;
  text-align: center;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mj-panel__artist {
  margin-top: 5px;
  font-size: 12px;
  text-align: center;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mj-panel__lrc {
  height: 92px;
  margin: 16px 0 4px;
  overflow-y: auto;
  scrollbar-width: none;
  text-align: left;
  -webkit-mask-image: linear-gradient(to bottom, transparent, #000 22%, #000 78%, transparent);
  mask-image: linear-gradient(to bottom, transparent, #000 22%, #000 78%, transparent);

  &::-webkit-scrollbar {
    display: none;
  }
}

.mj-panel__lrc-line {
  padding: 6px 2px;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: color 0.2s ease;

  &:hover {
    color: var(--text-secondary);
  }

  &.is-active {
    color: var(--accent);
    font-weight: 600;
  }
}

.mj-panel__lrc-empty {
  padding: 30px 0;
  text-align: center;
  font-size: 12px;
  color: var(--text-tertiary);
}

// 歌曲列表：与歌词区等高的滚动区，序号用等宽对齐
.mj-panel__list {
  height: 92px;
  margin: 16px 0 4px;
  padding: 0;
  list-style: none;
  overflow-y: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.mj-panel__list-item {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 7px 6px;
  border-radius: 2px;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease;

  &:hover {
    background: var(--hover-bg);
  }

  &.is-active {
    background: var(--accent-soft);

    .mj-panel__list-name {
      color: var(--accent);
      font-weight: 600;
    }
  }
}

.mj-panel__list-index {
  flex-shrink: 0;
  font-family: $font-mono;
  font-size: 11px;
  color: var(--text-tertiary);
}

.mj-panel__list-meta {
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.mj-panel__list-name {
  font-size: 13px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mj-panel__list-artist {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.mj-panel__time {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  font-family: $font-mono;
  font-size: 11px;
  letter-spacing: 0.04em;
  color: var(--text-tertiary);
}

.mj-panel__bar {
  position: relative;
  height: 18px;
  margin-top: 2px;
  cursor: pointer;
  touch-action: none;

  &:hover .mj-panel__bar-thumb {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

.mj-panel__bar-track,
.mj-panel__bar-loaded,
.mj-panel__bar-played {
  position: absolute;
  left: 0;
  top: 50%;
  height: 2px;
  transform: translateY(-50%);
}

.mj-panel__bar-track {
  width: 100%;
  background: var(--rule);
}

.mj-panel__bar-loaded {
  background: var(--rule-strong);
}

.mj-panel__bar-played {
  background: var(--accent);
}

.mj-panel__bar-thumb {
  position: absolute;
  top: 50%;
  width: 9px;
  height: 9px;
  border-radius: 1px;
  background: var(--accent);
  transform: translate(-50%, -50%) scale(0.8);
  opacity: 0;
  transition: opacity 0.18s ease, transform 0.18s ease;
  pointer-events: none;
}

.mj-panel__controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 14px;
}

.mj-ctl {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.18s ease, transform 0.18s ease;

  svg {
    width: 18px;
    height: 18px;
    fill: none;
    stroke: currentColor;
    stroke-width: 1.6;
    stroke-linecap: round;
    stroke-linejoin: round;
  }

  &:hover {
    color: var(--text-primary);
  }

  &:active {
    transform: scale(0.92);
  }

  &.is-on {
    color: var(--accent);
  }

  &:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
    border-radius: 2px;
  }
}

.mj-ctl__dot {
  fill: currentColor;
  stroke: none;
}

.mj-ctl--play {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;

  svg {
    width: 20px;
    height: 20px;
    fill: currentColor;
    stroke: none;
  }

  &:hover {
    color: #fff;
    transform: scale(1.05);
  }
}

.mj-panel__volume {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--rule);
}

.mj-ctl--vol {
  width: 26px;
  height: 26px;
  flex-shrink: 0;

  svg {
    width: 16px;
    height: 16px;
  }
}

.mj-panel__range {
  flex: 1;
  height: 2px;
  accent-color: var(--accent);
  cursor: pointer;
}

// APlayer 宿主：只做音频与歌词引擎，任何视觉都不露出
.mj-player__engine {
  position: absolute;
  left: 0;
  top: 0;
  width: 1px;
  height: 1px;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}

@keyframes mj-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes mj-panel-in {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@include responsive(sm) {
  .mj-orb {
    width: 48px;
    height: 48px;
  }

  .mj-orb__chip {
    width: 22px;
    height: 22px;
  }

  .mj-panel {
    left: 12px;
    right: 12px;
    bottom: 12px;
    width: auto;
    max-height: 82vh;
    overflow-y: auto;
  }

  .mj-panel__disc {
    width: 100px;
    height: 100px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mj-orb__disc,
  .mj-panel__disc {
    animation: none !important;
  }

  .mj-orb,
  .mj-panel,
  .mj-orb__ring-bar,
  .mj-orb__chip {
    transition: none !important;
  }
}
</style>
