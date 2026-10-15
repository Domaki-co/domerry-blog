<template>
  <div
    v-show="audio.length > 0"
    ref="root"
    class="cp"
    :class="{ 'is-dragging': dragging, 'is-snap': snap }"
    :style="rootStyle"
    :data-state="open ? 'open' : 'capsule'"
    :data-playing="paused ? 'false' : 'true'"
    :data-shuffle="order === 'random' ? 'true' : 'false'"
    :data-repeat="loop === 'one' ? 'one' : loop === 'none' ? 'off' : 'all'"
    :data-muted="muted ? 'true' : 'false'"
    :data-docked="docked && !open ? 'true' : 'false'"
    :data-sheet="showList ? 'open' : 'closed'"
    :data-side="side"
    role="region"
    aria-label="音乐播放器"
    @pointerenter="onRootPointerEnter"
    @pointerleave="onRootPointerLeave"
    @focusin="onRootFocusIn"
    @focusout="onRootFocusOut"
  >
    <!-- ── 胶囊态 ── -->
    <div class="cp__cap" :inert="open" :aria-hidden="open ? 'true' : 'false'">
      <button
        type="button"
        class="cp__cap-open"
        aria-label="展开播放器"
        :aria-expanded="open ? 'true' : 'false'"
        @click="onCapOpen"
      >
        <span class="cp__art cp__disc" :class="{ 'has-img': coverReady }" :style="artStyle" aria-hidden="true"></span>
        <span class="cp__cap-text">
          <span class="cp__cap-title">{{ currentSong ? currentSong.name : '暂无曲目' }}</span>
          <span class="cp__cap-artist" :class="{ 'is-lyric': capIsLyric }">{{ capLine }}</span>
        </span>
      </button>

      <button
        type="button"
        class="cp__btn cp__ring-btn"
        data-nodrag
        :aria-label="paused ? '播放' : '暂停'"
        @click="toggle"
      >
        <svg class="cp__ring" viewBox="0 0 40 40" aria-hidden="true">
          <circle class="cp__ring-track" cx="20" cy="20" r="17" />
          <circle class="cp__ring-prog" cx="20" cy="20" r="17" :style="{ strokeDashoffset: ringOffset }" />
        </svg>
        <svg v-if="paused" class="cp__ico cp__ico--fill" viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5v14l11-7z" /></svg>
        <svg v-else class="cp__ico cp__ico--fill" viewBox="0 0 24 24" aria-hidden="true"><path d="M7 5h4v14H7zM13 5h4v14h-4z" /></svg>
      </button>
    </div>

    <!-- ── 展开卡片 ── -->
    <div class="cp__open" :inert="!open" :aria-hidden="open ? 'false' : 'true'">
      <div class="cp__glow" aria-hidden="true"></div>

      <button
        type="button"
        class="cp__btn cp__hdr cp__hdr--l"
        data-nodrag
        aria-label="播放队列"
        :aria-expanded="showList ? 'true' : 'false'"
        @click="toggleList"
      >
        <svg class="cp__ico" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M4 6h13M4 11h13M4 16h6" />
          <path d="M18.5 13.5v6" />
          <circle cx="16.6" cy="19.6" r="1.9" />
          <path d="M18.5 13.5c1.6.2 2.5 1 2.8 2.4" />
        </svg>
      </button>
      <button type="button" class="cp__btn cp__hdr cp__hdr--r" data-nodrag aria-label="收起播放器" @click="closePanel">
        <svg class="cp__ico" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
      </button>

      <div class="cp__top">
        <span class="cp__art cp__cover" :class="{ 'has-img': coverReady }" :style="artStyle" aria-hidden="true"></span>
        <h2 class="cp__title" :title="currentSong ? currentSong.name : ''">{{ currentSong ? currentSong.name : '暂无曲目' }}</h2>
        <p class="cp__artist">{{ currentSong && currentSong.artist ? currentSong.artist : '—' }}</p>
      </div>

      <div class="cp__lyrics">
        <div ref="lrcBox" class="cp__ly-list" :class="{ 'is-empty': !lyricLines.length }">
          <template v-if="lyricLines.length">
            <button
              v-for="(line, i) in lyricLines"
              :key="i"
              type="button"
              class="cp__ly"
              :class="{ 'is-active': i === activeLine, 'is-past': i < activeLine }"
              @click="seekTo(line.time)"
            >{{ line.text }}</button>
          </template>
          <div v-else class="cp__ly-empty">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M9 18V6l10-2v12" />
              <circle cx="6.5" cy="18" r="2.5" />
              <circle cx="16.5" cy="16" r="2.5" />
            </svg>
            <span>{{ lyricLoading ? '歌词载入中…' : '纯音乐，暂无歌词' }}</span>
          </div>
        </div>
      </div>

      <div class="cp__time">
        <span>{{ formatTime(currentTime) }}</span>
        <span>{{ formatTime(duration) }}</span>
      </div>

      <input
        class="cp__rng cp__scrub"
        data-nodrag
        type="range"
        min="0"
        max="1000"
        step="1"
        :value="scrubValue"
        :style="{ '--p': scrubPct }"
        aria-label="播放进度"
        @input="onScrubInput"
        @change="onScrubChange"
      />

      <div class="cp__ctrl">
        <button
          type="button"
          class="cp__btn cp__toggle"
          data-nodrag
          :aria-label="order === 'list' ? '顺序播放' : '随机播放'"
          :aria-pressed="order === 'random' ? 'true' : 'false'"
          @click="cycleOrder"
        >
          <svg class="cp__ico" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M16 3h5v5" /><path d="M4 20L21 3" /><path d="M21 16v5h-5" />
            <path d="M15 15l6 6" /><path d="M4 4l5 5" />
          </svg>
        </button>

        <button type="button" class="cp__btn cp__small" data-nodrag aria-label="上一首" @click="prev">
          <svg class="cp__ico" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 5v14" /><path d="M19 5.8v12.4L9 12z" /></svg>
        </button>

        <button type="button" class="cp__btn cp__big" data-nodrag :aria-label="paused ? '播放' : '暂停'" @click="toggle">
          <svg v-if="paused" class="cp__ico cp__ico--fill" viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5v14l11-7z" /></svg>
          <svg v-else class="cp__ico cp__ico--fill" viewBox="0 0 24 24" aria-hidden="true"><path d="M7 5h4v14H7zM13 5h4v14h-4z" /></svg>
        </button>

        <button type="button" class="cp__btn cp__small" data-nodrag aria-label="下一首" @click="next">
          <svg class="cp__ico" viewBox="0 0 24 24" aria-hidden="true"><path d="M18 5v14" /><path d="M5 5.8v12.4L15 12z" /></svg>
        </button>

        <button type="button" class="cp__btn cp__toggle" data-nodrag :aria-label="loopTitle" @click="cycleLoop">
          <svg v-if="loop === 'one'" class="cp__ico" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M17 3l3 3-3 3" /><path d="M4 11V9a3 3 0 0 1 3-3h13" />
            <path d="M7 21l-3-3 3-3" /><path d="M20 13v2a3 3 0 0 1-3 3H4" /><path d="M11 10.2l1.4-.9v5.7" />
          </svg>
          <svg v-else class="cp__ico" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M17 3l3 3-3 3" /><path d="M4 11V9a3 3 0 0 1 3-3h13" />
            <path d="M7 21l-3-3 3-3" /><path d="M20 13v2a3 3 0 0 1-3 3H4" />
          </svg>
        </button>
      </div>

      <hr class="cp__rule" />
      <div class="cp__vol-row">
        <button type="button" class="cp__btn cp__vol-btn" data-nodrag :aria-label="muted ? '取消静音' : '静音'" @click="toggleMute">
          <svg v-if="!muted" class="cp__ico" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 9.5h3.5L13 6v12l-4.5-3.5H5z" /></svg>
          <svg v-else class="cp__ico" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M5 9.5h3.5L13 6v12l-4.5-3.5H5z" /><path d="M16.5 9.5l4.5 5M21 9.5l-4.5 5" />
          </svg>
        </button>
        <input
          class="cp__rng cp__vol"
          data-nodrag
          type="range"
          min="0"
          max="100"
          step="1"
          :value="Math.round(volume * 100)"
          :style="{ '--p': volPct }"
          aria-label="音量"
          @input="onVolume"
        />
        <button type="button" class="cp__btn cp__vol-btn" data-nodrag aria-label="最大音量" @click="maxVolume">
          <svg class="cp__ico" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M3.5 9.5H7L11.5 6v12L7 14.5H3.5z" /><path d="M15 9a4 4 0 0 1 0 6" /><path d="M17.8 6.3a8 8 0 0 1 0 11.4" />
          </svg>
        </button>
      </div>

      <section class="cp__sheet" :inert="!showList" aria-label="播放队列">
        <div class="cp__sheet-head">
          <h3>Queue</h3>
          <p>{{ queueMeta }}</p>
        </div>
        <div ref="qList" class="cp__q-list" @scroll.passive="onQueueScroll">
          <button
            v-for="(song, i) in audio"
            :key="i"
            type="button"
            class="cp__q-row"
            :class="{ 'is-cur': i === index }"
            data-nodrag
            @click="selectTrack(i)"
          >
            <span class="cp__art cp__q-art" :class="{ 'has-img': !!song.cover && song.coverLoaded }" :style="qArtStyle(song)" aria-hidden="true"></span>
            <span class="cp__q-text">
              <span class="cp__q-title">{{ song.name }}</span>
              <span class="cp__q-tag">{{ song.lrcUrl ? '歌词' : '纯音乐' }}</span>
            </span>
            <span class="cp__q-dur">{{ String(i + 1).padStart(2, '0') }}</span>
            <span class="cp__eq" aria-hidden="true"><i></i><i></i><i></i></span>
          </button>
        </div>
      </section>
    </div>

    <!-- APlayer 仅作音频引擎与歌词解析，视觉全部隐藏 -->
    <div ref="aplayer" class="cp__engine" aria-hidden="true"></div>
  </div>
</template>

<script>
import { getSongList } from '../api/file'
import APlayer from 'aplayer'
import 'aplayer/dist/APlayer.min.css'
import axios from 'axios'
import { decodeText } from '@/utils/encoding'

const OPEN_W = 340
const OPEN_H = 560
const CAP_W = 212
const CAP_H = 56
const M = 16
const RING_C = 2 * Math.PI * 17
const POS_KEY = 'cp-pos'
const SHUFFLE_KEY = 'cp-shuffle'
const REPEAT_KEY = 'cp-repeat'
const VOL_KEY = 'cp-vol'
const DOCK_DELAY = 2000   // 空闲多久后吸附到侧边（毫秒）


function readStore(key) {
  try {
    return localStorage.getItem(key)
  } catch (error) {
    return null
  }
}

// 后端歌单不含色相，用曲名做稳定哈希给每首歌一个固定的唱片色
function hueFor(song) {
  const key = (song && (song.name || song.artist)) || ''
  let hue = 0
  for (let i = 0; i < key.length; i++) {
    hue = (hue * 31 + key.charCodeAt(i)) % 360
  }
  return hue
}

function writeStore(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch (error) {
    /* 隐私模式等场景下忽略写入失败 */
  }
}

export default {
  name: 'Player',
  data() {
    return {
      audio: [],
      isLoading: true,
      player: null,
      errorHandler: null,
      // APlayer 内部状态不是响应式的，这里手动镜像
      index: 0,
      paused: true,
      currentTime: 0,
      duration: 0,
      volume: 0.7,
      muted: false,
      prevVolume: 0.7,
      order: 'list',
      loop: 'all',
      lyricLines: [],
      activeLine: 0,
      lyricLoading: false,
      // 展开 / 队列
      open: false,
      showList: false,
      // 拖拽 / 吸附
      side: 'r',
      by: 28,
      x: 0,
      y: 0,
      k: 1,
      docked: true,
      dragging: false,
      snap: false,
      capLine: ''
    }
  },
  computed: {
    currentSong() {
      return this.audio[this.index] || null
    },
    cover() {
      return this.currentSong && this.currentSong.cover ? this.currentSong.cover : ''
    },
    artStyle() {
      return this.cover ? { '--img': `url("${this.cover}")` } : {}
    },
    // 封面图真正 onload 之后才切到图片；加载中/加载失败都保持光盘样式
    coverReady() {
      return !!(this.currentSong && this.currentSong.cover && this.currentSong.coverLoaded)
    },
    progress() {
      return this.duration > 0 ? Math.min(1, this.currentTime / this.duration) : 0
    },
    ringOffset() {
      return (RING_C * (1 - this.progress)).toFixed(2)
    },
    scrubValue() {
      return Math.round(this.progress * 1000)
    },
    scrubPct() {
      return `${(this.progress * 100).toFixed(2)}%`
    },
    volPct() {
      return `${(this.volume * 100).toFixed(2)}%`
    },
    capIsLyric() {
      return !this.paused && this.lyricLines.length > 0 && this.activeLine >= 0
    },
    loopTitle() {
      return { all: '列表循环', one: '单曲循环', none: '不循环' }[this.loop]
    },
    queueMeta() {
      const mode = this.order === 'random' ? '随机播放' : '顺序播放'
      const repeat = { all: '列表循环', one: '单曲循环', none: '不循环' }[this.loop]
      return `${mode} · ${repeat} · ${this.audio.length} 首`
    },
    rootStyle() {
      return {
        '--h': hueFor(this.currentSong),
        left: `${this.x}px`,
        top: `${this.y}px`,
        width: `${this.dims().w}px`,
        height: `${this.dims().h}px`,
        '--k': this.k,
        '--dx': this.dockDx
      }
    },
    dockDx() {
      if (!this.docked || this.open) return '0px'
      const d = this.dims()
      const dx = M + d.w - 42
      return `${this.side === 'l' ? -dx : dx}px`
    }
  },
  async created() {
    this._lyricRef = undefined
    this._seeking = false
    this._drag = null
    this._suppress = false
    this._idleTimer = 0
    this._snapTimer = 0

    // 记住的随机 / 循环 / 音量
    const savedShuffle = readStore(SHUFFLE_KEY)
    if (savedShuffle !== null) this.order = savedShuffle === '1' ? 'random' : 'list'
    const savedRepeat = readStore(REPEAT_KEY)
    if (['all', 'one', 'off'].includes(savedRepeat)) this.loop = savedRepeat
    const savedVol = parseFloat(readStore(VOL_KEY))
    if (Number.isFinite(savedVol)) {
      this.volume = Math.min(1, Math.max(0, savedVol))
      this.prevVolume = this.volume || 0.7
      this.muted = this.volume === 0
    }
    // 记住的吸附位置
    try {
      const saved = JSON.parse(readStore(POS_KEY) || 'null')
      if (saved && (saved.side === 'l' || saved.side === 'r') && Number.isFinite(saved.by)) {
        this.side = saved.side
        this.by = saved.by
      }
    } catch (error) { /* 忽略损坏的本地记录 */ }

    try {
      const resp = await getSongList()
      if (!resp.data || resp.data.length === 0) {
        console.warn('No songs found in the list')
        return
      }
      // 歌词不随歌单一次性下载，只保留地址，播放到某首歌时按需请求
      const processedData = resp.data.map((item) => {
        // 没有封面就留空，交给 CSS 用生成式唱片兜底，不再塞默认头像
        if (item.cover === null || item.cover === undefined) {
          item.cover = ''
        }
        item.lrcUrl = item.lrc || ''
        item.lrc = ''
        item.lrcLoading = false
        item.coverLoaded = false
        item.coverLoading = false
        return item
      })
      this.audio = processedData
      // 这里刻意不预载任何封面：后端封面单张就有几百 KB（首曲实测 868KB），
      // 而播放器默认是收起的胶囊态、也没在播放，为它拉图纯属浪费首屏带宽。
      // 收起态用 CSS 生成的黑胶兜底，等用户播放或展开时再按需加载。

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
    this.layout()
    // 首次进入默认吸附在边缘（只露半边），不展开；空闲计时照常启动
    this.scheduleDock()
    window.addEventListener('resize', this.onResize)
    document.addEventListener('pointerdown', this.onDocPointerDown)
    document.addEventListener('pointerdown', this.onDocPointerDownOutside)
    document.addEventListener('keydown', this.onKeydown)
    window.addEventListener('pointermove', this.onWindowPointerMove)
    window.addEventListener('pointerup', this.onWindowPointerUp)
    window.addEventListener('pointercancel', this.onWindowPointerUp)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.onResize)
    document.removeEventListener('pointerdown', this.onDocPointerDown)
    document.removeEventListener('pointerdown', this.onDocPointerDownOutside)
    document.removeEventListener('keydown', this.onKeydown)
    window.removeEventListener('pointermove', this.onWindowPointerMove)
    window.removeEventListener('pointerup', this.onWindowPointerUp)
    window.removeEventListener('pointercancel', this.onWindowPointerUp)
    clearTimeout(this._idleTimer)
    clearTimeout(this._snapTimer)
    if (this.player) {
      const audio = this.player.audio
      if (audio && this.errorHandler) {
        audio.removeEventListener('error', this.errorHandler, true)
      }
      this.player.destroy()
      this.player = null
      this.errorHandler = null
    }
  },
  methods: {
    // ── APlayer 引擎 ──
    initAPlayer() {
      if (this.audio.length === 0) return

      if (this.player) {
        this.player.destroy()
      }

      // APlayer 只当音频引擎和歌词解析器用，界面完全隐藏（.cp__engine 是 1px 的透明盒）。
      // 但它仍会为当前曲目渲染 <img> 去拉封面，那张图没人看得见却要几百 KB，
      // 所以喂给它的 audio 里把 cover 抹掉 —— 封面由我们自己的 .cp__art 按需加载。
      const engineAudio = this.audio.map((song) => ({ ...song, cover: '' }))

      this.player = new APlayer({
        container: this.$refs.aplayer,
        lrcType: 1,
        autoplay: false,
        theme: '#a86a10',
        listFolded: true,
        order: this.order,
        loop: this.loop,
        volume: this.volume,
        audio: engineAudio
      })

      this.order = this.player.options.order
      this.loop = this.player.options.loop
      this.volume = this.player.audio.volume
      this.prevVolume = this.volume || 0.7
      this.muted = this.volume === 0

      this.patchAudioPlay()
      this.bindPlayerEvents()
      this.bindPlayerErrorHandler()
      this.refreshLyric()
      this.updateCapLine()
    },
    bindPlayerEvents() {
      const player = this.player

      player.on('play', () => {
        this.paused = false
        this.loadCover(this.index)
        this.refreshLyric()
        this.loadCurrentLyric()
        this.updateCapLine()
      })
      player.on('pause', () => {
        this.paused = true
        this.updateCapLine()
      })
      player.on('timeupdate', () => {
        this.duration = player.duration || this.duration
        if (this._seeking) return
        this.currentTime = player.audio.currentTime || 0
        this.refreshLyric()
        this.updateActiveLine()
      })
      player.on('listswitch', (data) => {
        if (data && typeof data.index === 'number') this.index = data.index
        this.currentTime = 0
        this.activeLine = 0
        this.lyricLines = []
        this._lyricRef = undefined
        this.loadCover(this.index)
        this.updateCapLine()
        this.$nextTick(() => {
          this.refreshLyric()
          if (this.showList) this.scrollQueue()
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
      this.updateCapLine()
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
        this.updateCapLine()
      }
    },
    updateCapLine() {
      const lyricOn = !this.paused && this.lyricLines.length > 0 && this.activeLine >= 0
      const text = lyricOn
        ? this.lyricLines[this.activeLine].text
        : (this.currentSong && this.currentSong.artist ? this.currentSong.artist : '—')
      if (text !== this.capLine) this.capLine = text
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
    // APlayer 有些内部路径（如 setAudio 切歌）直接调用 audio.play() 却不处理返回的
    // Promise，快速切歌/暂停时 play 会被 pause 打断，浏览器抛 AbortError 变成
    // Uncaught (in promise)。这里在音频元素层面统一接管：AbortError 静默吞掉，
    // 其它错误保留给 APlayer 自己的 catch（如 NotAllowedError 会回退 UI 状态），
    // 同时兜底标记已处理，避免任何调用方漏 catch 时再次抛出。
    patchAudioPlay() {
      const audio = this.player && this.player.audio
      if (!audio || audio.__cpPlayPatched) return
      const nativePlay = audio.play.bind(audio)
      audio.play = () => {
        const result = nativePlay()
        if (!result || typeof result.catch !== 'function') return result
        const handled = result.catch((error) => {
          if (error && error.name === 'AbortError') return undefined
          throw error
        })
        handled.catch(() => {})
        return handled
      }
      audio.__cpPlayPatched = true
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
      if (this.paused) this.player.play()
    },
    onScrubInput(e) {
      this._seeking = true
      this.currentTime = (Number(e.target.value) / 1000) * this.duration
    },
    onScrubChange(e) {
      const time = (Number(e.target.value) / 1000) * this.duration
      if (this.player) this.player.seek(time)
      this.currentTime = time
      this.updateActiveLine()
      this._seeking = false
    },
    cycleOrder() {
      if (!this.player) return
      this.order = this.order === 'list' ? 'random' : 'list'
      this.player.options.order = this.order
      writeStore(SHUFFLE_KEY, this.order === 'random' ? '1' : '0')
    },
    cycleLoop() {
      if (!this.player) return
      this.loop = { all: 'one', one: 'none', none: 'all' }[this.loop]
      this.player.options.loop = this.loop
      writeStore(REPEAT_KEY, this.loop)
    },
    onVolume(e) {
      const value = Number(e.target.value) / 100
      this.volume = value
      this.muted = value === 0
      if (this.player) this.player.volume(value)
      writeStore(VOL_KEY, String(value))
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
      writeStore(VOL_KEY, String(this.volume))
    },
    maxVolume() {
      this.volume = 1
      this.muted = false
      if (this.player) this.player.volume(1)
      writeStore(VOL_KEY, '1')
    },
    formatTime(time) {
      if (!time || Number.isNaN(time)) return '0:00'
      const minutes = Math.floor(time / 60)
      const seconds = Math.floor(time % 60)
      return `${minutes}:${String(seconds).padStart(2, '0')}`
    },
    // 封面按需加载：只拉当前曲目和队列里可见的那几行，不再一次性预载整张歌单。
    // 后端封面存在 MinIO，单张要 1~5s，27 首全预载会占满并发连接、把首屏一起拖慢。
    // 加载完成后才把圆盘切成图片，加载中保持光盘；失败则回退成光盘。
    loadCover(i) {
      const song = this.audio[i]
      if (!song || !song.cover || song.coverLoaded || song.coverLoading) return
      song.coverLoading = true
      const img = new Image()
      img.onload = () => {
        song.coverLoading = false
        song.coverLoaded = true
      }
      img.onerror = () => {
        song.coverLoading = false
        song.cover = ''
        song.coverLoaded = false
      }
      img.src = song.cover
    },
    // 队列展开时只补视口内的封面，滚动时再补新露出来的行
    loadVisibleQueueCovers() {
      const box = this.$refs.qList
      if (!box) return
      const top = box.scrollTop
      const bottom = top + box.clientHeight
      for (let i = 0; i < box.children.length; i++) {
        const el = box.children[i]
        if (el.offsetTop + el.offsetHeight < top || el.offsetTop > bottom) continue
        this.loadCover(i)
      }
    },
    qArtStyle(song) {
      const style = { '--h': hueFor(song) }
      if (song && song.cover) style['--img'] = `url("${song.cover}")`
      return style
    },

    // ── 队列 ──
    toggleList() {
      this.showList = !this.showList
      if (this.showList) {
        this.$nextTick(() => {
          this.scrollQueue()
          this.loadVisibleQueueCovers()
        })
      }
    },
    onQueueScroll() {
      this.loadVisibleQueueCovers()
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
    scrollQueue() {
      const box = this.$refs.qList
      if (!box) return
      const el = box.children[this.index]
      if (!el) return
      box.scrollTo({
        top: el.offsetTop - box.clientHeight / 2 + el.clientHeight / 2,
        behavior: 'smooth'
      })
    },

    // ── 尺寸 / 定位 / 吸附 ──
    dims() {
      const vw = window.innerWidth
      const vh = window.innerHeight
      if (this.open) {
        const k = Math.max(0.5, Math.min(1, (vw - 2 * M) / OPEN_W, (vh - 2 * M) / OPEN_H))
        return { w: OPEN_W * k, h: OPEN_H * k, k }
      }
      return { w: Math.min(CAP_W, vw - 2 * M), h: CAP_H, k: 1 }
    },
    clampTop(top, h) {
      return Math.max(M, Math.min(top, window.innerHeight - h - M))
    },
    layout() {
      const d = this.dims()
      const vw = window.innerWidth
      const vh = window.innerHeight
      this.k = d.k
      this.x = this.side === 'l' ? M : vw - d.w - M
      this.y = this.clampTop(vh - this.by - d.h, d.h)
    },
    onResize() {
      this.layout()
    },
    savePos() {
      writeStore(POS_KEY, JSON.stringify({ side: this.side, by: this.by }))
    },
    undock() {
      if (this.docked) this.docked = false
    },
    resetIdle() {
      this.undock()
      this.scheduleDock()
    },
    scheduleDock() {
      clearTimeout(this._idleTimer)
      if (this.open || this._drag) return
      this._idleTimer = setTimeout(this.tryDock, DOCK_DELAY)
    },
    tryDock() {
      if (this.open || this._drag) return
      const root = this.$refs.root
      if (root && (root.matches(':hover') || root.querySelector(':focus-visible'))) {
        this.resetIdle()
        return
      }
      this.docked = true
    },
    // ── 拖拽（真正拖动后才接管，保证点击仍能落到按钮上）──
    onDocPointerDown(e) {
      if (e.button !== 0) return
      const root = this.$refs.root
      if (!root || !root.contains(e.target)) return
      if (e.target.closest('[data-nodrag]')) return
      if (this.open && !e.target.closest('.cp__top')) return
      const rect = root.getBoundingClientRect()
      this._drag = {
        id: e.pointerId,
        sx: e.clientX,
        sy: e.clientY,
        l: rect.left,
        t: rect.top,
        moved: false
      }
    },
    onWindowPointerMove(e) {
      const drag = this._drag
      if (!drag || e.pointerId !== drag.id) return
      const dx = e.clientX - drag.sx
      const dy = e.clientY - drag.sy
      if (!drag.moved) {
        if (Math.hypot(dx, dy) < 6) return
        drag.moved = true
        clearTimeout(this._idleTimer)
        this.docked = false
        this.dragging = true
        const root = this.$refs.root
        if (root) {
          try { root.setPointerCapture(e.pointerId) } catch (err) { /* 忽略 */ }
        }
      }
      const d = this.dims()
      this.x = Math.max(8, Math.min(drag.l + dx, window.innerWidth - d.w - 8))
      this.y = Math.max(8, Math.min(drag.t + dy, window.innerHeight - d.h - 8))
    },
    onWindowPointerUp(e) {
      const drag = this._drag
      if (!drag || (e && e.pointerId !== drag.id)) return
      const moved = drag.moved
      this._drag = null
      if (!moved) return
      const d = this.dims()
      const rect = this.$refs.root.getBoundingClientRect()
      this.side = rect.left + d.w / 2 < window.innerWidth / 2 ? 'l' : 'r'
      this.by = Math.max(M, window.innerHeight - (rect.top + d.h))
      this.dragging = false
      this.snap = true
      this.layout()
      this.savePos()
      clearTimeout(this._snapTimer)
      this._snapTimer = setTimeout(() => { this.snap = false }, 650)
      this._suppress = true
      setTimeout(() => { this._suppress = false }, 60)
      this.resetIdle()
    },
    onRootPointerEnter() {
      clearTimeout(this._idleTimer)
      this.undock()
    },
    onRootPointerLeave() {
      if (!this._drag) this.resetIdle()
    },
    onRootFocusIn() {
      clearTimeout(this._idleTimer)
      this.undock()
    },
    onRootFocusOut() {
      this.resetIdle()
    },

    // ── 展开 / 收起 ──
    onCapOpen() {
      // 拖拽结束后浏览器会补发一次 click，用 _suppress 忽略它
      if (this._suppress) return
      this.openPanel()
    },
    openPanel() {
      this.open = true
      this.undock()
      this.layout()
      this.resetIdle()
      this.$nextTick(() => {
        if (this.showList) this.scrollQueue()
        else this.scrollLyricIntoView()
      })
    },
    closePanel() {
      this.open = false
      this.showList = false
      this.undock()
      this.layout()
      this.resetIdle()
    },
    onDocPointerDownOutside(e) {
      const root = this.$refs.root
      if (!this.open || !root || root.contains(e.target)) return
      this.closePanel()
    },
    onKeydown(e) {
      if (e.key !== 'Escape' || !this.open) return
      if (this.showList) this.showList = false
      else this.closePanel()
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  全站音乐播放器 · 胶囊
//
//  折叠态是一枚贴着视口边缘的胶囊：旋转的唱片、曲名、随播放滚动的歌词、
//  一圈琥珀进度环；空闲五秒自动滑到边缘只留一点。展开是一张 340×560 的
//  卡片：大唱片、歌词、进度与音量、播放队列浮层。
//  颜色一律走全站 CSS 变量，深浅色自动跟随；唱片色相由曲目 hue 决定。
// ════════════════════════════════════════════════════════════════

.cp {
  --h: 210;
  position: fixed;
  z-index: 1040;
  background: var(--surface);
  color: var(--text-primary);
  outline: 1px solid var(--rule);
  outline-offset: -1px;
  border-radius: 28px;
  box-shadow: $shadow-lg;
  overflow: hidden;
  font-family: $font-sans;
  transform: translateX(var(--dx, 0px));
  transition:
    left 0.45s cubic-bezier(0.3, 0.8, 0.25, 1),
    top 0.45s cubic-bezier(0.3, 0.8, 0.25, 1),
    width 0.45s cubic-bezier(0.3, 0.8, 0.25, 1),
    height 0.45s cubic-bezier(0.3, 0.8, 0.25, 1),
    border-radius 0.45s cubic-bezier(0.3, 0.8, 0.25, 1),
    transform 0.5s cubic-bezier(0.3, 0.8, 0.25, 1),
    box-shadow 0.45s ease;
  -webkit-tap-highlight-color: transparent;
  user-select: none;
  -webkit-user-select: none;

  &.is-snap {
    transition:
      left 0.6s cubic-bezier(0.34, 1.45, 0.5, 1),
      top 0.6s cubic-bezier(0.34, 1.45, 0.5, 1),
      width 0.45s cubic-bezier(0.3, 0.8, 0.25, 1),
      height 0.45s cubic-bezier(0.3, 0.8, 0.25, 1),
      border-radius 0.45s cubic-bezier(0.3, 0.8, 0.25, 1),
      transform 0.5s cubic-bezier(0.3, 0.8, 0.25, 1);
  }

  &.is-dragging {
    transition: none;
    cursor: grabbing;
    box-shadow: 0 24px 60px rgba(30, 22, 12, 0.28), 0 3px 10px rgba(30, 22, 12, 0.16);
  }

  &[data-docked='true'] {
    box-shadow: $shadow-md;
  }
}

// ── 通用按钮 / 图标 ──
.cp__btn {
  appearance: none;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
  padding: 0;
  display: grid;
  place-items: center;
  border-radius: 50%;
  flex: none;
  position: relative;

  &:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
  }
}

.cp__ico {
  width: 22px;
  height: 22px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.cp__ico--fill {
  fill: currentColor;
  stroke: none;
}

// ── 唱片 ──
// 无封面：参考稿的光盘 —— 生成式 CD 渐变（色相来自曲目）+ 白色中心孔 + 黑胶纹路。
// 有封面：用自己的图，保持干净；图片作为第一层叠在渐变之上，URL 失效也能回退成唱片。
$cd-art:
  radial-gradient(circle at 30% 25%, rgba(255, 255, 255, 0.55), transparent 48%),
  conic-gradient(
    from 25deg,
    hsl(var(--h) 78% 78%),
    hsl(calc(var(--h) + 40) 80% 66%),
    hsl(calc(var(--h) - 30) 65% 52%),
    hsl(var(--h) 78% 78%)
  );

.cp__art {
  flex: none;
  position: relative;
  background-size: cover;
  background-position: center;
  background-color: #1b1f24;
  background-image: $cd-art;

  &.has-img {
    background-image: var(--img), $cd-art;
  }

  // 无封面时才是参考稿的光盘：渐变盘面 + 白色中心孔 + 黑胶纹路；
  // 有封面就用自己的图，保持干净，不加白块。
  &:not(.has-img)::before {
    content: '';
    position: absolute;
    z-index: 1;
    inset: var(--hole, 38%);
    border-radius: 50%;
    background: var(--surface);
    box-shadow: 0 0 0 1px var(--rule);
  }

  &:not(.has-img)::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: repeating-radial-gradient(circle at 50% 50%, transparent 0 3px, rgba(0, 0, 0, 0.06) 3px 4px);
  }
}

.cp__disc {
  --hole: 36%;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  animation: cp-spin 6s linear infinite;
  animation-play-state: paused;
}

.cp[data-playing='true'] .cp__disc,
.cp[data-playing='true'] .cp__cover {
  animation-play-state: running;
}

@keyframes cp-spin {
  to { transform: rotate(360deg); }
}

// ── 胶囊态 ──
.cp__cap {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 8px;
  touch-action: none;
  transition: opacity 0.15s;
}

.cp[data-state='open'] .cp__cap {
  opacity: 0;
  pointer-events: none;
}

// 吸附在左侧时把胶囊镜像过来：唱片始终朝外，贴边后露出的仍是半张光盘，
// 而不是播放按钮。
.cp[data-side='l'] .cp__cap {
  flex-direction: row-reverse;
}

.cp[data-side='l'] .cp__cap-open {
  flex-direction: row-reverse;
  padding: 0 1px 0 0;
  text-align: right;
}

.cp__cap-open {
  appearance: none;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0 0 0 1px;
  border-radius: 999px;
  cursor: pointer;

  &:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
  }
}

.cp__cap-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.2;
}

.cp__cap-title {
  font-family: $font-serif;
  font-weight: 700;
  font-size: 16px;
  line-height: 1.1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cp__cap-artist {
  font-size: 11.5px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;

  &.is-lyric {
    color: var(--text-primary);
    font-style: italic;
  }
}

.cp__ring-btn {
  width: 40px;
  height: 40px;

  &:hover {
    background: var(--hover-bg);
  }
}

.cp__ring {
  position: absolute;
  inset: 0;
  width: 40px;
  height: 40px;
  transform: rotate(-90deg);

  circle {
    fill: none;
    stroke-width: 2.5;
  }
}

.cp__ring-track {
  stroke: var(--rule);
}

.cp__ring-prog {
  stroke: var(--accent);
  stroke-linecap: round;
  stroke-dasharray: 106.81;
  stroke-dashoffset: 106.81;
  transition: stroke-dashoffset 0.25s linear;
}

.cp__ring-btn .cp__ico {
  width: 18px;
  height: 18px;
  position: relative;
}

// ── 展开卡片 ──
.cp__open {
  position: absolute;
  left: 0;
  top: 0;
  width: 340px;
  height: 560px;
  transform-origin: 0 0;
  transform: scale(var(--k, 1));
  visibility: hidden;
  opacity: 0;
  pointer-events: none;
  display: flex;
  flex-direction: column;
  padding: 0 22px 18px;
  transition: opacity 0.2s, visibility 0s 0.2s;
}

.cp[data-state='open'] .cp__open {
  visibility: visible;
  opacity: 1;
  pointer-events: auto;
  transition: opacity 0.25s 0.16s, visibility 0s;
}

.cp__glow {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 250px;
  pointer-events: none;
  background: radial-gradient(ellipse 75% 100% at 50% 0%, hsl(var(--h) 80% 70% / 0.3), transparent 72%);
}

.cp__hdr {
  position: absolute;
  top: 12px;
  width: 34px;
  height: 34px;
  z-index: 3;

  &:hover {
    background: var(--hover-bg);
  }

  .cp__ico {
    width: 19px;
    height: 19px;
  }
}

.cp__hdr--l { left: 12px; }
.cp__hdr--r { right: 12px; }

.cp[data-sheet='open'] .cp__hdr--l {
  color: var(--accent);
  background: var(--hover-bg);
}

.cp__top {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 30px;
  cursor: grab;
  touch-action: none;
}

.cp__cover {
  --hole: 39%;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  box-shadow:
    0 0 0 5px var(--surface),
    0 0 0 6px var(--rule),
    0 18px 34px -8px hsl(var(--h) 55% 38% / 0.5);
  animation: cp-spin 20s linear infinite;
  animation-play-state: paused;
}

.cp__title {
  margin: 18px 0 0;
  font-family: $font-serif;
  font-weight: 700;
  font-size: 24px;
  line-height: 1.1;
  letter-spacing: 0.01em;
  text-align: center;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cp__artist {
  margin: 5px 0 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  text-align: center;
}

// ── 歌词 ──
.cp__lyrics {
  position: relative;
  height: 108px;
  margin-top: 12px;
  overflow: hidden;
  flex: none;
}

.cp__ly-list {
  height: 100%;
  overflow-y: auto;
  padding: 42px 0;
  scrollbar-width: none;
  position: relative;
  -webkit-mask-image: linear-gradient(transparent, #000 26%, #000 74%, transparent);
  mask-image: linear-gradient(transparent, #000 26%, #000 74%, transparent);

  &::-webkit-scrollbar {
    display: none;
  }

  &.is-empty {
    display: grid;
    place-items: center;
    padding: 0;
    -webkit-mask-image: none;
    mask-image: none;
  }
}

.cp__ly {
  appearance: none;
  border: 0;
  background: none;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  width: 100%;
  padding: 3px 6px;
  text-align: center;
  text-wrap: balance;
  cursor: pointer;
  font: 400 15px/1.35 $font-sans;
  color: var(--text-secondary);
  opacity: 0.85;
  transition: color 0.3s, opacity 0.3s;

  &.is-past {
    opacity: 0.38;
  }

  &:hover {
    opacity: 1;
  }

  &:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
    border-radius: 4px;
  }

  &.is-active {
    color: var(--text-primary);
    opacity: 1;
    font-weight: 600;
  }
}

.cp__ly-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 13px;

  svg {
    width: 24px;
    height: 24px;
    fill: none;
    stroke: var(--accent);
    stroke-width: 1.6;
    stroke-linecap: round;
    stroke-linejoin: round;
  }
}

// ── 进度 / 音量滑杆 ──
.cp__time {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  font-family: $font-mono;
  font-size: 12.5px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}

.cp__rng {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 18px;
  margin: 2px 0 0;
  background: transparent;
  cursor: pointer;
  --p: 0%;
  --tw: 14px;

  &::-webkit-slider-runnable-track {
    height: 3px;
    border-radius: 3px;
    background: linear-gradient(to right, var(--accent) var(--p), var(--rule) var(--p));
  }

  &::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: var(--tw);
    height: var(--tw);
    border-radius: 50%;
    background: var(--accent);
    border: 0;
    margin-top: calc((3px - var(--tw)) / 2);
    transition: transform 0.15s;
  }

  &::-moz-range-track {
    height: 3px;
    border-radius: 3px;
    background: var(--rule);
  }

  &::-moz-range-progress {
    height: 3px;
    border-radius: 3px;
    background: var(--accent);
  }

  &::-moz-range-thumb {
    width: var(--tw);
    height: var(--tw);
    border: 0;
    border-radius: 50%;
    background: var(--accent);
    transition: transform 0.15s;
  }

  &:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
    border-radius: 6px;
  }
}

.cp__scrub {
  --tw: 12px;

  &::-webkit-slider-thumb { transform: scale(0); }
  &::-moz-range-thumb { transform: scale(0); }

  &:hover::-webkit-slider-thumb,
  &:focus-visible::-webkit-slider-thumb,
  &:active::-webkit-slider-thumb { transform: scale(1); }

  &:hover::-moz-range-thumb,
  &:focus-visible::-moz-range-thumb,
  &:active::-moz-range-thumb { transform: scale(1); }
}

.cp__vol {
  --tw: 16px;
  flex: 1;
  min-width: 0;
}

// ── 控制排 ──
.cp__ctrl {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;

  .cp__btn {
    width: 44px;
    height: 44px;

    &:hover {
      background: var(--hover-bg);
    }
  }

  .cp__small {
    color: var(--text-primary);
  }
}

.cp__ctrl .cp__big:hover {
  background: var(--accent);
}

.cp__big {
  width: 58px !important;
  height: 58px !important;
  background: var(--accent);
  color: #fff;
  box-shadow: 0 8px 18px -6px var(--accent);
  transition: transform 0.15s cubic-bezier(0.3, 0.8, 0.25, 1);

  .cp__ico {
    width: 24px;
    height: 24px;
  }

  &:hover {
    transform: scale(1.06);
  }

  &:active {
    transform: scale(0.95) !important;
  }
}

.cp__toggle {
  color: var(--text-secondary);

  &::after {
    content: '';
    position: absolute;
    bottom: 4px;
    left: 50%;
    width: 4px;
    height: 4px;
    margin-left: -2px;
    border-radius: 50%;
    background: var(--accent);
    opacity: 0;
    transform: scale(0.4);
    transition: opacity 0.15s, transform 0.15s;
  }
}

.cp[data-shuffle='true'] .cp__toggle:first-child,
.cp[data-repeat='one'] .cp__toggle:last-child,
.cp[data-repeat='all'] .cp__toggle:last-child {
  color: var(--accent);
}

.cp[data-shuffle='true'] .cp__toggle:first-child::after,
.cp[data-repeat='one'] .cp__toggle:last-child::after,
.cp[data-repeat='all'] .cp__toggle:last-child::after {
  opacity: 1;
  transform: none;
}

.cp__rule {
  border: 0;
  height: 1px;
  background: var(--rule);
  margin: 14px 0 0;
  flex: none;
}

.cp__vol-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;

  .cp__vol-btn {
    width: 32px;
    height: 32px;
    color: var(--text-secondary);

    &:hover {
      color: var(--text-primary);
      background: var(--hover-bg);
    }
  }

  .cp__ico {
    width: 20px;
    height: 20px;
  }
}

// ── 播放队列浮层 ──
.cp__sheet {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 392px;
  z-index: 2;
  background: var(--surface);
  display: flex;
  flex-direction: column;
  padding: 12px 14px 0;
  opacity: 0;
  visibility: hidden;
  transform: translateY(12px);
  transition: opacity 0.22s, transform 0.3s cubic-bezier(0.3, 0.8, 0.25, 1), visibility 0s 0.3s;
}

.cp[data-sheet='open'] .cp__sheet {
  opacity: 1;
  visibility: visible;
  transform: none;
  transition: opacity 0.22s, transform 0.3s cubic-bezier(0.3, 0.8, 0.25, 1), visibility 0s;
}

.cp__sheet-head {
  padding: 0 40px 12px;
  text-align: center;

  h3 {
    margin: 0;
    font-family: $font-serif;
    font-weight: 700;
    font-size: 24px;
    line-height: 34px;
  }

  p {
    margin: 2px 0 0;
    font-size: 12.5px;
    color: var(--text-secondary);
  }
}

.cp__q-list {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  margin: 0 -6px;
  padding: 0 6px 10px;
  scrollbar-width: thin;
}

.cp__q-row {
  appearance: none;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 8px;
  border-radius: 14px;
  cursor: pointer;
  text-align: left;

  &:hover,
  &.is-cur {
    background: var(--hover-bg);
  }

  &:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: -2px;
  }
}

.cp__q-art {
  --hole: 38%;
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.cp__q-text {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  line-height: 1.25;
}

.cp__q-title {
  font-size: 14.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cp__q-tag {
  font-size: 12px;
  color: var(--text-secondary);
}

.cp__q-row.is-cur .cp__q-title {
  color: var(--accent);
}

.cp__q-dur {
  font-family: $font-mono;
  font-size: 12.5px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}

.cp__eq {
  display: none;
  align-items: flex-end;
  gap: 2px;
  height: 14px;
  width: 14px;

  i {
    flex: 1;
    height: 100%;
    background: var(--accent);
    border-radius: 1px;
    transform-origin: bottom;
    transform: scaleY(0.35);
    animation: cp-eq 0.9s ease-in-out infinite alternate;
    animation-play-state: paused;

    &:nth-child(2) {
      animation-duration: 0.65s;
      animation-delay: -0.3s;
    }

    &:nth-child(3) {
      animation-duration: 1.1s;
      animation-delay: -0.6s;
    }
  }
}

.cp__q-row.is-cur .cp__eq {
  display: flex;
}

.cp__q-row.is-cur .cp__q-dur {
  display: none;
}

.cp[data-playing='true'] .cp__q-row.is-cur .cp__eq i {
  animation-play-state: running;
}

@keyframes cp-eq {
  from { transform: scaleY(0.25); }
  to { transform: scaleY(1); }
}

// APlayer 宿主：只做音频与歌词引擎，任何视觉都不露出
.cp__engine {
  position: absolute;
  left: 0;
  top: 0;
  width: 1px;
  height: 1px;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}

@include responsive(sm) {
  .cp__cover {
    width: 112px;
    height: 112px;
  }

  .cp__title {
    font-size: 20px;
  }

  .cp__lyrics {
    height: 96px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cp,
  .cp.is-snap,
  .cp__open,
  .cp__sheet {
    transition-duration: 0.01s !important;
    transition-delay: 0s !important;
  }

  .cp__disc,
  .cp__cover,
  .cp__eq i {
    animation: none !important;
  }
}
</style>
