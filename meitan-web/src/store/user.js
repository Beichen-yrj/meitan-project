import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userId = ref('')
  const username = ref('')
  const realName = ref('')
  const role = ref('')

  async function login(usernameVal, passwordVal) {
    const res = await loginApi({ username: usernameVal, password: passwordVal })
    token.value = res.data.token
    userId.value = res.data.userId
    username.value = res.data.username
    realName.value = res.data.realName
    role.value = res.data.role
    return res.data
  }

  function logout() {
    token.value = ''
    userId.value = ''
    username.value = ''
    realName.value = ''
    role.value = ''
  }

  return { token, userId, username, realName, role, login, logout }
}, {
  persist: {
    key: 'meitan-user',
    storage: localStorage,
    paths: ['token', 'userId', 'username', 'realName', 'role'],
  },
})
