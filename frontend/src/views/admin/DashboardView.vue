<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Calendar,
  Clock,
  OfficeBuilding,
  Tickets,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { getDashboardStatsApi } from '@/api/admin'
import { appointmentStatus, formatTimeRange, periodLabel } from '@/utils/display'

const loading = ref(false)
const stats = ref({
  userCount: 0,
  patientCount: 0,
  doctorCount: 0,
  departmentCount: 0,
  scheduleCount: 0,
  upcomingScheduleCount: 0,
  appointmentCount: 0,
  pendingAppointmentCount: 0,
  completedAppointmentCount: 0,
  canceledAppointmentCount: 0,
  noShowAppointmentCount: 0,
  todayAppointmentCount: 0,
  recentAppointments: [],
})

const mainCards = computed(() => [
  { label: '预约总数', value: stats.value.appointmentCount, icon: Tickets, tone: 'teal' },
  { label: '待就诊', value: stats.value.pendingAppointmentCount, icon: Clock, tone: 'amber' },
  { label: '今日预约', value: stats.value.todayAppointmentCount, icon: Calendar, tone: 'blue' },
  { label: '医生总数', value: stats.value.doctorCount, icon: UserFilled, tone: 'green' },
  { label: '科室总数', value: stats.value.departmentCount, icon: OfficeBuilding, tone: 'rose' },
  { label: '系统用户', value: stats.value.userCount, icon: User, tone: 'slate' },
])

const statusCards = computed(() => [
  { label: '已完成', value: stats.value.completedAppointmentCount, type: 'success' },
  { label: '已取消', value: stats.value.canceledAppointmentCount, type: 'info' },
  { label: '爽约', value: stats.value.noShowAppointmentCount, type: 'danger' },
  { label: '未来排班', value: stats.value.upcomingScheduleCount, type: 'warning' },
])

async function loadStats() {
  loading.value = true
  try {
    stats.value = await getDashboardStatsApi()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div v-loading="loading" class="dashboard-page">
    <section class="metric-grid">
      <article v-for="card in mainCards" :key="card.label" class="metric-card">
        <span class="metric-icon" :class="card.tone">
          <el-icon><component :is="card.icon" /></el-icon>
        </span>
        <div>
          <p>{{ card.label }}</p>
          <strong>{{ card.value }}</strong>
        </div>
      </article>
    </section>

    <section class="status-strip">
      <div v-for="item in statusCards" :key="item.label" class="status-item">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <section class="surface-card recent-panel">
      <div class="panel-heading">
        <div>
          <h2>最近预约</h2>
          <p>按预约时间倒序显示最近提交的预约记录。</p>
        </div>
        <router-link to="/admin/appointments" class="panel-link">查看全部</router-link>
      </div>

      <el-table :data="stats.recentAppointments" stripe>
        <el-table-column prop="appointmentNo" label="预约单号" min-width="170" />
        <el-table-column prop="patientName" label="就诊人" width="100" />
        <el-table-column label="医生" min-width="130">
          <template #default="{ row }">{{ row.doctorName }} · {{ row.departmentName }}</template>
        </el-table-column>
        <el-table-column label="就诊时间" min-width="190">
          <template #default="{ row }">
            {{ row.visitDate }} {{ periodLabel(row.period) }}
            {{ formatTimeRange(row.startTime, row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="appointmentStatus(row.status).type" effect="light" size="small">
              {{ row.statusText || appointmentStatus(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 13px;
}

.metric-card {
  display: flex;
  min-height: 96px;
  align-items: center;
  gap: 13px;
  padding: 17px;
  border: 1px solid var(--color-line);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(21, 55, 47, 0.035);
}

.metric-icon {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 7px;
  font-size: 18px;
}

.metric-icon.teal {
  background: #e5f3f0;
  color: #0f766e;
}

.metric-icon.amber {
  background: #fbf0df;
  color: #b9732f;
}

.metric-icon.blue {
  background: #e8f0fb;
  color: #476f9f;
}

.metric-icon.green {
  background: #e9f5e9;
  color: #4a8052;
}

.metric-icon.rose {
  background: #f9eaea;
  color: #a85454;
}

.metric-icon.slate {
  background: #edf0f1;
  color: #596b70;
}

.metric-card p {
  color: var(--color-muted);
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin-top: 5px;
  font-size: 25px;
  line-height: 1;
}

.status-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  margin-top: 16px;
  border: 1px solid var(--color-line);
  border-radius: 8px;
  background: var(--color-line);
}

.status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: #fff;
}

.status-item span {
  color: var(--color-muted);
  font-size: 12px;
}

.status-item strong {
  font-size: 18px;
}

.recent-panel {
  margin-top: 18px;
  padding: 22px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.panel-heading h2 {
  font-size: 18px;
}

.panel-heading p {
  margin-top: 5px;
  color: var(--color-muted);
  font-size: 12px;
}

.panel-link {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .status-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .recent-panel {
    overflow-x: auto;
  }
}
</style>
