<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Calendar,
  Clock,
  Search,
  Tickets,
  View,
} from '@element-plus/icons-vue'
import {
  cancelAppointmentApi,
  getAppointmentDetailApi,
  getMyAppointmentsApi,
} from '@/api/appointment'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(6)
const statusFilter = ref('all')
const keyword = ref('')
const dateRange = ref([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const cancelVisible = ref(false)
const cancelLoading = ref(false)
const cancelTarget = ref(null)
const cancelReason = ref('')

const statusTabs = [
  { label: '全部', value: 'all' },
  { label: '待就诊', value: '0' },
  { label: '已完成', value: '1' },
  { label: '已取消', value: '2' },
  { label: '爽约', value: '3' },
]

const statusMeta = {
  0: { label: '待就诊', type: 'warning' },
  1: { label: '已完成', type: 'success' },
  2: { label: '已取消', type: 'info' },
  3: { label: '爽约', type: 'danger' },
}

const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

function getStatusMeta(status) {
  return statusMeta[status] || { label: '未知', type: 'info' }
}

function dateParts(value) {
  if (!value) {
    return { day: '--', month: '--', weekday: '', year: '' }
  }
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  return {
    day: String(day).padStart(2, '0'),
    month: `${month}月`,
    weekday: weekdays[date.getDay()],
    year: String(year),
  }
}

function timeRange(item) {
  if (!item?.startTime || !item?.endTime) {
    return '--'
  }
  return `${item.startTime.slice(0, 5)} - ${item.endTime.slice(0, 5)}`
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatFee(value) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function canCancel(item) {
  if (!item || item.status !== 0) {
    return false
  }
  const startAt = new Date(`${item.visitDate}T${item.startTime}`).getTime()
  return startAt - Date.now() > 2 * 60 * 60 * 1000
}

async function fetchAppointments() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      status: statusFilter.value === 'all' ? undefined : Number(statusFilter.value),
      keyword: keyword.value.trim() || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
    }
    const data = await getMyAppointmentsApi(params)
    records.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function changeStatus(value) {
  if (statusFilter.value === value) {
    return
  }
  statusFilter.value = value
  page.value = 1
  fetchAppointments()
}

function search() {
  page.value = 1
  fetchAppointments()
}

function resetFilters() {
  keyword.value = ''
  dateRange.value = []
  statusFilter.value = 'all'
  page.value = 1
  fetchAppointments()
}

function changePage(value) {
  page.value = value
  fetchAppointments()
}

async function openDetail(item) {
  detail.value = item
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getAppointmentDetailApi(item.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    detailLoading.value = false
  }
}

function openCancel(item) {
  cancelTarget.value = item
  cancelReason.value = ''
  cancelVisible.value = true
}

async function confirmCancel() {
  if (!cancelTarget.value || cancelLoading.value) {
    return
  }
  cancelLoading.value = true
  try {
    await cancelAppointmentApi(cancelTarget.value.id, {
      cancelReason: cancelReason.value.trim() || null,
    })
    ElMessage.success('预约已取消，号源已释放')
    cancelVisible.value = false
    detailVisible.value = false
    await fetchAppointments()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    cancelLoading.value = false
  }
}

function handleAiAppointmentUpdated() {
  fetchAppointments()
}

onMounted(() => {
  fetchAppointments()
  window.addEventListener('ai:appointment-updated', handleAiAppointmentUpdated)
})

onBeforeUnmount(() => {
  window.removeEventListener('ai:appointment-updated', handleAiAppointmentUpdated)
})
</script>

<template>
  <div class="page-container appointments-page">
    <div class="page-heading">
      <div>
        <h1>我的预约</h1>
        <p>查看待就诊安排，以及已完成、已取消和爽约的历史记录。</p>
      </div>
      <el-tag effect="plain" round>共 {{ total }} 条预约</el-tag>
    </div>

    <section class="surface-card filter-panel">
      <div class="status-tabs" role="tablist" aria-label="预约状态">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          type="button"
          class="status-tab"
          :class="{ active: statusFilter === tab.value }"
          @click="changeStatus(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="filter-row">
        <el-input
          v-model="keyword"
          class="keyword-input"
          placeholder="搜索预约单号、医生或科室"
          clearable
          @keyup.enter="search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-date-picker
          v-model="dateRange"
          class="date-filter"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          unlink-panels
        />

        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section v-loading="loading" class="appointment-results">
      <el-empty
        v-if="!loading && records.length === 0"
        description="暂无符合条件的预约记录"
        :image-size="96"
      />

      <article v-for="item in records" :key="item.id" class="appointment-card">
        <div class="date-tile" :class="`status-${item.status}`">
          <span class="date-day">{{ dateParts(item.visitDate).day }}</span>
          <span class="date-month">{{ dateParts(item.visitDate).month }}</span>
          <span class="date-weekday">{{ dateParts(item.visitDate).weekday }}</span>
        </div>

        <div class="appointment-main">
          <div class="appointment-title-row">
            <div>
              <h2>
                {{ item.doctorName }}
                <span>{{ item.doctorTitle }}</span>
              </h2>
              <p>{{ item.departmentName }} · {{ item.periodText }}门诊</p>
            </div>
            <el-tag :type="getStatusMeta(item.status).type" effect="light" round>
              {{ item.statusText || getStatusMeta(item.status).label }}
            </el-tag>
          </div>

          <div class="appointment-meta">
            <span>
              <el-icon><Calendar /></el-icon>
              {{ item.visitDate }} {{ dateParts(item.visitDate).weekday }}
            </span>
            <span>
              <el-icon><Clock /></el-icon>
              {{ timeRange(item) }}
            </span>
            <span>
              <el-icon><Tickets /></el-icon>
              {{ item.appointmentNo }}
            </span>
          </div>
        </div>

        <div class="appointment-actions">
          <el-button text type="primary" @click="openDetail(item)">
            <el-icon><View /></el-icon>
            查看详情
          </el-button>
          <el-button
            v-if="item.status === 0"
            plain
            type="danger"
            :disabled="!canCancel(item)"
            :title="canCancel(item) ? '取消预约' : '就诊开始前2小时内不可取消'"
            @click="openCancel(item)"
          >
            取消预约
          </el-button>
        </div>
      </article>
    </section>

    <div v-if="total > size" class="pagination-row">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="changePage"
      />
    </div>

    <el-drawer v-model="detailVisible" title="预约详情" size="440px">
      <div v-loading="detailLoading" class="detail-content">
        <template v-if="detail">
          <div class="detail-head">
            <div>
              <span>预约单号</span>
              <strong>{{ detail.appointmentNo }}</strong>
            </div>
            <el-tag :type="getStatusMeta(detail.status).type" effect="light" round>
              {{ detail.statusText || getStatusMeta(detail.status).label }}
            </el-tag>
          </div>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="医生">
              {{ detail.doctorName }} · {{ detail.doctorTitle }}
            </el-descriptions-item>
            <el-descriptions-item label="科室">{{ detail.departmentName }}</el-descriptions-item>
            <el-descriptions-item label="就诊日期">
              {{ detail.visitDate }} {{ dateParts(detail.visitDate).weekday }}
            </el-descriptions-item>
            <el-descriptions-item label="就诊时段">
              {{ detail.periodText }} {{ timeRange(detail) }}
            </el-descriptions-item>
            <el-descriptions-item label="就诊人">
              {{ detail.patientName }} · {{ detail.patientPhone }}
            </el-descriptions-item>
            <el-descriptions-item label="挂号费">
              {{ formatFee(detail.registrationFee) }}
            </el-descriptions-item>
            <el-descriptions-item label="预约时间">
              {{ formatDateTime(detail.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.cancelTime" label="取消时间">
              {{ formatDateTime(detail.cancelTime) }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.cancelReason" label="取消原因">
              {{ detail.cancelReason }}
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail?.status === 0"
          type="danger"
          :disabled="!canCancel(detail)"
          @click="openCancel(detail)"
        >
          取消预约
        </el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="cancelVisible" title="取消预约" width="440px" align-center>
      <div v-if="cancelTarget" class="cancel-summary">
        <span>{{ cancelTarget.departmentName }}</span>
        <strong>{{ cancelTarget.doctorName }} · {{ cancelTarget.visitDate }}</strong>
        <p>{{ cancelTarget.periodText }} {{ timeRange(cancelTarget) }}</p>
      </div>

      <el-form label-position="top">
        <el-form-item label="取消原因">
          <el-input
            v-model="cancelReason"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="选填，可填写取消原因"
          />
        </el-form-item>
      </el-form>

      <p class="rule-tip">就诊开始前 2 小时内不可取消；取消成功后号源会自动释放。</p>

      <template #footer>
        <el-button @click="cancelVisible = false">暂不取消</el-button>
        <el-button type="danger" :loading="cancelLoading" @click="confirmCancel">
          确认取消
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.appointments-page {
  max-width: 1120px;
}

.filter-panel {
  overflow: hidden;
}

.status-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--color-line);
  background: #fbfdfc;
}

.status-tab {
  position: relative;
  padding: 9px 14px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #66766f;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.status-tab:hover {
  background: #edf5f2;
  color: var(--color-primary-dark);
}

.status-tab.active {
  background: var(--color-primary);
  color: #fff;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
}

.keyword-input {
  width: min(340px, 100%);
}

.date-filter {
  width: 270px;
}

.appointment-results {
  min-height: 260px;
  margin-top: 18px;
}

.appointment-card {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) auto;
  gap: 22px;
  align-items: center;
  margin-bottom: 12px;
  padding: 18px 20px 18px 18px;
  border: 1px solid var(--color-line);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 6px 22px rgba(21, 55, 47, 0.035);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.appointment-card:hover {
  border-color: #b9d4cd;
  box-shadow: 0 10px 28px rgba(21, 55, 47, 0.07);
  transform: translateY(-1px);
}

.date-tile {
  display: flex;
  width: 76px;
  height: 78px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  border-radius: 8px;
  background: #edf5f2;
  color: #0b5e58;
}

.date-tile.status-1 {
  background: #edf8ef;
  color: #3b7d48;
}

.date-tile.status-2 {
  background: #f2f4f3;
  color: #7d8984;
}

.date-tile.status-3 {
  background: #fdf0ef;
  color: #b44d49;
}

.date-day {
  font-size: 25px;
  font-weight: 700;
  line-height: 1;
}

.date-month {
  margin-top: 5px;
  font-size: 12px;
  font-weight: 600;
}

.date-weekday {
  margin-top: 2px;
  opacity: 0.7;
  font-size: 11px;
}

.appointment-main {
  min-width: 0;
}

.appointment-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.appointment-title-row h2 {
  font-size: 17px;
  line-height: 1.35;
}

.appointment-title-row h2 span {
  margin-left: 6px;
  color: #7a8983;
  font-size: 12px;
  font-weight: 500;
}

.appointment-title-row p {
  margin-top: 4px;
  color: #4e615a;
  font-size: 13px;
}

.appointment-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  margin-top: 14px;
  color: #7a8983;
  font-size: 12px;
}

.appointment-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.appointment-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.appointment-actions .el-button + .el-button {
  margin-left: 4px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
}

.detail-content {
  min-height: 220px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding: 18px;
  border-radius: 8px;
  background: #f5f9f7;
}

.detail-head div {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.detail-head span {
  color: var(--color-muted);
  font-size: 12px;
}

.detail-head strong {
  font-size: 15px;
}

.cancel-summary {
  margin-bottom: 20px;
  padding: 15px 16px;
  border: 1px solid #e6ece9;
  border-radius: 6px;
  background: #f8faf9;
}

.cancel-summary span {
  display: block;
  color: var(--color-muted);
  font-size: 12px;
}

.cancel-summary strong {
  display: block;
  margin-top: 5px;
  font-size: 15px;
}

.cancel-summary p {
  margin-top: 4px;
  color: #5e6e68;
  font-size: 13px;
}

.rule-tip {
  color: #8a6a3f;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 860px) {
  .filter-row {
    flex-wrap: wrap;
  }

  .keyword-input,
  .date-filter {
    width: 100%;
  }

  .appointment-card {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .date-tile {
    width: 66px;
    height: 72px;
  }

  .appointment-actions {
    grid-column: 2;
    justify-content: flex-end;
  }
}

@media (max-width: 560px) {
  .status-tabs {
    overflow-x: auto;
    padding: 10px 12px;
  }

  .status-tab {
    flex: 0 0 auto;
    padding: 8px 12px;
  }

  .filter-row {
    padding: 14px;
  }

  .appointment-card {
    grid-template-columns: 1fr;
    gap: 14px;
    padding: 16px;
  }

  .date-tile {
    width: 100%;
    height: auto;
    flex-direction: row;
    gap: 6px;
    padding: 8px 12px;
  }

  .date-day {
    font-size: 18px;
  }

  .date-month,
  .date-weekday {
    margin-top: 0;
  }

  .appointment-actions {
    grid-column: 1;
    justify-content: flex-start;
  }

  .appointment-meta {
    gap: 7px 12px;
  }
}
</style>
