import { expect, test } from '@playwright/test'

test('访客登录并提交预约、查看记录', async ({ page }) => {
  await page.goto('/login')

  await page.getByPlaceholder('请输入手机号').fill('13911110001')
  await page.getByPlaceholder('请输入密码').fill('123456')
  await page.getByRole('button', { name: '账号登录' }).click()

  await expect(page).toHaveURL(/\/v\/home/)
  await page.getByText('预约申请').click()
  await expect(page).toHaveURL(/\/v\/apply/)

  await page.getByPlaceholder('输入姓名或手机号搜索').fill('张老师')
  await page.getByRole('button', { name: '搜索' }).click()
  await expect(page.getByText('张老师').first()).toBeVisible()
  await page.locator('.van-radio').first().click()

  await page.getByRole('button', { name: '提交申请' }).click()
  await expect(page).toHaveURL(/\/v\/records/)
  await expect(page.getByText('我的预约').first()).toBeVisible()
})

test('未登录访问预约页跳转登录', async ({ page }) => {
  await page.goto('/v/apply')
  await expect(page).toHaveURL(/\/login/)
})
