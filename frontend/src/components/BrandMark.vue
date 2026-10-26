<script setup>
import { FirstAidKit } from '@element-plus/icons-vue'

defineProps({
  light: {
    type: Boolean,
    default: false,
  },
  compact: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <div class="brand-mark" :class="{ light, compact }">
    <span class="brand-icon">
      <el-icon><FirstAidKit /></el-icon>
    </span>
    <span class="brand-copy">
      <strong>安和医院</strong>
      <small>预约挂号系统</small>
    </span>
  </div>
</template>

<style scoped>
.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  color: #173a34;
}

.brand-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 8px;
  background: #0f766e;
  color: #fff;
  font-size: 21px;
  box-shadow: 0 5px 14px rgba(15, 118, 110, 0.22);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-copy strong {
  font-size: 17px;
  letter-spacing: 0.04em;
}

.brand-copy small {
  margin-top: 4px;
  color: #71827c;
  font-size: 10px;
  letter-spacing: 0.14em;
}

.brand-mark.light {
  color: #fff;
}

.brand-mark.light .brand-icon {
  background: #fff;
  color: #0f766e;
  box-shadow: none;
}

.brand-mark.light .brand-copy small {
  color: rgba(255, 255, 255, 0.68);
}

.brand-mark.compact .brand-icon {
  width: 32px;
  height: 32px;
  font-size: 18px;
}

.brand-mark.compact .brand-copy strong {
  font-size: 15px;
}

.brand-mark.compact .brand-copy small {
  display: none;
}
</style>
