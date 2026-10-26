<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import AuthShell from '@/components/AuthShell.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  gender: 0,
  email: '',
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度必须为 4 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须为 6 到 20 个字符', trigger: 'blur' },
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { max: 50, message: '真实姓名不能超过 50 个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
    { max: 100, message: '邮箱不能超过 100 个字符', trigger: 'blur' },
  ],
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
    const user = await auth.register(form)
    ElMessage.success(`注册成功，欢迎你，${user.realName || user.username}`)
    router.replace('/profile')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell
    wide
    eyebrow="CREATE ACCOUNT"
    title="注册账号，轻松就诊。"
    description="建立个人就诊账号后，即可查询医生排班、预约挂号并随时查看预约记录。"
  >
    <div class="auth-heading">
      <p class="form-kicker">新用户注册</p>
      <h2>创建就诊账号</h2>
      <p class="form-subtitle">请填写真实信息，便于后续预约就诊。</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
      <div class="form-grid">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" placeholder="4-20 位字母、数字或下划线" autocomplete="username" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="6-20 位密码"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model.trim="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender" class="gender-group">
            <el-radio :value="0">保密</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="邮箱（选填）" prop="email">
          <el-input v-model.trim="form.email" placeholder="用于接收预约通知" autocomplete="email" />
        </el-form-item>
      </div>

      <el-button class="submit-button" type="primary" :loading="loading" @click="handleSubmit">
        <span>注册并登录</span>
        <el-icon v-if="!loading"><ArrowRight /></el-icon>
      </el-button>
    </el-form>

    <div class="auth-switch">
      已有账号？
      <router-link to="/login">返回登录</router-link>
    </div>
  </AuthShell>
</template>

<style scoped>
.auth-heading {
  margin-bottom: 26px;
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
  font-size: 29px;
  line-height: 1.25;
}

.form-subtitle {
  margin-top: 9px;
  color: var(--color-muted);
  font-size: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.gender-group {
  min-height: 42px;
}

.submit-button {
  width: 100%;
  height: 44px;
  gap: 8px;
  font-size: 15px;
}

.auth-switch {
  margin-top: 22px;
  color: var(--color-muted);
  font-size: 14px;
  text-align: center;
}

.auth-switch a {
  margin-left: 4px;
  color: var(--color-primary);
  font-weight: 700;
}

@media (max-width: 620px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
