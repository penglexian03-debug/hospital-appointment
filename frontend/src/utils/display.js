export const appointmentStatusMap = {
  0: { label: '待就诊', type: 'warning' },
  1: { label: '已完成', type: 'success' },
  2: { label: '已取消', type: 'info' },
  3: { label: '爽约', type: 'danger' },
}

export function appointmentStatus(status) {
  return appointmentStatusMap[status] || { label: '未知', type: 'info' }
}

export function formatDate(value) {
  if (!value) {
    return '--'
  }
  return String(value).slice(0, 10)
}

export function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

export function formatTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).slice(0, 5)
}

export function formatTimeRange(startTime, endTime) {
  return `${formatTime(startTime)} - ${formatTime(endTime)}`
}

export function formatMoney(value) {
  return `¥${Number(value || 0).toFixed(2)}`
}

export function roleLabel(role) {
  return role === 2 ? '管理员' : '患者'
}

export function genderLabel(gender) {
  return { 0: '保密', 1: '男', 2: '女' }[gender] || '未设置'
}

export function periodLabel(period) {
  return period === 1 ? '上午' : period === 2 ? '下午' : '--'
}
