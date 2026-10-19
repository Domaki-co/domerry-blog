import axios from 'axios'
import { getToken } from '@/utils/cookie'
import store from '@/store'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 30000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 可以在这里添加请求头等配置
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

/**
 * 登录过期统一处理：清登录态 + 跳登录页 + reject。
 * HTTP 401 和业务体 code 401 共用这一份实现，避免两条路径行为跑偏。
 */
function handleUnauthorized() {
  //这里获取不到this，所以需要使用全局变量
  //CLEAR_LOGIN_STATE 会一并清掉 token cookie、内存里的 token、以及 sessionStorage 里的用户信息
  store.commit('CLEAR_LOGIN_STATE')
  router.push('/login')
  return Promise.reject(new Error('当前登录已过期，请重新登录'))
}

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else if(res.code === 404){
      return Promise.reject(new Error('请求路径不存在'))
    }else if(res.code === 401){
      return handleUnauthorized()
    }else {
      // 业务码要跟着 Error 一起抛出去：调用方需要区分"无权限"(403) 和服务器错误(500)
      const error = new Error(res.message || '请求失败')
      error.code = res.code
      error.response = response
      return Promise.reject(error)
    }
  },
  error => {
    //HTTP 状态码 401 走的是错误分支，这里同样要清登录态并跳登录页
    if (error.response && error.response.status === 401) {
      return handleUnauthorized()
    }
    return Promise.reject(error)
  }
)

export default service
