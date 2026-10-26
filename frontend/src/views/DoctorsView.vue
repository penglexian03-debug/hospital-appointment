<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Search } from '@element-plus/icons-vue'
import { getDepartmentsApi, getDoctorDetailApi, getDoctorsApi } from '@/api/catalog'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const departments = ref([])
const doctors = ref([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const filters = reactive({
  departmentId: route.query.departmentId ? Number(route.query.departmentId) : undefined,
  keyword: '',
})

const currentDepartmentName = computed(() => {
  if (!filters.departmentId) {
    return '全部科室'
  }
  return departments.value.find((item) => item.id === filters.departmentId)?.name || '当前科室'
})

async function loadDepartments() {
  try {
    departments.value = await getDepartmentsApi()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadDoctors() {
  loading.value = true
  try {
    doctors.value = await getDoctorsApi({
      departmentId: filters.departmentId || undefined,
      keyword: filters.keyword.trim() || undefined,
    })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function search() {
  loadDoctors()
}

function resetFilters() {
  filters.departmentId = undefined
  filters.keyword = ''
  loadDoctors()
}

async function openDetail(doctor) {
  detail.value = doctor
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getDoctorDetailApi(doctor.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    detailLoading.value = false
  }
}

function viewSchedules(doctor) {
  router.push({
    name: 'schedules',
    query: { doctorId: doctor.id, departmentId: doctor.departmentId },
  })
}

onMounted(async () => {
  await Promise.all([loadDepartments(), loadDoctors()])
})
</script>

<template>
  <div class="page-container doctors-page">
    <div class="page-heading">
      <div>
        <h1>医生团队</h1>
        <p>按科室或姓名查找医生，查看擅长方向与近期排班。</p>
      </div>
      <el-tag effect="plain" round>{{ currentDepartmentName }} · {{ doctors.length }} 位医生</el-tag>
    </div>

    <section class="surface-card catalog-toolbar">
      <el-select v-model="filters.departmentId" clearable placeholder="选择科室" class="department-select">
        <el-option v-for="department in departments" :key="department.id" :label="department.name" :value="department.id" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        clearable
        placeholder="搜索医生姓名或擅长方向"
        class="keyword-input"
        @keyup.enter="search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </section>

    <section v-loading="loading" class="doctor-grid">
      <el-empty v-if="!loading && doctors.length === 0" description="没有找到相关医生" :image-size="96" />

      <article v-for="doctor in doctors" :key="doctor.id" class="doctor-card">
        <div class="doctor-head">
          <span class="doctor-avatar">{{ doctor.name.slice(0, 1) }}</span>
          <div class="doctor-identity">
            <div class="doctor-name-row">
              <h2>{{ doctor.name }}</h2>
              <el-tag size="small" effect="plain">{{ doctor.title }}</el-tag>
            </div>
            <p>{{ doctor.departmentName }}</p>
          </div>
        </div>

        <div class="doctor-specialty">
          <span>擅长方向</span>
          <p>{{ doctor.specialty || '暂无擅长方向说明' }}</p>
        </div>

        <p class="doctor-intro">{{ doctor.introduction || '暂无医生简介' }}</p>

        <div class="doctor-actions">
          <el-button text type="primary" @click="openDetail(doctor)">查看简介</el-button>
          <el-button type="primary" plain @click="viewSchedules(doctor)">
            <el-icon><Calendar /></el-icon>
            查看排班
          </el-button>
        </div>
      </article>
    </section>

    <el-drawer v-model="detailVisible" title="医生简介" size="440px">
      <div v-loading="detailLoading" class="doctor-detail">
        <template v-if="detail">
          <div class="detail-doctor-head">
            <span class="detail-avatar">{{ detail.name.slice(0, 1) }}</span>
            <div>
              <h2>{{ detail.name }}</h2>
              <p>{{ detail.title }} · {{ detail.departmentName }}</p>
            </div>
          </div>

          <div class="detail-section">
            <h3>擅长方向</h3>
            <p>{{ detail.specialty || '暂无擅长方向说明' }}</p>
          </div>

          <div class="detail-section">
            <h3>医生简介</h3>
            <p>{{ detail.introduction || '暂无医生简介' }}</p>
          </div>

          <el-button class="drawer-action" type="primary" @click="viewSchedules(detail)">
            查看近期排班
          </el-button>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.doctors-page {
  max-width: 1120px;
}

.catalog-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
}

.department-select {
  width: 180px;
}

.keyword-input {
  width: min(340px, 100%);
}

.doctor-grid {
  display: grid;
  min-height: 240px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.doctor-card {
  display: flex;
  min-height: 270px;
  flex-direction: column;
  padding: 22px;
  border: 1px solid var(--color-line);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 7px 24px rgba(21, 55, 47, 0.035);
}

.doctor-head {
  display: flex;
  align-items: center;
  gap: 13px;
}

.doctor-avatar,
.detail-avatar {
  display: grid;
  width: 52px;
  height: 52px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  background: #173f39;
  color: #fff;
  font-size: 19px;
  font-weight: 700;
}

.doctor-identity {
  min-width: 0;
}

.doctor-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.doctor-name-row h2 {
  font-size: 18px;
}

.doctor-identity p {
  margin-top: 5px;
  color: var(--color-muted);
  font-size: 12px;
}

.doctor-specialty {
  margin-top: 18px;
  padding: 12px 14px;
  border-radius: 6px;
  background: #f4f8f6;
}

.doctor-specialty span {
  color: #7b8a84;
  font-size: 11px;
  font-weight: 600;
}

.doctor-specialty p {
  margin-top: 5px;
  color: #3f534c;
  font-size: 13px;
  line-height: 1.55;
}

.doctor-intro {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 13px;
  color: #6b7b75;
  font-size: 12px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.doctor-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #edf2ef;
}

.doctor-detail {
  min-height: 220px;
}

.detail-doctor-head {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 18px;
  border-radius: 8px;
  background: #f5f9f7;
}

.detail-doctor-head h2 {
  font-size: 20px;
}

.detail-doctor-head p {
  margin-top: 5px;
  color: var(--color-muted);
  font-size: 13px;
}

.detail-section {
  margin-top: 24px;
}

.detail-section h3 {
  margin-bottom: 8px;
  font-size: 14px;
}

.detail-section p {
  color: #5b6c65;
  font-size: 13px;
  line-height: 1.8;
}

.drawer-action {
  width: 100%;
  margin-top: 28px;
}

@media (max-width: 860px) {
  .catalog-toolbar {
    flex-wrap: wrap;
  }

  .department-select,
  .keyword-input {
    width: 100%;
  }
}

@media (max-width: 680px) {
  .doctor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
