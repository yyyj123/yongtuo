import { describe, it, expect, vi } from 'vitest'
import { createApiClient, requireSession } from './client'
const ok = (data: unknown) => new Response(JSON.stringify({ code: 0, data }), { status: 200 })
const denied = () => new Response(JSON.stringify({ code: 10003 }), { status: 401 })
describe('authenticated client', () => {
  it('redirects protected routes and preserves a local destination', async () => {
    expect(await requireSession({ hasSession: () => false } as any, '/products/4')).toEqual({ path: '/login', query: { redirect: '/products/4' } })
  })
  it('shares one refresh across concurrent expired requests and retries with the rotated token', async () => {
    const fetcher = vi.fn(async (url: string, init: RequestInit) => {
      if (url.endsWith('/auth/refresh')) return ok({ accessToken: 'new', refreshToken: 'rotated' })
      return new Headers(init.headers).get('Authorization') === 'Bearer new' ? ok({ id: 1 }) : denied()
    })
    const client = createApiClient(fetcher as any)
    client.setTokens({ accessToken: 'old', refreshToken: 'refresh' })
    expect(await Promise.all([client.get('/products'), client.get('/categories')])).toEqual([{ id: 1 }, { id: 1 }])
    expect(fetcher.mock.calls.filter(([url]) => url.endsWith('/auth/refresh'))).toHaveLength(1)
  })
  it('clears the session and reports expiration when refresh fails, without retry loops', async () => {
    const fetcher = vi.fn(async () => denied())
    const expired = vi.fn()
    const client = createApiClient(fetcher as any, undefined, expired)
    client.setTokens({ accessToken: 'old', refreshToken: 'refresh' })
    await expect(client.get('/products')).rejects.toThrow('登录已过期')
    expect(client.hasSession()).toBe(false)
    expect(expired).toHaveBeenCalledOnce()
    expect(fetcher).toHaveBeenCalledTimes(2)
  })
})
