<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createDepartmentApi,
  getAdminDepartmentsApi,
  updateDepartmentApi,
  updateDepartmentStatusApi,
} from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive({ keyword: '', status: undefined })

const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  name: '',
  code: '',
  description: '',
  location: '',
  phone: '',
  sortOrder: 0,
  status: 1,
})

const rules = {
  name: [
    { required: true, message: '请输入科室名称', trigger: 'blur' },
    { max: 50, message: '科室名称不能超过 50 个字符', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入科室编码', trigger: 'blur' },
    { pattern: /^[A-Z0-9_]+$/, message: '编码只能包含大写字母、数字和下划线', trigger: 'blur' },
    { max: 20, message: '科室编码不能超过 20 个字符', trigger: 'blur' },
  ],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }],
}

async function loadDepartments() {
  loading.value = true
  try {
    const data = await getAdminDepartmentsApi({
      keyword: filters.keyword.trim() || undefined,
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
  loadDepartments()
}

function resetFilters() {
  filters.keyword = ''
  filters.status = undefined
  page.value = 1
  loadDepartments()
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    code: '',
    description: '',
    location: '',
    phone: '',
    sortOrder: 0,
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
    name: row.name,
    code: row.code,
    description: row.description || '',
    location: row.location || '',
    phone: row.phone || '',
    sortOrder: row.sortOrder ?? 0,
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
    name: form.name.trim(),
    code: form.code.trim(),
    description: form.description.trim() || null,
    location: form.location.trim() || null,
    phone: form.phone.trim() || null,
    sortOrder: Number(form.sortOrder) || 0,
    status: Number(form.status),
  }
  try {
    if (dialogMode.value === 'create') {
      await createDepartmentApi(payload)
      ElMessage.success('科室已创建')
    } else {
      await updateDepartmentApi(editingId.value, payload)
      ElMessage.success('科室信息已更新')
    }
    dialogVisible.value = false
    await loadDepartments()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function changeStatus(row, status) {
  const action = status === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}科室“${row.name}”吗？${status === 0 ? '停用后患者将无法看到该科室。' : ''}`,
      `${action}科室`,
      { confirmButtonText: action, cancelButtonText: '取消', type: status === 0 ? 'warning' : 'info' },
    )
  } catch {
    return
  }
  try {
    await updateDepartmentStatusApi(row.id, status)
    ElMessage.success(`科室已${action}`)
    await loadDepartments()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadDepartments)
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-heading">
      <div>
        <h2>科室维护</h2>
        <p>维护科室基础信息、就诊位置和对外展示状态。</p>
      </div>
      <div class="heading-actions">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增科室
        </el-button>
      </div>
    </div>

    <section class="surface-card admin-toolbar">
      <el-input
        v-model="filters.keyword"
        class="toolbar-input"
        clearable
        placeholder="搜索科室名称、编码或位置"
        @keyup.enter="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filters.status" class="toolbar-select" clearable placeholder="全部状态">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetFilters">
        <el-icon><Refresh /></el-icon>
        重置
      </el-button>
    </section>

    <section class="surface-card admin-table-card">
      <div class="admin-table-caption">
        <h3>科室列表</h3>
        <span>共 {{ total }} 个科室</span>
      </div>
      <el-table v-loading="loading" :data="rows" row-key="id" stripe>
        <el-table-column prop="name" label="科室名称" min-width="150">
          <template #default="{ row }">
            <strong>{{ row.name }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="科室编码" width="130" />
        <el-table-column prop="location" label="就诊位置" min-width="150">
          <template #default="{ row }">{{ row.location || '--' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="140">
          <template #default="{ row }">{{ row.phone || '--' }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
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
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无科室数据" :image-size="84" />
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
          @current-change="loadDepartments"
          @size-change="search"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增科室' : '编辑科室'"
      width="620px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="admin-form-grid">
          <el-form-item label="科室名称" prop="name">
            <el-input v-model.trim="form.name" maxlength="50" placeholder="例如：心血管内科" />
          </el-form-item>
          <el-form-item label="科室编码" prop="code">
            <el-input v-model.trim="form.code" maxlength="20" placeholder="例如：CARDIOLOGY" />
          </el-form-item>
          <el-form-item label="就诊位置" prop="location">
            <el-input v-model.trim="form.location" maxlength="100" placeholder="例如：门诊楼 3 层" />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model.trim="form.phone" maxlength="20" placeholder="可选" />
          </el-form-item>
          <el-form-item label="排序值" prop="sortOrder">
            <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio-button :value="1">启用</el-radio-button>
              <el-radio-button :value="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item class="form-full" label="科室简介" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="简要说明科室诊疗范围"
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
