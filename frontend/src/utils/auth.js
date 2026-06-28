const TOKEN_KEY = 'superstudy_token'
const USER_INFO_KEY = 'superstudy_user_info'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  return localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  return localStorage.removeItem(TOKEN_KEY)
}

export function getUserInfo() {
  const info = localStorage.getItem(USER_INFO_KEY)
  return info ? JSON.parse(info) : null
}

export function setUserInfo(info) {
  return localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
}

export function removeUserInfo() {
  return localStorage.removeItem(USER_INFO_KEY)
}

export function getRole() {
  const userInfo = getUserInfo()
  return userInfo ? userInfo.role : null
}

/**
 * 获取当前学生的 studentId
 * 先从用户信息中取，没有则从 localStorage 取，最后返回 null
 */
export function getStudentId() {
  const userInfo = getUserInfo()
  if (userInfo && userInfo.studentId) {
    return userInfo.studentId
  }
  const stored = localStorage.getItem('superstudy_student_id')
  if (stored) return parseInt(stored)
  return null
}

export function clearAuth() {
  removeToken()
  removeUserInfo()
}
