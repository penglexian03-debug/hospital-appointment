<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound,
  Close,
  Promotion,
  RefreshLeft,
  WarningFilled,
} from '@element-plus/icons-vue'
import { chatWithAiApi, confirmAiActionApi, deleteAiConversationApi } from '@/api/ai'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const visible = ref(false)
const input = ref('')
const sending = ref(false)
const confirming = ref(false)
const conversationId = ref('')
const messages = ref([])
const scrollBody = ref(null)
const inputRef = ref(null)
const viewportWidth = ref(window.innerWidth)

const drawerSize = computed(() => (viewportWidth.value <= 560 ? '100%' : '440px'))
const busy = computed(() => sending.value || confirming.value)

const detailLabels = {
  departmentName: '科室',
  doctorName: '医生',
  doctorTitle: '职称',
  visitDate: '就诊日期',
  periodText: '时段',
  startTime: '开始时间',
  endTime: '结束时间',
  patientName: '就诊人',
  patientPhone: '手机号',
  appointmentNo: '预约单号',
  statusText: '状态',
  registrationFee: '挂号费',
  cancelReason: '取消原因',
}

function makeId(prefix) {
  return prefix + '-' + Date.now() + '-' + Math.random().toString(16).slice(2)
}

function requestId() {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID()
  }
  return makeId('request')
}

function currentTime() {
  return new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function welcomeMessage() {
  const name = auth.displayName?.value || '你好'
  return {
    id: makeId('welcome'),
    role: 'assistant',
    content:
      name +
      '，你好。我可以帮你查询科室、医生、号源和预约记录，也可以解答医院介绍、就诊须知、医保和交通等问题。需要预约或取消时，我会先请你确认。',
    time: currentTime(),
    pendingAction: null,
  }
}

messages.value = [welcomeMessage()]

function updateViewport() {
  viewportWidth.value = window.innerWidth
}

async function scrollToBottom() {
  await nextTick()
  const element = scrollBody.value
  if (element) {
    element.scrollTop = element.scrollHeight
  }
}

function openAssistant() {
  visible.value = true
  scrollToBottom()
}

function focusInput() {
  inputRef.value?.focus()
}

async function sendMessage() {
  const text = input.value.trim()
  if (!text || busy.value) {
    return
  }

  messages.value.push({
    id: makeId('user'),
    role: 'user',
    content: text,
    time: currentTime(),
    pendingAction: null,
  })
  input.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    const data = await chatWithAiApi({
      message: text,
      conversation_id: conversationId.value || null,
      request_id: requestId(),
    })
    if (!data || typeof data.reply !== 'string') {
      throw new Error('AI 服务返回格式不正确')
    }
    conversationId.value = data.conversation_id || conversationId.value
    messages.value.push({
      id: makeId('assistant'),
      role: 'assistant',
      content: data.reply,
      time: currentTime(),
      pendingAction: data.pending_action || null,
    })
  } catch (error) {
    messages.value.push({
      id: makeId('error'),
      role: 'assistant',
      content: error.message || 'AI 服务暂时不可用，请稍后重试。',
      time: currentTime(),
      pendingAction: null,
      isError: true,
    })
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault()
    sendMessage()
  }
}

async function resetConversation() {
  if (busy.value) {
    return
  }
  if (conversationId.value) {
    try {
      await deleteAiConversationApi(conversationId.value)
    } catch {
      // A local reset is still useful when the server conversation already expired.
    }
  }
  conversationId.value = ''
  messages.value = [welcomeMessage()]
  input.value = ''
  await scrollToBottom()
  focusInput()
}

async function handlePending(message, approved) {
  const action = message.pendingAction
  if (!action || busy.value) {
    return
  }
  if (!conversationId.value) {
    ElMessage.warning('当前会话已失效，请重新发起')
    return
  }

  confirming.value = true
  try {
    const data = await confirmAiActionApi({
      conversation_id: conversationId.value,
      action_id: action.action_id,
      approved,
      request_id: requestId(),
    })
    message.pendingAction = null
    messages.value.push({
      id: makeId('result'),
      role: 'assistant',
      content: data?.reply || (approved ? '操作已完成' : '已取消本次操作'),
      time: currentTime(),
      pendingAction: null,
    })
    ElMessage.success(approved ? '操作已完成' : '已取消本次操作')
    window.dispatchEvent(
      new CustomEvent('ai:appointment-updated', {
        detail: data?.result || null,
      }),
    )
  } catch (error) {
    ElMessage.error(error.message || '操作失败，请稍后重试')
  } finally {
    confirming.value = false
    await scrollToBottom()
  }
}

function detailEntries(action) {
  const details = action?.details || {}
  return Object.entries(details)
    .filter(([key, value]) => {
      if (value === null || value === undefined || value === '') {
        return false
      }
      return Boolean(detailLabels[key])
    })
    .map(([key, value]) => ({
      key,
      label: detailLabels[key],
      value:
        key === 'registrationFee' && !Number.isNaN(Number(value))
          ? '¥' + Number(value).toFixed(2)
          : String(value),
    }))
}

function formatExpiry(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '稍后'
  }
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  window.addEventListener('resize', updateViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
})
</script>

<template>
  <button
    v-if="!visible"
    type="button"
    class="ai-fab"
    aria-label="打开 AI 助手"
    title="AI 助手"
    @click="openAssistant"
  >
    <el-icon><ChatDotRound /></el-icon>
    <span>AI 助手</span>
  </button>

  <el-drawer
    v-model="visible"
    :size="drawerSize"
    :with-header="false"
    append-to-body
    class="ai-drawer"
    @opened="focusInput"
  >
    <section class="ai-panel">
      <header class="ai-header">
        <div class="ai-title-block">
          <span class="ai-mark"><el-icon><ChatDotRound /></el-icon></span>
          <div>
            <h2>智能导诊助手</h2>
            <p><span class="status-dot"></span>实时号源与预约服务</p>
          </div>
        </div>
        <div class="header-actions">
          <el-tooltip content="新对话" placement="bottom">
            <button
              type="button"
              class="icon-button"
              aria-label="新对话"
              :disabled="busy"
              @click="resetConversation"
            >
              <el-icon><RefreshLeft /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="关闭" placement="bottom">
            <button
              type="button"
              class="icon-button"
              aria-label="关闭"
              @click="visible = false"
            >
              <el-icon><Close /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </header>

      <div ref="scrollBody" class="ai-messages" aria-live="polite">
        <div
          v-for="message in messages"
          :key="message.id"
          :class="['message-row', message.role]"
        >
          <span v-if="message.role === 'assistant'" class="message-avatar">
            <el-icon><ChatDotRound /></el-icon>
          </span>
          <div class="message-stack">
            <div
              :class="[
                'message-bubble',
                { 'message-error': message.isError },
              ]"
            >
              {{ message.content }}
            </div>

            <div v-if="message.pendingAction" class="pending-card">
              <div class="pending-heading">
                <el-icon><WarningFilled /></el-icon>
                <strong>
                  {{
                    message.pendingAction.action_type === 'create_appointment'
                      ? '请确认预约信息'
                      : '请确认取消信息'
                  }}
                </strong>
              </div>
              <p class="pending-summary">{{ message.pendingAction.summary }}</p>
              <div class="pending-details">
                <div
                  v-for="item in detailEntries(message.pendingAction)"
                  :key="item.key"
                  class="detail-row"
                >
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
              <p class="pending-expiry">
                请在 {{ formatExpiry(message.pendingAction.expires_at) }} 前完成确认
              </p>
              <div class="pending-actions">
                <el-button
                  plain
                  :disabled="confirming"
                  @click="handlePending(message, false)"
                >
                  暂不操作
                </el-button>
                <el-button
                  type="primary"
                  :loading="confirming"
                  @click="handlePending(message, true)"
                >
                  {{
                    message.pendingAction.action_type === 'create_appointment'
                      ? '确认预约'
                      : '确认取消'
                  }}
                </el-button>
              </div>
            </div>

            <span class="message-time">{{ message.time }}</span>
          </div>
        </div>

        <div v-if="sending" class="message-row assistant">
          <span class="message-avatar"><el-icon><ChatDotRound /></el-icon></span>
          <div class="typing-bubble" aria-label="AI 正在回复">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <footer class="ai-composer">
        <div class="composer-box">
          <el-input
            ref="inputRef"
            v-model="input"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            resize="none"
            placeholder="描述你的症状、科室或预约需求"
            :disabled="confirming"
            @keydown="handleKeydown"
          />
          <button
            type="button"
            class="send-button"
            aria-label="发送消息"
            :disabled="busy || !input.trim()"
            @click="sendMessage"
          >
            <el-icon><Promotion /></el-icon>
          </button>
        </div>
      </footer>
    </section>
  </el-drawer>
</template>

<style scoped>
.ai-fab {
  position: fixed;
  z-index: 35;
  right: 28px;
  bottom: 28px;
  display: flex;
  height: 48px;
  align-items: center;
  gap: 8px;
  padding: 0 17px;
  border: 0;
  border-radius: 8px;
  background: #0f766e;
  box-shadow: 0 12px 30px rgba(15, 118, 110, 0.28);
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.ai-fab:hover {
  background: #0b5e58;
  transform: translateY(-2px);
}

.ai-fab .el-icon {
  font-size: 19px;
}

:global(.ai-drawer) {
  background: #f3f7f5;
}

:global(.ai-drawer .el-drawer__body) {
  overflow: hidden;
  padding: 0;
}

.ai-panel {
  display: flex;
  height: 100vh;
  flex-direction: column;
  background:
    linear-gradient(rgba(15, 118, 110, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 118, 110, 0.025) 1px, transparent 1px),
    #f3f7f5;
  background-size: 28px 28px;
}

.ai-header {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 18px;
  border-bottom: 1px solid #dce7e2;
  background: rgba(255, 255, 255, 0.96);
}

.ai-title-block {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 11px;
}

.ai-mark {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: #173f39;
  color: #fff;
  font-size: 20px;
}

.ai-title-block h2 {
  overflow: hidden;
  color: #17231f;
  font-size: 16px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-title-block p {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  color: #65756f;
  font-size: 11px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2f9e73;
  box-shadow: 0 0 0 3px rgba(47, 158, 115, 0.12);
}

.header-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 4px;
}

.icon-button {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #52645e;
  cursor: pointer;
  font-size: 17px;
}

.icon-button:hover:not(:disabled) {
  border-color: #dce7e2;
  background: #f4f8f6;
  color: #0f766e;
}

.icon-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.ai-messages {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 18px;
  overflow-y: auto;
  padding: 20px 18px 28px;
  scrollbar-color: #c7d8d2 transparent;
  scrollbar-width: thin;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-avatar {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  background: #173f39;
  color: #fff;
  font-size: 15px;
}

.message-stack {
  display: flex;
  max-width: calc(100% - 42px);
  flex-direction: column;
}

.message-row.user .message-stack {
  align-items: flex-end;
}

.message-bubble {
  max-width: 100%;
  padding: 10px 13px;
  border: 1px solid #dce7e2;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(21, 55, 47, 0.035);
  color: #263b34;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.user .message-bubble {
  border-color: #0f766e;
  border-bottom-right-radius: 3px;
  background: #0f766e;
  color: #fff;
}

.message-row.assistant .message-bubble {
  border-bottom-left-radius: 3px;
}

.message-bubble.message-error {
  border-color: #f0c9c4;
  background: #fff8f7;
  color: #a44036;
}

.message-time {
  margin-top: 5px;
  padding: 0 2px;
  color: #8b9a94;
  font-size: 10px;
}

.pending-card {
  width: 100%;
  margin-top: 10px;
  padding: 13px;
  border: 1px solid #edc9aa;
  border-radius: 8px;
  background: #fffdf9;
  box-shadow: 0 6px 18px rgba(126, 82, 37, 0.06);
}

.pending-heading {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #8b4d1e;
}

.pending-heading .el-icon {
  color: #d9833f;
  font-size: 17px;
}

.pending-heading strong {
  font-size: 13px;
}

.pending-summary {
  margin-top: 8px;
  color: #3e342c;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
}

.pending-details {
  margin-top: 10px;
  padding: 9px 10px;
  border-radius: 6px;
  background: #fbf6ef;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 4px 0;
  font-size: 12px;
  line-height: 1.45;
}

.detail-row span {
  flex: 0 0 auto;
  color: #86766a;
}

.detail-row strong {
  color: #3b312a;
  font-weight: 600;
  text-align: right;
  word-break: break-word;
}

.pending-expiry {
  margin-top: 8px;
  color: #9a7658;
  font-size: 11px;
}

.pending-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.typing-bubble {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid #dce7e2;
  border-radius: 8px;
  border-bottom-left-radius: 3px;
  background: #fff;
}

.typing-bubble span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #87a49b;
  animation: typing 1.1s infinite ease-in-out;
}

.typing-bubble span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-bubble span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing {
  0%,
  70%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }

  35% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.ai-composer {
  padding: 12px 16px 16px;
  border-top: 1px solid #dce7e2;
  background: #fff;
}

.composer-box {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 6px 6px 6px 12px;
  border: 1px solid #d4e1dc;
  border-radius: 8px;
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.composer-box:focus-within {
  border-color: #4d9f98;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.08);
}

.composer-box :deep(.el-textarea__inner) {
  min-height: 36px !important;
  padding: 8px 0;
  border: 0;
  box-shadow: none;
  color: #17231f;
  font-size: 14px;
  line-height: 1.5;
}

.composer-box :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.send-button {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  place-items: center;
  border: 0;
  border-radius: 6px;
  background: #0f766e;
  color: #fff;
  cursor: pointer;
  font-size: 18px;
  transition: background-color 0.2s ease;
}

.send-button:hover:not(:disabled) {
  background: #0b5e58;
}

.send-button:disabled {
  background: #d4dfdb;
  cursor: not-allowed;
}

@media (max-width: 560px) {
  .ai-fab {
    right: 16px;
    bottom: 18px;
    width: 50px;
    height: 50px;
    justify-content: center;
    padding: 0;
  }

  .ai-fab span {
    display: none;
  }

  .ai-header {
    min-height: 66px;
    padding: 0 14px;
  }

  .ai-messages {
    padding: 16px 14px 24px;
  }

  .message-stack {
    max-width: calc(100% - 39px);
  }

  .ai-composer {
    padding: 10px 12px 14px;
  }
}
</style>
