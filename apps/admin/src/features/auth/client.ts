export class ApiError extends Error {
  constructor(public code: number, message: string, public status = 0) { super(message) }
}
type Tokens = { accessToken: string; refreshToken: string }
export function createApiClient(fetcher: typeof fetch = (...args) => fetch(...args), storage?: Storage, expired = () => {}) {
  let access = ''
  let refresh = storage?.getItem('yongtuo.refresh') || ''
  let rotating: Promise<void> | undefined
  let generation = 0
  function setTokens(tokens: Tokens) {
    access = tokens.accessToken; refresh = tokens.refreshToken
    if (refresh) storage?.setItem('yongtuo.refresh', refresh)
    else storage?.removeItem('yongtuo.refresh')
  }
  function clear() { generation++; setTokens({ accessToken: '', refreshToken: '' }) }
  async function rotate() {
    if (rotating) return rotating
    const current = generation
    rotating = (async () => {
      try {
        const response = await fetcher('/api/v1/admin/auth/refresh', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken: refresh }) })
        const body = await response.json()
        if (!response.ok || !body.data?.accessToken || current !== generation) throw new Error()
        setTokens(body.data)
      } catch {
        if (current === generation) { clear(); expired() }
        throw new ApiError(10003, '登录已过期，请重新登录。', 401)
      }
    })().finally(() => { rotating = undefined })
    return rotating
  }
  async function request<T = any>(path: string, init: RequestInit = {}, retry = true, binary = false): Promise<T> {
    if (!access && refresh) await rotate()
    const usedAccess = access
    const headers = new Headers(init.headers)
    if (access) headers.set('Authorization', `Bearer ${access}`)
    if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json')
    let response: Response
    try { response = await fetcher('/api/v1/admin' + path, { ...init, headers }) }
    catch { throw new ApiError(0, '网络连接失败，请检查连接后重试。') }
    if (response.status === 401 && retry && refresh) {
      if (usedAccess === access) await rotate()
      return request(path, init, false, binary)
    }
    if (response.status === 401 && !path.startsWith('/auth/login')) { clear(); expired() }
    if (response.ok && binary) return await response.blob() as T
    const body = await response.json().catch(() => ({}))
    if (!response.ok) throw new ApiError(body.code || 0,
      ({36002:'英文内容已变更，请刷新后重新核对并确认。',21001:'分类不存在或已删除。',21002:'分类仍含产品或子分类，请先移动内容再删除。',21003:'此分类网址标识已被使用，请换一个。',21004:'上级分类无效，请重新选择。',21005:'不能把分类移到自己或子分类下面。'} as Record<number,string>)[body.code] || (body.code === 50001 ? '此功能尚未配置服务，请联系维护人员完成配置。' :
      response.status === 401 ? '用户名或密码不正确，或登录已过期。' :
      response.status === 429 ? '尝试次数过多，请稍后再试。' : body.message || '操作未完成，请重试。'), response.status)
    return body.data
  }
  return {
    request, download: (path: string) => request<Blob>(path, {}, true, true), get: <T = any>(path: string) => request<T>(path),
    post: <T = any>(path: string, data?: unknown) => request<T>(path, { method: 'POST', body: data instanceof FormData ? data : JSON.stringify(data) }),
    put: <T = any>(path: string, data: unknown) => request<T>(path, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (path: string) => request(path, { method: 'DELETE' }),
    setTokens, clear, hasSession: () => Boolean(access || refresh),
    async login(username: string, password: string) { clear(); const result = await request<Tokens>('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }, false); setTokens(result) },
    async logout() { const token = refresh; try { if (token) await request('/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken: token }) }) } finally { clear() } }
  }
}
export async function requireSession(client: Pick<ReturnType<typeof createApiClient>, 'hasSession'>, path: string) {
  if (!client.hasSession()) return { path: '/login', query: { redirect: path } }
  return true
}
export const api = createApiClient(undefined, typeof sessionStorage !== 'undefined' ? sessionStorage : undefined,
  () => window.dispatchEvent(new Event('auth-expired')))
