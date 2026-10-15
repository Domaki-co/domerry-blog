<template>
  <div>
    <!-- banner -->
    <div class="message-banner" :style="cover">
      <!-- 弹幕输入框 -->
      <div class="message-container">
        <h1 class="message-title">留言板</h1>
        <div class="message-input-wrapper">
          <input
            class="message-input"
            v-model="content"
            placeholder="说点什么吧"
            @keyup.enter="addToList"
            @focus="show = true"
          >
          <button class="message-send" @click="addToList" v-show="show">发送</button>
        </div>
      </div>
      <!-- 弹幕列表 -->
      <div class="barrage-container">
        <vue-danmaku
        class="danmaku"
          :danmus="barrageList"
          style="height: 100%; width: 100%"
          :speeds="150"
          :channels="15"
        >
          <!-- vue-danmaku 2.x 取消了 useSlot 开关，插槽名也从 dm 改成了 danmu；名字不对会静默不渲染并报 NO_DANMU_SLOT -->
          <template #danmu="{ danmu }">
            <span class="barrage-items">
              <img
                :src="danmu.avatar"
                width="30"
                height="30"
                style="border-radius: 50%"
              />
              {{ danmu.nickname }}:{{ danmu.content }}</span
            >
          </template>
        </vue-danmaku>
      </div>
    </div>
  </div>
</template>

<script>
import { getMessagesApi, addMessageApi } from "@/api/message";
import VueDanmaku from 'vue-danmaku';
export default {
  components: {
    VueDanmaku
  },

  data() {
    return {
      show: false,
      content: "",
      count: null,
      timer: null,
      barrageList: [],
      user: this.$store.state.userInfo,
    };
  },
  mounted() {
    this.listMessage();
  },
  methods: {
    /**
     * 添加留言
     */
    addToList() {
      if (this.count) {
        this.$message.error("30秒后才能再次留言");
        return false;
      }
      if (this.content.trim() === "") {

        this.$message.error("留言不能为空");
        return false;
      }
      var message = {
        avatar: this.user ? this.user.avatar : this.$store.state.webSiteInfo.touristAvatar,
        status: 1,
        nickname: this.user ? this.user.nickname : "游客",
        content: this.content
      };

      this.content = "";
      addMessageApi(message).then(res => {
        this.barrageList.push(message);
        this.$message.success("留言成功");
      }).catch(err => {
        this.$message.error("留言失败");
      });
      const TIME_COUNT = 30;
      if (!this.timer) {
        this.count = TIME_COUNT;
        this.timer = setInterval(() => {
          if (this.count > 0 && this.count <= 30) {
            this.count--;
          } else {
            clearInterval(this.timer);
            this.timer = null;
          }
        }, 1000);
      }
    },
    /**
     * 获取留言列表
     */
    listMessage() {
      getMessagesApi().then(res => {
        this.barrageList = res.data;
      });
    }
  },
  computed: {
    cover() {
      var cover = "https://img.shiyit.com/1642481294001.png";
      return "background: url(" + cover + ") center center / cover no-repeat";
    }
  }
};
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  留言板 · 技术台账
//
//  全屏留言区：标题走衬线左对齐，输入是一条细线输入 + 近直角发送键。
//
//  弹幕条目是本页唯一的例外：全站台账取向用近直角，但留言是「人」的
//  痕迹而非数据行，用方框会读成表格单元格。这里改用胶囊（999px）+
//  玻璃底 + 高光描边，让它读起来像一枚会飘的按钮。
// ════════════════════════════════════════════════════════════════

.message-banner {
  position: relative;
  min-height: calc(100vh - 64px);
  overflow: hidden;
  background-color: #0b0e12;

  // 背景图之上压一层暗罩，保证白字可读
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      to bottom,
      rgba(5, 7, 10, 0.72) 0%,
      rgba(5, 7, 10, 0.52) 45%,
      rgba(5, 7, 10, 0.82) 100%
    );
    pointer-events: none;
  }
}

.message-container {
  position: relative;
  z-index: 5;
  max-width: 1400px;
  margin: 0 auto;
  padding: clamp(72px, 16vh, 160px) clamp(20px, 4vw, 48px) 0;
  color: #fff;

  .message-title {
    font-family: $font-serif;
    font-size: clamp(1.8rem, 4vw, 2.6rem);
    font-weight: 600;
    color: #fff;
    margin: 0;
  }

  .message-input-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
    max-width: 520px;
    margin-top: 28px;

    .message-input {
      flex: 1;
      min-width: 0;
      padding: 12px 20px;
      border: 1px solid rgba(255, 255, 255, 0.28);
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.06);
      color: #fff;
      font-size: 0.95rem;
      transition: border-color 0.2s ease, background 0.2s ease;

      &::placeholder {
        color: rgba(255, 255, 255, 0.5);
      }

      &:focus {
        outline: none;
        border-color: rgba(255, 255, 255, 0.6);
        background: rgba(255, 255, 255, 0.1);
      }
    }

    .message-send {
      flex: none;
      padding: 12px 26px;
      border: 1px solid transparent;
      border-radius: 999px;
      background: var(--accent);
      color: #fff;
      font-size: 0.9rem;
      font-weight: 500;
      letter-spacing: 0.02em;
      cursor: pointer;
      box-shadow: 0 2px 10px rgba(0, 0, 0, 0.24),
        inset 0 1px 0 rgba(255, 255, 255, 0.22);
      transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1),
        box-shadow 0.22s ease, filter 0.22s ease;

      &:hover {
        transform: translateY(-1px);
        filter: brightness(1.08);
        box-shadow: 0 6px 18px rgba(0, 0, 0, 0.3),
          inset 0 1px 0 rgba(255, 255, 255, 0.28);
      }

      &:active {
        transform: translateY(0);
        filter: brightness(0.96);
      }
    }
  }
}

.barrage-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  z-index: 3;

  .barrage-items {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 5px 16px 5px 5px;
    margin-top: 10px;
    border-radius: 999px;
    color: #fff;
    font-size: 0.85rem;
    line-height: 1.4;
    white-space: nowrap;

    // 玻璃底 + 顶部高光：让胶囊在任意背景图上都有「浮起来」的实体感
    background: linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.16) 0%,
      rgba(255, 255, 255, 0.06) 100%
    );
    border: 1px solid rgba(255, 255, 255, 0.22);
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.28),
      inset 0 1px 0 rgba(255, 255, 255, 0.18);
    backdrop-filter: blur(10px) saturate(1.4);
    -webkit-backdrop-filter: blur(10px) saturate(1.4);
    transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1),
      border-color 0.22s ease, box-shadow 0.22s ease;

    &:hover {
      transform: translateY(-2px);
      border-color: rgba(255, 255, 255, 0.42);
      box-shadow: 0 6px 18px rgba(0, 0, 0, 0.34),
        inset 0 1px 0 rgba(255, 255, 255, 0.24);
    }

    img {
      flex: none;
      border-radius: 50%;
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.35);
    }
  }
}

@include responsive(md) {
  .message-container {
    padding-top: 96px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .message-input,
  .message-send,
  .barrage-items {
    transition: none !important;
  }

  .message-send:hover,
  .barrage-items:hover {
    transform: none;
  }
}
</style>