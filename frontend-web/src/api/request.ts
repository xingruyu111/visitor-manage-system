import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('roleCode')
  if (role) {
    config.headers['X-Current-Role'] = role
  }
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 200) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body?.data ?? body
  },
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem('token')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

/** 已解包的业务请求 helper：直接返回 Result.data */
export const http = {
  get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    return request.get(url, { params }) as unknown as Promise<T>
  },
  post<T>(url: string, data?: unknown): Promise<T> {
    return request.post(url, data) as unknown as Promise<T>
  },
  put<T>(url: string, data?: unknown): Promise<T> {
    return request.put(url, data) as unknown as Promise<T>
  },
  delete<T>(url: string): Promise<T> {
    return request.delete(url) as unknown as Promise<T>
  },
}

export default request
