<template>
  <div class="advanced-page">
    <header class="header">
      <div class="logo" @click="goHome">企智搜</div>
      <div class="header-title">高级搜索</div>
      <el-button size="small" class="back-btn" @click="goHome">返回首页</el-button>
    </header>

    <main class="main-content">
      <div class="filter-container">
        <div class="filter-row">
          <div class="row-label">关键词</div>
          <div class="row-content">
            <el-input
                v-model="searchForm.keyword"
                placeholder="请输入关键词 (非必填)"
                class="keyword-input"
                clearable
            />
            <span v-if="errorMessage" class="inline-error">{{ errorMessage }}</span>
          </div>
        </div>

        <div class="filter-row">
          <div class="row-label">成立年限</div>
          <div class="row-content">
            <el-checkbox-group v-model="searchForm.yearTags" class="tag-group">
              <el-checkbox label="3个月内">3个月内</el-checkbox>
              <el-checkbox label="半年内">半年内</el-checkbox>
              <el-checkbox label="1年内">1年内</el-checkbox>
              <el-checkbox label="1-3年">1-3年</el-checkbox>
              <el-checkbox label="3-5年">3-5年</el-checkbox>
              <el-checkbox label="5-10年">5-10年</el-checkbox>
              <el-checkbox label="10年以上">10年以上</el-checkbox>
            </el-checkbox-group>
          </div>
        </div>

        <div class="filter-row no-border">
          <div class="row-label align-center">登记状态</div>
          <div class="status-col">
            <div class="status-sub-row" v-for="(group, index) in statusGroups" :key="index">
              <el-checkbox
                  class="parent-check"
                  :model-value="isAllChecked(group)"
                  :indeterminate="isIndeterminate(group)"
                  @change="(val) => handleParentChange(val, group)"
              >{{ group.label }}</el-checkbox>

              <el-checkbox-group v-model="searchForm.statusTags" class="tag-group child-group">
                <el-checkbox v-for="child in group.children" :key="child.value" :label="child.value">
                  {{ child.label }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </div>

        <div class="filter-row">
          <div class="row-label">注册资本</div>
          <div class="row-content">
            <el-checkbox-group v-model="searchForm.capitalTags" class="tag-group">
              <el-checkbox label="0-100万">0-100万</el-checkbox>
              <el-checkbox label="100-200万">100-200万</el-checkbox>
              <el-checkbox label="200-500万">200-500万</el-checkbox>
              <el-checkbox label="500-1000万">500-1000万</el-checkbox>
              <el-checkbox label="1000-5000万">1000-5000万</el-checkbox>
              <el-checkbox label="5000万以上">5000万以上</el-checkbox>
            </el-checkbox-group>
          </div>
        </div>

        <div class="action-footer">
          <el-button type="primary" class="search-btn" @click="handleSearch">查 一 下</el-button>
          <el-button class="reset-btn" @click="resetForm">清 空 条 件</el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const errorMessage = ref('')

const searchForm = reactive({
  keyword: '',
  statusTags: [],
  capitalTags: [],
  yearTags: [],
  pageNum: 1,
  pageSize: 10
})

const statusGroups = [
  { label: '正常状态', children: [{ label: '存续/在业', value: '存续/在业' }, { label: '迁入', value: '迁入' }, { label: '迁出', value: '迁出' }] },
  { label: '异常状态', children: [{ label: '破产清算', value: '清算' }] },
  { label: '法律调查', children: [{ label: '接受法律调查', value: '接受法律调查' }] },
  { label: '其他状态', children: [{ label: '注销', value: '注销' }, { label: '吊销', value: '吊销' }, { label: '撤销', value: '撤销' }, { label: '停业', value: '停业' }, { label: '已歇业', value: '已歇业' }, { label: '责令关闭', value: '责令关闭' }, { label: '解散', value: '解散' }] }
]

const isAllChecked = (group) => group.children.every(child => searchForm.statusTags.includes(child.value))
const isIndeterminate = (group) => {
  const count = group.children.filter(child => searchForm.statusTags.includes(child.value)).length
  return count > 0 && count < group.children.length
}
const handleParentChange = (val, group) => {
  const values = group.children.map(c => c.value)
  if (val) {
    values.forEach(v => { if (!searchForm.statusTags.includes(v)) searchForm.statusTags.push(v) })
  } else {
    searchForm.statusTags = searchForm.statusTags.filter(v => !values.includes(v))
  }
}

const goHome = () => router.push('/')
const handleSearch = () => {
  errorMessage.value = ''
  if (!searchForm.keyword.trim() && !searchForm.statusTags.length && !searchForm.capitalTags.length && !searchForm.yearTags.length) {
    errorMessage.value = '请选择搜索条件'
    return
  }
  router.push({ path: '/list', query: { advancedData: JSON.stringify(searchForm) } })
}
const resetForm = () => {
  Object.assign(searchForm, { keyword: '', statusTags: [], capitalTags: [], yearTags: [] })
  errorMessage.value = ''
}
</script>

<style scoped>
.advanced-page {
  min-height: 100vh;
  background-color: #f6f7ff; /* 极淡的冷紫色底 */
  font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
}

/* Header: 更有质感的紫色渐变 */
.header {
  height: 55px;
  background: linear-gradient(135deg, #6648ff 0%, #8b6eff 100%);
  display: flex;
  align-items: center;
  padding: 0 25px;
  color: white;
  box-shadow: 0 2px 12px rgba(102, 72, 255, 0.15);
  position: relative;
}
.logo { font-size: 19px; font-weight: 800; cursor: pointer; margin-right: auto; letter-spacing: 1px; }
.header-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 17px;
  font-weight: 600;
}
.back-btn { background: rgba(255,255,255,0.15); color: white; border: none; border-radius: 18px; padding: 8px 15px; }
.back-btn:hover { background: rgba(255,255,255,0.25); color: white; }

.main-content {
  max-width: 1100px;
  margin: 40px auto;
  padding: 0 20px;
}
.filter-container {
  background: white;
  border: 1px solid #e2e5f0;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(102, 72, 255, 0.05);
  overflow: hidden;
}

.filter-row {
  display: flex;
  border-bottom: 1px solid #f0f3f7;
  min-height: 54px;
}
.filter-row.no-border { border-bottom: none; }

.row-label {
  width: 140px;
  background-color: #f9faff; /* 极淡紫灰 */
  color: #4e5969;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  padding-left: 25px;
  border-right: 1px solid #f0f3f7;
  flex-shrink: 0;
}
.row-label.align-center { align-items: flex-start; padding-top: 18px; }

.row-content { flex: 1; padding: 12px 30px; display: flex; align-items: center; flex-wrap: wrap; }

.status-col { flex: 1; display: flex; flex-direction: column; }
.status-sub-row {
  display: flex;
  align-items: center;
  padding: 12px 30px;
  border-bottom: 1px solid #f0f3f7;
}

.parent-check { width: 120px; margin-right: 30px !important; }
:deep(.el-checkbox__label) { color: #606266; font-size: 13.5px; }

/* 🌟 修改选中颜色为核心紫 */
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #7c4dff;
  border-color: #7c4dff;
}
:deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #6648ff;
  font-weight: 500;
}

.keyword-input { width: 380px; }
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #7c4dff inset !important;
}

/* 🌟 Footer 按钮优化 */
.action-footer {
  padding: 50px 0;
  display: flex;
  justify-content: center;
  gap: 25px;
}
.search-btn {
  width: 160px; height: 46px; font-size: 16px; font-weight: 600;
  background: linear-gradient(90deg, #6e4aff 0%, #9065ff 100%);
  border: none;
  border-radius: 23px;
  box-shadow: 0 4px 15px rgba(110, 74, 255, 0.3);
  letter-spacing: 2px;
}
.search-btn:hover { transform: translateY(-2px); opacity: 0.95; box-shadow: 0 6px 20px rgba(110, 74, 255, 0.4); }

.reset-btn { width: 130px; height: 46px; border-radius: 23px; color: #86909c; border: 1px solid #e5e6eb; }
.reset-btn:hover { background: #f7f8fa; border-color: #c9cdd4; }

.inline-error {
  margin-left: 15px; color: #f56c6c; font-size: 13px;
  background: #fef0f0; padding: 5px 15px; border-radius: 6px;
}
</style>