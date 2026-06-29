/**
 * 认证工具模块
 *
 * 安全说明：
 * - Token 使用 sessionStorage（标签页关闭即失效），降低 XSS 持久化窃取风险
 * - 生产环境建议升级为 httpOnly Cookie + CSRF Token 方案
 */
const TOKEN_KEY = 'superstudy_token'
const USER_INFO_KEY = 'superstudy_user_info'

// ---- Token ----
export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  return sessionStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  return sessionStorage.removeItem(TOKEN_KEY)
}

// ---- 用户信息 ----
export function getUserInfo() {
  const info = sessionStorage.getItem(USER_INFO_KEY)
  return info ? JSON.parse(info) : null
}

export function setUserInfo(info) {
  // 移除冗余的 student_id key，统一从 userInfo 中读取
  sessionStorage.removeItem('superstudy_student_id')
  return sessionStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
}

export function removeUserInfo() {
  return sessionStorage.removeItem(USER_INFO_KEY)
}

// ---- 角色 & 学生ID ----
export function getRole() {
  const userInfo = getUserInfo()
  return userInfo ? userInfo.role : null
}

export function getStudentId() {
  const userInfo = getUserInfo()
  if (userInfo && userInfo.studentId) {
    return userInfo.studentId
  }
  return null
}

// ---- 登出清理 ----
export function clearAuth() {
  removeToken()
  removeUserInfo()
}

/**
 * 检查 Token 是否过期（解析 JWT exp 字段，不依赖网络请求）
 * @returns {boolean} true 表示已过期
 */
export function isTokenExpired() {
  const token = getToken()
  if (!token) return true
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    if (!payload.exp) return false
    // exp 是秒级时间戳
    return Date.now() >= payload.exp * 1000
  } catch {
    return true
  }
}
