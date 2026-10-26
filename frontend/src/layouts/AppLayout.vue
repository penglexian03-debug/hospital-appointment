<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Avatar,
  Calendar,
  DataBoard,
  OfficeBuilding,
  SwitchButton,
  Tickets,
  UserFilled,
} from '@element-plus/icons-vue'
import BrandMark from '@/components/BrandMark.vue'
import AiAssistant from '@/components/AiAssistant.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const user = computed(() => auth.state.user)
const initial = computed(() => {
  const name = user.value?.realName || user.value?.username || 'U'
  return name.slice(0, 1).toUpperCase()
})
const roleLabel = computed(() => (user.value?.role === 2 ? '管理员' : '患者'))

async function handleCommand(command) {
  if (command !== 'logout') {
    return
  }
  try {
    await ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await auth.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  } catch {
    // User cancelled the confirmation dialog.
  }
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="header-inner">
        <router-link to="/profile" class="brand-link" aria-label="返回个人中心">
          <BrandMark compact />
        </router-link>

        <nav class="main-nav" aria-label="主导航">
          <router-link v-if="user?.role === 2" to="/admin" class="nav-link" title="管理后台">
            <el-icon><DataBoard /></el-icon>
            <span>管理后台</span>
          </router-link>
          <router-link v-if="user?.role === 1" to="/departments" class="nav-link" title="科室导航">
            <el-icon><OfficeBuilding /></el-icon>
            <span>科室导航</span>
          </router-link>
          <router-link v-if="user?.role === 1" to="/doctors" class="nav-link" title="医生团队">
            <el-icon><UserFilled /></el-icon>
            <span>医生团队</span>
          </router-link>
          <router-link v-if="user?.role === 1" to="/schedules" class="nav-link" title="预约挂号">
            <el-icon><Calendar /></el-icon>
            <span>预约挂号</span>
          </router-link>
          <router-link v-if="user?.role === 1" to="/appointments" class="nav-link" title="我的预约">
            <el-icon><Tickets /></el-icon>
            <span>我的预约</span>
          </router-link>
          <router-link to="/profile" class="nav-link" title="个人中心">
            <el-icon><Avatar /></el-icon>
            <span>个人中心</span>
          </router-link>
        </nav>

        <el-dropdown trigger="click" @command="handleCommand">
          <button type="button" class="user-trigger">
            <span class="user-avatar">{{ initial }}</span>
            <span class="user-copy">
              <strong>{{ user?.realName || user?.username }}</strong>
              <small>{{ roleLabel }}</small>
            </span>
            <el-icon class="trigger-arrow"><ArrowDown /></el-icon>
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
      </div>
    </header>

    <main class="app-main">
      <router-view />
    </main>

    <AiAssistant v-if="user?.role === 1" />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    linear-gradient(rgba(15, 118, 110, 0.022) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 118, 110, 0.022) 1px, transparent 1px),
    var(--color-canvas);
  background-size: 36px 36px;
}

.app-header {
  position: sticky;
  z-index: 20;
  top: 0;
  border-bottom: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(12px);
}

.header-inner {
  display: flex;
  width: min(1180px, calc(100% - 48px));
  height: 66px;
  align-items: center;
  gap: 34px;
  margin: 0 auto;
}

.brand-link {
  flex: 0 0 auto;
}

.main-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-inner > .el-dropdown {
  margin-left: auto;
}

.nav-link {
  display: flex;
  min-height: 36px;
  align-items: center;
  gap: 7px;
  padding: 0 13px;
  border-radius: 6px;
  color: #52645e;
  font-size: 14px;
  font-weight: 600;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.nav-link:hover,
.nav-link.router-link-active {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 7px 5px 5px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--color-ink);
  cursor: pointer;
  text-align: left;
}

.user-trigger:hover {
  border-color: var(--color-line);
  background: #f7faf8;
}

.user-avatar {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  background: #173f39;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.user-copy {
  display: flex;
  min-width: 76px;
  flex-direction: column;
  line-height: 1.25;
}

.user-copy strong {
  overflow: hidden;
  max-width: 120px;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.user-copy small {
  margin-top: 2px;
  color: var(--color-muted);
  font-size: 11px;
}

.trigger-arrow {
  color: #87958f;
  font-size: 12px;
}

.app-main {
  min-height: calc(100vh - 66px);
}

@media (max-width: 720px) {
  .header-inner {
    width: calc(100% - 28px);
    gap: 14px;
  }

  .main-nav {
    gap: 2px;
  }

  .nav-link {
    padding: 0 7px;
  }

  .nav-link span {
    display: none;
  }

  .user-copy {
    display: none;
  }
}
</style>
