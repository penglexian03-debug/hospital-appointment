<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createScheduleApi,
  getAdminDepartmentsApi,
  getAdminDoctorsApi,
  getAdminSchedulesApi,
  updateScheduleApi,
  updateScheduleStatusApi,
} from '@/api/admin'
import { formatMoney, formatTimeRange, periodLabel } from '@/utils/display'

const loading = ref(false)
const rows = ref([])
const departments = ref([])
const doctors = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive({
  doctorId: undefined,
  departmentId: undefined,
  dateRange: [],
  period: undefined,
  status: undefined,
})

const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  doctorId: undefined,
  workDate: '',
  period: 1,
  startTime: '08:00:00',
  endTime: '12:00:00',
  totalSlots: 20,
  registrationFee: 30,
  status: 1,
})

const filteredDoctors = computed(() => {
  if (!filters.departmentId) {
    return doctors.value
  }
  return doctors.value.filter((item) => item.departmentId === filters.departmentId)
})

function validateEndTime(rule, value, callback) {
  if (!value || !form.startTime) {
    callback()
    return
  }
  if (value <= form.startTime) {
    callback(new Error('结束时间必须晚于开始时间'))
    return
  }
  callback()
}

const rules = {
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择出诊日期', trigger: 'change' }],
  period: [{ required: true, message: '请选择出诊时段', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    { validator: validateEndTime, trigger: 'change' },
  ],
  totalSlots: [{ required: true, message: '请输入总号源', trigger: 'change' }],
  registrationFee: [{ required: true, message: '请输入挂号费', trigger: 'change' }],
}

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

async function loadSchedules() {
  loading.value = true
  try {
    const data = await getAdminSchedulesApi({
      doctorId: filters.doctorId,
      departmentId: filters.departmentId,
      startDate: filters.dateRange?.[0] || undefined,
      endDate: filters.dateRange?.[1] || undefined,
      period: filters.period,
      status: filters.status,
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
  loadSchedules()
}

function resetFilters() {
  filters.doctorId = undefined
  filters.departmentId = undefined
  filters.dateRange = []
  filters.period = undefined
  filters.status = undefined
  page.value = 1
  loadSchedules()
}

function handleDepartmentFilterChange() {
  if (
    filters.doctorId &&
    !filteredDoctors.value.some((item) => item.id === filters.doctorId)
  ) {
    filters.doctorId = undefined
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    doctorId: undefined,
    workDate: '',
    period: 1,
    startTime: '08:00:00',
    endTime: '12:00:00',
    totalSlots: 20,
    registrationFee: 30,
    status: 1,
  })
  formRef.value?.clearValidate()
}

async function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function openEdit(row) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, {
    doctorId: row.doctorId,
    workDate: row.workDate,
    period: row.period,
    startTime: row.startTime,
    endTime: row.endTime,
    totalSlots: row.totalSlots,
    registrationFee: Number(row.registrationFee || 0),
    status: row.status,
  })
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

function applyPeriodTime() {
  if (form.period === 1) {
    form.startTime = '08:00:00'
    form.endTime = '12:00:00'
  } else {
    form.startTime = '14:00:00'
    form.endTime = '17:30:00'
  }
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || saving.value) {
    return
  }
  saving.value = true
  const payload = {
    doctorId: form.doctorId,
    workDate: form.workDate,
    period: Number(form.period),
    startTime: form.startTime,
    endTime: form.endTime,
    totalSlots: Number(form.totalSlots),
    registrationFee: Number(form.registrationFee),
    status: Number(form.status),
  }
  try {
    if (dialogMode.value === 'create') {
      await createScheduleApi(payload)
      ElMessage.success('排班已创建')
    } else {
      await updateScheduleApi(editingId.value, payload)
      ElMessage.success('排班已更新')
    }
    dialogVisible.value = false
    await loadSchedules()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function changeStatus(row, status) {
  const action = status === 1 ? '开放' : '停诊'
  try {
    await ElMessageBox.confirm(
      `确定要将 ${row.workDate} ${periodLabel(row.period)} 的排班设为${action}吗？`,
      `${action}排班`,
      { confirmButtonText: action, cancelButtonText: '取消', type: status === 0 ? 'warning' : 'info' },
    )
  } catch {
    return
  }
  try {
    await updateScheduleStatusApi(row.id, status)
    ElMessage.success(`排班已${action}`)
    await loadSchedules()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(async () => {
  await Promise.all([loadOptions(), loadSchedules()])
})
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-heading">
      <div>
        <h2>排班计划</h2>
        <p>安排医生出诊日期、时段、号源数量和挂号费用。</p>
      </div>
      <div class="heading-actions">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增排班
        </el-button>
      </div>
    </div>

    <section class="surface-card admin-toolbar">
      <el-select
        v-model="filters.departmentId"
        class="toolbar-select"
        clearable
        filterable
        placeholder="全部科室"
        @change="handleDepartmentFilterChange"
      >
        <el-option v-for="item in departments" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="filters.doctorId" class="toolbar-select" clearable filterable placeholder="全部医生">
        <el-option
          v-for="item in filteredDoctors"
          :key="item.id"
          :label="`${item.name} · ${item.title}`"
          :value="item.id"
        />
      </el-select>
      <el-date-picker
        v-model="filters.dateRange"
        class="toolbar-date"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        unlink-panels
      />
      <el-select v-model="filters.period" class="toolbar-select" clearable placeholder="全部时段">
        <el-option label="上午" :value="1" />
        <el-option label="下午" :value="2" />
      </el-select>
      <el-select v-model="filters.status" class="toolbar-select" clearable placeholder="全部状态">
        <el-option label="开放" :value="1" />
        <el-option label="停诊" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
      <el-button @click="resetFilters">
        <el-icon><Refresh /></el-icon>
        重置
      </el-button>
    </section>

    <section class="surface-card admin-table-card">
      <div class="admin-table-caption">
        <h3>排班列表</h3>
        <span>共 {{ total }} 条排班</span>
      </div>
      <el-table v-loading="loading" :data="rows" row-key="id" stripe>
        <el-table-column prop="workDate" label="出诊日期" width="120" />
        <el-table-column label="时段" width="90" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ periodLabel(row.period) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="医生" min-width="180">
          <template #default="{ row }">
            <strong>{{ row.doctorName }}</strong>
            <span class="subtext">{{ row.doctorTitle }} · {{ row.departmentName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="出诊时间" width="150">
          <template #default="{ row }">{{ formatTimeRange(row.startTime, row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="号源" width="130" align="center">
          <template #default="{ row }">
            <strong :class="{ 'slots-low': row.availableSlots <= 3 }">{{ row.availableSlots }}</strong>
            <span class="subtext">/ {{ row.totalSlots }} 个</span>
          </template>
        </el-table-column>
        <el-table-column label="挂号费" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.registrationFee) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ row.status === 1 ? '开放' : '停诊' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="admin-table-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button
                link
                :type="row.status === 1 ? 'danger' : 'success'"
                @click="changeStatus(row, row.status === 1 ? 0 : 1)"
              >
                {{ row.status === 1 ? '停诊' : '开放' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无排班数据" :image-size="84" />
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
          @current-change="loadSchedules"
          @size-change="search"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增排班' : '编辑排班'"
      width="680px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="admin-form-grid">
          <el-form-item class="form-full" label="出诊医生" prop="doctorId">
            <el-select v-model="form.doctorId" filterable placeholder="请选择医生">
              <el-option
                v-for="item in doctors"
                :key="item.id"
                :label="`${item.name} · ${item.title} · ${item.departmentName}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="出诊日期" prop="workDate">
            <el-date-picker
              v-model="form.workDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
            />
          </el-form-item>
          <el-form-item label="出诊时段" prop="period">
            <el-radio-group v-model="form.period" @change="applyPeriodTime">
              <el-radio-button :value="1">上午</el-radio-button>
              <el-radio-button :value="2">下午</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="开始时间" prop="startTime">
            <el-time-picker
              v-model="form.startTime"
              value-format="HH:mm:ss"
              format="HH:mm"
              :clearable="false"
              placeholder="开始时间"
            />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-time-picker
              v-model="form.endTime"
              value-format="HH:mm:ss"
              format="HH:mm"
              :clearable="false"
              placeholder="结束时间"
            />
          </el-form-item>
          <el-form-item label="总号源" prop="totalSlots">
            <el-input-number v-model="form.totalSlots" :min="1" :max="999" controls-position="right" />
          </el-form-item>
          <el-form-item label="挂号费（元）" prop="registrationFee">
            <el-input-number
              v-model="form.registrationFee"
              :min="0"
              :max="99999999"
              :precision="2"
              :step="5"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item class="form-full" label="排班状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio-button :value="1">开放预约</el-radio-button>
              <el-radio-button :value="0">停诊</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.subtext {
  display: block;
  margin-top: 3px;
  color: var(--color-muted);
  font-size: 11px;
}

.slots-low {
  color: #c05621;
}
</style>
