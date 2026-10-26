<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import { getAdminUsersApi, updateUserStatusApi } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime, genderLabel, roleLabel } from '@/utils/display'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive({ keyword: '', role: undefined, status: undefined })
const detailVisible = ref(false)
const detail = ref(null)

const currentUserId = computed(() => auth.state.user?.id)

async function loadUsers() {
  loading.value = true
  try {
    const data = await getAdminUsersApi({
      keyword: filters.keyword.trim() || undefined,
      role: filters.role,
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
  loadUsers()
}

function resetFilters() {
  filters.keyword = ''
  filters.role = undefined
  filters.status = undefined
  page.value = 1
  loadUsers()
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

async function changeStatus(row, status) {
  const action = status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}账号“${row.username}”吗？${status === 0 ? '禁用后该账号将无法登录系统。' : ''}`,
      `${action}账号`,
      { confirmButtonText: action, cancelButtonText: '取消', type: status === 0 ? 'warning' : 'info' },
    )
  } catch {
    return
  }
  try {
    await updateUserStatusApi(row.id, status)
    ElMessage.success(`账号已${action}`)
    await loadUsers()
    if (detail.value?.id === row.id) {
      detail.value = rows.value.find((item) => item.id === row.id) || { ...row, status }
    }
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadUsers)
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-heading">
      <div>
        <h2>系统用户</h2>
        <p>查询患者与管理员账号，并控制账号的登录权限。</p>
      </div>
      <el-tag effect="plain" round>共 {{ total }} 个账号</el-tag>
    </div>

    <section class="surface-card admin-toolbar">
      <el-input
        v-model="filters.keyword"
        class="toolbar-input"
        clearable
        placeholder="搜索账号、姓名或手机号"
        @keyup.enter="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filters.role" class="toolbar-select" clearable placeholder="全部角色">
        <el-option label="患者" :value="1" />
        <el-option label="管理员" :value="2" />
      </el-select>
      <el-select v-model="filters.status" class="toolbar-select" clearable placeholder="全部状态">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetFilters">
        <el-icon><Refresh /></el-icon>
        重置
      </el-button>
    </section>

    <section class="surface-card admin-table-card">
      <div class="admin-table-caption">
        <h3>账号列表</h3>
        <span>患者与管理员统一管理</span>
      </div>
      <el-table v-loading="loading" :data="rows" row-key="id" stripe>
        <el-table-column label="用户" min-width="170">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="34">{{ (row.realName || row.username).slice(0, 1) }}</el-avatar>
              <div>
                <strong>{{ row.realName }}</strong>
                <span class="subtext">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="135" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '--' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 2 ? 'warning' : 'primary'" effect="plain" size="small">
              {{ row.roleName || roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="155" fixed="right">
          <template #default="{ row }">
            <div class="admin-table-actions">
              <el-button link type="primary" @click="openDetail(row)">
                <el-icon><View /></el-icon>
                详情
              </el-button>
              <el-tooltip
                :disabled="!(row.id === currentUserId && row.status === 1)"
                content="不能禁用当前登录账号"
                placement="top"
              >
                <span>
                  <el-button
                    link
                    :type="row.status === 1 ? 'danger' : 'success'"
                    :disabled="row.id === currentUserId && row.status === 1"
                    @click="changeStatus(row, row.status === 1 ? 0 : 1)"
                  >
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                </span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户数据" :image-size="84" />
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
          @current-change="loadUsers"
          @size-change="search"
        />
      </div>
    </section>

    <el-drawer v-model="detailVisible" title="用户详情" size="430px">
      <div v-if="detail">
        <div class="admin-detail-name">
          <span class="admin-detail-avatar">{{ (detail.realName || detail.username).slice(0, 1) }}</span>
          <div>
            <strong>{{ detail.realName }}</strong>
            <span>@{{ detail.username }} · {{ detail.roleName || roleLabel(detail.role) }}</span>
          </div>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '--' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderLabel(detail.gender) }}</el-descriptions-item>
          <el-descriptions-item label="账号状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ detail.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        </el-descriptions>
        <div class="admin-detail-actions">
          <el-button
            :type="detail.status === 1 ? 'danger' : 'success'"
            :disabled="detail.id === currentUserId && detail.status === 1"
            @click="changeStatus(detail, detail.status === 1 ? 0 : 1)"
          >
            {{ detail.status === 1 ? '禁用账号' : '启用账号' }}
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.user-cell strong,
.subtext {
  display: block;
}

.subtext {
  margin-top: 3px;
  color: var(--color-muted);
  font-size: 11px;
}
</style>
