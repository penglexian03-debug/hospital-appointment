<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Message, Phone, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const formRef = ref()
const saving = ref(false)

const form = reactive({
  realName: '',
  phone: '',
  gender: 0,
  email: '',
})

const rules = {
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

const user = computed(() => auth.state.user)
const initial = computed(() => {
  const name = user.value?.realName || user.value?.username || 'U'
  return name.slice(0, 1).toUpperCase()
})
const roleLabel = computed(() => (user.value?.role === 2 ? '管理员' : '患者'))
const genderLabel = computed(() => {
  const labels = { 0: '保密', 1: '男', 2: '女' }
  return labels[user.value?.gender] || '未设置'
})

watch(
  user,
  (value) => {
    if (!value) {
      return
    }
    form.realName = value.realName || ''
    form.phone = value.phone || ''
    form.gender = value.gender ?? 0
    form.email = value.email || ''
  },
  { immediate: true, deep: true },
)

function formatDate(value) {
  if (!value) {
    return '—'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

async function handleSave() {
  if (saving.value) {
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    await auth.updateProfile(form)
    ElMessage.success('个人资料已更新')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-container profile-page">
    <div class="page-heading">
      <div>
        <h1>个人中心</h1>
        <p>查看账号信息并维护用于就诊联系的基本资料。</p>
      </div>
      <el-tag type="success" effect="light" round>账号正常</el-tag>
    </div>

    <div class="profile-grid">
      <aside class="surface-card identity-card">
        <div class="identity-top">
          <span class="profile-avatar">{{ initial }}</span>
          <h2>{{ user?.realName || user?.username }}</h2>
          <p>@{{ user?.username }}</p>
          <el-tag :type="user?.role === 2 ? 'warning' : 'success'" effect="plain" size="small">
            {{ roleLabel }}
          </el-tag>
        </div>

        <div class="identity-details">
          <div class="detail-row">
            <span>手机号</span>
            <strong>{{ user?.phone || '未设置' }}</strong>
          </div>
          <div class="detail-row">
            <span>性别</span>
            <strong>{{ genderLabel }}</strong>
          </div>
          <div class="detail-row">
            <span>邮箱</span>
            <strong>{{ user?.email || '未设置' }}</strong>
          </div>
          <div class="detail-row">
            <span>注册时间</span>
            <strong>{{ formatDate(user?.createTime) }}</strong>
          </div>
        </div>

        <div class="identity-note">
          <el-icon><User /></el-icon>
          <span>账号信息仅用于预约与就诊联系。</span>
        </div>
      </aside>

      <section class="surface-card profile-form-card">
        <div class="card-heading">
          <div>
            <h2>基本资料</h2>
            <p>修改后会在下一次预约中使用最新信息。</p>
          </div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
          <div class="form-grid">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model.trim="form.realName" placeholder="请输入真实姓名">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model.trim="form.phone" placeholder="请输入手机号" maxlength="11">
                <template #prefix>
                  <el-icon><Phone /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender" class="gender-group">
                <el-radio :value="0">保密</el-radio>
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model.trim="form.email" placeholder="请输入邮箱">
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>

          <div class="readonly-block">
            <div>
              <span>登录账号</span>
              <strong>{{ user?.username }}</strong>
            </div>
            <div>
              <span>账号角色</span>
              <strong>{{ roleLabel }}</strong>
            </div>
          </div>

          <div class="form-actions">
            <el-button type="primary" :loading="saving" @click="handleSave">
              <el-icon v-if="!saving"><Check /></el-icon>
              保存修改
            </el-button>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 1120px;
}

.profile-grid {
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.identity-card {
  overflow: hidden;
}

.identity-top {
  display: flex;
  align-items: center;
  flex-direction: column;
  padding: 34px 28px 26px;
  border-bottom: 1px solid var(--color-line);
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.09), rgba(217, 131, 63, 0.04)),
    #fbfdfc;
  text-align: center;
}

.profile-avatar {
  display: grid;
  width: 76px;
  height: 76px;
  place-items: center;
  margin-bottom: 16px;
  border: 4px solid #fff;
  border-radius: 50%;
  background: #173f39;
  box-shadow: 0 6px 20px rgba(23, 63, 57, 0.2);
  color: #fff;
  font-size: 27px;
  font-weight: 700;
}

.identity-top h2 {
  font-size: 20px;
  line-height: 1.3;
}

.identity-top p {
  margin: 5px 0 13px;
  color: var(--color-muted);
  font-size: 13px;
}

.identity-details {
  padding: 10px 26px 14px;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 13px 0;
  border-bottom: 1px solid #edf2ef;
  font-size: 13px;
}

.detail-row:last-child {
  border-bottom: 0;
}

.detail-row span {
  flex: 0 0 auto;
  color: var(--color-muted);
}

.detail-row strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
  text-align: right;
}

.identity-note {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 20px 20px;
  padding: 11px 12px;
  border-radius: 6px;
  background: #f3f7f5;
  color: #687872;
  font-size: 12px;
  line-height: 1.5;
}

.profile-form-card {
  padding: 28px 30px 30px;
}

.card-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 19px;
  border-bottom: 1px solid var(--color-line);
}

.card-heading h2 {
  font-size: 19px;
  line-height: 1.35;
}

.card-heading p {
  margin-top: 6px;
  color: var(--color-muted);
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.gender-group {
  min-height: 42px;
}

.readonly-block {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 4px;
  padding: 17px 18px;
  border: 1px solid #e5ece8;
  border-radius: 6px;
  background: #f8faf9;
}

.readonly-block div {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.readonly-block span {
  color: var(--color-muted);
  font-size: 12px;
}

.readonly-block strong {
  font-size: 14px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 26px;
  padding-top: 22px;
  border-top: 1px solid var(--color-line);
}

.form-actions .el-button {
  min-width: 124px;
  height: 42px;
  gap: 6px;
}

@media (max-width: 860px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .identity-card {
    display: grid;
    grid-template-columns: 260px minmax(0, 1fr);
  }

  .identity-top {
    border-right: 1px solid var(--color-line);
    border-bottom: 0;
  }

  .identity-note {
    align-self: end;
    grid-column: 2;
  }
}

@media (max-width: 620px) {
  .profile-form-card {
    padding: 22px 20px 24px;
  }

  .identity-card {
    display: block;
  }

  .identity-top {
    border-right: 0;
    border-bottom: 1px solid var(--color-line);
  }

  .form-grid,
  .readonly-block {
    grid-template-columns: 1fr;
  }
}
</style>
