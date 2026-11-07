import { expect, test } from '@playwright/test'

const TEACHER = { phone: '13911110001', password: '123456' }

async function apiLogin(request: import('@playwright/test').APIRequestContext) {
  const response = await request.post('/api/auth/login', { data: TEACHER })
  const body = await response.json()
  return body.data.token as string
}

test('审批人通过申请后记录变为已通过', async ({ page, request }) => {
  const token = await apiLogin(request)
  const auth = { Authorization: `Bearer ${token}` }

  // 清空历史待办，保证本次只有一条
  const pending = await request.get('/api/web/approval/tasks?page=1&size=100', { headers: auth })
  for (const task of (await pending.json()).data.records) {
    await request.post(`/api/web/approval/tasks/${task.id}/reject?comment=clean`, { headers: auth })
  }

  const pad = (n: number) => String(n).padStart(2, '0')
  const start = new Date()
  start.setDate(start.getDate() + 1 + (Date.now() % 27))
  start.setHours(9, 0, 0, 0)
  const end = new Date(start.getTime() + 2 * 3600 * 1000)
  const format = (date: Date) =>
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00`
  const applied = await request.post('/api/web/visitor/records', {
    headers: auth,
    data: {
      targetId: 1,
      visitorType: 'NORMAL',
      visitPurpose: 'E2E审批',
      expectedTime: format(start),
      leaveTime: format(end),
    },
  })
  const appliedBody = await applied.json()
  expect(appliedBody.code).toBe(200)
  const recordId = appliedBody.data.recordId as number
  await page.goto('/login')
  await page.getByPlaceholder('请输入手机号').fill(TEACHER.phone)
  await page.getByPlaceholder('请输入密码').fill(TEACHER.password)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/dashboard/)
  await page.goto('/approval-tasks')
  await expect(page.getByText('我的待审批任务')).toBeVisible()
  await page.locator('button:visible', { hasText: /^通过$/ }).first().click()
  const box = page.locator('.el-message-box')
  await expect(box).toBeVisible()
  await box.locator('input').fill('E2E 通过')
  await box.locator('.el-message-box__btns .el-button--primary').click()

  await expect
    .poll(async () => {
      const response = await request.get(`/api/web/visitor/records/${recordId}`, { headers: auth })
      return (await response.json()).data.status
    })
    .toBe(1)
})
