<template>
  <div>
    <div class="login-container">
      <!-- 登录表单 -->
      <div class="login-body">
        <el-tooltip
          class="item"
          effect="dark"
          content="回到首页"
          placement="top"
        >
          <button class="back-btn" @click="backToHome">
            <el-icon><Back /></el-icon>
          </button>
        </el-tooltip>
        <el-tooltip
          class="item"
          effect="dark"
          :content="currentForm === 'login' ? '账号密码登录' : '扫码登录'"
          placement="top"
        >
          <button class="switch-form-btn" @click="handleSwitchForm">
            <i
              :class="
                currentForm === 'login' ? 'fas fa-user' : 'fas fa-qrcode'
              "
            ></i>
          </button>
        </el-tooltip>
        <!-- 微信扫码登录 -->
        <div v-show="currentForm === 'login'" class="form-container">
          <div class="qrcode-content">
            <div class="qrcode-box">
              <!-- 这里放二维码图片 -->
              <img
                v-lazy="'/static/img/wechat.jpg'"
                :key="'/static/img/wechat.jpg'"
                alt="微信二维码"
              />
            </div>
            <p class="qrcode-tip">
              登录验证码：
              <span class="code-text">{{ wechatForm.code }}</span>
              <span class="code-text" v-if="wechatForm.code === '验证码已失效'">
                <i class="fas fa-sync-alt" @click="getWechatLoginCode"></i>
              </span>
            </p>
            <p class="qrcode-tip">微信扫码关注公众号，并发送验证码</p>
          </div>

          <div class="divider">
            <el-divider>其他登录方式</el-divider>
          </div>

          <div class="third-party-login">
            <div
              v-for="(item, type) in thirdPartyLoginTypes"
              :key="type"
              class="login-icon-wrapper"
              @click="handleThirdPartyLogin(type)"
            >
              <el-tooltip :content="item.title" placement="top">
                <div :class="['login-icon', type]">
                  <i :class="item.icon"></i>
                </div>
              </el-tooltip>
            </div>
          </div>
        </div>

        <!-- 账号密码登录表单 -->
        <div v-show="currentForm === 'account'" class="form-container">
          <div class="form-header">
            <h2 class="form-title">账号密码登录</h2>
            <p class="form-subtitle">欢迎回来,请输入您的账号</p>
          </div>

          <el-form :model="loginForm" :rules="rules" ref="ruleFrom">
            <el-form-item class="form-item" prop="username">
              <el-input
                prefix-icon="UserFilled"
                v-model="loginForm.username"
                placeholder="请输入用户名"
                @keyup.enter="handleLogin"
                size="large"
              />
            </el-form-item>

            <el-form-item class="form-item" prop="password">
              <el-input
                prefix-icon="Lock"
                v-model="loginForm.password"
                placeholder="请输入密码"
                @keyup.enter="handleLogin"
                show-password
                size="large"
              />
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            </div>

            <el-form-item class="form-item">
              <el-button
                class="submit-btn ripple"
                :loading="loading"
                @click="handleLogin"
                type="primary"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-switch">
            <a @click="switchForm('register')">立即注册</a>
            <span class="divider-line">|</span>
            <a @click="switchForm('forgot')">忘记密码?</a>
          </div>
        </div>

        <!-- 注册表单 -->
        <div v-show="currentForm === 'register'" class="form-container">
          <div class="form-header">
            <h2 class="form-title">注册账号</h2>
            <p class="form-subtitle">欢迎注册,请输入您的账号</p>
          </div>
          <el-form :model="registerForm" :rules="rules" ref="registerForm">
            <el-form-item class="form-item" prop="nickname">
              <el-input
                prefix-icon="UserFilled"
                v-model="registerForm.nickname"
                placeholder="请输入昵称"
                size="large"
              />
            </el-form-item>

            <el-form-item class="form-item" prop="email">
              <el-input
                prefix-icon="Message"
                v-model="registerForm.email"
                placeholder="请输入邮箱"
                size="large"
              />
            </el-form-item>

            <el-form-item class="form-item" prop="code">
              <el-input
                prefix-icon="Key"
                v-model="registerForm.code"
                placeholder="请输入验证码"
                size="large"
              >
                <template #append>
                  <el-button @click="sendRegisterCode" :disabled="codeSending">
                    {{ codeButtonText }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item class="form-item" prop="password">
              <el-input
                prefix-icon="Lock"
                v-model="registerForm.password"
                placeholder="请输入密码"
                show-password
                size="large"
              />
            </el-form-item>

            <el-form-item class="form-item">
              <el-button
                class="submit-btn"
                :loading="loading"
                @click="handleRegister"
              >
                注 册
              </el-button>
            </el-form-item>

            <div class="form-switch">
              已有账号？<a @click="switchForm('account')">立即登录</a>
            </div>
          </el-form>
        </div>

        <!-- 忘记密码表单 -->
        <div v-show="currentForm === 'forgot'" class="form-container">
          <div class="form-header">
            <h2 class="form-title">找回账号</h2>
            <p class="form-subtitle">重置密码,请输入您的邮箱</p>
          </div>
          <el-form :model="forgotForm" :rules="rules" ref="forgotForm">
            <el-form-item class="form-item" prop="email">
              <el-input
                prefix-icon="Message"
                v-model="forgotForm.email"
                placeholder="请输入注册邮箱"
                size="large"
              />
            </el-form-item>

            <el-form-item class="form-item" prop="code">
              <el-input
                prefix-icon="Key"
                v-model="forgotForm.code"
                placeholder="请输入验证码"
                size="large"
              >
                <template #append>
                  <el-button
                    @click="sendVerificationCode"
                    :disabled="codeSending"
                  >
                    {{ codeButtonText }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item class="form-item" prop="password">
              <el-input
                prefix-icon="Lock"
                v-model="forgotForm.password"
                placeholder="请输入新密码"
                show-password
                size="large"
              />
            </el-form-item>

            <el-form-item class="form-item">
              <el-button
                class="submit-btn"
                :loading="loading"
                @click="handleResetPassword"
              >
                重置密码
              </el-button>
            </el-form-item>

            <div class="form-switch">
              <a @click="switchForm('account')">返回登录</a>
            </div>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 滑块验证 -->
    <el-dialog
      title="请拖动滑块完成拼图"
      width="360px"
      v-model="isShowSliderVerify"
      :close-on-click-modal="false"
      @close="refresh"
      append-to-body
    >
      <slider-verify
        ref="sliderVerify"
        @success="onSuccess"
        @fail="onFail"
        @again="onAgain"
      />
    </el-dialog>
  </div>
</template>

<script>
import { disableScroll, enableScroll } from "@/utils/scroll";
import {
  sendEmailCodeApi,
  registerApi,
  forgotPasswordApi,
  getWechatLoginCodeApi,
  getWechatIsLoginApi,
  getAuthRenderApi,
  getCaptchaSwitchApi,
} from "@/api/auth";
import { setCookie } from "@/utils/cookie";
import SliderVerify from "./components/SliderVerify.vue";

// 「记住我」的存储键：本实现只记住用户名，不改变 token 有效期
// （token 仍是 7 天 cookie，由 store 的 SET_TOKEN 决定）
const REMEMBERED_USERNAME_KEY = "remembered_username";

export default {
  name: "Login",
  components: {
    SliderVerify,
  },
  data() {
    return {
      currentForm: "login",
      loading: false,
      wechatForm: {
        code: "",
        showQrcode: false,
      },
      countdown: 0,
      loginForm: {
        username: "",
        password: "",
        source: "PC",
      },
      registerForm: {
        nickname: "",
        email: "",
        password: "",
        code: "",
      },
      forgotForm: {
        email: "",
        code: "",
        password: "",
      },
      loginTypes: {
        github: {
          title: "GitHub账号登录",
          icon: "fab fa-github",
        },
        qq: {
          title: "QQ账号登录",
          icon: "fab fa-qq",
        },
        wechat: {
          title: "微信扫码登录",
          icon: "fab fa-weixin",
        },
        gitee: {
          title: "码云账号登录",
          icon: "fab fa-git-alt",
        },
        weibo: {
          title: "微博账号登录",
          icon: "fab fa-weibo",
        },
      },
      codeSending: false,
      codeButtonText: "发送验证码",
      // 微信倒计时与邮箱倒计时各自持有句柄：共用 codeTimer 时，任一方的清理都会误杀另一方
      codeTimer: null,
      emailCodeTimer: null,
      pollingTimer: null,
      // 微信轮询已执行次数，达到上限即停止，避免用户始终不扫码时无限轮询
      wechatPollingCount: 0,
      isShowSliderVerify: false,
      sliderVerify: null,
      rules: {
        nickname: [
          { required: true, message: "请输入昵称", trigger: "blur" },
          {
            min: 3,
            max: 10,
            message: "长度在 3 到 10 个字符",
            trigger: "blur",
          },
        ],
        username: [
          { required: true, message: "请输入用户名", trigger: "blur" },
          {
            min: 3,
            max: 50,
            message: "长度在 3 到 50 个字符",
            trigger: "blur",
          },
        ],
        email: [
          { required: true, message: "请输入邮箱", trigger: "blur" },
          { type: "email", message: "请输入正确的邮箱", trigger: "blur" },
        ],
        password: [
          { required: true, message: "请输入密码", trigger: "blur" },
          {
            min: 6,
            max: 16,
            message: "长度在 6 到 16 个字符",
            trigger: "blur",
          },
        ],
        code: [{ required: true, message: "请输入验证码", trigger: "blur" }],
      },
      rememberMe: false,
    };
  },

  computed: {
    // 模板里 v-for 与 v-if 同元素时，v-if 拿到的是 _ctx.type（undefined），条件恒真、
    // 过滤从未生效；改到这里过滤，且必须基于 created() 已按后端 loginTypeList 删过键的
    // loginTypes 求值，否则会把后端没开放的登录方式又渲染出来
    thirdPartyLoginTypes() {
      const types = {};
      Object.keys(this.loginTypes).forEach((key) => {
        if (key !== "wechat") {
          types[key] = this.loginTypes[key];
        }
      });
      return types;
    },
  },

  created() {
    Object.keys(this.loginTypes).forEach((key) => {
      if (!this.$store.state.webSiteInfo?.loginTypeList?.includes(key)) {
        delete this.loginTypes[key];
      }
    });
    // 有存档就预填用户名并勾上复选框：本实现只记住用户名，不改变 token 有效期
    const rememberedUsername = localStorage.getItem(REMEMBERED_USERNAME_KEY);
    if (rememberedUsername) {
      this.loginForm.username = rememberedUsername;
      this.rememberMe = true;
    }
    this.getWechatLoginCode();
    this.$nextTick(() => {
      disableScroll();
    });
  },
  methods: {
    /**
     * 滑块验证成功
     */
    async onSuccess(captcha) {
      this.loginForm.nonceStr = captcha.nonceStr;
      this.loginForm.value = captcha.value;
      this.login();
    },
    async login() {
      this.loading = true;
      try {
        await this.$store.dispatch("loginAction", this.loginForm);
        // 勾了就存用户名，没勾就清掉存档，否则用户取消勾选后存档永远删不掉
        if (this.rememberMe) {
          localStorage.setItem(REMEMBERED_USERNAME_KEY, this.loginForm.username);
        } else {
          localStorage.removeItem(REMEMBERED_USERNAME_KEY);
        }
        this.$refs.sliderVerify?.verifySuccessEvent();
        this.$message.success("登录成功");
        this.handleClose();
      } catch (error) {
        this.$message.error(error.message || "登录失败，请重试");
        this.refresh();
      } finally {
        this.loading = false;
      }
    },
    /**
     * 滑块验证失败
     */
    onFail() {
      this.$message.error("验证失败，请重试");
    },
    /**
     * 滑块验证重新开始
     */
    onAgain() {
      this.$message.error("验证失败，请重试");
    },
    /**
     * 刷新
     */
    refresh() {
      this.$refs.sliderVerify.refresh();
    },
    /**
     * 切换表单
     * @param form
     */
    switchForm(form) {
      this.currentForm = form;
      this.loading = false;
      this.clearTimer();
      if (form === "login") {
        this.getWechatLoginCode();
      }
    },
    /**
     * 登录
     */
    async handleLogin() {
      this.$refs["ruleFrom"].validate(async (valid) => {
        if (valid) {
          getCaptchaSwitchApi().then((res) => {
            if (!res.data || res.data.configValue === "Y") {
              this.isShowSliderVerify = true;
            } else {
              this.login();
            }
          });
        } else {
          return false;
        }
      });
    },
    /**
     * 注册
     */
    async handleRegister() {
      this.$refs["registerForm"].validate(async (valid) => {
        if (valid) {
          this.loading = true;
          try {
            await registerApi(this.registerForm);
            this.$message.success("注册成功");
            this.switchForm("login");
          } catch (error) {
            this.$message.error(error.message || "注册失败，请重试");
          } finally {
            this.loading = false;
          }
        } else {
          console.log("error submit!!");
          return false;
        }
      });
    },
    /**
     * 忘记密码
     */
    async handleResetPassword() {
      this.$refs["forgotForm"].validate(async (valid) => {
        if (valid) {
          this.loading = true;
          try {
            // 调用重置密码接口
            await forgotPasswordApi(this.forgotForm);
            this.$message.success("密码重置成功");
            this.switchForm("login");
          } catch (error) {
            this.$message.error(error.message || "重置失败，请重试");
          } finally {
            this.loading = false;
          }
        } else {
          console.log("error submit!!");
          return false;
        }
      });
    },
    /**
     * 发送忘记密码邮箱验证码
     */
    async sendVerificationCode() {
      if (this.codeSending) return;

      if (!this.forgotForm.email) {
        this.$message.error("请先输入邮箱");
        return;
      }

      this.codeSending = true;
      this.sendEmailCode(this.forgotForm.email);
    },

    /**
     * 第三方登录
     */
    handleThirdPartyLogin(type) {
      if (type === "wechat") {
        this.wechatForm.showQrcode = true;
        this.getWechatLoginCode();
        return;
      }
      getAuthRenderApi(type).then((res) => {
        //将当前地址存到cookie中：只存同源相对路径（path + query + hash），
        //不再存整串 href，避免把外部地址写进 cookie 变成开放重定向
        if (this.$route.path !== "/login") {
          setCookie("redirectUrl", this.$route.fullPath);
        }
        window.open(res.data, "_self");
      });
    },
    /**
     * 获取微信登录验证码
     */
    getWechatLoginCode() {
      // 本方法有三个调用点（created / switchForm / 模板里的刷新图标），
      // 先清掉上一轮的轮询与倒计时，否则旧句柄被覆盖后再也无法清除
      this.clearWechatTimer();
      getWechatLoginCodeApi().then((res) => {
        this.wechatForm.code = res.data;
        this.pollingWechatIsLogin();
        // 开始倒计时
        let countdown = 60;
        this.codeTimer = setInterval(() => {
          countdown--;
          if (countdown <= 0) {
            this.clearWechatTimer();
            this.wechatForm.code = "验证码已失效";
          }
        }, 1000);
      });
    },
    /**
     * 定时轮询获取微信登录状态
     */
    pollingWechatIsLogin() {
      const MAX_POLLING_COUNT = 60; // 与验证码 60 秒有效期对齐
      this.wechatPollingCount = 0;
      const timer = setInterval(() => {
        // 句柄已被替换说明轮询被重新发起过，本次轮询自行退出，避免误清新句柄
        if (this.pollingTimer !== timer) {
          clearInterval(timer);
          return;
        }
        if (this.wechatPollingCount >= MAX_POLLING_COUNT) {
          this.clearWechatTimer();
          this.wechatForm.code = "验证码已失效";
          return;
        }
        this.wechatPollingCount++;
        getWechatIsLoginApi(this.wechatForm.code)
          .then((res) => {
            if (res.code === 200) {
              this.$store.commit("SET_TOKEN", res.data.token);
              this.$store.commit("SET_USER_INFO", res.data);
              // 只清自己持有的句柄，避免清掉后续重新发起的轮询
              if (this.pollingTimer === timer) {
                this.clearWechatTimer();
              }
              this.$message.success("登录成功");
              this.handleClose();
            }
          })
          // 未扫码时接口返回非 200，request.js 会 reject，这是轮询的预期结果，忽略即可
          .catch(() => {});
      }, 1000);
      this.pollingTimer = timer;
    },

    /**
     * 关闭登录弹窗
     */
    handleClose() {
      this.$router.go(-1);
    },

    /**
     * 发送邮箱验证码
     */
    sendRegisterCode() {
      if (this.codeSending) return;

      if (!this.registerForm.email) {
        this.$message.error("请先输入邮箱");
        return;
      }
      this.codeSending = true;
      this.sendEmailCode(this.registerForm.email);
    },

    /**
     * 发送邮箱验证码
     */
    sendEmailCode(email) {
      sendEmailCodeApi(email)
        .then((res) => {
          this.$message.success("发送成功，请前往邮箱查看验证码");
          // 开始倒计时
          let countdown = 60;
          this.codeButtonText = `${countdown}秒后重试`;

          this.emailCodeTimer = setInterval(() => {
            countdown--;
            if (countdown <= 0) {
              clearInterval(this.emailCodeTimer);
              this.emailCodeTimer = null;
              this.codeSending = false;
              this.codeButtonText = "发送验证码";
            } else {
              this.codeButtonText = `${countdown}秒后重试`;
            }
          }, 1000);
        })
        .catch((err) => {
          this.$message.error(err.message || "发送失败");
          this.codeSending = false;
        });
    },

    /**
     * 清理定时器
     */
    clearWechatTimer() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer);
        this.pollingTimer = null;
      }
      if (this.codeTimer) {
        clearInterval(this.codeTimer);
        this.codeTimer = null;
      }
    },

    clearTimer() {
      this.clearWechatTimer();
      // 邮箱倒计时被提前清掉时同步复位按钮状态，否则 codeSending 会一直为 true，按钮永久禁用
      if (this.emailCodeTimer) {
        clearInterval(this.emailCodeTimer);
        this.emailCodeTimer = null;
        this.codeSending = false;
        this.codeButtonText = "发送验证码";
      }
    },

    handleSwitchForm() {
      if (this.currentForm === "login") {
        this.switchForm("account");
      } else if (this.currentForm === "account") {
        this.switchForm("login");
      } else {
        this.switchForm("login");
      }
    },

    /**
     * 回到首页
     */
    backToHome() {
      this.$router.push("/");
    },
  },
  beforeUnmount() {
    enableScroll();
    this.clearTimer();
  },
};
</script>
<style scoped lang="scss">
.login-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: url("@/assets/login-bg.svg") no-repeat;
  background-size: cover;
  min-height: 100vh;
  z-index: 2000;
}

.login-body {
  width: 420px;
  max-width: calc(100vw - 32px);
  padding: 36px 32px 32px;
  border-radius: $border-radius-lg;
  border: 1px solid var(--rule);
  background: var(--surface);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  position: relative;
}

.form-container {
  animation: fadeIn 0.3s ease;
}

.form-item {
  margin-bottom: 20px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: $border-radius-md;
  background: var(--accent);
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.05em;
  box-shadow: 0 4px 14px rgba(168, 106, 16, 0.22);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    filter: brightness(1.08);
    box-shadow: 0 6px 18px rgba(168, 106, 16, 0.3);
    transform: translateY(-1px);
  }

  &:active {
    filter: brightness(0.95);
    transform: translateY(0);
  }
}

.divider {
  margin: 24px 0;
  color: var(--text-tertiary);

  :deep(.el-divider__text) {
    background-color: var(--card-bg);
    padding: 0 12px;
    font-size: 14px;
  }
}

.third-party-login {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 24px;
}

.login-icon {
  width: 40px;
  height: 40px;
  border-radius: $border-radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--hover-bg);

  // 第三方登录按钮保留各家品牌色（这是品牌识别，不属于本站配色体系）
  &.github {
    color: #24292e;
  }
  &.qq {
    color: #12b7f5;
  }
  &.wechat {
    color: #07c160;
  }
  &.gitee {
    color: #c71d23;
  }
  &.weibo {
    color: #e6162d;
  }
}

.form-switch {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 24px;
  color: var(--text-secondary);
  font-size: 14px;

  a {
    color: $primary;
    text-decoration: none;
    font-weight: 500;
    cursor: pointer;

    &:hover {
      filter: brightness(0.85);
    }
  }
}

.divider-line {
  color: var(--rule);
  margin: 0 12px;
}

.qrcode-content {
  padding: 24px;
  text-align: center;
  animation: fadeIn 0.3s ease;
}

.qrcode-box {
  width: 200px;
  height: 200px;
  margin: 0 auto 16px;
  padding: 8px;
  border: 1px solid var(--rule);
  border-radius: $border-radius-sm;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.qrcode-tip {
  margin: 8px 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.code-text {
  color: var(--accent);
  font-weight: 500;
  i {
    cursor: pointer;
    margin-left: $spacing-sm;
  }
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-title {
  font-family: $font-serif;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.form-subtitle {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin: 0;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.forgot-link {
  color: $primary;
  font-size: 14px;
  cursor: pointer;

  &:hover {
    color: darken($primary, 10%);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.switch-form-btn,
.back-btn {
  position: absolute;
  top: 18px;
  width: 34px;
  height: 34px;
  border-radius: $border-radius-md;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--hover-bg);
  border: 1px solid var(--rule);
  color: var(--text-secondary);

  &:hover {
    background: var(--surface);
    border-color: var(--rule-strong);
    color: var(--accent);
  }

  i,
  .el-icon {
    font-size: 16px;
  }
}

.switch-form-btn {
  right: 18px;
}

.back-btn {
  right: 58px;
  z-index: 1;
}
</style>
