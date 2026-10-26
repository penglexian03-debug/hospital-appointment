<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createDoctorApi,
  getAdminDepartmentsApi,
  getAdminDoctorsApi,
  updateDoctorApi,
  updateDoctorStatusApi,
} from '@/api/admin'
import { genderLabel } from '@/utils/display'

const loading = ref(false)
const rows = ref([])
const departments = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive({ keyword: '', departmentId: undefined, status: undefined })

const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  departmentId: undefined,
  name: '',
  title: '',
  gender: 0,
  specialty: '',
  introduction: '',
  avatar: '',
  status: 1,
})

const rules = {
  departmentId: [{ required: true, message: '请选择所属科室', trigger: 'change' }],
  name: [
    { required: true, message: '请输入医生姓名', trigger: 'blur' },
    { max: 50, message: '医生姓名不能超过 50 个字符', trigger: 'blur' },
  ],
  title: [
    { required: true, message: '请输入医生职称', trigger: 'blur' },
    { max: 30, message: '医生职称不能超过 30 个字符', trigger: 'blur' },
  ],
}

async function loadDepartments() {
  try {
    const data = await getAdminDepartmentsApi({ page: 1, size: 100 })
    departments.value = data.records || []
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadDoctors() {
  loading.value = true
  try {
    const data = await getAdminDoctorsApi({
      keyword: filters.keyword.trim() || undefined,
      departmentId: filters.departmentId,
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
  loadDoctors()
}

function resetFilters() {
  filters.keyword = ''
  filters.departmentId = undefined
  filters.status = undefined
  page.value = 1
  loadDoctors()
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    departmentId: undefined,
    name: '',
    title: '',
    gender: 0,
    specialty: '',
    introduction: '',
    avatar: '',
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
    departmentId: row.departmentId,
    name: row.name,
    title: row.title,
    gender: row.gender ?? 0,
    specialty: row.specialty || '',
    introduction: row.introduction || '',
    avatar: row.avatar || '',
    status: row.status,
  })
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || saving.value) {
    return
  }
  saving.value = true
  const payload = {
    departmentId: form.departmentId,
    name: form.name.trim(),
    title: form.title.trim(),
    gender: Number(form.gender),
    specialty: form.specialty.trim() || null,
    introduction: form.introduction.trim() || null,
    avatar: form.avatar.trim() || null,
    status: Number(form.status),
  }
  try {
    if (dialogMode.value === 'create') {
      await createDoctorApi(payload)
      ElMessage.success('医生档案已创建')
    } else {
      await updateDoctorApi(editingId.value, payload)
      ElMessage.success('医生档案已更新')
    }
    dialogVisible.value = false
    await loadDoctors()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function changeStatus(row, status) {
  const action = status === 1 ? '恢复出诊' : '停诊'
  try {
    await ElMessageBox.confirm(
      `确定要将医生“${row.name}”设为${action}吗？`,
      action,
      { confirmButtonText: action, cancelButtonText: '取消', type: status === 0 ? 'warning' : 'info' },
    )
  } catch {
    return
  }
  try {
    await updateDoctorStatusApi(row.id, status)
    ElMessage.success(`医生已${action}`)
    await loadDoctors()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(async () => {
  await Promise.all([loadDepartments(), loadDoctors()])
})
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-heading">
      <div>
        <h2>医生档案</h2>
        <p>维护医生所属科室、职称、擅长方向与出诊状态。</p>
      </div>
      <div class="heading-actions">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增医生
        </el-button>
      </div>
    </div>

    <section class="surface-card admin-toolbar">
      <el-input
        v-model="filters.keyword"
        class="toolbar-input"
        clearable
        placeholder="搜索姓名或擅长方向"
        @keyup.enter="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select
        v-model="filters.departmentId"
        class="toolbar-select"
        clearable
        filterable
        placeholder="全部科室"
      >
        <el-option v-for="item in departments" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="filters.status" class="toolbar-select" clearable placeholder="全部状态">
        <el-option label="正常出诊" :value="1" />
        <el-option label="停诊" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetFilters">
        <el-icon><Refresh /></el-icon>
        重置
      </el-button>
    </section>

    <section class="surface-card admin-table-card">
      <div class="admin-table-caption">
        <h3>医生列表</h3>
        <span>共 {{ total }} 位医生</span>
      </div>
      <el-table v-loading="loading" :data="rows" row-key="id" stripe>
        <el-table-column label="医生" min-width="190">
          <template #default="{ row }">
            <div class="doctor-cell">
              <el-avatar :size="36" :src="row.avatar || undefined">
                {{ row.name.slice(0, 1) }}
              </el-avatar>
              <div>
                <strong>{{ row.name }}</strong>
                <small>{{ row.title }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="departmentName" label="所属科室" min-width="130" />
        <el-table-column label="性别" width="80" align="center">
          <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="specialty" label="擅长方向" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.specialty || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ row.status === 1 ? '正常出诊' : '停诊' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="admin-table-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button
                link
                :type="row.status === 1 ? 'danger' : 'success'"
                @click="changeStatus(row, row.status === 1 ? 0 : 1)"
              >
                {{ row.status === 1 ? '停诊' : '恢复' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无医生数据" :image-size="84" />
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
          @current-change="loadDoctors"
          @size-change="search"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增医生' : '编辑医生'"
      width="720px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="admin-form-grid">
          <el-form-item label="所属科室" prop="departmentId">
            <el-select v-model="form.departmentId" filterable placeholder="请选择科室">
              <el-option
                v-for="item in departments"
                :key="item.id"
                :label="`${item.name}${item.status === 0 ? '（已停用）' : ''}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="医生姓名" prop="name">
            <el-input v-model.trim="form.name" maxlength="50" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="职称" prop="title">
            <el-input v-model.trim="form.title" maxlength="30" placeholder="例如：主任医师" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio-button :value="0">保密</el-radio-button>
              <el-radio-button :value="1">男</el-radio-button>
              <el-radio-button :value="2">女</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="出诊状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio-button :value="1">正常出诊</el-radio-button>
              <el-radio-button :value="0">停诊</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="头像地址" prop="avatar">
            <el-input v-model.trim="form.avatar" maxlength="255" placeholder="可选，填写图片 URL" />
          </el-form-item>
          <el-form-item class="form-full" label="擅长方向" prop="specialty">
            <el-input
              v-model="form.specialty"
              maxlength="255"
              show-word-limit
              placeholder="例如：高血压、冠心病、心律失常"
            />
          </el-form-item>
          <el-form-item class="form-full" label="医生简介" prop="introduction">
            <el-input
              v-model="form.introduction"
              type="textarea"
              :rows="5"
              maxlength="2000"
              show-word-limit
              placeholder="填写医生的执业经历和专业背景"
            />
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
.doctor-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.doctor-cell strong,
.doctor-cell small {
  display: block;
}

.doctor-cell small {
  margin-top: 3px;
  color: var(--color-muted);
  font-size: 11px;
}
</style>
