<template>
  <div class="message-cosmos-page" :class="{ 'is-wall-mode': viewMode === 'wall' }">
    <!-- 背景流光层（白日模式为晴空微光云海，黑夜模式为深邃星空墨夜） -->
    <div class="cosmos-background">
      <div class="nebula nebula-1"></div>
      <div class="nebula nebula-2"></div>
      <div class="stars-layer stars-small"></div>
      <div class="stars-layer stars-medium"></div>
    </div>

    <!-- 顶部操作台与发射中心 -->
    <div class="cosmos-station">
      <div class="station-meta">✦ CONSTELLATION GUESTBOOK ✦</div>
      <h1 class="station-title">星空留言岛</h1>
      <p class="station-desc">在浩瀚星海与晴空云海间，留下一枚属于你的微光手记</p>

      <!-- 留言输入胶囊 -->
      <div class="message-input-wrapper">
        <div class="input-inner">
          <input
            v-model="content"
            class="message-input"
            type="text"
            placeholder="写下一句微光絮语... (Enter 发送)"
            maxlength="200"
            @focus="isHovered = true"
            @blur="isHovered = false"
            @keyup.enter="submitMessage"
          />
          <button
            class="message-send-btn"
            type="button"
            :disabled="Boolean(count) || sending"
            @click="submitMessage"
          >
            <span v-if="count" class="countdown-text">{{ count }}s</span>
            <span v-else-if="sending" class="sending-text">投递中...</span>
            <span v-else class="send-label">投递</span>
          </button>
        </div>
      </div>

      <!-- 视图切换与漫游操作栏 -->
      <div class="station-actions">
        <!-- 核心视图切换器：浮岛漫游 ⇋ 信笺长卷 -->
        <div class="view-switch-group">
          <button
            type="button"
            class="switch-btn"
            :class="{ active: viewMode === 'orbit' }"
            @click="viewMode = 'orbit'"
            title="沉浸式微浮岛漫游"
          >
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3" />
              <path d="M3 12a9 9 0 0 1 18 0" stroke-dasharray="2 2" />
              <path d="M3 12a9 9 0 0 0 18 0" />
            </svg>
            <span>浮岛漫游</span>
          </button>
          <button
            type="button"
            class="switch-btn"
            :class="{ active: viewMode === 'wall' }"
            @click="viewMode = 'wall'"
            title="平铺瀑布流阅览"
          >
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7" rx="1.5" />
              <rect x="14" y="3" width="7" height="7" rx="1.5" />
              <rect x="14" y="14" width="7" height="7" rx="1.5" />
              <rect x="3" y="14" width="7" height="7" rx="1.5" />
            </svg>
            <span>信笺长卷 ({{ barrageList.length }})</span>
          </button>
        </div>

        <!-- 浮岛模式下的漫游控制按钮 -->
        <template v-if="viewMode === 'orbit'">
          <button
            type="button"
            class="action-pill-btn"
            @click="shuffleConstellation"
            title="重新排布并切换一批星岛"
          >
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.19" />
            </svg>
            漫游换位
          </button>
          <button
            v-if="barrageList.length > 2"
            type="button"
            class="action-pill-btn"
            :class="{ 'is-cruise-active': autoCruise }"
            @click="toggleAutoCruise"
            :title="autoCruise ? '暂停自动漫游' : '开启自动漫游'"
          >
            <svg v-if="autoCruise" viewBox="0 0 24 24" width="12" height="12" fill="currentColor">
              <rect x="6" y="4" width="4" height="16" rx="1" />
              <rect x="14" y="4" width="4" height="16" rx="1" />
            </svg>
            <svg v-else viewBox="0 0 24 24" width="12" height="12" fill="currentColor">
              <polygon points="5 3 19 12 5 21 5 3" />
            </svg>
            {{ autoCruise ? '巡航中' : '自动巡航' }}
          </button>
        </template>

        <!-- 查看全量名册抽屉 -->
        <button
          type="button"
          class="action-pill-btn"
          @click="drawerVisible = true"
          title="呼出完整手札日志"
        >
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 6h16M4 12h16M4 18h12" />
          </svg>
          查阅名册 ({{ barrageList.length }})
        </button>
      </div>
    </div>

    <!-- 视图过渡容器 -->
    <transition name="view-fade" mode="out-in">
      <!-- 视图 1：星空微浮留言岛（沉浸微重力漂浮，支持消失渐进过渡与动态随机落点） -->
      <div
        v-if="viewMode === 'orbit'"
        key="orbit-view"
        class="orbit-view-wrapper"
      >
        <transition-group
          :name="isInitialized ? 'island-trans' : ''"
          type="transition"
          tag="div"
          class="floating-orbit-field"
          ref="orbitField"
        >
          <div
            v-for="item in activeIslands"
            :key="item.uniqueKey"
            class="floating-island"
            :class="{ 'is-highlight': item.msg.isNew }"
            :style="item.style"
            @mouseenter="isHovered = true"
            @mouseleave="isHovered = false"
          >
            <div
              class="island-motion"
              :class="`orbit-${item.orbitType}`"
              :style="item.motionStyle"
            >
              <div class="island-card">
                <div class="island-header">
                  <img :src="item.msg.avatar || defaultAvatar" class="island-avatar" alt="avatar" />
                  <div class="island-meta">
                    <span class="island-name">{{ item.msg.nickname || '星海旅人' }}</span>
                    <time class="island-time">{{ formatDisplayTime(item.msg.createTime) }}</time>
                  </div>
                </div>
                <div class="island-content">
                  {{ item.msg.content }}
                </div>
              </div>
            </div>
          </div>
        </transition-group>
      </div>

      <!-- 视图 2：信笺长卷瀑布流（大容量自适应网格，承载数百上千条留言，随心平铺纵览） -->
      <div
        v-else
        key="wall-view"
        class="message-wall-field"
      >
        <div class="wall-meta-bar">
          <span class="wall-count-badge">共 {{ barrageList.length }} 封手记</span>
          <span class="wall-tip">滑动卷轴 · 细细品读每一枚往来心迹</span>
        </div>

        <div class="wall-grid">
          <article
            v-for="(msg, idx) in barrageList"
            :key="msg.id || idx"
            class="wall-card"
            :class="{ 'is-highlight': msg.isNew }"
          >
            <div class="wall-card-header">
              <img :src="msg.avatar || defaultAvatar" class="wall-card-avatar" alt="avatar" />
              <div class="wall-card-meta">
                <span class="wall-card-nickname">{{ msg.nickname || '星海旅人' }}</span>
                <time class="wall-card-time">{{ formatDisplayTime(msg.createTime) }}</time>
              </div>
              <span class="wall-card-index">#{{ String(barrageList.length - idx).padStart(3, '0') }}</span>
            </div>
            <p class="wall-card-content">{{ msg.content }}</p>
          </article>
        </div>

        <div v-if="barrageList.length === 0" class="wall-empty">
          暂无留言，写下第一笔手记吧
        </div>
      </div>
    </transition>

    <!-- 右下角悬浮快捷抽屉入口 -->
    <button
      class="drawer-trigger-bubble"
      type="button"
      @click="drawerVisible = true"
      title="打开留言手札"
    >
      <span class="trigger-icon">📜</span>
      <span class="trigger-label">留言手札</span>
      <span class="trigger-badge">{{ barrageList.length }}</span>
    </button>

    <!-- 侧边栏「留言手札」抽屉面板 -->
    <teleport to="body">
      <transition name="drawer-fade">
        <div
          v-if="drawerVisible"
          class="cosmos-drawer-mask"
          @click.self="drawerVisible = false"
        >
          <div class="cosmos-drawer-panel">
            <div class="drawer-header">
              <div>
                <div class="drawer-badge">GUESTBOOK ARCHIVE</div>
                <h3 class="drawer-title">全部留言手札</h3>
              </div>
              <button
                class="drawer-close-btn"
                type="button"
                aria-label="关闭抽屉"
                @click="drawerVisible = false"
              >
                ✕
              </button>
            </div>

            <div class="drawer-list">
              <article
                v-for="(item, idx) in barrageList"
                :key="item.id || idx"
                class="drawer-item"
              >
                <div class="item-avatar-col">
                  <img :src="item.avatar || defaultAvatar" class="avatar" alt="avatar" />
                  <span class="item-index">#{{ String(barrageList.length - idx).padStart(3, '0') }}</span>
                </div>
                <div class="item-body">
                  <div class="item-header">
                    <span class="item-nickname">{{ item.nickname || '星海旅人' }}</span>
                    <time class="item-time">{{ formatDisplayTime(item.createTime) }}</time>
                  </div>
                  <p class="item-content">{{ item.content }}</p>
                </div>
              </article>

              <div v-if="barrageList.length === 0" class="drawer-empty">
                暂无留言，写下星空第一笔记录吧
              </div>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script>
import { getMessagesApi, addMessageApi } from '@/api/message'
import { formatTime } from '@/utils/time'

// 接口一次性返回整张留言表且无分页，全量渲染会拖垮长卷视图；
// 只保留最近 N 条进入 DOM（数组本身仍是"最新在前"的顺序，浮岛巡航语义不变）
const MAX_BARRAGE_RENDER = 200

export default {
  name: 'Messages',

  data() {
    return {
      content: '',
      count: null,
      timer: null,
      sending: false,
      barrageList: [],
      drawerVisible: false,
      defaultAvatar: 'https://img.shiyit.com/avatar.jpg',

      // 视图控制：'orbit' (微浮岛漫游) | 'wall' (信笺长卷瀑布流)
      viewMode: 'orbit',
      isInitialized: false,
      // 首屏过渡定时器句柄（回调只写 isInitialized），卸载时须撤掉
      initTransitionTimer: null,
      autoCruise: true,
      isHovered: false,
      cruiseTimer: null,
      cruiseIndex: 0,
      rotateSlotCursor: 0,

      // 活动浮岛数组，每项携带独立的随机位姿与唯一 key，支持消失渐进动画
      activeIslands: [],

      // 12 个分散的环绕安全扇区定义（避免卡片聚集与重叠，且避开正中发射台）
      sectors: [
        // 0: 左上翼
        { minL: 3, maxL: 16, minT: 22, maxT: 32 },
        // 1: 中偏左上
        { minL: 18, maxL: 32, minT: 24, maxT: 34 },
        // 2: 中偏右上
        { minL: 66, maxL: 80, minT: 24, maxT: 34 },
        // 3: 右上翼
        { minL: 82, maxL: 92, minT: 22, maxT: 32 },

        // 4: 左中侧
        { minL: 4, maxL: 18, minT: 42, maxT: 54 },
        // 5: 中左下
        { minL: 20, maxL: 36, minT: 44, maxT: 56 },
        // 6: 中右下
        { minL: 64, maxL: 78, minT: 44, maxT: 56 },
        // 7: 右中侧
        { minL: 80, maxL: 92, minT: 42, maxT: 54 },

        // 8: 左下角
        { minL: 4, maxL: 18, minT: 64, maxT: 78 },
        // 9: 下中偏左
        { minL: 22, maxL: 40, minT: 66, maxT: 82 },
        // 10: 下中偏右
        { minL: 60, maxL: 76, minT: 66, maxT: 82 },
        // 11: 右下角
        { minL: 78, maxL: 90, minT: 64, maxT: 78 }
      ]
    }
  },

  async mounted() {
    window.addEventListener('resize', this.handleResize)
    await this.listMessage()
    this.initActiveIslands()
    this.$nextTick(() => {
      // 首屏已绘制，激活后续巡航与新增时的渐隐渐现过渡体系
      this.initTransitionTimer = setTimeout(() => {
        this.isInitialized = true
      }, 60)
    })
    this.startCruise()
  },

  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    this.stopCruise()
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
    // 首屏过渡定时器：回调只写 isInitialized，但组件卸载后仍会执行一次，
    // 留着等于让已卸载的实例继续被闭包引用
    if (this.initTransitionTimer) {
      clearTimeout(this.initTransitionTimer)
      this.initTransitionTimer = null
    }
  },

  methods: {
    formatDisplayTime(time) {
      if (!time) return '刚刚'
      return formatTime(time)
    },

    getCapacity() {
      if (typeof window === 'undefined') return 10
      if (window.innerWidth <= 768) return 5
      if (window.innerWidth <= 1200) return 8
      return 11
    },

    createIslandItem(msg, sector, sectorIndex) {
      const minL = sector.minL
      const maxL = sector.maxL
      const minT = sector.minT
      const maxT = sector.maxT

      // 扇区边界内的随机位姿坐标，确保换位与巡航时位置不再死板固定
      const left = (minL + Math.random() * (maxL - minL)).toFixed(1)
      const top = (minT + Math.random() * (maxT - minT)).toFixed(1)

      const orbitType = Math.floor(Math.random() * 8) + 1
      const delay = -(Math.random() * 10).toFixed(1)

      return {
        uniqueKey: `${msg.id || 'msg'}_${sectorIndex}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
        msg,
        sectorIndex,
        orbitType,
        style: {
          left: `${left}%`,
          top: `${top}%`
        },
        motionStyle: {
          animationDelay: `${delay}s`
        }
      }
    },

    initActiveIslands() {
      if (!this.barrageList.length) {
        this.activeIslands = []
        return
      }

      const capacity = Math.min(this.getCapacity(), this.barrageList.length)
      const list = []
      for (let i = 0; i < capacity; i++) {
        const msg = this.barrageList[i]
        const sector = this.sectors[i % this.sectors.length]
        list.push(this.createIslandItem(msg, sector, i))
      }
      this.activeIslands = list
      this.cruiseIndex = capacity
      this.rotateSlotCursor = 0
    },

    handleResize() {
      if (!this.barrageList.length) return
      const targetCap = Math.min(this.getCapacity(), this.barrageList.length)
      if (this.activeIslands.length > targetCap) {
        this.activeIslands.splice(targetCap)
      } else if (this.activeIslands.length < targetCap) {
        const diff = targetCap - this.activeIslands.length
        for (let i = 0; i < diff; i++) {
          const msgIdx = (this.cruiseIndex + i) % this.barrageList.length
          const sector = this.sectors[(this.activeIslands.length + i) % this.sectors.length]
          this.activeIslands.push(
            this.createIslandItem(this.barrageList[msgIdx], sector, this.activeIslands.length + i)
          )
        }
        this.cruiseIndex = (this.cruiseIndex + diff) % this.barrageList.length
      }
    },

    shuffleConstellation() {
      if (this.barrageList.length <= 1) return

      const capacity = Math.min(this.getCapacity(), this.barrageList.length)
      const nextIslands = []

      // 推进游标换一批
      this.cruiseIndex = (this.cruiseIndex + capacity) % this.barrageList.length

      for (let i = 0; i < capacity; i++) {
        const msgIdx = (this.cruiseIndex + i) % this.barrageList.length
        const msg = this.barrageList[msgIdx]
        const sector = this.sectors[i % this.sectors.length]
        nextIslands.push(this.createIslandItem(msg, sector, i))
      }

      // 整批替换：Vue transition-group 触发旧卡片平滑淡出，新卡片在新随机坐标优雅淡入
      this.activeIslands = nextIslands
      this.$message?.success('已换一批浮岛视角')
    },

    toggleAutoCruise() {
      this.autoCruise = !this.autoCruise
      if (this.autoCruise) {
        this.startCruise()
        this.$message?.success('已开启星岛平滑巡航')
      } else {
        this.stopCruise()
        this.$message?.info('已暂停自动巡航')
      }
    },

    startCruise() {
      this.stopCruise()
      // 每 7 秒巡航平滑换批 2~3 枚浮岛，带有平滑淡入淡出动效与全新随机落点
      this.cruiseTimer = setInterval(() => {
        this.cruiseStep()
      }, 7000)
    },

    stopCruise() {
      if (this.cruiseTimer) {
        clearInterval(this.cruiseTimer)
        this.cruiseTimer = null
      }
    },

    cruiseStep() {
      if (
        this.viewMode !== 'orbit' ||
        !this.autoCruise ||
        this.isHovered ||
        !this.activeIslands.length
      ) {
        return
      }

      // 如果有未展示的候补留言，则轮替展示新留言
      if (this.barrageList.length > this.activeIslands.length) {
        const replaceCount = Math.min(
          window.innerWidth <= 768 ? 2 : 3,
          this.barrageList.length - this.activeIslands.length
        )

        for (let i = 0; i < replaceCount; i++) {
          const slotIdx = (this.rotateSlotCursor + i) % this.activeIslands.length
          const nextMsgIdx = this.cruiseIndex % this.barrageList.length
          this.cruiseIndex = (this.cruiseIndex + 1) % this.barrageList.length

          const nextMsg = this.barrageList[nextMsgIdx]
          const sector = this.sectors[slotIdx % this.sectors.length]
          const newItem = this.createIslandItem(nextMsg, sector, slotIdx)

          // 局部更新：旧项平滑隐去，新项携全新随机坐标渐进淡入
          this.activeIslands.splice(slotIdx, 1, newItem)
        }

        this.rotateSlotCursor = (this.rotateSlotCursor + replaceCount) % this.activeIslands.length
      } else if (this.barrageList.length >= 2) {
        // 如果留言全量已在屏幕上，轻柔将其中 1 枚卡片微移重轨，赋予呼吸感
        const slotIdx = this.rotateSlotCursor % this.activeIslands.length
        const currentMsg = this.activeIslands[slotIdx].msg
        const sector = this.sectors[slotIdx % this.sectors.length]
        const newItem = this.createIslandItem(currentMsg, sector, slotIdx)
        this.activeIslands.splice(slotIdx, 1, newItem)
        this.rotateSlotCursor = (this.rotateSlotCursor + 1) % this.activeIslands.length
      }
    },

    insertNewIsland(newMessage) {
      // 优选靠近中央侧翼的活跃槽位
      const primeSector = this.sectors[1] || { minL: 18, maxL: 32, minT: 24, maxT: 34 }
      const newIsland = this.createIslandItem(newMessage, primeSector, 0)
      newIsland.msg.isNew = true

      const capacity = this.getCapacity()
      if (this.activeIslands.length >= capacity) {
        this.activeIslands.pop()
      }
      this.activeIslands.unshift(newIsland)
    },

    async listMessage() {
      try {
        const res = await getMessagesApi()
        const rawList = Array.isArray(res.data) ? res.data : []
        // 按时间倒序排列，最新留言排在最前；超出上限的部分不进入 DOM
        this.barrageList = rawList.slice().reverse().slice(0, MAX_BARRAGE_RENDER)
      } catch (err) {
        console.error('获取留言列表失败:', err)
      }
    },

    async submitMessage() {
      if (this.count) {
        this.$message?.warning(`${this.count} 秒后才能再次投递留言`)
        return
      }
      const trimmed = this.content.trim()
      if (!trimmed) {
        this.$message?.warning('留言内容不能为空')
        return
      }

      this.sending = true
      // 提交时才读 store：data() 里取的是初始化快照，登录后进来的用户会一直按游客发言
      const userInfo = this.$store.state.userInfo
      // 身份由服务端按会话填充，前端只提交内容
      const payload = { content: trimmed }
      const now = new Date()
      const pad = (value) => String(value).padStart(2, '0')
      // createTime 由后端 MyMetaObjectHandler 自动填充，前端不要提交
      // 本地乐观渲染用的昵称/头像只是展示，不代表服务端落库的身份
      const touristAvatar = this.$store.state.webSiteInfo?.touristAvatar || this.defaultAvatar
      const newMessage = {
        ...payload,
        avatar: userInfo ? userInfo.avatar : touristAvatar,
        nickname: userInfo ? userInfo.nickname : '星海旅人',
        createTime: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`,
        isNew: true
      }

      try {
        await addMessageApi(payload)
        this.barrageList.unshift(newMessage)
        // 立即以平滑渐入动画呈现在当前浮岛星空前台
        this.insertNewIsland(newMessage)
        this.content = ''
        this.$message?.success('手记已汇入星海')

        // 30 秒冷却倒计时
        const TIME_COUNT = 30
        this.count = TIME_COUNT
        this.timer = setInterval(() => {
          if (this.count > 0 && this.count <= TIME_COUNT) {
            this.count--
          } else {
            clearInterval(this.timer)
            this.timer = null
          }
        }, 1000)
      } catch (err) {
        this.$message?.error('投递失败，请稍后重试')
      } finally {
        this.sending = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  星空留言岛 · 白日晴空云海 / 深邃星空双模体系
//
//  白天：晴空微光云海 · 晨曦微尘暖阳信笺
//  黑夜：浩瀚深邃星空 · 墨夜双星云微浮岛
//  容量保障：浮岛视口恒定 12 枚 + 自动巡航 + 信笺长卷瀑布流
// ════════════════════════════════════════════════════════════════

.message-cosmos-page {
  // ── 默认：白日模式设计令牌 ──
  --msg-page-bg: #eef2f7;
  --msg-text-primary: #12171d;
  --msg-text-secondary: #59636f;
  --msg-text-tertiary: #8b949f;

  --msg-bg-gradient: radial-gradient(ellipse at 50% 12%, #ffffff 0%, #edf2f9 45%, #e1e9f4 100%);
  --msg-nebula-1: radial-gradient(circle, rgba(217, 154, 61, 0.18) 0%, rgba(217, 154, 61, 0) 70%);
  --msg-nebula-2: radial-gradient(circle, rgba(74, 128, 220, 0.16) 0%, rgba(74, 128, 220, 0) 70%);
  --msg-nebula-opacity: 0.7;

  --msg-star-color-1: rgba(90, 115, 155, 0.32);
  --msg-star-color-2: rgba(190, 135, 40, 0.38);
  --msg-stars-opacity-min: 0.35;
  --msg-stars-opacity-max: 0.75;

  --msg-title-gradient: linear-gradient(180deg, #12171d 20%, #374151 100%);
  --msg-title-meta: var(--accent);
  --msg-title-meta-shadow: none;

  --msg-input-box-bg: rgba(255, 255, 255, 0.86);
  --msg-input-box-border: rgba(18, 23, 29, 0.12);
  --msg-input-box-shadow: 0 8px 24px rgba(60, 75, 100, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.95);
  --msg-input-box-focus-bg: #ffffff;
  --msg-input-text: #12171d;
  --msg-input-placeholder: #8b949f;

  --msg-action-btn-bg: rgba(255, 255, 255, 0.78);
  --msg-action-btn-border: rgba(18, 23, 29, 0.12);
  --msg-action-btn-text: #4a5568;
  --msg-action-btn-hover-bg: #ffffff;
  --msg-action-btn-hover-text: #12171d;

  --msg-tab-bg: rgba(18, 23, 29, 0.06);
  --msg-tab-border: rgba(18, 23, 29, 0.08);
  --msg-tab-active-bg: #ffffff;
  --msg-tab-active-text: #12171d;
  --msg-tab-active-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  --msg-island-bg: rgba(255, 255, 255, 0.88);
  --msg-island-border: rgba(18, 23, 29, 0.09);
  --msg-island-shadow: 0 8px 22px rgba(60, 75, 100, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.95);
  --msg-island-hover-bg: rgba(255, 255, 255, 0.98);
  --msg-island-hover-border: rgba(168, 106, 16, 0.55);
  --msg-island-hover-shadow: 0 16px 36px rgba(45, 60, 90, 0.14), 0 0 16px rgba(168, 106, 16, 0.16);
  --msg-island-name: #12171d;
  --msg-island-time: #8b949f;
  --msg-island-content: #374151;
  --msg-island-avatar-border: rgba(18, 23, 29, 0.12);

  --msg-float-btn-bg: rgba(255, 255, 255, 0.92);
  --msg-float-btn-border: rgba(18, 23, 29, 0.12);
  --msg-float-btn-text: #12171d;
  --msg-float-btn-shadow: 0 6px 20px rgba(40, 60, 90, 0.1);
  --msg-float-btn-hover-bg: #ffffff;

  --msg-wall-card-bg: rgba(255, 255, 255, 0.88);
  --msg-wall-card-border: rgba(18, 23, 29, 0.09);
  --msg-wall-card-shadow: 0 4px 16px rgba(60, 75, 100, 0.06);
  --msg-wall-card-hover-shadow: 0 12px 28px rgba(45, 60, 90, 0.12);

  position: relative;
  min-height: calc(100vh - 64px);
  width: 100%;
  overflow: hidden;
  background-color: var(--msg-page-bg);
  color: var(--msg-text-primary);
  transition: background-color 0.3s ease, color 0.3s ease;

  &.is-wall-mode {
    overflow-y: auto;
    padding-bottom: 70px;
  }
}

// ── 黑夜模式：深邃星空墨夜设计令牌 ──
:root[data-theme='dark'] .message-cosmos-page {
  --msg-page-bg: #06090e;
  --msg-text-primary: #ffffff;
  --msg-text-secondary: rgba(255, 255, 255, 0.65);
  --msg-text-tertiary: rgba(255, 255, 255, 0.45);

  --msg-bg-gradient: radial-gradient(ellipse at 50% 20%, #101a2e 0%, #070a10 60%, #040507 100%);
  --msg-nebula-1: radial-gradient(circle, rgba(168, 106, 16, 0.42) 0%, rgba(168, 106, 16, 0) 70%);
  --msg-nebula-2: radial-gradient(circle, rgba(46, 84, 168, 0.36) 0%, rgba(46, 84, 168, 0) 70%);
  --msg-nebula-opacity: 0.35;

  --msg-star-color-1: rgba(255, 255, 255, 0.85);
  --msg-star-color-2: rgba(217, 154, 61, 0.75);
  --msg-stars-opacity-min: 0.4;
  --msg-stars-opacity-max: 0.9;

  --msg-title-gradient: linear-gradient(180deg, #ffffff 30%, rgba(255, 255, 255, 0.78) 100%);
  --msg-title-meta: var(--accent);
  --msg-title-meta-shadow: 0 0 12px rgba(168, 106, 16, 0.4);

  --msg-input-box-bg: rgba(255, 255, 255, 0.07);
  --msg-input-box-border: rgba(255, 255, 255, 0.22);
  --msg-input-box-shadow: 0 8px 28px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  --msg-input-box-focus-bg: rgba(255, 255, 255, 0.1);
  --msg-input-text: #ffffff;
  --msg-input-placeholder: rgba(255, 255, 255, 0.45);

  --msg-action-btn-bg: rgba(255, 255, 255, 0.05);
  --msg-action-btn-border: rgba(255, 255, 255, 0.16);
  --msg-action-btn-text: rgba(255, 255, 255, 0.75);
  --msg-action-btn-hover-bg: rgba(255, 255, 255, 0.12);
  --msg-action-btn-hover-text: #ffffff;

  --msg-tab-bg: rgba(255, 255, 255, 0.06);
  --msg-tab-border: rgba(255, 255, 255, 0.14);
  --msg-tab-active-bg: rgba(255, 255, 255, 0.18);
  --msg-tab-active-text: #ffffff;
  --msg-tab-active-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);

  --msg-island-bg: rgba(255, 255, 255, 0.08);
  --msg-island-border: rgba(255, 255, 255, 0.18);
  --msg-island-shadow: 0 8px 24px rgba(0, 0, 0, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.18);
  --msg-island-hover-bg: rgba(18, 25, 38, 0.88);
  --msg-island-hover-border: rgba(217, 154, 61, 0.6);
  --msg-island-hover-shadow: 0 16px 40px rgba(0, 0, 0, 0.6), 0 0 24px rgba(168, 106, 16, 0.35);
  --msg-island-name: rgba(255, 255, 255, 0.95);
  --msg-island-time: rgba(255, 255, 255, 0.5);
  --msg-island-content: rgba(255, 255, 255, 0.88);
  --msg-island-avatar-border: rgba(255, 255, 255, 0.3);

  --msg-float-btn-bg: rgba(14, 20, 32, 0.85);
  --msg-float-btn-border: rgba(255, 255, 255, 0.22);
  --msg-float-btn-text: #ffffff;
  --msg-float-btn-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
  --msg-float-btn-hover-bg: rgba(22, 32, 50, 0.95);

  --msg-wall-card-bg: rgba(255, 255, 255, 0.06);
  --msg-wall-card-border: rgba(255, 255, 255, 0.12);
  --msg-wall-card-shadow: 0 6px 20px rgba(0, 0, 0, 0.35);
  --msg-wall-card-hover-shadow: 0 14px 32px rgba(0, 0, 0, 0.55);
}

// ── 全景流光背景层 ──
.cosmos-background {
  position: fixed;
  inset: 0;
  pointer-events: none;
  background: var(--msg-bg-gradient);
  overflow: hidden;
  z-index: 0;
  transition: background 0.3s ease;

  .nebula {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: var(--msg-nebula-opacity);
    pointer-events: none;
    transition: opacity 0.3s ease;
  }

  .nebula-1 {
    width: 620px;
    height: 620px;
    top: -120px;
    left: 20%;
    background: var(--msg-nebula-1);
    animation: nebulaFloat 22s ease-in-out infinite alternate;
  }

  .nebula-2 {
    width: 720px;
    height: 720px;
    bottom: -150px;
    right: 15%;
    background: var(--msg-nebula-2);
    animation: nebulaFloat 28s ease-in-out infinite alternate-reverse;
  }
}

@keyframes nebulaFloat {
  0% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(40px, 30px) scale(1.08); }
  100% { transform: translate(-30px, 50px) scale(0.95); }
}

// 纯 CSS 微星点阵 / 晨光微尘
.stars-layer {
  position: absolute;
  inset: 0;
  background-repeat: repeat;

  &.stars-small {
    background-image:
      radial-gradient(1px 1px at 15% 20%, var(--msg-star-color-1) 50%, transparent 100%),
      radial-gradient(1px 1px at 45% 65%, var(--msg-star-color-1) 50%, transparent 100%),
      radial-gradient(1px 1px at 75% 30%, var(--msg-star-color-1) 50%, transparent 100%),
      radial-gradient(1px 1px at 85% 80%, var(--msg-star-color-1) 50%, transparent 100%),
      radial-gradient(1.5px 1.5px at 30% 85%, var(--msg-star-color-2) 50%, transparent 100%),
      radial-gradient(1px 1px at 60% 15%, var(--msg-star-color-1) 50%, transparent 100%);
    background-size: 320px 320px;
    animation: twinkle 5s ease-in-out infinite alternate;
  }

  &.stars-medium {
    background-image:
      radial-gradient(1.5px 1.5px at 20% 40%, var(--msg-star-color-1) 50%, transparent 100%),
      radial-gradient(2px 2px at 70% 60%, var(--msg-star-color-1) 50%, transparent 100%),
      radial-gradient(1.5px 1.5px at 90% 10%, var(--msg-star-color-2) 50%, transparent 100%);
    background-size: 460px 460px;
    animation: twinkle 7s ease-in-out infinite alternate-reverse;
  }
}

@keyframes twinkle {
  0% { opacity: var(--msg-stars-opacity-min); }
  100% { opacity: var(--msg-stars-opacity-max); }
}

// ── 顶部操作台 ──
.cosmos-station {
  position: relative;
  z-index: 20;
  max-width: 720px;
  margin: 0 auto;
  padding: clamp(32px, 7vh, 56px) 20px 18px;
  text-align: center;

  .station-meta {
    font-family: $font-mono;
    font-size: 0.75rem;
    letter-spacing: 0.16em;
    color: var(--msg-title-meta);
    margin-bottom: 8px;
    text-shadow: var(--msg-title-meta-shadow);
  }

  .station-title {
    font-family: $font-serif;
    font-size: clamp(1.8rem, 3.8vw, 2.5rem);
    font-weight: 600;
    margin: 0 0 8px;
    letter-spacing: 0.02em;
    background: var(--msg-title-gradient);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .station-desc {
    color: var(--msg-text-secondary);
    font-size: 0.9rem;
    margin: 0 0 22px;
    letter-spacing: 0.02em;
  }
}

// 输入框胶囊
.message-input-wrapper {
  max-width: 520px;
  margin: 0 auto;

  .input-inner {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 5px 6px 5px 18px;
    background: var(--msg-input-box-bg);
    border: 1px solid var(--msg-input-box-border);
    border-radius: 999px;
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    box-shadow: var(--msg-input-box-shadow);
    transition: border-color 0.25s ease, box-shadow 0.25s ease, background 0.25s ease;

    &:focus-within {
      border-color: rgba(217, 154, 61, 0.65);
      background: var(--msg-input-box-focus-bg);
      box-shadow: 0 10px 32px rgba(40, 60, 90, 0.16), 0 0 16px rgba(168, 106, 16, 0.25);
    }
  }

  .message-input {
    flex: 1;
    min-width: 0;
    border: none;
    background: transparent;
    color: var(--msg-input-text);
    font-size: 0.92rem;

    &::placeholder {
      color: var(--msg-input-placeholder);
    }

    &:focus {
      outline: none;
    }
  }

  .message-send-btn {
    flex: none;
    padding: 8px 20px;
    border: none;
    border-radius: 999px;
    background: var(--accent);
    color: #fff;
    font-size: 0.86rem;
    font-weight: 500;
    cursor: pointer;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
    transition: transform 0.2s ease, filter 0.2s ease, opacity 0.2s ease;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      filter: brightness(1.12);
    }

    &:active:not(:disabled) {
      transform: translateY(0);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .countdown-text {
      font-family: $font-mono;
    }
  }
}

// ── 切换与操作栏 ──
.station-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;

  // 分段选项卡切换
  .view-switch-group {
    display: inline-flex;
    align-items: center;
    padding: 3px;
    border-radius: 999px;
    background: var(--msg-tab-bg);
    border: 1px solid var(--msg-tab-border);

    .switch-btn {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 4px 12px;
      border-radius: 999px;
      border: none;
      background: transparent;
      color: var(--msg-text-secondary);
      font-size: 0.78rem;
      cursor: pointer;
      transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;

      &.active {
        background: var(--msg-tab-active-bg);
        color: var(--msg-tab-active-text);
        font-weight: 500;
        box-shadow: var(--msg-tab-active-shadow);
      }
    }
  }

  .action-pill-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 5px 14px;
    border-radius: 999px;
    border: 1px solid var(--msg-action-btn-border);
    background: var(--msg-action-btn-bg);
    color: var(--msg-action-btn-text);
    font-size: 0.78rem;
    cursor: pointer;
    backdrop-filter: blur(8px);
    transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

    &:hover {
      background: var(--msg-action-btn-hover-bg);
      border-color: rgba(168, 106, 16, 0.4);
      color: var(--msg-action-btn-hover-text);
    }

    &.is-cruise-active {
      color: var(--accent);
      border-color: rgba(168, 106, 16, 0.5);
      background: rgba(168, 106, 16, 0.08);
    }
  }
}

// ── 视图切换渐变动画 ──
.view-fade-enter-active,
.view-fade-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease;
}

.view-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.view-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

// ── 模式 1：微浮留言岛集群 ──
.orbit-view-wrapper {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 10;
  pointer-events: none;
  isolation: isolate;
  transform: translate3d(0, 0, 0);
}

.floating-orbit-field {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 10;
  pointer-events: none;
  isolation: isolate;
  transform: translate3d(0, 0, 0);
}

.floating-island {
  position: absolute;
  z-index: 10;
  pointer-events: auto;
  max-width: 290px;
  opacity: 1;
  will-change: transform, opacity;
  transform: translate3d(0, 0, 0);
  isolation: isolate;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;

  &:hover {
    z-index: 50 !important;

    .island-motion {
      animation-play-state: paused !important;
    }

    .island-card {
      transform: scale(1.08) translateY(-4px);
      border-color: var(--msg-island-hover-border);
      background: var(--msg-island-hover-bg);
      box-shadow: var(--msg-island-hover-shadow);
    }
  }

  &.is-highlight .island-card {
    border-color: var(--accent);
    box-shadow: 0 0 24px rgba(168, 106, 16, 0.45);
  }
}

// ── 独立漫游动力层（彻底与过渡层解耦，杜绝同节点动画/过渡冲突） ──
.island-motion {
  width: 100%;
  will-change: transform;
  transform: translate3d(0, 0, 0);
}

// ── 浮岛平滑巡航入场 / 离场动画体系 ──
.island-trans-enter-active {
  transition: opacity 0.5s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
  will-change: opacity, transform;
}

.island-trans-leave-active {
  position: absolute;
  pointer-events: none;
  z-index: 5;
  transition: opacity 0.4s ease, transform 0.4s ease;
  will-change: opacity, transform;
}

.island-trans-enter-from {
  opacity: 0 !important;
  transform: scale(0.86) translateY(12px) !important;
}

.island-trans-enter-to {
  opacity: 1;
  transform: scale(1) translateY(0);
}

.island-trans-leave-from {
  opacity: 1;
  transform: scale(1) translateY(0);
}

.island-trans-leave-to {
  opacity: 0 !important;
  transform: scale(0.86) translateY(-12px) !important;
}

// 岛屿卡片玻璃体
.island-card {
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--msg-island-bg);
  border: 1px solid var(--msg-island-border);
  backdrop-filter: blur(14px) saturate(140%);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
  box-shadow: var(--msg-island-shadow);
  transform: translateZ(0);
  will-change: transform;
  transition: transform 0.28s cubic-bezier(0.2, 0, 0.25, 1),
    border-color 0.25s ease,
    background 0.25s ease,
    box-shadow 0.28s ease;

  .island-header {
    display: flex;
    align-items: center;
    gap: 9px;
    margin-bottom: 8px;

    .island-avatar {
      flex: none;
      width: 28px;
      height: 28px;
      border-radius: 50%;
      object-fit: cover;
      box-shadow: 0 0 0 1.5px var(--msg-island-avatar-border);
    }

    .island-meta {
      flex: 1;
      min-width: 0;
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      gap: 8px;

      .island-name {
        font-size: 0.84rem;
        font-weight: 500;
        color: var(--msg-island-name);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .island-time {
        font-family: $font-mono;
        font-size: 0.68rem;
        color: var(--msg-island-time);
        flex: none;
      }
    }
  }

  .island-content {
    font-size: 0.85rem;
    line-height: 1.55;
    color: var(--msg-island-content);
    word-break: break-word;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

// ── 8 组独立的微重力漫游轨迹 ──
.orbit-1 { animation: orbitPath1 11s ease-in-out infinite alternate; }
.orbit-2 { animation: orbitPath2 13s ease-in-out infinite alternate; }
.orbit-3 { animation: orbitPath3 9.5s ease-in-out infinite alternate; }
.orbit-4 { animation: orbitPath4 14s ease-in-out infinite alternate; }
.orbit-5 { animation: orbitPath5 12s ease-in-out infinite alternate; }
.orbit-6 { animation: orbitPath6 10s ease-in-out infinite alternate; }
.orbit-7 { animation: orbitPath7 12.5s ease-in-out infinite alternate; }
.orbit-8 { animation: orbitPath8 15s ease-in-out infinite alternate; }

@keyframes orbitPath1 {
  0% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(12px, -14px) rotate(1deg); }
  100% { transform: translate(-10px, 10px) rotate(-1deg); }
}

@keyframes orbitPath2 {
  0% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(-14px, 12px) rotate(-1.5deg); }
  100% { transform: translate(12px, -8px) rotate(1deg); }
}

@keyframes orbitPath3 {
  0% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(10px, 14px) rotate(1.2deg); }
  100% { transform: translate(-12px, -12px) rotate(-0.8deg); }
}

@keyframes orbitPath4 {
  0% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(-12px, -14px) rotate(-1deg); }
  100% { transform: translate(14px, 10px) rotate(1.5deg); }
}

@keyframes orbitPath5 {
  0% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(8px, -12px) rotate(0.8deg); }
  100% { transform: translate(-14px, 14px) rotate(-1.2deg); }
}

@keyframes orbitPath6 {
  0% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(-10px, 10px) rotate(-1deg); }
  100% { transform: translate(10px, -12px) rotate(1deg); }
}

@keyframes orbitPath7 {
  0% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(14px, -10px) rotate(1.4deg); }
  100% { transform: translate(-8px, 12px) rotate(-1deg); }
}

@keyframes orbitPath8 {
  0% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(-12px, 8px) rotate(-1deg); }
  100% { transform: translate(10px, -14px) rotate(1.2deg); }
}

// ── 模式 2：信笺长卷瀑布流平铺视图 ──
.message-wall-field {
  position: relative;
  z-index: 15;
  max-width: 1200px;
  margin: 0 auto;
  padding: 10px 24px 48px;

  .wall-meta-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
    padding: 0 4px;

    .wall-count-badge {
      font-family: $font-mono;
      font-size: 0.76rem;
      padding: 3px 10px;
      border-radius: 999px;
      background: var(--msg-tab-bg);
      border: 1px solid var(--msg-tab-border);
      color: var(--accent);
      font-weight: 500;
    }

    .wall-tip {
      font-size: 0.8rem;
      color: var(--msg-text-tertiary);
    }
  }

  .wall-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
  }

  .wall-card {
    padding: 16px 18px;
    border-radius: 12px;
    background: var(--msg-wall-card-bg);
    border: 1px solid var(--msg-wall-card-border);
    backdrop-filter: blur(14px);
    -webkit-backdrop-filter: blur(14px);
    box-shadow: var(--msg-wall-card-shadow);
    transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;

    &:hover {
      transform: translateY(-3px);
      box-shadow: var(--msg-wall-card-hover-shadow);
      border-color: rgba(168, 106, 16, 0.45);
    }

    &.is-highlight {
      border-color: var(--accent);
      box-shadow: 0 0 20px rgba(168, 106, 16, 0.35);
    }

    .wall-card-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 12px;

      .wall-card-avatar {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        object-fit: cover;
        box-shadow: 0 0 0 1.5px var(--msg-island-avatar-border);
      }

      .wall-card-meta {
        flex: 1;
        min-width: 0;

        .wall-card-nickname {
          display: block;
          font-size: 0.88rem;
          font-weight: 500;
          color: var(--msg-island-name);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .wall-card-time {
          font-family: $font-mono;
          font-size: 0.7rem;
          color: var(--msg-island-time);
        }
      }

      .wall-card-index {
        font-family: $font-mono;
        font-size: 0.72rem;
        color: var(--msg-text-tertiary);
      }
    }

    .wall-card-content {
      margin: 0;
      font-size: 0.88rem;
      line-height: 1.6;
      color: var(--msg-island-content);
      white-space: pre-wrap;
      word-break: break-word;
    }
  }

  .wall-empty {
    text-align: center;
    color: var(--msg-text-tertiary);
    padding: 60px 0;
    font-size: 0.9rem;
  }
}

// ── 右下角悬浮快捷按钮 ──
.drawer-trigger-bubble {
  position: fixed;
  right: 24px;
  bottom: 28px;
  z-index: 35;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid var(--msg-float-btn-border);
  background: var(--msg-float-btn-bg);
  backdrop-filter: blur(12px);
  color: var(--msg-float-btn-text);
  font-size: 0.84rem;
  cursor: pointer;
  box-shadow: var(--msg-float-btn-shadow);
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(217, 154, 61, 0.6);
    background: var(--msg-float-btn-hover-bg);
  }

  .trigger-badge {
    font-family: $font-mono;
    font-size: 0.72rem;
    padding: 2px 7px;
    border-radius: 999px;
    background: var(--accent);
    color: #fff;
  }
}

// ── 侧边栏「留言手札」抽屉 ──
.cosmos-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(6px);
  display: flex;
  justify-content: flex-end;
}

// 抽屉面板（默认白天模式）
.cosmos-drawer-panel {
  width: 100%;
  max-width: 440px;
  height: 100%;
  background: #ffffff;
  border-left: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.12);
  color: #12171d;

  .drawer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24px 20px 16px;
    border-bottom: 1px solid #edf2f7;

    .drawer-badge {
      font-family: $font-mono;
      font-size: 0.7rem;
      letter-spacing: 0.12em;
      color: var(--accent);
    }

    .drawer-title {
      font-family: $font-serif;
      font-size: 1.25rem;
      font-weight: 600;
      margin: 4px 0 0;
      color: #12171d;
    }

    .drawer-close-btn {
      border: none;
      background: transparent;
      color: #64748b;
      font-size: 1.1rem;
      cursor: pointer;
      padding: 6px;
      border-radius: 4px;
      transition: color 0.2s ease, background 0.2s ease;

      &:hover {
        color: #12171d;
        background: #f1f5f9;
      }
    }
  }

  .drawer-list {
    flex: 1;
    overflow-y: auto;
    padding: 16px 20px 32px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .drawer-item {
    display: flex;
    gap: 14px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f1f5f9;

    .item-avatar-col {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;

      .avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        object-fit: cover;
        box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.08);
      }

      .item-index {
        font-family: $font-mono;
        font-size: 0.68rem;
        color: #94a3b8;
      }
    }

    .item-body {
      flex: 1;
      min-width: 0;

      .item-header {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        gap: 8px;
        margin-bottom: 6px;

        .item-nickname {
          font-size: 0.88rem;
          font-weight: 500;
          color: #1e293b;
        }

        .item-time {
          font-family: $font-mono;
          font-size: 0.72rem;
          color: #94a3b8;
        }
      }

      .item-content {
        margin: 0;
        font-size: 0.88rem;
        line-height: 1.6;
        color: #334155;
        white-space: pre-wrap;
        word-break: break-word;
      }
    }
  }

  .drawer-empty {
    text-align: center;
    color: #94a3b8;
    padding: 48px 0;
    font-size: 0.88rem;
  }
}

// 黑夜模式下的抽屉面板
:root[data-theme='dark'] .cosmos-drawer-panel {
  background: #0f141d;
  border-left: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.6);
  color: #fff;

  .drawer-header {
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .drawer-title {
      color: #fff;
    }

    .drawer-close-btn {
      color: rgba(255, 255, 255, 0.6);

      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.08);
      }
    }
  }

  .drawer-item {
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);

    .item-avatar-col .avatar {
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.2);
    }

    .item-avatar-col .item-index {
      color: rgba(255, 255, 255, 0.4);
    }

    .item-body {
      .item-nickname {
        color: rgba(255, 255, 255, 0.95);
      }

      .item-time {
        color: rgba(255, 255, 255, 0.45);
      }

      .item-content {
        color: rgba(255, 255, 255, 0.82);
      }
    }
  }

  .drawer-empty {
    color: rgba(255, 255, 255, 0.5);
  }
}

// 抽屉展开与退场过渡
.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.28s ease;

  .cosmos-drawer-panel {
    transition: transform 0.28s cubic-bezier(0.16, 1, 0.3, 1);
  }
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;

  .cosmos-drawer-panel {
    transform: translateX(100%);
  }
}

// ── 响应式适配 ──
@media screen and (max-width: 768px) {
  .floating-island {
    max-width: 220px;

    .island-card {
      padding: 8px 10px;

      .island-content {
        font-size: 0.78rem;
        -webkit-line-clamp: 2;
      }
    }
  }

  .cosmos-station {
    padding-top: 28px;

    .station-title {
      font-size: 1.65rem;
    }
  }

  .station-actions {
    gap: 8px;

    .view-switch-group .switch-btn {
      padding: 3px 10px;
      font-size: 0.74rem;
    }

    .action-pill-btn {
      padding: 4px 11px;
      font-size: 0.74rem;
    }
  }

  .message-wall-field {
    padding: 10px 16px 40px;

    .wall-grid {
      grid-template-columns: 1fr;
    }
  }

  .drawer-trigger-bubble {
    right: 16px;
    bottom: 20px;
    padding: 6px 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .floating-island,
  .nebula,
  .stars-layer {
    animation: none !important;
  }
}
</style>
