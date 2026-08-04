import request from './request'

export const chatWithXiaowa = (data, apiKey) => request.post('/assistant/chat', data, {
  headers: { 'X-DeepSeek-Api-Key': apiKey },
  timeout: 70000,
})
