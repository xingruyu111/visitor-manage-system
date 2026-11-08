<template>
  <div class="page">
    <van-nav-bar title="智慧校园访客" />

    <div class="content">
      <div class="hero">
        <h2>访客预约</h2>
        <p>选择被访人、填写到访时间，提交后由被访人审批，通过后生成通行码。</p>
      </div>

      <van-cell-group inset title="访客">
        <van-cell title="预约申请" is-link to="/v/apply" />
        <van-cell title="我的预约" is-link to="/v/records" />
      </van-cell-group>

      <van-cell-group inset title="校内人员" class="group">
        <van-cell title="审批人 · 我的待办" is-link to="/a/tasks" />
        <van-cell title="部门经办人 · 批量预约" is-link to="/d/batch" />
        <van-cell title="门卫 · 扫码核验 / 车主查询" is-link to="/g/verify" />
      </van-cell-group>

      <div class="actions">
        <van-button block plain type="primary" @click="$router.push('/login')">切换账号登录</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
}
.content {
  padding: 16px 0 24px;
}
.hero {
  padding: 22px 20px;
  margin: 0 16px 4px;
  border-radius: 16px;
}
.hero h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #fff;
}
.hero p {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  line-height: 1.6;
}
.group {
  margin-top: 16px;
}
.actions {
  margin: 24px 16px;
}
</style>
