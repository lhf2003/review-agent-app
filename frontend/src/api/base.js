/**
 * API 基础配置模块
 * 包含加密、请求函数、token 管理等基础功能
 */
import forge from 'node-forge'
import { ElMessage } from 'element-plus'

const BASE_URL = "/api"
const AES_KEY_STR = 'ReviewAgentSecureKey20250101!!!!';

/**
 * 密码加密函数
 * 优先使用 Web Crypto API，失败时回退到 node-forge
 */
async function encryptPassword(password) {
  if (!password) return password;
  if (window.crypto && window.crypto.subtle) {
    try {
      const enc = new TextEncoder();
      const keyMaterial = await window.crypto.subtle.importKey(
        'raw',
        enc.encode(AES_KEY_STR),
        { name: 'AES-GCM' },
        false,
        ['encrypt']
      );
      const iv = window.crypto.getRandomValues(new Uint8Array(12));
      const encodedPassword = enc.encode(password);
      const ciphertext = await window.crypto.subtle.encrypt(
        { name: 'AES-GCM', iv },
        keyMaterial,
        encodedPassword
      );
      const combined = new Uint8Array(iv.length + ciphertext.byteLength);
      combined.set(iv);
      combined.set(new Uint8Array(ciphertext), iv.length);
      let binary = '';
      const bytes = combined;
      const len = bytes.byteLength;
      for (let i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
      }
      return window.btoa(binary);
    } catch (e) {
      console.warn('WebCrypto AES-GCM failed, falling back to forge', e);
    }
  }
  try {
    const ivRaw = forge.random.getBytesSync(12);
    const cipher = forge.cipher.createCipher('AES-GCM', AES_KEY_STR);
    cipher.start({ iv: ivRaw, tagLength: 128 });
    cipher.update(forge.util.createBuffer(password, 'utf8'));
    const ok = cipher.finish();
    if (!ok) throw new Error('forge cipher.finish() failed');
    const ctRaw = cipher.output.getBytes();
    const tagRaw = cipher.mode.tag.getBytes();
    const combinedRaw = ivRaw + ctRaw + tagRaw;
    return window.btoa(combinedRaw);
  } catch (e) {
    console.error('Encryption fallback failed', e);
    throw new Error('加密不可用：请使用HTTPS或更新浏览器');
  }
}

/**
 * 获取存储的 JWT Token
 */
export function getToken() {
  try {
    const token = localStorage.getItem('token')
    return token || null
  } catch (e) {
    console.error('Failed to get token from localStorage', e)
  }
  return null
}

/**
 * 获取用户 ID（从 localStorage 的 auth 对象）
 * 注意：不用于认证，仅用于获取用户信息
 */
export function getUserId() {
  try {
    const authRaw = localStorage.getItem('auth')
    if (authRaw) {
      const auth = JSON.parse(authRaw)
      return auth.userId
    }
  } catch (e) {
    console.error('Failed to parse auth from localStorage', e)
  }
  return null
}

/**
 * 统一请求函数
 */
export async function request(path, { method = 'GET', params, body, headers } = {}) {
  const baseUrl = '/api'
  let url = path.startsWith('http') ? path : (baseUrl + path)

  if (params) {
    const cleanParams = Object.fromEntries(
      Object.entries(params).filter(([_, v]) => v != null)
    )
    const usp = new URLSearchParams(cleanParams)
    url += `?${usp.toString()}`
  }

  const token = getToken()

  const finalHeaders = {
    'Content-Type': 'application/json',
    ...(headers || {}),
  }

  if (token) {
    finalHeaders['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(url, {
    method,
    headers: finalHeaders,
    body: body ? JSON.stringify(body) : undefined,
  })

  if (!res.ok) {
    const text = await res.text()

    let errorMsg = ''
    try {
      const json = JSON.parse(text)
      errorMsg = json.message || json.msg || `HTTP ${res.status} 错误`
    } catch (e) {
      errorMsg = text || `HTTP ${res.status} 错误`
    }

    if (res.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('auth')
      ElMessage.error('登录已过期，请重新登录')
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      throw new Error('未授权，请重新登录')
    }

    if (res.status === 429) {
      ElMessage.error('请求过于频繁，请稍后再试')
      throw new Error('请求过于频繁')
    }

    throw new Error(errorMsg)
  }

  const data = await res.json().catch(() => null)
  return normalizeResponse(data)
}

/**
 * SSE 流式请求处理器
 */
export function handleStream(fetchPromise, handlers) {
  return fetchPromise
    .then((res) => {
      if (!res.ok) throw new Error('HTTP ' + res.status)
      if (handlers.onOpen) handlers.onOpen()
      const reader = res.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      function pump() {
        return reader.read().then(({ done, value }) => {
          if (done) {
            if (handlers.onDone) handlers.onDone()
            return
          }
          buffer += decoder.decode(value, { stream: true })
          const parts = buffer.split('\n\n')
          buffer = parts.pop() || ''
          for (const part of parts) {
            const lines = part.split('\n')

            let eventType = null
            const eventLines = lines.filter((l) => l.startsWith('event:'))
            if (eventLines.length) {
              const eventContent = eventLines[0].slice(6).trim()
              eventType = eventContent || null
            }

            const dataLines = lines.filter((l) => l.startsWith('data:'))
            if (dataLines.length) {
              const data = dataLines.map((l) => {
                let content = l.slice(5)
                if (content.startsWith(' ')) {
                  content = content.slice(1)
                }
                return content
              }).join('\n')

              if (eventType === 'error' && handlers.onErrorEvent) {
                handlers.onErrorEvent(data)
              } else if (eventType === 'stage' && handlers.onStage) {
                handlers.onStage(data)
              } else if (handlers.onEvent) {
                handlers.onEvent(data)
              }
            }
          }
          return pump()
        })
      }
      return pump()
    })
    .catch((err) => {
      if (handlers.onError) handlers.onError(err)
    })
}

/**
 * 响应标准化函数
 */
function normalizeResponse(resp) {
  if (resp == null) return resp
  const hasCode = Object.prototype.hasOwnProperty.call(resp, 'code')
  if (!hasCode) return resp
  const codeNum = Number(resp.code)
  if (Number.isNaN(codeNum)) return resp
  if (codeNum === 0) {
    return resp.data !== undefined ? resp.data : resp
  }
  const msg = resp.message || resp.msg || '请求失败'
  throw new Error(msg)
}

// 导出常量和工具函数
export { BASE_URL, encryptPassword }
