<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Location, Phone, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getDepartmentsApi } from '@/api/catalog'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const departments = ref([])

const filteredDepartments = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) {
    return departments.value
  }
  return departments.value.filter((item) =>
    [item.name, item.code, item.description, item.location]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
  )
})

async function loadDepartments() {
  loading.value = true
  try {
    departments.value = await getDepartmentsApi()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function viewDoctors(department) {
  router.push({ name: 'doctors', query: { departmentId: department.id } })
}

onMounted(loadDepartments)
</script>

<template>
  <div class="page-container departments-page">
    <div class="page-heading">
      <div>
        <h1>科室导航</h1>
        <p>先选择就诊科室，再查看该科室的医生与出诊安排。</p>
      </div>
      <el-tag effect="plain" round>共 {{ departments.length }} 个科室</el-tag>
    </div>

    <section class="surface-card catalog-toolbar">
      <el-input v-model="keyword" clearable placeholder="搜索科室名称、位置或简介">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <span class="toolbar-note">显示 {{ filteredDepartments.length }} 个科室</span>
    </section>

    <section v-loading="loading" class="department-grid">
      <el-empty
        v-if="!loading && filteredDepartments.length === 0"
        description="没有找到相关科室"
        :image-size="96"
      />

      <article
        v-for="department in filteredDepartments"
        :key="department.id"
        class="department-card"
        @click="viewDoctors(department)"
      >
        <div class="department-head">
          <span class="department-icon">{{ department.name.slice(0, 1) }}</span>
          <div>
            <h2>{{ department.name }}</h2>
            <p>{{ department.code }}</p>
          </div>
        </div>

        <p class="department-description">{{ department.description || '暂无科室简介' }}</p>

        <div class="department-meta">
          <span>
            <el-icon><Location /></el-icon>
            {{ department.location || '位置待完善' }}
          </span>
          <span>
            <el-icon><Phone /></el-icon>
            {{ department.phone || '电话待完善' }}
          </span>
        </div>

        <div class="department-footer">
          <span>查看科室医生</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.departments-page {
  max-width: 1120px;
}

.catalog-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
}

.catalog-toolbar .el-input {
  width: min(380px, 100%);
}

.toolbar-note {
  color: var(--color-muted);
  font-size: 13px;
}

.department-grid {
  display: grid;
  min-height: 240px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.department-card {
  display: flex;
  min-height: 258px;
  flex-direction: column;
  padding: 22px;
  border: 1px solid var(--color-line);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 7px 24px rgba(21, 55, 47, 0.035);
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.department-card:hover {
  border-color: #b5d3cb;
  box-shadow: 0 12px 30px rgba(21, 55, 47, 0.08);
  transform: translateY(-2px);
}

.department-head {
  display: flex;
  align-items: center;
  gap: 13px;
}

.department-icon {
  display: grid;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
  font-size: 19px;
  font-weight: 700;
}

.department-head h2 {
  font-size: 18px;
}

.department-head p {
  margin-top: 4px;
  color: #8a9892;
  font-size: 11px;
  letter-spacing: 0.06em;
}

.department-description {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 18px;
  color: #586a63;
  font-size: 13px;
  line-height: 1.75;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.department-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 17px;
  color: #7b8a84;
  font-size: 12px;
}

.department-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.department-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 18px;
  border-top: 1px solid #edf2ef;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 940px) {
  .department-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .catalog-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .department-grid {
    grid-template-columns: 1fr;
  }
}
</style>
