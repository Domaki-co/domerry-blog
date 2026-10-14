<template>
  <div class="friends-page">
    <div class="friends-inner">
      <header class="page-header">
        <div class="head-title">
          <h1>友链</h1>
          <span class="head-meta">{{ friends.length }} 个伙伴</span>
        </div>
        <p class="head-sub">与优秀的人同行，分享技术与生活</p>
        <div class="site-info">
          <div class="site-avatar">
            <div class="avatar-wrapper">
              <el-avatar class="avatar" :src="$store.state.webSiteInfo.logo" />
              <div class="copy-overlay" @click="copyLogoUrl">
                <i class="fas fa-copy"></i>
                <span>复制图片链接</span>
                <input type="text" :value="$store.state.webSiteInfo.logo" readonly ref="logoInput" style="position: absolute; opacity: 0;">
              </div>
            </div>
          </div>
          <div class="site-details">
            <h2>{{ $store.state.webSiteInfo.name }}</h2>
            <p>{{ $store.state.webSiteInfo.summary }}</p>
            <div class="site-url">
              <i class="fas fa-link"></i>
              <input type="text" :value="$store.state.webSiteInfo.webUrl" readonly ref="urlInput" @click="copyUrl">
              <button class="copy-btn" @click="copyUrl">
                <i class="fas fa-copy"></i>
              </button>
            </div>
          </div>
        </div>
        <div class="copy-tip">
          <i class="fas fa-info-circle"></i>
          申请友链前请先添加本站链接
          <span class="tip-highlight">「 点击上方链接可快速复制 」</span>
        </div>
        <button class="apply-btn" @click="handleApply">
          <i class="fas fa-plus"></i>
          申请友链
        </button>
      </header>

      <div class="friends-container">
        <div class="section-title">
          <h2>友链列表</h2>
        </div>
        <div class="friends-grid">
          <div v-for="friend in friends" :key="friend.id" class="friend-card" @click="visitFriend(friend.url)">
            <div class="friend-avatar">
              <img v-lazy="friend.avatar" :key="friend.avatar" :alt="friend.name">
              <div class="status" :class="{ online: friend.online }"></div>
            </div>
            <div class="friend-info">
              <h3>{{ friend.name }}</h3>
              <p>{{ friend.info }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 申请表单弹窗 -->
      <el-dialog
        v-model="showApplyForm"
        title="申请友链"
        width="500px"
        top="3vh"
        :append-to-body="true"
      >
        <div class="apply-form">
          <el-form
            size="small"
            :model="form"
            :rules="rules"
            ref="ruleForm"
            label-width="100px"
          >
          <div class="form-group">
              <el-form-item prop="name">
                <template v-slot:label>
                  <i class="fas fa-signature"></i> 网站名称
                </template>
              </el-form-item>
              <el-input type="text" v-model="form.name" placeholder="请输入您的网站名称" />
            </div>
            <div class="form-group">
              <el-form-item prop="url">
                <template v-slot:label>
                  <i class="fas fa-link"></i> 网站链接
                </template>
              </el-form-item>
              <el-input type="url" v-model="form.url" placeholder="请输入您的网站链接" />
            </div>
            <div class="form-group">
              <el-form-item prop="info">
                <template v-slot:label>
                  <i class="fas fa-quote-left"></i> 网站描述
                </template>
              </el-form-item>
              <el-input type="url" v-model="form.info" placeholder="一句话描述您的网站" />
            </div>
            <div class="form-group">
              <el-form-item prop="avatar">
                <template v-slot:label>
                  <i class="fas fa-image"></i> 头像链接
                </template>
              </el-form-item>
              <el-input type="url" v-model="form.avatar" placeholder="请输入您的头像链接" />
            </div>
            <div class="form-group">
              <el-form-item prop="email">
                <template v-slot:label>
                  <i class="fas fa-envelope"></i> 联系邮箱
                </template>
              </el-form-item>
              <el-input type="url" v-model="form.email" placeholder="邮箱用于联系" />
            </div>


            <div class="form-footer">
              <el-button class="submit-btn" type="primary" @click="submitApplication">
                <i class="fas fa-paper-plane"></i>
                提交申请
              </el-button>
            </div>
          </el-form>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import { getFriendsApi, applyFriendApi } from '@/api/friends'

export default {
  name: 'Friends',

  data() {
    return {
      showApplyForm: false,
      form: {
        name: '',
        url: '',
        info: '',
        avatar: '',
        email: ''
      },
      friends: [],
      rules: {
        name: [
          { required: true, message: '请输入网站名称', trigger: 'blur' }
        ],
        url: [
          { required: true, message: '请输入网站链接', trigger: 'blur' },
          { type: 'url', message: '请输入正确的链接格式', trigger: 'blur' }
        ],
        info: [
          { required: true, message: '请输入网站描述', trigger: 'blur' }
        ],
        avatar: [
          { required: true, message: '请输入头像链接', trigger: 'blur' },
          { type: 'url', message: '请输入正确的链接格式', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入联系邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ]
      }
    }
  },
  async created() {
    const res = await getFriendsApi()
    this.friends = res.data
  },
  methods: {
    visitFriend(url) {
      window.open(url, '_blank')
    },
    handleApply() {
      this.form = {
        name: '',
        url: '',
        info: '',
        avatar: '',
        email: ''
      },
        this.showApplyForm = true
    },
    handleClose() {
      this.showApplyForm = false
    },
    submitApplication() {
      this.$refs['ruleForm'].validate((valid) => {
        if (valid) {
          try {
            const res = applyFriendApi(this.form)
            this.showApplyForm = false
            this.$message.success('申请已提交，请等待审核')
            this.$refs['ruleForm'].resetFields();
          } catch (error) {
            this.$message.error(error.message)
          }
        } else {
          console.log('error submit!!')
        }
      })

    },
    copyLogoUrl() {
      const input = this.$refs.logoInput
      input.select()
      document.execCommand('copy')
      this.$message.success('Logo链接已复制到剪贴板')
    },
    copyUrl() {
      const input = this.$refs.urlInput
      input.select()
      document.execCommand('copy')
      this.$message.success('链接已复制到剪贴板')
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  友链 · 技术台账
//
//  本站信息是一块「名片」：站点 URL 用等宽字，可点即复制。
//  友链列表是行式台账（头像 | 名称+描述），不用上浮卡片和脉冲动效。
// ════════════════════════════════════════════════════════════════

:deep(.el-form-item__content) {
  margin-left: 0 !important;
}

.friends-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: clamp(24px, 4vh, 44px) clamp(20px, 4vw, 48px);
  min-height: calc(100vh - 200px);

  @include responsive(lg) {
    padding: $spacing-md;
  }
}

// ── 页头 ──
.page-header {
  margin-bottom: clamp(32px, 5vh, 52px);

  .head-title {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: $spacing-md;
    padding-bottom: 16px;
    border-bottom: 1px solid var(--rule-strong);

    h1 {
      font-family: $font-serif;
      font-size: clamp(1.6rem, 3vw, 2.1rem);
      font-weight: 600;
      color: var(--text-primary);
      margin: 0;
    }

    .head-meta {
      @include ledger-meta;
    }
  }

  .head-sub {
    color: var(--text-secondary);
    font-size: 0.95rem;
    margin: 16px 0 0;
  }
}

// ── 本站信息名片 ──
.site-info {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 24px;
  padding: 20px;
  border: 1px solid var(--rule);
  border-radius: $border-radius-md;

  @include responsive(sm) {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .avatar-wrapper {
    position: relative;
    width: 64px;
    height: 64px;
    flex: none;
    cursor: pointer;

    .avatar {
      width: 64px;
      height: 64px;
      border-radius: $border-radius-sm;
      border: 1px solid var(--rule);
    }

    .copy-overlay {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 2px;
      background: rgba(18, 23, 29, 0.72);
      color: #fff;
      font-size: 0.6rem;
      border-radius: $border-radius-sm;
      opacity: 0;
      transition: opacity 0.2s ease;

      i {
        font-size: 0.9rem;
      }
    }

    &:hover .copy-overlay {
      opacity: 1;
    }
  }

  .site-details {
    flex: 1;
    min-width: 0;

    h2 {
      font-family: $font-serif;
      font-size: 1.25rem;
      font-weight: 600;
      color: var(--text-primary);
      margin: 0 0 4px;
    }

    p {
      color: var(--text-secondary);
      font-size: 0.9rem;
      line-height: 1.6;
      margin: 0 0 12px;
    }

    .site-url {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 12px;
      border: 1px solid var(--rule);
      border-radius: $border-radius-sm;
      transition: border-color 0.2s ease;

      &:hover {
        border-color: var(--accent);
      }

      i {
        font-size: 0.85rem;
        color: var(--accent);
      }

      input {
        flex: 1;
        min-width: 0;
        background: none;
        border: none;
        font-family: $font-mono;
        font-size: 0.82rem;
        color: var(--text-primary);
        cursor: pointer;

        &:focus {
          outline: none;
        }
      }

      .copy-btn {
        background: none;
        border: none;
        color: var(--text-tertiary);
        cursor: pointer;
        padding: 2px 4px;
        transition: color 0.2s ease;

        &:hover {
          color: var(--accent);
        }
      }
    }
  }
}

.copy-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 0;
  font-size: 0.85rem;
  color: var(--text-secondary);

  i {
    color: var(--text-tertiary);
  }

  .tip-highlight {
    color: var(--text-tertiary);
    font-family: $font-mono;
    font-size: 0.78rem;
  }
}

.apply-btn {
  @include ledger-button;
  margin-top: 20px;

  i {
    font-size: 0.85rem;
  }
}

// ── 友链列表 ──
.friends-container {
  margin-top: clamp(36px, 5vh, 56px);
}

.section-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--rule-strong);
  margin-bottom: 2px;

  h2 {
    font-family: $font-serif;
    font-size: 1.15rem;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
    flex: none;
  }

  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: var(--rule);
    transform: translateY(-3px);
  }
}

.friends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 0 clamp(24px, 4vw, 56px);
}

.friend-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 2px;
  border-bottom: 1px solid var(--rule);
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: var(--row-hover);

    h3 {
      color: var(--accent);
    }
  }

  .friend-avatar {
    position: relative;
    width: 44px;
    height: 44px;
    flex: none;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      border-radius: $border-radius-sm;
      border: 1px solid var(--rule);
      filter: saturate(0.92);
    }

    .status {
      position: absolute;
      right: -2px;
      bottom: -2px;
      width: 9px;
      height: 9px;
      border-radius: 50%;
      background: var(--text-tertiary);
      border: 2px solid var(--surface);

      &.online {
        background: var(--accent);
      }
    }
  }

  .friend-info {
    min-width: 0;

    h3 {
      font-size: 0.98rem;
      font-weight: 500;
      color: var(--text-primary);
      margin: 0 0 3px;
      transition: color 0.2s ease;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    p {
      color: var(--text-secondary);
      font-size: 0.84rem;
      line-height: 1.5;
      margin: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

// ── 申请表单 ──
.apply-form {
  .el-form-item {
    margin-bottom: $spacing-md;

    &__label {
      font-weight: 500;
      color: var(--text-primary);
    }
  }

  .el-input .el-input__inner {
    border-radius: $border-radius-sm;
    border: 1px solid var(--rule);
    background: var(--input-bg);
    color: var(--text-primary);

    &:focus {
      border-color: var(--accent);
    }

    &::placeholder {
      color: var(--text-tertiary);
    }
  }

  .form-footer {
    display: flex;
    justify-content: flex-end;
    margin-top: $spacing-lg;

    .submit-btn {
      @include ledger-button;
      height: auto;
      padding: 9px 22px;

      i {
        margin-right: 2px;
      }
    }
  }
}

.form-group {
  margin-bottom: $spacing-md;
}

@media (prefers-reduced-motion: reduce) {
  .friend-card,
  .friend-card h3,
  .site-info .site-url,
  .copy-overlay {
    transition: none !important;
  }
}
</style>
