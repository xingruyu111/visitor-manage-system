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

  const visitorPhone = '139' + String(Date.now()).slice(-8)
  await page.getByPlaceholder('请输入手机号').fill(visitorPhone)
  await page.getByPlaceholder('请输入密码').fill('123456')
  await page.getByRole('button', { name: '账号登录' }).click()

  await expect(page).toHaveURL(/\/v\/home/)
  await page.getByText('预约申请').click()
  await expect(page).toHaveURL(/\/v\/apply/)

  await page.getByPlaceholder('输入姓名或工号').fill('E2E老师')
  await page.getByRole('button', { name: '搜索' }).click()
  await expect(page.getByText('E2E老师').first()).toBeVisible()
  await page.locator('.target-dropdown .van-cell').first().click()

  await page.locator('.van-cell').filter({ hasText: '到访时间' }).first().click()
  await page.locator('.time-confirm').click()
  await page.locator('.van-cell').filter({ hasText: '离校时间' }).first().click()
  await page.locator('.time-confirm').click()

  const requiredFields = page.locator('.van-field input[placeholder="必填"]')
  for (let i = 0; i < await requiredFields.count(); i++) {
    await requiredFields.nth(i).fill('E2E补充')
  }

  await page.getByRole('button', { name: '提交申请' }).click()
  await expect(page).toHaveURL(/\/v\/records/)
  await expect(page.getByText('我的预约').first()).toBeVisible()
})

test('未登录访问预约页跳转登录', async ({ page }) => {
  await page.goto('/v/apply')
  await expect(page).toHaveURL(/\/login/)
})
