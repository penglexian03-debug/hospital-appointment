<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import {
  getAdminAppointmentsApi,
  getAdminDepartmentsApi,
  getAdminDoctorsApi,
  updateAppointmentStatusApi,
} from '@/api/admin'
import {
  appointmentStatus,
  formatDateTime,
  formatMoney,
  formatTimeRange,
  periodLabel,
} from '@/utils/display'

const loading = ref(false)
const rows = ref([])
const departments = ref([])
const doctors = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive({
  keyword: '',
  status: undefined,
  departmentId: undefined,
  doctorId: undefined,
  dateRange: [],
})

const detailVisible = ref(false)
const detail = ref(null)

async function loadOptions() {
  try {
    const [departmentData, doctorData] = await Promise.all([
      getAdminDepartmentsApi({ page: 1, size: 100 }),
      getAdminDoctorsApi({ page: 1, size: 100 }),
    ])
    departments.value = departmentData.records || []
    doctors.value = doctorData.records || []
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadAppointments() {
  loading.value = true
  try {
    const data = await getAdminAppointmentsApi({
      keyword: filters.keyword.trim() || undefined,
      status: filters.status,
      departmentId: filters.departmentId,
      doctorId: filters.doctorId,
      startDate: filters.dateRange?.[0] || undefined,
      endDate: filters.dateRange?.[1] || undefined,
      page: page.value,
      size: size.value,
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadAppointments()
}

function resetFilters() {
  filters.keyword = ''
  filters.status = undefined
  filters.departmentId = undefined
  filters.doctorId = undefined
  filters.dateRange = []
  page.value = 1
  loadAppointments()
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

async function changeStatus(row, status) {
  const action = status === 1 ? '标记完成' : '标记爽约'
  try {
    await ElMessageBox.confirm(
      `确定将预约 ${row.appointmentNo} ${action}吗？此操作不可撤销。`,
      action,
      { confirmButtonText: action, cancelButtonText: '取消', type: status === 3 ? 'warning' : 'info' },
    )
  } catch {
    return
  }
  try {
    await updateAppointmentStatusApi(row.id, status)
    ElMessage.success(`预约已${action}`)
    await loadAppointments()
    if (detail.value?.id === row.id) {
      detail.value = rows.value.find((item) => item.id === row.id) || { ...row, status }
    }
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(async () => {
  await Promise.all([loadOptions(), loadAppointments()])
})
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-heading">
      <div>
        <h2>预约记录</h2>
        <p>查询患者预约，处理待就诊记录并查看完整就诊信息。</p>
      </div>
      <el-tag effect="plain" round>共 {{ total }} 条记录</el-tag>
    </div>

    <section class="surface-card admin-toolbar">
      <el-input
        v-model="filters.keyword"
        class="toolbar-input"
        clearable
        placeholder="搜索预约号、患者、医生或科室"
        @keyup.enter="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filters.status" class="toolbar-select" clearable placeholder="全部状态">
        <el-option label="待就诊" :value="0" />
        <el-option label="已完成" :value="1" />
        <el-option label="已取消" :value="2" />
        <el-option label="爽约" :value="3" />
      </el-select>
      <el-select
        v-model="filters.departmentId"
        class="toolbar-select"
        clearable
        filterable
        placeholder="全部科室"
      >
        <el-option v-for="item in departments" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="filters.doctorId" class="toolbar-select" clearable filterable placeholder="全部医生">
        <el-option
          v-for="item in doctors"
          :key="item.id"
          :label="`${item.name} · ${item.departmentName}`"
          :value="item.id"
        />
      </el-select>
      <el-date-picker
        v-model="filters.dateRange"
        class="toolbar-date"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="就诊开始日"
        end-placeholder="就诊结束日"
        unlink-panels
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetFilters">
        <el-icon><Refresh /></el-icon>
        重置
      </el-button>
    </section>

    <section class="surface-card admin-table-card">
      <div class="admin-table-caption">
        <h3>预约列表</h3>
        <span>按就诊日期倒序排列</span>
      </div>
      <el-table v-loading="loading" :data="rows" row-key="id" stripe>
        <el-table-column prop="appointmentNo" label="预约单号" min-width="175" />
        <el-table-column label="患者" min-width="140">
          <template #default="{ row }">
            <strong>{{ row.patientName }}</strong>
            <span class="subtext">{{ row.patientPhone }}</span>
          </template>
        </el-table-column>
        <el-table-column label="就诊安排" min-width="220">
          <template #default="{ row }">
            <strong>{{ row.doctorName }} · {{ row.departmentName }}</strong>
            <span class="subtext">
              {{ row.visitDate }} {{ periodLabel(row.period) }}
              {{ formatTimeRange(row.startTime, row.endTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="挂号费" width="105" align="right">
          <template #default="{ row }">{{ formatMoney(row.registrationFee) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="appointmentStatus(row.status).type" effect="light" size="small">
              {{ row.statusText || appointmentStatus(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="145">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="admin-table-actions">
              <el-button link type="primary" @click="openDetail(row)">
                <el-icon><View /></el-icon>
                详情
              </el-button>
              <template v-if="row.status === 0">
                <el-button link type="success" @click="changeStatus(row, 1)">完成</el-button>
                <el-button link type="danger" @click="changeStatus(row, 3)">爽约</el-button>
              </template>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无预约记录" :image-size="84" />
        </template>
      </el-table>
      <div class="admin-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadAppointments"
          @size-change="search"
        />
      </div>
    </section>

    <el-drawer v-model="detailVisible" title="预约详情" size="500px">
      <div v-if="detail" class="appointment-detail">
        <div class="detail-summary">
          <div>
            <span>预约单号</span>
            <strong>{{ detail.appointmentNo }}</strong>
          </div>
          <el-tag :type="appointmentStatus(detail.status).type" effect="light">
            {{ detail.statusText || appointmentStatus(detail.status).label }}
          </el-tag>
        </div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="患者姓名">{{ detail.patientName }}</el-descriptions-item>
          <el-descriptions-item label="患者手机号">{{ detail.patientPhone }}</el-descriptions-item>
          <el-descriptions-item label="科室">{{ detail.departmentName }}</el-descriptions-item>
          <el-descriptions-item label="医生">
            {{ detail.doctorName }} · {{ detail.doctorTitle }}
          </el-descriptions-item>
          <el-descriptions-item label="就诊日期">{{ detail.visitDate }}</el-descriptions-item>
          <el-descriptions-item label="就诊时段">
            {{ periodLabel(detail.period) }} {{ formatTimeRange(detail.startTime, detail.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="挂号费">{{ formatMoney(detail.registrationFee) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{ formatDateTime(detail.cancelTime) }}</el-descriptions-item>
          <el-descriptions-item label="取消原因">{{ detail.cancelReason || '--' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.status === 0" class="admin-detail-actions">
          <el-button type="success" @click="changeStatus(detail, 1)">标记完成</el-button>
          <el-button type="danger" plain @click="changeStatus(detail, 3)">标记爽约</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.subtext {
  display: block;
  margin-top: 3px;
  color: var(--color-muted);
  font-size: 11px;
}

.detail-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid #edf2ef;
}

.detail-summary span {
  display: block;
  color: var(--color-muted);
  font-size: 12px;
}

.detail-summary strong {
  display: block;
  margin-top: 6px;
  font-size: 17px;
}
</style>
