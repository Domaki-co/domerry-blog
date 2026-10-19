import { createStore } from 'vuex'
import { loginApi,logoutApi,getUserInfoApi } from '@/api/auth'
import { updateUserSettingsApi } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/cookie'

/**
 * 从 sessionStorage 恢复登录用户信息
 * 这里加了 try/catch：历史脏数据（比如 "undefined" 这类非 JSON 文本）会让 JSON.parse 直接抛错，
 * 而这里是模块顶层的求值，一抛错整个应用都起不来（白屏），所以宁可丢掉缓存
 * 只取展示字段：缓存可被任意脚本改写，roles/permissions 这类授权字段一律不认，
 * 权限判断只信本次会话里接口返回并 SET_USER_INFO 提交进来的 state.userInfo
 */
const CACHED_DISPLAY_FIELDS = ['id', 'username', 'nickname', 'avatar', 'userSettings']

function readCachedUser() {
  const raw = sessionStorage.getItem("user")
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') return null
    const display = {}
    CACHED_DISPLAY_FIELDS.forEach(key => {
      if (parsed[key] !== undefined) display[key] = parsed[key]
    })
    return display
  } catch (e) {
    sessionStorage.removeItem("user")
    return null
  }
}

function readInitialMusicEnabled() {
  const user = readCachedUser()
  if (user && user.userSettings && typeof user.userSettings.musicEnabled === 'boolean') {
    return user.userSettings.musicEnabled
  }
  const guestVal = localStorage.getItem('guest_music_enabled')
  if (guestVal !== null) {
    return guestVal !== 'false'
  }
  return true
}

export default createStore({
  state: {
    userInfo: readCachedUser(),
    musicEnabled: readInitialMusicEnabled(),
    webSiteInfo: {
      showList:[]
    },
    token: getToken() || '',
    searchVisible: false,
    mobileMenuVisible: false,
    visitorAccess: 0,
    siteAccess: 0,
    isLoading: false,
    notice: null,
    isUnread: false
  },
  getters: {
    musicEnabled(state) {
      return state.musicEnabled
    },
    /**
     * 是否管理员（后端 /auth/info 返回 roles 数组）
     * roles 只可能来自接口响应：sessionStorage 的缓存在 readCachedUser 里已被剥掉授权字段，
     * 伪造缓存不会让这里变 true
     */
    isAdmin(state) {
      const info = state.userInfo
      if (!info) return false
      if (Array.isArray(info.roles)) return info.roles.includes('admin')
      // 兼容旧的 role 字段写法
      return info.role === 'admin'
    }
  },
  mutations: {
    setSiteInfo(state, info) {
      state.webSiteInfo = info
    },
    SET_TOKEN(state, token) {
      state.token = token
      setToken(token)
    },
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo || null
      if (userInfo) {
        if (userInfo.userSettings && typeof userInfo.userSettings.musicEnabled === 'boolean') {
          state.musicEnabled = userInfo.userSettings.musicEnabled
        }
        sessionStorage.setItem("user", JSON.stringify(userInfo))
      } else {
        //退出登录时不能往 sessionStorage 里写字符串 "null" 占位，直接删掉 key
        sessionStorage.removeItem("user")
      }
    },

    SET_MUSIC_ENABLED(state, enabled) {
      state.musicEnabled = enabled
      if (state.userInfo) {
        if (!state.userInfo.userSettings) {
          state.userInfo.userSettings = {}
        }
        state.userInfo.userSettings.musicEnabled = enabled
        sessionStorage.setItem("user", JSON.stringify(state.userInfo))
      } else {
        localStorage.setItem('guest_music_enabled', String(enabled))
      }
    },

    /**
     * 清空本地登录态（token + 用户信息）
     * 登出接口调用失败时也必须走这里，否则 token 还留在 cookie 里，
     * 刷新页面后 App.vue 的 getUserInfo 会用旧 token 把用户重新拉回登录状态
     */
    CLEAR_LOGIN_STATE(state) {
      state.token = ''
      state.userInfo = null
      removeToken()
      sessionStorage.removeItem("user")
    },

    SET_SEARCH_VISIBLE(state, visible) {
      state.searchVisible = visible
    },
    SET_MOBILE_MENU_VISIBLE(state, visible) {
      state.mobileMenuVisible = visible
    },
    setMobileMenuVisible(state, value) {
      state.mobileMenuVisible = value
    },
    setVisitorAccess(state, value) {
      state.visitorAccess = value
    },
    setSiteAccess(state, value) {
      state.siteAccess = value
    },
    SET_LOADING(state, status) {
      state.isLoading = status
    },
    SET_NOTICE(state, notice) {
      state.notice = notice
    }
  },
  actions: {
    
    /**
     * 设置公告信息
     */
    setNotice({ commit }, notice) {
      commit('SET_NOTICE', notice)
    },

    /**
     * 设置站点信息
     */
    setSiteInfo({ commit }, info) {
      commit('setSiteInfo', info)
    },
    /**
     * 获取用户信息
     */
    async getUserInfo({ commit }) {
      if (!getToken()) return
      const res = await getUserInfoApi()
      commit('SET_USER_INFO', res.data)
    },

    /**
     * 登录
     */
    async loginAction({ commit, dispatch }, loginData) {
      try {
        const res = await loginApi(loginData)
        if (res.data) {
          commit('SET_TOKEN', res.data.token)
          commit('SET_USER_INFO', res.data)
          // 登录接口不返回 roles，补拉一次用户信息供前端判断管理员身份
          await dispatch('getUserInfo').catch(() => {})
          return Promise.resolve(res)
        }
       return Promise.reject(res)
      } catch (error) {
        return Promise.reject(error)
      }
    },

    /**
     * 退出登录
     */
    async logout({ commit }) {
      try {
        await logoutApi()
      } catch (e) {
        //后端登出失败（token 已过期、网络异常等）不能阻塞本地登出：
        //否则 removeToken 不会执行，刷新页面时又会以旧 token 拉回登录态
        console.warn('退出登录接口调用失败，仍清除本地登录态', e)
      } finally {
        commit('CLEAR_LOGIN_STATE')
      }
    },

    showLoading({ commit }) {
      commit('SET_LOADING', true)
    },

    hideLoading({ commit }) {
      commit('SET_LOADING', false)
    },

    /**
     * 切换音乐播放器开启/关闭状态，并自动持久化到数据库（登录用户）或 localStorage（游客）
     */
    async toggleMusicEnabled({ commit, state }, enabled) {
      commit('SET_MUSIC_ENABLED', enabled)
      if (state.userInfo && getToken()) {
        try {
          await updateUserSettingsApi({ musicEnabled: enabled })
        } catch (e) {
          console.warn('更新用户偏好配置失败:', e)
        }
      }
    }
  }
})