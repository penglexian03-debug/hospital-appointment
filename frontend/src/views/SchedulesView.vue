<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Clock, Money, Search, Tickets } from '@element-plus/icons-vue'
import { getDepartmentsApi, getDoctorsApi, getSchedulesApi } from '@/api/catalog'
import { createAppointmentApi } from '@/api/appointment'
import { useAuthStore } from '@/stores/auth'
import { formatMoney, formatTimeRange, periodLabel } from '@/utils/display'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const departments = ref([])
const doctors = ref([])
const schedules = ref([])

const bookingVisible = ref(false)
const bookingLoading = ref(false)
const bookingSchedule = ref(null)
const bookingFormRef = ref()
const bookingForm = reactive({
  patientName: '',
  patientPhone: '',
})

const bookingRules = {
  patientName: [
    { required: true, message: '请输入就诊人姓名', trigger: 'blur' },
    { max: 50, message: '就诊人姓名不能超过 50 个字符', trigger: 'blur' },
  ],
  patientPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
}

function isoDate(offsetDays = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const filters = reactive({
  departmentId: route.query.departmentId ? Number(route.query.departmentId) : undefined,
  doctorId: route.query.doctorId ? Number(route.query.doctorId) : undefined,
  period: undefined,
  dateRange: [isoDate(0), isoDate(6)],
  onlyAvailable: true,
})

const doctorOptions = computed(() => {
  if (!filters.departmentId) {
    return doctors.value
  }
  return doctors.value.filter((item) => item.departmentId === filters.departmentId)
})

const groupedSchedules = computed(() => {
  const groups = new Map()
  for (const item of schedules.value) {
    if (!groups.has(item.workDate)) {
      groups.set(item.workDate, [])
    }
    groups.get(item.workDate).push(item)
  }
  return Array.from(groups.entries()).map(([date, items]) => ({
    date,
    label: dateLabel(date),
    items,
  }))
})

function dateLabel(value) {
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${month}月${day}日 ${weekdays[date.getDay()]}`
}

async function loadFilters() {
  try {
    const [departmentData, doctorData] = await Promise.all([
      getDepartmentsApi(),
      getDoctorsApi(),
    ])
    departments.value = departmentData
    doctors.value = doctorData
    if (filters.doctorId) {
      const selected = doctorData.find((item) => item.id === filters.doctorId)
      if (selected && !filters.departmentId) {
        filters.departmentId = selected.departmentId
      }
    }
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadSchedules() {
  loading.value = true
  try {
    schedules.value = await getSchedulesApi({
      departmentId: filters.departmentId || undefined,
      doctorId: filters.doctorId || undefined,
      startDate: filters.dateRange?.[0] || undefined,
      endDate: filters.dateRange?.[1] || undefined,
      period: filters.period || undefined,
      onlyAvailable: filters.onlyAvailable,
    })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function handleDepartmentChange() {
  if (filters.doctorId && !doctorOptions.value.some((item) => item.id === filters.doctorId)) {
    filters.doctorId = undefined
  }
  loadSchedules()
}

function search() {
  loadSchedules()
}

function resetFilters() {
  filters.departmentId = undefined
  filters.doctorId = undefined
  filters.period = undefined
  filters.dateRange = [isoDate(0), isoDate(6)]
  filters.onlyAvailable = true
  loadSchedules()
}

function openBooking(schedule) {
  bookingSchedule.value = schedule
  bookingForm.patientName = auth.state.user?.realName || ''
  bookingForm.patientPhone = auth.state.user?.phone || ''
  bookingVisible.value = true
}

async function confirmBooking() {
  if (bookingLoading.value) {
    return
  }
  try {
    await bookingFormRef.value.validate()
  } catch {
    return
  }

  bookingLoading.value = true
  try {
    await createAppointmentApi({
      scheduleId: bookingSchedule.value.id,
      patientName: bookingForm.patientName.trim(),
      patientPhone: bookingForm.patientPhone.trim(),
    })
    ElMessage.success('预约成功')
    bookingVisible.value = false
    router.push('/appointments')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    bookingLoading.value = false
  }
}

onMounted(async () => {
  await loadFilters()
  await loadSchedules()
})
</script>

<template>
  <div class="page-container schedules-page">
    <div class="page-heading">
      <div>
        <h1>预约挂号</h1>
        <p>查看未来七天的出诊安排，选择合适的医生和时段完成预约。</p>
      </div>
      <el-tag effect="plain" round>{{ schedules.length }} 个可预约排班</el-tag>
    </div>

    <section class="surface-card schedule-toolbar">
      <div class="filter-row">
        <el-select
          v-model="filters.departmentId"
          clearable
          placeholder="全部科室"
          class="filter-select"
          @change="handleDepartmentChange"
        >
          <el-option v-for="department in departments" :key="department.id" :label="department.name" :value="department.id" />
        </el-select>

        <el-select v-model="filters.doctorId" clearable placeholder="全部医生" class="filter-select" @change="search">
          <el-option
            v-for="doctor in doctorOptions"
            :key="doctor.id"
            :label="`${doctor.name} · ${doctor.title}`"
            :value="doctor.id"
          />
        </el-select>

        <el-select v-model="filters.period" clearable placeholder="全部时段" class="period-select" @change="search">
          <el-option label="上午" :value="1" />
          <el-option label="下午" :value="2" />
        </el-select>

        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="date-filter"
          unlink-panels
        />
      </div>

      <div class="filter-row secondary-row">
        <el-switch v-model="filters.onlyAvailable" active-text="仅看有号" @change="search" />
        <div class="toolbar-actions">
          <el-button type="primary" @click="search">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>
    </section>

    <section v-loading="loading" class="schedule-results">
      <el-empty v-if="!loading && groupedSchedules.length === 0" description="暂无符合条件的排班" :image-size="96" />

      <div v-for="group in groupedSchedules" :key="group.date" class="schedule-group">
        <div class="group-heading">
          <span class="group-date">{{ group.label }}</span>
          <span class="group-count">{{ group.items.length }} 个排班</span>
        </div>

        <article v-for="schedule in group.items" :key="schedule.id" class="schedule-card">
          <div class="time-block">
            <span>{{ periodLabel(schedule.period) }}</span>
            <strong>{{ formatTimeRange(schedule.startTime, schedule.endTime) }}</strong>
          </div>

          <div class="schedule-main">
            <div class="schedule-title">
              <h2>
                {{ schedule.doctorName }}
                <small>{{ schedule.doctorTitle }}</small>
              </h2>
              <el-tag :type="schedule.availableSlots > 0 ? 'success' : 'info'" effect="plain" size="small">
                剩余 {{ schedule.availableSlots }} 号
              </el-tag>
            </div>
            <p>{{ schedule.departmentName }} · {{ periodLabel(schedule.period) }}门诊</p>
            <div class="schedule-meta">
              <span><el-icon><Calendar /></el-icon>{{ group.label }}</span>
              <span><el-icon><Clock /></el-icon>{{ formatTimeRange(schedule.startTime, schedule.endTime) }}</span>
              <span><el-icon><Money /></el-icon>{{ formatMoney(schedule.registrationFee) }}</span>
            </div>
          </div>

          <el-button
            type="primary"
            class="booking-button"
            :disabled="schedule.availableSlots <= 0 || schedule.status !== 1"
            @click="openBooking(schedule)"
          >
            <el-icon><Tickets /></el-icon>
            {{ schedule.availableSlots > 0 ? '立即预约' : '号源已满' }}
          </el-button>
        </article>
      </div>
    </section>

    <el-dialog v-model="bookingVisible" title="确认预约" width="460px" align-center>
      <div v-if="bookingSchedule" class="booking-summary">
        <div>
          <span>{{ bookingSchedule.departmentName }}</span>
          <strong>{{ bookingSchedule.doctorName }} · {{ bookingSchedule.doctorTitle }}</strong>
        </div>
        <div class="booking-time">
          {{ bookingSchedule.workDate }} {{ periodLabel(bookingSchedule.period) }}
          {{ formatTimeRange(bookingSchedule.startTime, bookingSchedule.endTime) }}
        </div>
        <div class="booking-fee">
          挂号费 <b>{{ formatMoney(bookingSchedule.registrationFee) }}</b>
        </div>
      </div>

      <el-form ref="bookingFormRef" :model="bookingForm" :rules="bookingRules" label-position="top">
        <el-form-item label="就诊人姓名" prop="patientName">
          <el-input v-model.trim="bookingForm.patientName" placeholder="请输入就诊人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="patientPhone">
          <el-input v-model.trim="bookingForm.patientPhone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="bookingVisible = false">取消</el-button>
        <el-button type="primary" :loading="bookingLoading" @click="confirmBooking">
          确认预约
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.schedules-page {
  max-width: 1120px;
}

.schedule-toolbar {
  padding: 15px 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.secondary-row {
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #edf2ef;
}

.filter-select {
  width: 180px;
}

.period-select {
  width: 130px;
}

.date-filter {
  width: 270px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.schedule-results {
  min-height: 260px;
  margin-top: 20px;
}

.schedule-group + .schedule-group {
  margin-top: 26px;
}

.group-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding-left: 2px;
}

.group-date {
  font-size: 15px;
  font-weight: 700;
}

.group-count {
  color: var(--color-muted);
  font-size: 12px;
}

.schedule-card {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
  margin-bottom: 10px;
  padding: 17px 18px;
  border: 1px solid var(--color-line);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(21, 55, 47, 0.035);
}

.time-block {
  display: flex;
  min-height: 62px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  border-radius: 7px;
  background: #edf5f2;
  color: #0b5e58;
}

.time-block span {
  font-size: 11px;
  font-weight: 600;
}

.time-block strong {
  margin-top: 4px;
  font-size: 14px;
}

.schedule-main {
  min-width: 0;
}

.schedule-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.schedule-title h2 {
  font-size: 17px;
}

.schedule-title small {
  margin-left: 6px;
  color: #7b8a84;
  font-size: 12px;
  font-weight: 500;
}

.schedule-main > p {
  margin-top: 5px;
  color: #53665f;
  font-size: 13px;
}

.schedule-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  margin-top: 12px;
  color: #7b8a84;
  font-size: 12px;
}

.schedule-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.booking-button {
  min-width: 108px;
}

.booking-summary {
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid #e4ece8;
  border-radius: 7px;
  background: #f7faf8;
}

.booking-summary span {
  display: block;
  color: var(--color-muted);
  font-size: 12px;
}

.booking-summary strong {
  display: block;
  margin-top: 5px;
  font-size: 16px;
}

.booking-time {
  margin-top: 10px;
  color: #53665f;
  font-size: 13px;
}

.booking-fee {
  margin-top: 8px;
  color: #53665f;
  font-size: 13px;
}

.booking-fee b {
  color: #b66a30;
}

@media (max-width: 900px) {
  .filter-row {
    flex-wrap: wrap;
  }

  .filter-select,
  .period-select,
  .date-filter {
    width: calc(50% - 5px);
  }
}

@media (max-width: 680px) {
  .filter-select,
  .period-select,
  .date-filter {
    width: 100%;
  }

  .schedule-card {
    grid-template-columns: 1fr;
    gap: 13px;
  }

  .time-block {
    min-height: auto;
    align-items: flex-start;
    padding: 10px 13px;
  }

  .booking-button {
    width: 100%;
  }
}
</style>
