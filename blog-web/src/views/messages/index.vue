<template>
  <div class="message-cosmos-page">
    <!-- 星空背景与星云流光层 -->
    <div class="cosmos-background">
      <div class="nebula nebula-1"></div>
      <div class="nebula nebula-2"></div>
      <div class="stars-layer stars-small"></div>
      <div class="stars-layer stars-medium"></div>
    </div>

    <!-- 顶部星空操作台与发射中心 -->
    <div class="cosmos-station">
      <div class="station-meta">✦ CONSTELLATION GUESTBOOK ✦</div>
      <h1 class="station-title">星空留言岛</h1>
      <p class="station-desc">在浩瀚星海中，留下一枚属于你的微光手记</p>

      <div class="message-input-wrapper">
        <div class="input-inner">
          <input
            v-model="content"
            class="message-input"
            type="text"
            placeholder="写下一句微光絮语... (Enter 发送)"
            maxlength="200"
            @keyup.enter="submitMessage"
          />
          <button
            class="message-send-btn"
            type="button"
            :disabled="Boolean(count) || sending"
            @click="submitMessage"
          >
            <span v-if="count" class="countdown-text">{{ count }}s</span>
            <span v-else-if="sending" class="sending-text">发射中...</span>
            <span v-else class="send-label">投递</span>
          </button>
        </div>
      </div>

      <!-- 快速视图切换栏 -->
      <div class="station-actions">
        <button
          type="button"
          class="action-pill-btn"
          @click="shuffleConstellation"
          title="重新排布星岛"
        >
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.19" />
          </svg>
          漫游换位
        </button>
        <button
          type="button"
          class="action-pill-btn"
          @click="drawerVisible = true"
        >
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 6h16M4 12h16M4 18h12" />
          </svg>
          查阅手札名册 ({{ barrageList.length }})
        </button>
      </div>
    </div>

    <!-- 星空微浮留言岛集群 -->
    <div class="floating-orbit-field" ref="orbitField">
      <div
        v-for="(msg, index) in visibleIslands"
        :key="msg.id || index"
        class="floating-island"
        :class="[`orbit-${(index % 8) + 1}`, { 'is-highlight': msg.isNew }]"
        :style="getIslandStyle(index)"
      >
        <div class="island-card">
          <div class="island-header">
            <img :src="msg.avatar || defaultAvatar" class="island-avatar" alt="avatar" />
            <div class="island-meta">
              <span class="island-name">{{ msg.nickname || '星海旅人' }}</span>
              <time class="island-time">{{ formatDisplayTime(msg.createTime) }}</time>
            </div>
          </div>
          <div class="island-content">
            {{ msg.content }}
          </div>
        </div>
      </div>
    </div>

    <!-- 右下角浮动快捷抽屉触发按钮 -->
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

    <!-- 侧边栏「留言手札」沉浸抽屉面板 -->
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
                <div class="drawer-badge">GUESTBOOK LOG</div>
                <h3 class="drawer-title">全部留言手札</h3>
              </div>
              <button
                class="drawer-close-btn"
                type="button"
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

export default {
  name: 'Messages',

  data() {
    return {
      content: '',
      count: null,
      timer: null,
      sending: false,
      barrageList: [],
      displayOffset: 0,
      drawerVisible: false,
      user: this.$store.state.userInfo,
      defaultAvatar: 'https://img.shiyit.com/avatar.jpg',
      // 网格槽位分布定义，避免微浮岛互相严重重叠
      slotPositions: [
        { left: 6, top: 28 },
        { left: 34, top: 24 },
        { left: 68, top: 26 },
        { left: 14, top: 48 },
        { left: 48, top: 46 },
        { left: 78, top: 44 },
        { left: 8, top: 68 },
        { left: 38, top: 66 },
        { left: 72, top: 70 },
        { left: 24, top: 84 },
        { left: 58, top: 82 },
        { left: 82, top: 86 }
      ]
    }
  },

  computed: {
    visibleIslands() {
      if (!this.barrageList.length) return []
      // 提取最多 12 个微浮岛呈现在当前星空画布中
      const total = this.barrageList.length
      const count = Math.min(12, total)
      const list = []
      for (let i = 0; i < count; i++) {
        const targetIndex = (this.displayOffset + i) % total
        list.push(this.barrageList[targetIndex])
      }
      return list
    }
  },

  mounted() {
    this.listMessage()
  },

  beforeUnmount() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },

  methods: {
    formatDisplayTime(time) {
      if (!time) return '刚刚'
      return formatTime(time)
    },

    getIslandStyle(index) {
      const slot = this.slotPositions[index % this.slotPositions.length] || { left: 50, top: 50 }
      // 轻微伪随机扰动，让星空分布更加天然错落
      const jitterX = ((index * 7) % 7) - 3
      const jitterY = ((index * 5) % 7) - 3
      const left = Math.max(4, Math.min(84, slot.left + jitterX))
      const top = Math.max(22, Math.min(86, slot.top + jitterY))

      return {
        left: `${left}%`,
        top: `${top}%`,
        animationDelay: `${-(index * 1.3).toFixed(1)}s`
      }
    },

    shuffleConstellation() {
      if (this.barrageList.length <= 1) return
      this.displayOffset = (this.displayOffset + 5) % this.barrageList.length
      this.$message?.success('已切换星空视角')
    },

    async listMessage() {
      try {
        const res = await getMessagesApi()
        const rawList = res.data || []
        // 按时间倒序排列，最新留言排在最前
        this.barrageList = rawList.slice().reverse()
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
      const touristAvatar = this.$store.state.webSiteInfo?.touristAvatar || this.defaultAvatar
      // createTime 由后端 MyMetaObjectHandler 自动填充，前端不要提交
      // （LocalDateTime + @JsonFormat("yyyy-MM-dd HH:mm:ss") 无法解析 ISO 的 "T...Z"）
      const payload = {
        avatar: this.user ? this.user.avatar : touristAvatar,
        nickname: this.user ? this.user.nickname : '星海旅人',
        content: trimmed
      }
      const now = new Date()
      const pad = (value) => String(value).padStart(2, '0')
      const newMessage = {
        ...payload,
        createTime: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`,
        isNew: true
      }

      try {
        await addMessageApi(payload)
        // 插入到最前面并设为焦点
        this.barrageList.unshift(newMessage)
        this.displayOffset = 0
        this.content = ''
        this.$message?.success('手记已汇入星海')

        // 启动 30 秒冷却倒计时
        const TIME_COUNT = 1
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
//  星空微浮留言岛 · 漂流漫游
//
//  告别死沉发灰背景：深邃星空墨夜 + 柔和双星云呼吸流光
//  告别单向跑马灯：错落微浮岛，微重力慢速悬浮，悬停定格品读
//  严格遵从指令：不展示城市属地
// ════════════════════════════════════════════════════════════════

.message-cosmos-page {
  position: relative;
  min-height: calc(100vh - 64px);
  width: 100%;
  overflow: hidden;
  background-color: #06090e;
  color: #fff;
}

// ── 星空流光背景 ──
.cosmos-background {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(ellipse at 50% 20%, #101a2e 0%, #070a10 60%, #040507 100%);
  overflow: hidden;
  z-index: 1;

  .nebula {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.35;
    pointer-events: none;
  }

  .nebula-1 {
    width: 620px;
    height: 620px;
    top: -120px;
    left: 20%;
    background: radial-gradient(circle, rgba(168, 106, 16, 0.42) 0%, rgba(168, 106, 16, 0) 70%);
    animation: nebulaFloat 22s ease-in-out infinite alternate;
  }

  .nebula-2 {
    width: 720px;
    height: 720px;
    bottom: -150px;
    right: 15%;
    background: radial-gradient(circle, rgba(46, 84, 168, 0.36) 0%, rgba(46, 84, 168, 0) 70%);
    animation: nebulaFloat 28s ease-in-out infinite alternate-reverse;
  }
}

@keyframes nebulaFloat {
  0% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(40px, 30px) scale(1.08); }
  100% { transform: translate(-30px, 50px) scale(0.95); }
}

// 纯 CSS 微星点阵
.stars-layer {
  position: absolute;
  inset: 0;
  background-repeat: repeat;

  &.stars-small {
    background-image:
      radial-gradient(1px 1px at 15% 20%, rgba(255, 255, 255, 0.8) 50%, transparent 100%),
      radial-gradient(1px 1px at 45% 65%, rgba(255, 255, 255, 0.6) 50%, transparent 100%),
      radial-gradient(1px 1px at 75% 30%, rgba(255, 255, 255, 0.8) 50%, transparent 100%),
      radial-gradient(1px 1px at 85% 80%, rgba(255, 255, 255, 0.5) 50%, transparent 100%),
      radial-gradient(1.5px 1.5px at 30% 85%, rgba(217, 154, 61, 0.7) 50%, transparent 100%),
      radial-gradient(1px 1px at 60% 15%, rgba(255, 255, 255, 0.7) 50%, transparent 100%);
    background-size: 320px 320px;
    animation: twinkle 5s ease-in-out infinite alternate;
  }

  &.stars-medium {
    background-image:
      radial-gradient(1.5px 1.5px at 20% 40%, rgba(255, 255, 255, 0.9) 50%, transparent 100%),
      radial-gradient(2px 2px at 70% 60%, rgba(255, 255, 255, 0.8) 50%, transparent 100%),
      radial-gradient(1.5px 1.5px at 90% 10%, rgba(217, 154, 61, 0.8) 50%, transparent 100%);
    background-size: 460px 460px;
    animation: twinkle 7s ease-in-out infinite alternate-reverse;
  }
}

@keyframes twinkle {
  0% { opacity: 0.4; }
  100% { opacity: 0.9; }
}

// ── 顶部发射中心 ──
.cosmos-station {
  position: relative;
  z-index: 20;
  max-width: 680px;
  margin: 0 auto;
  padding: clamp(36px, 8vh, 64px) 20px 16px;
  text-align: center;

  .station-meta {
    font-family: $font-mono;
    font-size: 0.75rem;
    letter-spacing: 0.16em;
    color: var(--accent);
    margin-bottom: 8px;
    text-shadow: 0 0 12px rgba(168, 106, 16, 0.4);
  }

  .station-title {
    font-family: $font-serif;
    font-size: clamp(1.8rem, 3.8vw, 2.5rem);
    font-weight: 600;
    margin: 0 0 8px;
    letter-spacing: 0.02em;
    background: linear-gradient(180deg, #ffffff 30%, rgba(255, 255, 255, 0.78) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .station-desc {
    color: rgba(255, 255, 255, 0.65);
    font-size: 0.9rem;
    margin: 0 0 24px;
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
    background: rgba(255, 255, 255, 0.07);
    border: 1px solid rgba(255, 255, 255, 0.22);
    border-radius: 999px;
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    box-shadow: 0 8px 28px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.2);
    transition: border-color 0.25s ease, box-shadow 0.25s ease, background 0.25s ease;

    &:focus-within {
      border-color: rgba(217, 154, 61, 0.6);
      background: rgba(255, 255, 255, 0.1);
      box-shadow: 0 10px 32px rgba(0, 0, 0, 0.5), 0 0 16px rgba(168, 106, 16, 0.28);
    }
  }

  .message-input {
    flex: 1;
    min-width: 0;
    border: none;
    background: transparent;
    color: #fff;
    font-size: 0.92rem;

    &::placeholder {
      color: rgba(255, 255, 255, 0.45);
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
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
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

// 切换与操作栏
.station-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 18px;

  .action-pill-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 5px 14px;
    border-radius: 999px;
    border: 1px solid rgba(255, 255, 255, 0.16);
    background: rgba(255, 255, 255, 0.05);
    color: rgba(255, 255, 255, 0.75);
    font-size: 0.78rem;
    cursor: pointer;
    backdrop-filter: blur(8px);
    transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.12);
      border-color: rgba(255, 255, 255, 0.35);
      color: #fff;
    }
  }
}

// ── 微浮留言岛集群 ──
.floating-orbit-field {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 10;
  pointer-events: none;
}

.floating-island {
  position: absolute;
  pointer-events: auto;
  max-width: 290px;
  will-change: transform;
  transition: z-index 0s, filter 0.25s ease;

  // 鼠标悬停定格、升起、聚焦
  &:hover {
    animation-play-state: paused !important;
    z-index: 50 !important;

    .island-card {
      transform: scale(1.08) translateY(-4px);
      border-color: rgba(217, 154, 61, 0.6);
      background: rgba(18, 25, 38, 0.88);
      box-shadow: 0 16px 40px rgba(0, 0, 0, 0.6), 0 0 24px rgba(168, 106, 16, 0.35);
    }
  }

  &.is-highlight .island-card {
    border-color: var(--accent);
    box-shadow: 0 0 24px rgba(168, 106, 16, 0.45);
  }
}

// 岛屿卡片玻璃体
.island-card {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(14px) saturate(140%);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.18);
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
      box-shadow: 0 0 0 1.5px rgba(255, 255, 255, 0.3);
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
        color: rgba(255, 255, 255, 0.95);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .island-time {
        font-family: $font-mono;
        font-size: 0.68rem;
        color: rgba(255, 255, 255, 0.5);
        flex: none;
      }
    }
  }

  .island-content {
    font-size: 0.85rem;
    line-height: 1.5;
    color: rgba(255, 255, 255, 0.88);
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

// ── 右下角悬浮入口按钮 ──
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
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(14, 20, 32, 0.85);
  backdrop-filter: blur(12px);
  color: #fff;
  font-size: 0.84rem;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(217, 154, 61, 0.6);
    background: rgba(22, 32, 50, 0.95);
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
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(6px);
  display: flex;
  justify-content: flex-end;
}

.cosmos-drawer-panel {
  width: 100%;
  max-width: 440px;
  height: 100%;
  background: #0f141d;
  border-left: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.6);
  color: #fff;

  .drawer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24px 20px 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

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
    }

    .drawer-close-btn {
      border: none;
      background: transparent;
      color: rgba(255, 255, 255, 0.6);
      font-size: 1.1rem;
      cursor: pointer;
      padding: 6px;
      border-radius: 4px;
      transition: color 0.2s ease, background 0.2s ease;

      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.08);
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
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);

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
        box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.2);
      }

      .item-index {
        font-family: $font-mono;
        font-size: 0.68rem;
        color: rgba(255, 255, 255, 0.4);
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
          color: rgba(255, 255, 255, 0.95);
        }

        .item-time {
          font-family: $font-mono;
          font-size: 0.72rem;
          color: rgba(255, 255, 255, 0.45);
        }
      }

      .item-content {
        margin: 0;
        font-size: 0.88rem;
        line-height: 1.6;
        color: rgba(255, 255, 255, 0.82);
        white-space: pre-wrap;
        word-break: break-word;
      }
    }
  }

  .drawer-empty {
    text-align: center;
    color: rgba(255, 255, 255, 0.5);
    padding: 48px 0;
    font-size: 0.88rem;
  }
}

// 抽屉动画
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
    padding-top: 32px;

    .station-title {
      font-size: 1.7rem;
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
