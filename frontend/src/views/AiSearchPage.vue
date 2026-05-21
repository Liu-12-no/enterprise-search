<template>
  <div class="ai-layout">
    <aside class="sidebar">
      <div class="logo-group">
        <div class="logo-box">知</div>
        <span class="logo-name">知道 AI</span>
      </div>

      <nav class="nav-list">
        <div class="nav-item" :class="{ active: messages.length === 0 }" @click="startNewChat">
          <el-icon><Search /></el-icon>
          <span>新会话</span>
        </div>
      </nav>

      <div class="history-container">
        <div class="history-label">
          <el-icon><Clock /></el-icon>
          <span>历史记录</span>
        </div>
        <div class="history-scroll">
          <div
              v-for="item in historyList"
              :key="item.id"
              class="history-card"
              :class="{ 'active-history': String(sessionId) === String(item.id) }"
              @click="loadHistoryDetail(item.id, item.title)"
          >
            <span class="history-card-title">{{ item.title }}</span>

            <el-dropdown trigger="click" @click.stop>
              <div class="more-btn" @click.stop>
                <el-icon><More /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleRename(item)">
                    <el-icon><EditPen /></el-icon>重命名
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleDelete(item)" style="color: #f56c6c;">
                    <el-icon><Delete /></el-icon>删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div v-if="historyList.length === 0" class="empty-tips">暂无历史记录</div>
        </div>
      </div>

      <div class="sidebar-footer">
        <template v-if="userInfo.token">
          <el-dropdown trigger="click" placement="top" class="user-dropdown">
            <div class="user-profile-trigger interactive-btn">
              <el-avatar :size="28" icon="UserFilled" style="background-color: #7c4dff;" />
              <span class="user-info">{{ userInfo.trueName }}</span>
              <el-icon class="arrow-icon"><ArrowUp /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="custom-dropdown-menu">
                <el-dropdown-item @click="openProfileDialog">
                  <el-icon><User /></el-icon>个人资料
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <div class="user-profile-trigger interactive-btn" @click="showLoginDialog = true">
            <el-avatar :size="28" icon="User" style="background-color: #c0c4cc; color: #fff;" />
            <span class="user-info">点击登录</span>
          </div>
        </template>
      </div>
    </aside>

    <main class="chat-main" :class="{ 'center-mode': messages.length === 0 }">
      <div class="chat-header" v-if="messages.length > 0">
        <div class="current-title">{{ currentTitle }}</div>
      </div>

      <div class="chat-wrapper">
        <h1 v-if="messages.length === 0" class="welcome-title">开发者首选的 AI 搜索引擎</h1>

        <div class="message-list" ref="messageBox">
          <div
              v-for="(msg, index) in messages"
              :key="msg.localId || msg.id"
              v-show="!(msg.role === 'user' && msg.content === '我已经上传了文档，请帮我详细总结其内容。请使用中文回答。')"
              class="msg-row"
              :class="{ 'is-select-mode': isSelectMode, 'is-selected': selectedIds.includes(msg.id) }"
              @click="handleRowClick(msg, index)"
          >
            <div class="msg-checkbox" v-if="isSelectMode && msg.id">
              <el-checkbox :model-value="selectedIds.includes(msg.id)" size="large" @click.prevent />
            </div>

            <div :class="['msg-group', msg.role]">
              <div class="msg-avatar">
                <el-icon v-if="msg.role === 'assistant'"><Cpu /></el-icon>
                <el-icon v-else><User /></el-icon>
              </div>

              <div class="msg-content">
                <div class="role-title">{{ msg.role === 'assistant' ? '回答' : '提问' }}</div>

                <div v-if="msg.isDocument" class="history-file-card">
                  <div class="history-file-icon">
                    <img src="https://img.alicdn.com/imgextra/i2/O1CN01Zlq3x11HygmBqj22j_!!6000000000828-2-tps-128-128.png" alt="pdf" />
                  </div>
                  <div class="history-file-info">
                    <span class="history-file-name">{{ msg.fileName }}</span>
                    <span class="history-file-ext">PDF 文档</span>
                  </div>
                </div>

                <div v-if="getFileInfoList(msg).length > 0">
                  <div
                      v-for="(file, fIdx) in getFileInfoList(msg)"
                      :key="fIdx"
                      class="history-file-card"
                      style="cursor: pointer; border: 1px solid #ebeef5; transition: all 0.2s;"
                      @click.stop="previewFile(file.url)"
                  >
                    <div class="history-file-icon">
                      <img src="https://img.alicdn.com/imgextra/i2/O1CN01Zlq3x11HygmBqj22j_!!6000000000828-2-tps-128-128.png" alt="pdf" />
                    </div>
                    <div class="history-file-info">
                      <span class="history-file-name">{{ file.name }}</span>
                      <span class="history-file-ext">PDF · {{ file.size || '未知' }}</span>
                    </div>
                  </div>
                </div>

                <div v-if="msg.role === 'assistant' && isTyping && msg.content === ''" class="thinking-process-modern">
                  <div class="thinking-header">
                    <el-icon class="thinking-spin"><Loading /></el-icon>
                    <span class="thinking-title">Agent 深度思考中...</span>
                  </div>
                  <div class="thinking-logs-modern">
                    <div class="log-item" v-if="toolLogs.length === 0">正在理解您的语义需求并规划检索路径</div>
                    <div v-for="(log, idx) in toolLogs" :key="idx" class="log-item fade-in-up">{{ log }}</div>
                  </div>
                </div>

                <div class="msg-text" v-if="msg.content !== ''" v-html="marked.parse(msg.content)"></div>

                <div class="msg-actions" v-show="!isSelectMode">
                  <el-tooltip content="复制" placement="top" :show-after="500">
                    <el-button class="action-icon-btn" text circle @click.stop="handleCopy(msg.content)">
                      <el-icon><CopyDocument /></el-icon>
                    </el-button>
                  </el-tooltip>

                  <el-dropdown trigger="click" placement="bottom-end" v-show="msg.id && !isTyping">
                    <el-button class="action-icon-btn" text circle @click.stop>
                      <el-icon><MoreFilled /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu class="modern-dropdown-menu">
                        <el-dropdown-item class="danger-item" @click="enterSelectMode(msg, index)">
                          <el-icon><Delete /></el-icon>删除
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>

              </div>
            </div>
          </div>
        </div>

        <div class="bottom-anchor">
          <div class="batch-action-container" v-show="isSelectMode">
            <div class="batch-text">已选择 <span>{{ selectedIds.length }}</span> 条内容</div>
            <div class="batch-buttons">
              <el-button class="modern-btn-plain" round @click="cancelSelectMode">取消</el-button>
              <el-button type="danger" round :disabled="selectedIds.length === 0" @click="confirmBatchDelete">
                <el-icon><Delete /></el-icon> 删 除
              </el-button>
            </div>
          </div>

          <div class="gemini-input-container" v-show="!isSelectMode">

            <div class="staged-files-area" v-if="stagedFiles.length > 0">
              <div class="staged-file-chip" v-for="file in stagedFiles" :key="file.id">
                <div class="file-icon"><el-icon><Document /></el-icon></div>
                <div class="file-details">
                  <div class="file-name" :title="file.name">{{ file.name }}</div>
                  <div class="file-status">
                    <span v-if="file.status === 'uploading'">解析中...</span>
                    <span v-else-if="file.status === 'success'">PDF · {{ file.size }}</span>
                    <span v-else class="error-text" style="color: #f56c6c;">解析失败</span>
                  </div>
                </div>
                <div class="close-btn" @click="removeStagedFile(file.id)"><el-icon><Close /></el-icon></div>
              </div>

              <div class="quick-actions" v-if="stagedFiles.some(f => f.status === 'success')">
                <div class="action-chip" @click="handleSend('详细总结这篇文档内容')">详细总结这篇文档内容 <el-icon><Right /></el-icon></div>
                <div class="action-chip" @click="handleSend('提取文档中的核心数据')">生成脑图 <el-icon><Right /></el-icon></div>
              </div>
            </div>

            <div class="input-area">
              <textarea
                  v-model="query"
                  ref="inputRef"
                  placeholder="问问知道 AI..."
                  rows="1"
                  @input="autoResize"
                  @keydown.enter.exact.prevent="handleSend()"
                  @keydown.enter.ctrl.exact="query += '\n'"
              ></textarea>
            </div>
            <div class="input-toolbar">

              <div class="toolbar-left">
                <el-dropdown trigger="click" placement="top-start" popper-class="gemini-dropdown">
                  <div class="plus-btn interactive-btn">
                    <el-icon><Plus /></el-icon>
                  </div>
                  <template #dropdown>
                    <el-dropdown-menu class="modern-dropdown-menu">
                      <el-dropdown-item @click="triggerFileUpload">
                        <el-icon><Paperclip /></el-icon>上传文件
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>

                <input type="file" ref="fileInput" accept=".pdf" style="display: none" @change="handleFileUpload" />
              </div>

              <div class="toolbar-right">
                <button
                    class="gemini-send-btn interactive-btn"
                    :class="{ 'can-send': (query.trim() || stagedFiles.length > 0) && !isTyping, 'is-typing': isTyping }"
                    @click="isTyping ? handleStop() : handleSend()"
                >
                  <el-icon v-if="!isTyping"><Top /></el-icon>
                  <div v-else class="stop-icon"></div>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <el-dialog v-model="showLoginDialog" title="账号登录" width="360px" center align-center>
      <el-form :model="loginForm" @keyup.enter="handleLogin">
        <el-form-item><el-input v-model="loginForm.username" placeholder="请输入账号" prefix-icon="User" /></el-form-item>
        <el-form-item><el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" class="modern-btn" style="width: 100%;" @click="handleLogin" :loading="loginLoading">登 录 / 注 册</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showProfileDialog" title="个人资料" width="360px" center align-center>
      <div style="text-align: center; margin-bottom: 20px;">
        <el-avatar :size="72" icon="UserFilled" style="background-color: #7c4dff; font-size: 36px;" />
      </div>
      <el-form :model="profileForm" @keyup.enter="handleUpdateProfile">
        <el-form-item label="昵称"><el-input v-model="profileForm.trueName" placeholder="请输入新昵称" maxlength="40" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" class="modern-btn" style="width: 100%;" @click="handleUpdateProfile" :loading="profileLoading">保 存 修 改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import {
  Search, Clock, Cpu, User, Top, Loading, UserFilled, Lock,
  ArrowUp, SwitchButton, Compass, More, EditPen, Delete,
  CopyDocument, MoreFilled, Plus, Paperclip, FolderOpened, Grid,
  Document, Close, Right
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'

marked.setOptions({ breaks: true, gfm: true })

const query = ref('')
const messages = ref([])
const stagedFiles = ref([])
const isTyping = ref(false)
const messageBox = ref(null)
const inputRef = ref(null)
const toolLogs = ref([])
const historyList = ref([])
const currentTitle = ref('')
const sessionId = ref(String(Date.now()))

let abortController = null

const userInfo = ref({ token: '', trueName: '' })
const showLoginDialog = ref(false)
const loginLoading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const showProfileDialog = ref(false)
const profileLoading = ref(false)
const profileForm = reactive({ trueName: '' })
const isSelectMode = ref(false)
const selectedIds = ref([])
const fileInput = ref(null)

const autoResize = () => {
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
    inputRef.value.style.height = inputRef.value.scrollHeight + 'px'
  }
}

const checkAuthError = (status) => {
  if (status === 401) {
    ElMessage.error('登录状态已过期')
    handleLogout()
    return true
  }
  return false
}

let isScrolling = false
const scrollToBottom = () => {
  if (!isScrolling) {
    isScrolling = true
    requestAnimationFrame(() => {
      if (messageBox.value) {
        messageBox.value.scrollTop = messageBox.value.scrollHeight
      }
      isScrolling = false
    })
  }
}

onMounted(() => {
  const token = localStorage.getItem('token')
  const trueName = localStorage.getItem('trueName')
  if (token) {
    userInfo.value = { token, trueName }
    fetchHistoryList()
  }
})

// 🌟 解析 JSON 提取真实 URL (这是必须加的方法)
const getFileInfoList = (msg) => {
  let info = msg.fileInfo || msg.file_info;
  if (!info || info === 'null') return [];
  try {
    let parsed = info;
    if (typeof parsed === 'string') parsed = JSON.parse(parsed);
    if (typeof parsed === 'string') parsed = JSON.parse(parsed);
    return Array.isArray(parsed) ? parsed : [parsed];
  } catch (e) {
    return [];
  }
}

// 🌟 新标签页打开真实 PDF
const previewFile = (url) => {
  if (!url) return;
  // 加入时间戳强制突破浏览器可能残存的 MinIO 跳转缓存
  const finalUrl = url.includes('?') ? url : `${url}?t=${Date.now()}`;
  window.open(finalUrl, '_blank');
}

const triggerFileUpload = () => {
  if (!userInfo.value.token) {
    showLoginDialog.value = true
    return
  }
  fileInput.value.click()
}

// 原样还原：只是上传文件，在暂存区等待发送
const handleFileUpload = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.error('只支持 PDF 文件')
    return
  }

  const fileId = Date.now()
  const fileSize = (file.size / 1024 / 1024).toFixed(1) + 'MB'
  stagedFiles.value.push({ id: fileId, name: file.name, size: fileSize, status: 'uploading' })

  const formData = new FormData()
  formData.append('file', file)
  formData.append('sessionId', sessionId.value)
  formData.append('companyName', '私有知识库文件')

  try {
    const res = await fetch('/api/knowledge/upload', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${userInfo.value.token}` },
      body: formData
    })

    if (checkAuthError(res.status)) return
    const data = await res.json()
    const target = stagedFiles.value.find(f => f.id === fileId)

    if (data.code === 200 || data.code === 0) {
      if (target) target.status = 'success'
    } else {
      if (target) target.status = 'error'
      ElMessage.error(data.message || '解析失败')
    }
  } catch (error) {
    const target = stagedFiles.value.find(f => f.id === fileId)
    if (target) target.status = 'error'
  } finally {
    e.target.value = ''
  }
}

const removeStagedFile = (id) => {
  stagedFiles.value = stagedFiles.value.filter(f => f.id !== id)
}

// 原样还原：完整的旧逻辑，一字不改。
const handleSend = async (quickText = null) => {
  if (!userInfo.value.token) {
    showLoginDialog.value = true
    return
  }

  const isQuickAction = typeof quickText === 'string'
  const typedText = query.value.trim()
  let requestQuestion = isQuickAction ? quickText : typedText

  const successFiles = stagedFiles.value.filter(f => f.status === 'success')
  if (!requestQuestion && successFiles.length === 0) return
  if (isTyping.value) return

  let isFirstMessage = messages.value.length === 0
  if (isFirstMessage) currentTitle.value = "新会话"

  successFiles.forEach(file => {
    messages.value.push({
      localId: 'file_' + file.id,
      role: 'user',
      isDocument: true,
      fileName: file.name,
      content: ''
    })
  })
  stagedFiles.value = []

  // 👇 就是这里！原封不动地还给你！
  let backendQuestion = requestQuestion
  if (requestQuestion) {
    messages.value.push({
      localId: 'local_u_' + Date.now(),
      role: 'user',
      content: requestQuestion
    })
  } else if (successFiles.length > 0) {
    backendQuestion = "我已经上传了文档，请帮我详细总结其内容。请使用中文回答。"
  }

  query.value = ''
  nextTick(() => { if (inputRef.value) inputRef.value.style.height = 'auto' })

  isTyping.value = true
  toolLogs.value = []
  const aiMsg = reactive({ localId: 'local_a_' + Date.now(), role: 'assistant', content: '' })
  messages.value.push(aiMsg)
  scrollToBottom()

  const url = `/api/chat/stream?sessionId=${sessionId.value}&question=${encodeURIComponent(backendQuestion)}&token=${userInfo.value.token}`
  if (abortController) abortController.abort()
  abortController = new AbortController()

  try {
    const response = await fetch(url, { signal: abortController.signal })
    if (checkAuthError(response.status)) return

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop()

      for (const line of lines) {
        if (!line.trim().startsWith('data:')) continue

        let data = line.replace('data:', '')
        if (data.startsWith(' ')) data = data.substring(1)

        if (data === '[DONE]' || data.trim() === '[DONE]') break

        if (data.trim().startsWith('[TOOL]')) {
          toolLogs.value.push(data.trim().replace('[TOOL]', ''))
        } else {
          aiMsg.content += data.replace(/\\n/g, '\n')
          scrollToBottom()
        }
      }
    }
  } catch (e) {
  } finally {
    isTyping.value = false
    syncMessagesSilently()
    if (isFirstMessage) setTimeout(fetchHistoryList, 1000)
  }
}

const syncMessagesSilently = async () => {
  try {
    const res = await fetch(`/api/chat/history/detail/${sessionId.value}`, {
      headers: { 'Authorization': `Bearer ${userInfo.value.token}` }
    })
    const data = await res.json()
    if (data.code === 200 || data.code === 0) {
      const dbMessages = data.data || []
      if (dbMessages.length === messages.value.length) {
        for (let i = 0; i < messages.value.length; i++) {
          if (!messages.value[i].id) {
            messages.value[i].id = dbMessages[i].id
          }
          if (messages.value[i].role === 'assistant') {
            messages.value[i].content = dbMessages[i].content
          }
        }
      }
    }
  } catch (e) {}
}

const fetchHistoryList = async () => {
  try {
    const res = await fetch('/api/chat/history/list', {
      headers: { 'Authorization': `Bearer ${userInfo.value.token}` }
    })
    const data = await res.json()
    if (data.code === 200 || data.code === 0) {
      historyList.value = data.data || []
    }
  } catch (error) {}
}

const loadHistoryDetail = async (id, title) => {
  if (isTyping.value) {
    ElMessage.warning('AI 正在回复中，请稍后操作')
    return
  }
  sessionId.value = String(id)
  currentTitle.value = title
  messages.value = []
  try {
    const res = await fetch(`/api/chat/history/detail/${id}`, {
      headers: { 'Authorization': `Bearer ${userInfo.value.token}` }
    })
    const data = await res.json()
    if (data.code === 200 || data.code === 0) {
      messages.value = data.data || []
      scrollToBottom()
    }
  } catch (error) {}
}

const handleRename = (item) => {
  ElMessageBox.prompt('请输入新标题', '重命名', {
    inputValue: item.title,
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    customClass: 'modern-message-box',
    inputValidator: (val) => {
      if (!val || !val.trim()) return '标题不能为空'
      return true
    }
  }).then(async ({ value }) => {
    try {
      const res = await fetch('/api/chat/rename', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${userInfo.value.token}`
        },
        body: JSON.stringify({ id: item.id, title: value.trim() })
      })
      const data = await res.json()
      if (data.code === 200 || data.code === 0) {
        ElMessage.success('重命名成功')
        item.title = value.trim()
        if (String(sessionId.value) === String(item.id)) currentTitle.value = value.trim()
        fetchHistoryList()
      } else {
        ElMessage.error(data.message || '重命名失败')
      }
    } catch (error) {}
  }).catch(() => {})
}

const handleDelete = (item) => {
  ElMessageBox.confirm(`确定要删除历史会话 "${item.title}" 吗？`, '删除警告', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'modern-message-box'
  }).then(async () => {
    try {
      const res = await fetch(`/api/chat/delete/${item.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${userInfo.value.token}` }
      })
      const data = await res.json()
      if (data.code === 200 || data.code === 0) {
        ElMessage.success('删除成功')
        historyList.value = historyList.value.filter(i => String(i.id) !== String(item.id))
        if (String(sessionId.value) === String(item.id)) startNewChat()
      } else {
        ElMessage.error(data.message || '删除失败')
      }
    } catch (error) {}
  }).catch(() => {})
}

const enterSelectMode = (msg, index) => {
  isSelectMode.value = true
  selectedIds.value = [msg.id]
}

const cancelSelectMode = () => {
  isSelectMode.value = false
  selectedIds.value = []
}

const handleRowClick = (msg, index) => {
  if (!isSelectMode.value || !msg.id) return
  if (selectedIds.value.includes(msg.id)) {
    selectedIds.value = selectedIds.value.filter(i => i !== msg.id)
  } else {
    selectedIds.value.push(msg.id)
  }
}

const confirmBatchDelete = async () => {
  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条内容吗？`, '删除警告', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'modern-message-box'
  }).then(async () => {
    try {
      const res = await fetch('/api/chat/messages/delete', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${userInfo.value.token}`
        },
        body: JSON.stringify({ ids: selectedIds.value })
      })
      const data = await res.json()
      if (data.code === 200 || data.code === 0) {
        ElMessage.success('删除成功')
        messages.value = messages.value.filter(m => !selectedIds.value.includes(m.id))
        cancelSelectMode()
        if (messages.value.length === 0) startNewChat()
      } else {
        ElMessage.error(data.message || '删除失败')
      }
    } catch (error) {}
  }).catch(() => {})
}

const startNewChat = () => {
  if (abortController) {
    abortController.abort()
    isTyping.value = false
  }
  sessionId.value = String(Date.now())
  messages.value = []
  currentTitle.value = ''
  stagedFiles.value = []
  nextTick(() => { autoResize() })
}

const handleStop = () => {
  if (abortController) {
    abortController.abort()
  }
  isTyping.value = false
}

const handleCopy = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('复制成功')
  })
}

const handleLogout = () => {
  localStorage.clear()
  userInfo.value = { token: '', trueName: '' }
  startNewChat()
  historyList.value = []
}

const handleLogin = async () => {
  loginLoading.value = true
  try {
    const res = await fetch('/api/user/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(loginForm)
    })
    const data = await res.json()
    if (data.code === 200 || data.code === 0) {
      userInfo.value = { token: data.data.token, trueName: data.data.trueName }
      localStorage.setItem('token', data.data.token)
      localStorage.setItem('trueName', data.data.trueName)
      showLoginDialog.value = false
      fetchHistoryList()
      ElMessage.success('登录成功')
    } else {
      ElMessage.error(data.message || '登录失败')
    }
  } catch (error) {
    ElMessage.error('网络异常')
  } finally {
    loginLoading.value = false
  }
}

const openProfileDialog = () => {
  profileForm.trueName = userInfo.value.trueName
  showProfileDialog.value = true
}

const handleUpdateProfile = async () => {
  profileLoading.value = true
  try {
    const res = await fetch('/api/user/update', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userInfo.value.token}`
      },
      body: JSON.stringify(profileForm)
    })
    const data = await res.json()
    if (data.code === 200 || data.code === 0) {
      ElMessage.success('修改成功')
      userInfo.value.trueName = profileForm.trueName
      localStorage.setItem('trueName', profileForm.trueName)
      showProfileDialog.value = false
    } else {
      ElMessage.error(data.message || '修改失败')
    }
  } catch (error) {
    ElMessage.error('网络异常')
  } finally {
    profileLoading.value = false
  }
}
</script>

<style scoped>
/* 原有的所有 CSS 原封不动保留 */
.ai-layout { display: flex; height: 100vh; background: #fff; overflow: hidden; }
.sidebar { width: 260px; background: #f8f9fa; border-right: 1px solid #eaeaea; display: flex; flex-direction: column; padding: 24px 16px; z-index: 20;}
.logo-group { display: flex; align-items: center; gap: 12px; margin-bottom: 32px; padding-left: 8px; }
.logo-box { width: 34px; height: 34px; background: #7c4dff; color: #fff; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 18px; }
.logo-name { font-size: 20px; font-weight: 600; color: #1a1a1a; }
.nav-item { display: flex; align-items: center; gap: 12px; padding: 10px 16px; border-radius: 8px; cursor: pointer; color: #444; font-size: 14px; transition: 0.2s;}
.nav-item:hover { background: #f0f0f0; }
.nav-item.active { background: #eeebff; color: #7c4dff; font-weight: 600; }
.history-container { flex: 1; margin-top: 40px; overflow: hidden; display: flex; flex-direction: column; }
.history-label { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #999; margin-bottom: 16px; padding-left: 16px; }
.history-scroll { flex: 1; overflow-y: auto; }
.history-card { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px 10px 16px; font-size: 13px; color: #555; border-radius: 8px; cursor: pointer; margin-bottom: 4px; transition: 0.2s; }
.history-card-title { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.history-card:hover { background: #efefef; }
.active-history { background: #efefef; color: #1a1a1a; font-weight: 600;}
.more-btn { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; opacity: 0; transition: all 0.2s; color: #999; }
.history-card:hover .more-btn { opacity: 1; }
.more-btn:hover { background-color: #e5e7eb; color: #333; }
.message-list::-webkit-scrollbar, .history-scroll::-webkit-scrollbar { width: 5px; }
.message-list::-webkit-scrollbar-thumb, .history-scroll::-webkit-scrollbar-thumb { background: #e0e0e0; border-radius: 10px; }

.chat-main { flex: 1; display: flex; flex-direction: column; position: relative; overflow: hidden; background: #fff;}
.chat-header { position: absolute; top: 0; left: 0; right: 0; height: 60px; background: rgba(255, 255, 255, 0.9); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: center; border-bottom: 1px solid #f0f0f0; z-index: 10; }
.current-title { font-size: 16px; font-weight: 600; color: #333; }
.chat-wrapper { width: 100%; height: 100%; display: flex; flex-direction: column; position: relative; }
.welcome-title { font-size: 32px; font-weight: 700; color: #1a1a1a; text-align: center; margin-top: 20vh; }
.message-list { flex: 1; overflow-y: auto; padding: 80px 20% 220px 20%; }

.msg-row { display: flex; align-items: flex-start; padding: 12px 16px; border-radius: 16px; margin-bottom: 20px; transition: background-color 0.2s ease; }
.msg-row.is-select-mode { cursor: pointer; }
.msg-row.is-select-mode:hover { background-color: #fcfcfc; }
.msg-row.is-selected { background-color: #f4f5f8 !important; }

.msg-checkbox { margin-right: 16px; padding-top: 4px; }
:deep(.el-checkbox__inner) { width: 18px; height: 18px; }
:deep(.el-checkbox__inner::after) { height: 9px; left: 6px; width: 4px; }
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background-color: #7c4dff !important; border-color: #7c4dff !important; }

.msg-group { flex: 1; display: flex; gap: 16px; animation: fadeIn 0.4s ease; }
.msg-group.user { flex-direction: row-reverse; }
.msg-avatar { width: 34px; height: 34px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; color: #7c4dff; flex-shrink: 0; }
.msg-content { max-width: 85%; display: flex; flex-direction: column; }
.user .msg-content { text-align: right; }
.msg-text { font-size: 16px; line-height: 1.8; text-align: left; color: #333; }
.user .msg-text { background: #f4f4f4; padding: 10px 16px; border-radius: 18px 18px 2px 18px; display: inline-block;}

.msg-text :deep(p) { margin: 0 0 10px 0; }
.msg-text :deep(p:last-child) { margin-bottom: 0; }
.msg-text :deep(ul), .msg-text :deep(ol) { padding-left: 20px; margin-bottom: 10px; }

.msg-actions { display: flex; align-items: center; gap: 4px; margin-top: 4px; min-height: 28px; visibility: hidden; opacity: 0; transition: opacity 0.2s ease; }
.user .msg-actions { justify-content: flex-end; }
.assistant .msg-actions { justify-content: flex-start; }
.msg-row:hover .msg-actions { visibility: visible; opacity: 1; }

.action-icon-btn { width: 28px !important; height: 28px !important; padding: 0 !important; color: #909399 !important; background-color: transparent !important; border: none !important; }
.action-icon-btn:hover { background-color: #e5e7eb !important; color: #333 !important; }
.action-icon-btn:focus-visible, .el-dropdown:focus-visible { outline: none; }

.sidebar-footer { border-top: 1px solid #eee; padding-top: 12px; }
.user-profile-trigger { display: flex; align-items: center; gap: 10px; padding: 8px; border-radius: 8px; cursor: pointer;}
.user-profile-trigger:hover { background: #efefef; }

.thinking-process-modern { margin: 4px 0 16px 0; padding: 0; }
.thinking-header { display: flex; align-items: center; gap: 8px; color: #6b7280; font-size: 14px; font-weight: 500; }
.thinking-spin { animation: spin 2s linear infinite; color: #a8b3cf; }
.thinking-logs-modern { margin-top: 8px; margin-left: 6px; padding-left: 18px; border-left: 2px dashed #e5e7eb; display: flex; flex-direction: column; gap: 8px; }
.log-item { font-size: 13px; color: #9ca3af; line-height: 1.5; }

@keyframes spin { 100% { transform: rotate(360deg); } }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

/* === 核心还原区域 === */
.bottom-anchor {
  position: absolute; bottom: 0; left: 0; right: 0;
  background: linear-gradient(to top, #ffffff 0%, #ffffff 60%, rgba(255, 255, 255, 0) 100%);
  padding: 40px 20% 30px 20%; z-index: 100;
}

.gemini-input-container {
  background: #ffffff;
  border-radius: 24px;
  padding: 12px 16px 12px 24px;
  border: 1px solid #e0e0e0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}
.gemini-input-container:focus-within {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.input-area textarea {
  width: 100%; background: transparent; border: none; outline: none;
  font-size: 16px; line-height: 1.6; resize: none; max-height: 180px; padding: 4px 0;
}

.input-toolbar { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.toolbar-left { display: flex; align-items: center; }

/* 紫色 + 号按钮 */
.plus-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #7c4dff;
  cursor: pointer;
  transition: background-color 0.2s ease;
  background-color: transparent;
}
.plus-btn:hover {
  background-color: #f3f0ff;
}

/* 右侧紫化发送按钮 */
.gemini-send-btn {
  width: 40px; height: 40px; border-radius: 50%; border: none;
  background: #f0f0f0; color: #c4c7c5; display: flex; align-items: center; justify-content: center; transition: all 0.3s ease;
}
.gemini-send-btn.can-send { background: #7c4dff; color: #fff; cursor: pointer; }
.stop-icon { width: 14px; height: 14px; background-color: #444; border-radius: 2px; }

/* 批量操作条 */
.batch-action-container {
  background: #ffffff; border-radius: 20px; padding: 16px 32px;
  border: 1px solid #e0e0e0; box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px;
}
.batch-text span { color: #7c4dff; font-weight: bold; margin: 0 4px; }
.batch-buttons { display: flex; gap: 12px; }
.modern-btn-plain { border-radius: 20px !important; }

/* 暂存区样式 */
.staged-files-area {
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.staged-file-chip {
  display: flex;
  align-items: center;
  background-color: #f4f5f8;
  border-radius: 8px;
  padding: 8px 12px;
  width: fit-content;
  max-width: 240px;
  position: relative;
}
.staged-file-chip .file-icon {
  width: 32px; height: 32px;
  background-color: #707070; color: #fff;
  border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; margin-right: 10px;
}
.staged-file-chip .file-details {
  display: flex; flex-direction: column; overflow: hidden; margin-right: 20px;
}
.staged-file-chip .file-name {
  font-size: 14px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.staged-file-chip .file-status {
  font-size: 12px; color: #999; margin-top: 2px;
}
.staged-file-chip .close-btn {
  position: absolute; top: 8px; right: 8px; color: #999; cursor: pointer; font-size: 14px;
}
.staged-file-chip .close-btn:hover { color: #f56c6c; }

/* 快捷操作 Chips */
.quick-actions {
  display: flex; gap: 8px; flex-wrap: wrap;
}
.action-chip {
  background-color: #f4f5f8; color: #333; font-size: 13px;
  padding: 6px 12px; border-radius: 6px; cursor: pointer;
  display: flex; align-items: center; gap: 4px; transition: background 0.2s;
}
.action-chip:hover { background-color: #e5e7eb; }

/* 历史聊天记录中的文件卡片样式 */
.history-file-card {
  display: flex; align-items: center;
  background-color: #f4f5f8; border-radius: 12px;
  padding: 10px 16px; margin-bottom: 8px; width: fit-content; max-width: 280px;
}
.history-file-icon img { width: 36px; height: 36px; margin-right: 12px; }
.history-file-info { display: flex; flex-direction: column; overflow: hidden; text-align: left; }
.history-file-name { font-size: 14px; font-weight: 500; color: #1f1f1f; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 1.4; }
.history-file-ext { font-size: 12px; color: #999; margin-top: 2px; }
</style>

<style>
/* 下拉菜单干净排版 */
.gemini-dropdown {
  border-radius: 12px !important; padding: 6px 0 !important;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08) !important;
  border: 1px solid #ebeef5 !important;
}
.modern-dropdown-menu .el-dropdown-menu__item {
  padding: 10px 20px !important; font-size: 14px !important; color: #444 !important;
}
.modern-dropdown-menu .el-dropdown-menu__item .el-icon {
  margin-right: 12px !important; font-size: 18px !important; color: #5f6368 !important;
}

.gemini-dropdown .el-dropdown-menu__item:not(.is-disabled):focus,
.gemini-dropdown .el-dropdown-menu__item:not(.is-disabled):hover {
  background-color: #f3f0ff !important;
  color: #7c4dff !important;
}
.gemini-dropdown .el-dropdown-menu__item:not(.is-disabled):focus .el-icon,
.gemini-dropdown .el-dropdown-menu__item:not(.is-disabled):hover .el-icon {
  color: #7c4dff !important;
}

.modern-message-box { border-radius: 12px !important; padding-bottom: 20px !important; }
.modern-message-box .el-button--primary { background-color: #7c4dff !important; border-color: #7c4dff !important; border-radius: 8px !important; font-weight: 600; }
.el-dropdown-menu__item.danger-item { color: #f56c6c !important; transition: all 0.2s ease; }
.el-dropdown-menu__item.danger-item:hover { background-color: #fef0f0 !important; color: #f56c6c !important; }
</style>