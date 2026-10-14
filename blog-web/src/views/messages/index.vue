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
//  全屏留言区：标题走衬线左对齐，输入是一条细线输入 + 近直角发送键；
//  弹幕条目用近直角墨底，不用胶囊圆角。
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
      padding: 12px 14px;
      border: 1px solid rgba(255, 255, 255, 0.28);
      border-radius: $border-radius-sm;
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
      padding: 12px 22px;
      border: 1px solid var(--accent);
      border-radius: $border-radius-sm;
      background: var(--accent);
      color: #fff;
      font-size: 0.9rem;
      cursor: pointer;
      transition: background 0.2s ease, color 0.2s ease;

      &:hover {
        background: transparent;
        color: var(--accent);
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
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 10px 4px 4px;
    margin-top: 10px;
    background: rgba(5, 7, 10, 0.72);
    border: 1px solid rgba(255, 255, 255, 0.14);
    border-radius: $border-radius-sm;
    color: #fff;
    font-size: 0.85rem;

    img {
      border-radius: $border-radius-sm;
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
  .message-send {
    transition: none !important;
  }
}
</style>