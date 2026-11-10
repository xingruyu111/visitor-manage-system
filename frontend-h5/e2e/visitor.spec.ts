import { expect, test } from '@playwright/test'

test.beforeAll(async ({ request }) => {
  const admin = await request.post('/api/auth/login', { data: { phone: '13800000000', password: 'admin123' } })
  const token = (await admin.json()).data.token
  await request.post('/api/web/websuper/users', {
    headers: { Authorization: `Bearer ${token}` },
    data: { phone: '13911110001', name: 'E2E老师', role: 5 },
  })
  await request.post('/api/web/admin/staff', {
    headers: { Authorization: `Bearer ${token}` },
    data: { name: 'E2E老师', personNo: 'T-E2E-001', phone: '13911110001', personType: 1 },
  })
})

test('访客登录并提交预约、查看记录', async ({ page }) => {
  await page.goto('/login')

  await page.getByPlaceholder('请输入手机号').fill('13900008888')
  await page.getByPlaceholder('请输入密码').fill('123456')
  await page.getByRole('button', { name: '账号登录' }).click()

  await expect(page).toHaveURL(/\/v\/home/)
  await page.getByText('预约申请').click()
  await expect(page).toHaveURL(/\/v\/apply/)

  await page.getByPlaceholder('输入完整姓名或手机号').fill('E2E老师')
  await page.getByRole('button', { name: '搜索' }).click()
  await expect(page.getByText('E2E老师').first()).toBeVisible()
  await page.getByText('E2E老师').first().click()

  const start = new Date()
  start.setDate(start.getDate() + 1 + Math.floor(Math.random() * 60))
  start.setHours(8 + Math.floor(Math.random() * 8), Math.floor(Math.random() * 60), 0, 0)
  const end = new Date(start.getTime() + 2 * 3600 * 1000)
  const pad = (n: number) => String(n).padStart(2, '0')
  const format = (date: Date) =>
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
  const timeInputs = page.locator('input[type="datetime-local"]')
  await timeInputs.nth(0).fill(format(start))
  await timeInputs.nth(1).fill(format(end))

  await page.getByRole('button', { name: '提交申请' }).click()
  await expect(page).toHaveURL(/\/v\/records/)
  await expect(page.getByText('我的预约').first()).toBeVisible()
})

test('未登录访问预约页跳转登录', async ({ page }) => {
  await page.goto('/v/apply')
  await expect(page).toHaveURL(/\/login/)
})
