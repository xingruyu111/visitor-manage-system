import { expect, test } from '@playwright/test'

test('系统管理员登录并进入访客记录', async ({ page }) => {
  await page.goto('/login')

  await page.getByPlaceholder('请输入手机号').fill('13800000000')
  await page.getByPlaceholder('请输入密码').fill('admin123')
  await page.getByRole('button', { name: '登录' }).click()

  await expect(page).toHaveURL(/\/dashboard/)
  await expect(page.getByRole('menuitem', { name: '工作台' })).toBeVisible()

  await page.getByRole('menuitem', { name: /访客记录/ }).click()
  await expect(page).toHaveURL(/\/visitor-records/)
  await expect(page.getByRole('button', { name: '批量直录' })).toBeVisible()
  await expect(page.getByRole('button', { name: '导出' })).toBeVisible()
})

test('未登录访问后台会跳转登录页', async ({ page }) => {
  await page.goto('/visitor-records')
  await expect(page).toHaveURL(/\/login/)
})
