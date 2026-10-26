<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Calendar,
  DataBoard,
  OfficeBuilding,
  SwitchButton,
  Tickets,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import BrandMark from '@/components/BrandMark.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const user = computed(() => auth.state.user)
const pageTitle = computed(() => route.meta.title || '管理后台')
const initial = computed(() => (user.value?.realName || user.value?.username || 'A').slice(0, 1).toUpperCase())
const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
})

const navItems = [
  { name: 'admin-dashboard', label: '管理概览', icon: DataBoard },
  { name: 'admin-departments', label: '科室管理', icon: OfficeBuilding },
  { name: 'admin-doctors', label: '医生管理', icon: UserFilled },
  { name: 'admin-schedules', label: '排班管理', icon: Calendar },
  { name: 'admin-appointments', label: '预约管理', icon: Tickets },
  { name: 'admin-users', label: '用户管理', icon: User },
]

async function handleCommand(command) {
  if (command !== 'logout') {
    return
  }
  try {
    await ElMessageBox.confirm('确定要退出当前管理员账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await auth.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  } catch {
    // User cancelled.
  }
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <BrandMark light />
        <span class="admin-badge">管理后台</span>
      </div>

      <nav class="admin-nav" aria-label="后台导航">
        <router-link
          v-for="item in navItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="admin-nav-link"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <span>医院预约挂号系统</span>
        <small>Administration</small>
      </div>
    </aside>

    <div class="admin-workspace">
      <header class="admin-header">
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>{{ today }}</p>
        </div>

        <el-dropdown trigger="click" @command="handleCommand">
          <button type="button" class="admin-user">
            <span class="admin-avatar">{{ initial }}</span>
            <span class="admin-user-copy">
              <strong>{{ user?.realName || user?.username }}</strong>
              <small>系统管理员</small>
            </span>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background: #f2f5f3;
}

.admin-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  width: 230px;
  height: 100vh;
  flex: 0 0 auto;
  flex-direction: column;
  padding: 24px 16px 20px;
  background: #102f2b;
  color: #fff;
}

.sidebar-brand {
  padding: 0 6px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.admin-badge {
  display: inline-block;
  margin: 15px 0 0 49px;
  padding: 3px 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.09);
  color: rgba(255, 255, 255, 0.68);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-top: 22px;
}

.admin-nav-link {
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.admin-nav-link:hover {
  background: rgba(255, 255, 255, 0.07);
  color: #fff;
}

.admin-nav-link.router-link-active {
  background: #e9f5f1;
  color: #0b5e58;
  font-weight: 700;
}

.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: auto;
  padding: 16px 8px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
}

.sidebar-footer small {
  letter-spacing: 0.1em;
}

.admin-workspace {
  min-width: 0;
  flex: 1;
}

.admin-header {
  position: sticky;
  z-index: 10;
  top: 0;
  display: flex;
  height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 30px;
  border-bottom: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(12px);
}

.admin-header h1 {
  font-size: 20px;
  line-height: 1.3;
}

.admin-header p {
  margin-top: 4px;
  color: var(--color-muted);
  font-size: 12px;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 7px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
}

.admin-user:hover {
  border-color: var(--color-line);
  background: #f8faf9;
}

.admin-avatar {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  background: #173f39;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.admin-user-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.25;
}

.admin-user-copy strong {
  font-size: 13px;
}

.admin-user-copy small {
  margin-top: 2px;
  color: var(--color-muted);
  font-size: 10px;
}

.admin-main {
  padding: 28px 30px 48px;
}

@media (max-width: 900px) {
  .admin-shell {
    display: block;
  }

  .admin-sidebar {
    position: static;
    width: 100%;
    height: auto;
    padding: 15px 16px;
  }

  .sidebar-brand {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 13px;
  }

  .admin-badge {
    margin: 0;
  }

  .admin-nav {
    overflow-x: auto;
    flex-direction: row;
    margin-top: 13px;
    padding-bottom: 2px;
  }

  .admin-nav-link {
    flex: 0 0 auto;
    white-space: nowrap;
  }

  .sidebar-footer {
    display: none;
  }

  .admin-header {
    padding: 0 20px;
  }

  .admin-main {
    padding: 20px 18px 40px;
  }
}

@media (max-width: 560px) {
  .admin-user-copy {
    display: none;
  }

  .admin-header h1 {
    font-size: 18px;
  }
}
</style>
