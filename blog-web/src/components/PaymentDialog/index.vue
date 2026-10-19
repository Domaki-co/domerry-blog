<template>
  <el-dialog
    title="支付"
    v-model="dialogVisible"
    width="500px"
    :close-on-click-modal="false"
    class="payment-dialog"
    @close="handleClose"
  >
    <div class="payment-content">
      <!-- 商品信息 -->
      <div class="product-info">
        <h3>{{ title }}</h3>
        <div class="price">￥{{ price }}</div>
      </div>

      <!-- 支付方式选择 -->
      <div class="payment-methods">
        <div class="method-title">选择支付方式</div>
        <div class="method-list">
          <div
            class="method-item"
            :class="{ active: paymentMethod === 'alipay' }"
            @click="paymentMethod = 'alipay'"
          >
            <i class="fab fa-alipay method-icon"></i>
            <span class="method-name">支付宝</span>
            <i class="fas fa-check-circle check-icon"></i>
          </div>
          <div
            class="method-item"
            :class="{ active: paymentMethod === 'wechat' }"
            @click="paymentMethod = 'wechat'"
          >
            <i class="fab fa-weixin method-icon"></i>
            <span class="method-name">微信支付</span>
            <i class="fas fa-check-circle check-icon"></i>
          </div>
        </div>
      </div>

      <!-- 支付二维码区域 -->
      <div class="qr-section" v-loading="qrLoading">
        <template v-if="showQRCode">
          <div class="qr-wrapper">
            <div class="qr-code">
              <img :src="qrCodeUrl" alt="支付二维码">
            </div>
            <div class="qr-tip">
              <i class="fas" :class="paymentMethod === 'alipay' ? 'fa-alipay' : 'fa-weixin'"></i>
              <span>{{ paymentMethod === 'alipay' ? '请使用支付宝扫码支付' : '请使用微信扫码支付' }}</span>
            </div>
          </div>
          <div class="payment-status">
            <div class="amount">支付金额：<span>￥{{ price }}</span></div>
            <div class="order-no">订单号：{{ orderNo }}</div>
          </div>
        </template>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
      <template v-if="!showQRCode">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handlePay" :loading="loading">
          立即支付
        </el-button>
      </template>
      <template v-else>
        <el-button @click="handleClose">取消支付</el-button>
        <el-button type="primary" @click="checkPaymentStatus()">
          已完成支付
        </el-button>
      </template>
    </div>
    </template>
  </el-dialog>
</template>

<script>
import { createOrder, checkOrderStatus } from '@/api/payment'

// 轮询配置：最多 20 次，间隔 3s 起按 1.5 倍退避、上限 15s（总计约 4.5 分钟）
const CHECK_BASE_INTERVAL = 3000
const CHECK_MAX_INTERVAL = 15000
const CHECK_MAX_ATTEMPTS = 20

export default {
  name: 'PaymentDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      required: true
    },
    price: {
      type: Number,
      required: true
    },
    articleId: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      paymentMethod: 'alipay',
      loading: false,
      qrLoading: false,
      showQRCode: false,
      qrCodeUrl: this.paymentMethod === 'alipay' ? this.$store.state.webSiteInfo.aliPay : this.$store.state.webSiteInfo.weixinPay,
      orderNo: '',
      checkTimer: null,
      checkAttempts: 0,
      checkStopped: false,
      paymentHandled: false
    }
  },
  computed: {
    // Vue 3 的 props 是只读的：内层 el-dialog 用 v-model 关闭时必须回传事件，不能直接写 prop
    dialogVisible: {
      get() {
        return this.modelValue
      },
      set(val) {
        this.$emit('update:modelValue', val)
      }
    }
  },
  methods: {
    async handlePay() {
      this.loading = true
      this.qrLoading = true
      try {
        // 创建订单
        const res = await createOrder({
          articleId: this.articleId,
          paymentMethod: this.paymentMethod,
          amount: this.price
        })
        
        this.orderNo = res.data.orderNo
        this.qrCodeUrl = res.data.qrCodeUrl
        this.showQRCode = true
        
        // 开始轮询支付状态
        this.startCheckingPayment()
      } catch (error) {
        this.$message.error('创建订单失败，请重试')
      } finally {
        this.loading = false
        this.qrLoading = false
      }
    },
    startCheckingPayment() {
      this.checkAttempts = 0
      this.checkStopped = false
      this.paymentHandled = false
      this.scheduleNextCheck()
    },
    // 自调度 setTimeout 链：上一次请求返回后才排下一次，天然避免 setInterval 的重叠
    scheduleNextCheck() {
      if (this.checkTimer) {
        clearTimeout(this.checkTimer)
      }
      const delay = Math.min(CHECK_BASE_INTERVAL * Math.pow(1.5, this.checkAttempts), CHECK_MAX_INTERVAL)
      this.checkTimer = setTimeout(async () => {
        this.checkAttempts++
        // 成功处理由 checkPaymentStatus 独占，这里只决定是否继续轮询
        const status = await this.checkPaymentStatus(false)
        if (status === 'SUCCESS' || this.checkStopped) return
        if (this.checkAttempts >= CHECK_MAX_ATTEMPTS) {
          this.$message.warning('未检测到支付结果，请稍后在订单页确认')
          return
        }
        this.scheduleNextCheck()
      }, delay)
    },
    // 停止轮询：置位 checkStopped，让飞行中的那次请求返回后也不再续排
    clearCheckTimer() {
      this.checkStopped = true
      if (this.checkTimer) {
        clearTimeout(this.checkTimer)
        this.checkTimer = null
      }
    },
    async checkPaymentStatus(showMessage = true) {
      try {
        const res = await checkOrderStatus(this.orderNo)
        if (res.data.status === 'SUCCESS') {
          this.handlePaymentSuccess()
          return 'SUCCESS'
        } else if (showMessage) {
          this.$message.warning('未检测到支付完成，请确认支付状态')
        }
        return res.data.status
      } catch (error) {
        if (showMessage) {
          this.$message.error('检查支付状态失败，请刷新页面重试')
        }
        return 'ERROR'
      }
    },
    handlePaymentSuccess() {
      // 轮询与「已完成支付」按钮可能同时拿到 SUCCESS，这里保证只处理一次
      if (this.paymentHandled) return
      this.paymentHandled = true
      this.clearCheckTimer()
      this.$message.success('支付成功')
      this.$emit('payment-success')
      this.handleClose()
    },
    handleClose() {
      this.clearCheckTimer()
      this.showQRCode = false
      this.qrCodeUrl = ''
      this.orderNo = ''
      this.$emit('update:modelValue', false)
    }
  },
  beforeUnmount() {
    this.clearCheckTimer()
  }
}
</script>

<style lang="scss" scoped>
.payment-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.payment-content {
  padding: $spacing-lg;
}

.product-info {
  text-align: center;
  margin-bottom: $spacing-xl;
  padding-bottom: $spacing-lg;
  border-bottom: 1px solid var(--border-color);

  h3 {
    color: var(--text-primary);
    font-size: 1.2em;
    margin-bottom: $spacing-md;
  }

  .price {
    color: #E6162D;
    font-size: 1.8em;
    font-weight: bold;
  }
}

.payment-methods {
  margin-bottom: $spacing-xl;

  .method-title {
    font-size: 1.1em;
    color: var(--text-primary);
    margin-bottom: $spacing-md;
  }

  .method-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: $spacing-md;
  }

  .method-item {
    position: relative;
    padding: $spacing-md;
    border: 2px solid var(--border-color);
    border-radius: $border-radius-lg;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: $spacing-md;
    transition: all 0.3s ease;

    .method-icon {
      font-size: 24px;
      color: var(--text-primary);
      
      &.fa-alipay {
        color: #1677FF;
      }
      
      &.fa-weixin {
        color: #07C160;
      }
    }

    .method-name {
      color: var(--text-primary);
      font-size: 1.1em;
    }

    .check-icon {
      position: absolute;
      right: $spacing-md;
      color: $primary;
      opacity: 0;
      transform: scale(0.8);
      transition: all 0.3s ease;
    }

    &:hover {
      border-color: rgba($primary, 0.3);
      background: rgba($primary, 0.02);
    }

    &.active {
      border-color: $primary;
      background: rgba($primary, 0.05);

      .check-icon {
        opacity: 1;
        transform: scale(1);
      }
    }
  }
}

.qr-section {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $spacing-xl;
  padding: $spacing-lg;
  background: var(--hover-bg);
  border-radius: $border-radius-lg;

  .qr-wrapper {
    text-align: center;

    .qr-code {
      width: 200px;
      height: 200px;
      margin: 0 auto $spacing-md;
      padding: $spacing-md;
      background: white;
      border-radius: $border-radius-md;
      box-shadow: $shadow-md;

      img {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }
    }

    .qr-tip {
      color: var(--text-secondary);
      display: flex;
      align-items: center;
      justify-content: center;
      gap: $spacing-sm;

      i {
        font-size: 1.2em;
        
        &.fa-alipay {
          color: #1677FF;
        }
        
        &.fa-weixin {
          color: #07C160;
        }
      }
    }
  }

  .payment-status {
    text-align: center;
    color: var(--text-secondary);
    font-size: 0.9em;

    .amount {
      margin-bottom: $spacing-sm;
      
      span {
        color: #E6162D;
        font-weight: bold;
        font-size: 1.2em;
      }
    }

    .order-no {
      opacity: 0.8;
    }
  }
}
</style> 