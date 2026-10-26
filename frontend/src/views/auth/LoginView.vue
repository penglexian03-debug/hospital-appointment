<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Lock, User } from '@element-plus/icons-vue'
import AuthShell from '@/components/AuthShell.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit() {
  if (loading.value) {
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const user = await auth.login(form)
    ElMessage.success(`欢迎回来，${user.realName || user.username}`)
    const requestedRedirect =
      typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
        ? route.query.redirect
        : null
    const redirect = user.role === 2
      ? (requestedRedirect?.startsWith('/admin') ? requestedRedirect : '/admin')
      : (requestedRedirect && !requestedRedirect.startsWith('/admin')
          ? requestedRedirect
          : '/profile')
    router.replace(redirect)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function fillAccount(username) {
  form.username = username
  form.password = '123456'
}
</script>

<template>
  <AuthShell
    title="预约就医，从这里开始。"
    description="查询合适的科室与医生，查看近期出诊安排，用更从容的方式安排每一次就诊。"
  >
    <div class="auth-heading">
      <p class="form-kicker">欢迎回来</p>
      <h2>登录就诊账号</h2>
      <p class="form-subtitle">输入账号信息，继续管理你的预约。</p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      @keyup.enter="handleSubmit"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model.trim="form.username" placeholder="请输入用户名" autocomplete="username">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
          show-password
        >
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-button class="submit-button" type="primary" :loading="loading" @click="handleSubmit">
        <span>登录</span>
        <el-icon v-if="!loading"><ArrowRight /></el-icon>
      </el-button>
    </el-form>

    <div class="auth-switch">
      还没有账号？
      <router-link to="/register">立即注册</router-link>
    </div>

    <div class="demo-accounts">
      <span>测试账号</span>
      <button type="button" @click="fillAccount('patient')">患者 patient</button>
      <button type="button" @click="fillAccount('admin')">管理员 admin</button>
      <em>密码 123456</em>
    </div>
  </AuthShell>
</template>

<style scoped>
.auth-heading {
  margin-bottom: 30px;
}

.form-kicker {
  margin-bottom: 8px;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.auth-heading h2 {
  color: #17231f;
  font-size: 30px;
  line-height: 1.25;
}

.form-subtitle {
  margin-top: 10px;
  color: var(--color-muted);
  font-size: 14px;
  line-height: 1.6;
}

.submit-button {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  gap: 8px;
  font-size: 15px;
}

.auth-switch {
  margin-top: 24px;
  color: var(--color-muted);
  font-size: 14px;
  text-align: center;
}

.auth-switch a {
  margin-left: 4px;
  color: var(--color-primary);
  font-weight: 700;
}

.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--color-line);
  color: #7b8984;
  font-size: 12px;
}

.demo-accounts button {
  padding: 0;
  border: 0;
  background: none;
  color: #42564f;
  cursor: pointer;
  font-size: 12px;
}

.demo-accounts button:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.demo-accounts em {
  color: #9aa6a1;
  font-style: normal;
}
</style>
