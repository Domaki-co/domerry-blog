<template>
  <div v-show="visible"
       class="context-menu"
       :class="{ 'menu-show': visible }"
       :style="{ left: adjustedX + 'px', top: adjustedY + 'px' }">
    <div class="menu-item" @click="handleCopyText">
      <i class="fas fa-copy"></i>
      复制
    </div>
    <div class="menu-item" @click="handleRefresh">
      <i class="fas fa-sync"></i>
      刷新页面
    </div>
    <div class="menu-item" @click="handleBack">
      <i class="fas fa-arrow-left"></i>
      返回上页
    </div>
    <div class="menu-item" @click="handleForward">
      <i class="fas fa-arrow-right"></i>
      前进下页
    </div>
    <div class="menu-item" @click="handleCopyUrl">
      <i class="fas fa-copy"></i>
      复制链接
    </div>
    <div class="divider"></div>
    <div class="menu-item" @click="toggleTheme">
      <i :class="['fas', isDark ? 'fa-sun' : 'fa-moon']"></i>
      {{ isDark ? '浅色模式' : '深色模式' }}
    </div>
  </div>
</template>

<script>
import { getThemeMode, setThemeModeWithTransition } from '@/utils/theme'
export default {
  name: 'ContextMenu',
  data() {
    return {
      visible: false,
      x: 0,
      y: 0,
      menuWidth: 0,
      menuHeight: 0,
      isDark: false,
      excludeSelectors: ['.image-preview']
    }
  },
  computed: {
    adjustedX() {
      const windowWidth = window.innerWidth
      return this.x + this.menuWidth > windowWidth ? this.x - this.menuWidth : this.x
    },
    adjustedY() {
      const windowHeight = window.innerHeight
      return this.y + this.menuHeight > windowHeight ? this.y - this.menuHeight : this.y
    }
  },
  created() {
    this.$nextTick(() => {
      this.isDark = getThemeMode() === 'dark'
    })
  },
  methods: {
    /**
     * 显示右键菜单
     * @param event
     */
    show(event) {
      const isInExcludeArea = this.excludeSelectors.some(selector => {
        const element = event.target.closest(selector)
        return element !== null
      })

      if (isInExcludeArea) {
        return
      }

      event.preventDefault()
      this.x = event.clientX
      this.y = event.clientY
      this.visible = true

      this.$nextTick(() => {
        this.menuWidth = this.$el.offsetWidth
        this.menuHeight = this.$el.offsetHeight
        this.x = event.clientX
        this.y = event.clientY
      })
      // 保存当前选区
      const selection = window.getSelection();
      if (!selection.toString().trim() && !this.isInEditableArea(event.target)) {
        return this.hide();
      }
    },
    // 新增方法：检查是否在可编辑区域
    isInEditableArea(element) {
      return element.closest('[contenteditable], textarea, input');
    },
    /**
     * 隐藏右键菜单
     */
    hide() {
      this.visible = false
    },
    /**
     * 刷新页面
     */
    handleRefresh() {
      window.location.reload()
      this.hide()
    },
    /**
     * 返回上页
     */
    handleBack() {
      this.$router.back()
      this.hide()
    },
    /**
     * 前进下页
     */
    handleForward() {
      this.$router.forward()
      this.hide()
    },
    /**
     * 复制链接
     */
    handleCopyUrl() {
      navigator.clipboard.writeText(window.location.href)
      this.$message.success('链接已复制到剪贴板')
      this.hide()
    },
    /**
     * 切换主题
     */
    toggleTheme(event) {
      // 扩散起点要在 hide() 之前抓：菜单一隐藏就取不到布局盒了
      const origin = (event && event.currentTarget) || null
      this.isDark = !this.isDark
      const mode = this.isDark ? 'dark' : 'light'
      setThemeModeWithTransition(mode, origin)
      this.hide()
    },
    handleCopyText() {
      // 添加CSS样式检测
      // if (window.getComputedStyle(event.target).userSelect === 'none') {
      //   this.$message.warning('该区域不允许复制');
      //   return this.hide();
      // }
      const selection = document.getSelection();
      if (selection.rangeCount > 0) {
        const selectedText = selection.toString().trim();
        if (selectedText) {
          navigator.clipboard.writeText(selectedText)
              .then(() => {
                this.$message.success('文本已复制到剪贴板');
              })
              .catch(err => {
                this.$message.error('复制文本失败');
                console.error('复制失败:', err);
              });
        }
      } else {
        this.$message.warning('请先选择一段文本');
      }
      this.hide();
    }

  },
}
</script>

<style scoped>
.context-menu {
  position: fixed;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 5px 0;
  z-index: 9999;
  opacity: 0;
  transform: scale(0.95);
  transform-origin: top left;
  /* 确保菜单本身不影响文本选择 */
  user-select: none;
  -webkit-user-select: none;
}
/* 需要排除的不可选区域增强样式
   注意：原来这里用了 `--selectable-text: { ... }` + `@apply --selectable-text;`
   （Tailwind 语法），在 Vite/esbuild 下既不是合法 CSS 也不生效，
   构建时还会报 "Expected identifier but found whitespace"，已展开成普通声明。 */
.image-preview {
  user-select: none !important;
  -webkit-user-select: none !important;
  pointer-events: none; /* 防止在这些区域触发选择 */
}

/* 允许选择的区域（根据项目实际情况添加） */
.article-content, .text-area {
  user-select: text;
  -webkit-user-select: text;
  cursor: auto;
}
.menu-show {
  animation: showMenu 0.2s ease forwards;
}

@keyframes showMenu {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
}

.menu-item:hover {
  background-color: var(--hover-bg);
}

.menu-item i {
  width: 14px;
  /* 原来每个菜单项一个 Element 主题色（蓝/绿/橙），新设计只有一个强调色 */
  color: var(--text-tertiary);
}

.menu-item:hover i {
  color: var(--accent);
}

.divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 5px 0;
}
</style>
