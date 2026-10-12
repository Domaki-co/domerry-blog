import Cookies from 'js-cookie'

const TokenKey = 'blog_token'

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  // 设置 cookie，7天有效期；SameSite 防跨站携带，HTTPS 环境下标记 Secure
  return Cookies.set(TokenKey, token, {
    expires: 7,
    sameSite: 'Lax',
    secure: window.location.protocol === 'https:'
  })
}

export function removeToken() {
  return Cookies.remove(TokenKey)
} 

export function setCookieExpires(key,value,expires) {
  return Cookies.set(key,value,{expires:expires})
} 
export function setCookie(key,value) {
  return Cookies.set(key,value)
} 
export function getCookie(key) {
  return Cookies.get(key)
} 

export function removeCookie(key) {
  return Cookies.remove(key)
} 