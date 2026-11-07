<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="label">{{ card.label }}</div>
          <div class="value">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="chart-card">
      <template #header>近 7 日预约量</template>
      <div class="placeholder">图表区域（ECharts 已就绪，待接入统计接口）</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
const cards = [
  { label: '今日预约', value: '—' },
  { label: '待审批', value: '—' },
  { label: '在校访客', value: '—' },
  { label: '异常预警', value: '—' },
]
</script>

<style scoped>
.stat-card .label {
  font-size: 13px;
  color: #909399;
}

.stat-card .value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 600;
}

.chart-card {
  margin-top: 16px;
}

.placeholder {
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b1b3b8;
  background: #fafafa;
  border-radius: 6px;
}
</style>
