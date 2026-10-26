<script setup>
import { Check } from '@element-plus/icons-vue'
import BrandMark from './BrandMark.vue'
import authBackground from '@/assets/hospital-login-2.jpg'

defineProps({
  eyebrow: {
    type: String,
    default: 'PATIENT PORTAL',
  },
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
  wide: {
    type: Boolean,
    default: false,
  },
})

const features = ['查询科室与医生', '查看近期出诊安排', '在线预约与取消']
</script>

<template>
  <main class="auth-view">
    <section class="auth-shell" :class="{ wide }">
      <div
        class="auth-visual"
        :style="{ backgroundImage: `linear-gradient(90deg, rgba(8, 44, 39, 0.94) 0%, rgba(8, 44, 39, 0.76) 55%, rgba(8, 44, 39, 0.42) 100%), url(${authBackground})` }"
      >
        <BrandMark light />

        <div class="auth-story">
          <p class="auth-eyebrow">{{ eyebrow }}</p>
          <h1>{{ title }}</h1>
          <p class="auth-description">{{ description }}</p>
        </div>

        <ul class="auth-features">
          <li v-for="feature in features" :key="feature">
            <span class="feature-icon"><el-icon><Check /></el-icon></span>
            {{ feature }}
          </li>
        </ul>
      </div>

      <div class="auth-panel">
        <div class="auth-form-inner">
          <slot />
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-view {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 30px;
  background:
    linear-gradient(rgba(15, 118, 110, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 118, 110, 0.035) 1px, transparent 1px),
    #edf3f0;
  background-size: 32px 32px;
}

.auth-shell {
  display: grid;
  width: min(1120px, 100%);
  min-height: 690px;
  grid-template-columns: 1.06fr 0.94fr;
  overflow: hidden;
  border: 1px solid rgba(20, 75, 65, 0.1);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(17, 56, 47, 0.16);
}

.auth-shell.wide {
  width: min(1180px, 100%);
  grid-template-columns: 0.92fr 1.08fr;
}

.auth-visual {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  justify-content: space-between;
  padding: 44px 48px;
  background-position: center;
  background-size: cover;
}

.auth-story {
  max-width: 470px;
  padding: 34px 0;
}

.auth-eyebrow {
  margin-bottom: 18px;
  color: #f1bd7d;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.22em;
}

.auth-story h1 {
  color: #fff;
  font-size: clamp(34px, 3.5vw, 42px);
  font-weight: 700;
  line-height: 1.18;
  letter-spacing: 0;
}

.auth-description {
  max-width: 430px;
  margin-top: 20px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 15px;
  line-height: 1.8;
}

.auth-features {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 20px;
  margin: 0;
  padding: 0;
  color: rgba(255, 255, 255, 0.84);
  font-size: 13px;
  list-style: none;
}

.auth-features li {
  display: flex;
  align-items: center;
  gap: 7px;
}

.feature-icon {
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.13);
  color: #f1bd7d;
  font-size: 12px;
}

.auth-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 54px 56px;
  background: #fff;
}

.auth-form-inner {
  width: 100%;
  max-width: 390px;
}

.wide .auth-form-inner {
  max-width: 470px;
}

@media (max-width: 900px) {
  .auth-view {
    padding: 18px;
  }

  .auth-shell,
  .auth-shell.wide {
    width: min(620px, 100%);
    min-height: auto;
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: 210px;
    padding: 28px 32px;
  }

  .auth-story {
    padding: 24px 0 8px;
  }

  .auth-story h1 {
    max-width: 460px;
    font-size: 30px;
  }

  .auth-description {
    margin-top: 12px;
    font-size: 14px;
  }

  .auth-features {
    display: none;
  }

  .auth-panel {
    padding: 38px 34px 42px;
  }
}

@media (max-width: 560px) {
  .auth-view {
    display: block;
    padding: 0;
    background: #fff;
  }

  .auth-shell,
  .auth-shell.wide {
    min-height: 100vh;
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .auth-visual {
    min-height: 190px;
    padding: 24px;
  }

  .auth-story h1 {
    font-size: 27px;
  }

  .auth-description {
    display: none;
  }

  .auth-panel {
    align-items: flex-start;
    padding: 34px 24px 44px;
  }
}
</style>
