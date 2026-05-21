<template>
  <div class="detail-page" v-if="entData">

    <div class="page-header-actions">
      <el-button type="primary" color="#7c4dff" :icon="Download" @click="exportToPDF" :loading="isExporting">
        {{ isExporting ? '生成中，请稍候...' : '导出核心数据报表 (PDF)' }}
      </el-button>
    </div>

    <div class="ent-header">
      <div class="logo-box">{{ entData.name ? entData.name.substring(0, 2) : '企' }}</div>
      <div class="ent-info-main">
        <h1>{{ entData.name }}</h1>
        <div class="tags">
          <el-tag class="purple-tag">{{ entData.businessType || '企业' }}</el-tag>
          <el-tag type="info" class="ml-2">{{ entData.industry || '通用行业' }}</el-tag>
          <el-tag type="primary" effect="plain" class="ml-2 purple-plain-tag">{{ entData.statusTag || '存续' }}</el-tag>
        </div>

        <div class="contact-card">
          <div class="contact-item">
            <el-icon><Phone /></el-icon>
            <span class="label">电话：</span>
            <span class="value">{{ entData.tel || '暂无公布' }}</span>
          </div>
          <div class="contact-item">
            <el-icon><Message /></el-icon>
            <span class="label">邮箱：</span>
            <span class="value">
              <a v-if="entData.email" :href="'mailto:' + entData.email" class="contact-link">{{ entData.email }}</a>
              <span v-else>暂无公布</span>
            </span>
          </div>
          <div class="contact-item">
            <el-icon><Link /></el-icon>
            <span class="label">官网：</span>
            <span class="value">
              <a v-if="entData.website" :href="ensureAbsoluteUrl(entData.website)" target="_blank" class="contact-link">{{ entData.website }}</a>
              <span v-else>暂无公布</span>
            </span>
          </div>
          <div class="contact-item address-item">
            <el-icon><Location /></el-icon>
            <span class="label">地址：</span>
            <span class="value">{{ entData.address || entData.registeredAddress || '暂无公布' }}</span>
          </div>
        </div>

        <div class="header-sub-info">
          <span>统一社会信用代码：{{ entData.creditCode }}</span>
          <span class="divider">|</span>
          <span>成立日期：{{ entData.establishDate }}</span>
        </div>
      </div>
    </div>

    <div class="ai-summary-card">
      <div class="ai-header">
        <div class="ai-title"><span class="ai-icon">✨</span> AI 商业全景诊断</div>
        <!-- 保留原本的点击生成按钮 -->
        <el-button v-if="!aiState.started" color="#7c4dff" round class="ai-btn" @click="startAiSummary">
          点击生成 AI 诊断报告
        </el-button>
        <div v-else class="ai-actions">
          <el-button size="small" color="#7c4dff" plain round :disabled="aiState.isThinking" @click="regenerateAiSummary">
            <el-icon class="action-icon"><Refresh /></el-icon> 重新生成
          </el-button>
          <el-button size="small" type="info" plain round @click="closeAiSummary">
            <el-icon class="action-icon"><Close /></el-icon> 关闭
          </el-button>
        </div>
      </div>

      <div v-if="aiState.started" class="ai-content-box">
        <div v-if="aiState.isThinking && !aiState.text" class="thinking-box">
          <div class="spinner"></div><span>AI Agent 正在深度阅读企业多维档案...</span>
        </div>
        <div v-else class="typewriter-text" v-html="formattedAiText"></div>
      </div>
    </div>

    <el-tabs v-model="activeTab" type="border-card" class="detail-tabs">
      <el-tab-pane label="工商信息" name="basic">
        <el-descriptions :column="2" border title="基本信息">
          <el-descriptions-item label="企业名称">{{ entData.name }}</el-descriptions-item>
          <el-descriptions-item label="信用代码">{{ entData.creditCode }}</el-descriptions-item>
          <el-descriptions-item label="注册资本">{{ formatCapital(entData.regCapital) }}</el-descriptions-item>
          <el-descriptions-item label="成立日期">{{ entData.establishDate }}</el-descriptions-item>
          <el-descriptions-item label="人员规模">{{ entData.staffNumber || '0' }} 人</el-descriptions-item>
          <el-descriptions-item label="所属行业">{{ entData.industry }}</el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">
            <div class="overview-text">{{ entData.businessScope || '暂无简介' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane :label="`股东信息 ${entData.shareholderList?.length ? '('+entData.shareholderList.length+')' : ''}`" name="shareholders">
        <div class="shareholder-container">
          <div v-show="entData.shareholderList?.length > 0" ref="pieChartRef" class="shareholder-chart"></div>
          <el-table :data="entData.shareholderList" stripe border style="width: 100%">
            <!-- 取消股权这里的点击跳转，改回普通的文本展示 -->
            <el-table-column prop="shareholderName" label="股东名称" min-width="200" />
            <el-table-column prop="ownershipProportion" label="持股比例" align="center" width="150" />
            <el-table-column prop="shareholderType" label="股东类型" align="center" width="150">
              <template #default="scope">
                <el-tag v-if="scope.row.shareholderType === '10'" class="purple-tag">企业法人</el-tag>
                <el-tag v-else type="info">其他</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="`主要人员 ${entData.executiveList?.length ? '('+entData.executiveList.length+')' : ''}`" name="executives">
        <el-table :data="entData.executiveList" stripe border style="width: 100%">
          <el-table-column type="index" label="序号" width="80" align="center" />
          <el-table-column label="姓名" width="200">
            <template #default="scope">
              <el-link type="primary" :underline="false" @click="handleDeepSearch(scope.row.executiveName)" class="drill-link">
                {{ scope.row.executiveName }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column prop="position" label="职务" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="`专利信息 ${entData.patentList?.length ? '('+entData.patentList.length+')' : ''}`" name="patents">
        <!-- 专利趋势可视化图表 -->
        <div v-show="entData.patentList?.length > 0" ref="patentChartRef" class="patent-chart"></div>
        <el-table :data="entData.patentList" stripe border style="width: 100%">
          <el-table-column prop="patentName" label="专利名称" />
          <el-table-column prop="type" label="专利类型" width="150" align="center" />
          <el-table-column prop="applicationDate" label="申请日期" width="150" align="center" />
          <el-table-column prop="openNumber" label="公开(公告)号" width="200" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="`企业资质 ${entData.qualificationList?.length ? '('+entData.qualificationList.length+')' : ''}`" name="qualifications">
        <el-table :data="entData.qualificationList" stripe border style="width: 100%">
          <el-table-column prop="category" label="荣誉/资质名称" />
          <el-table-column prop="year" label="认定年份" width="120" align="center" />
          <el-table-column prop="batch" label="批次" width="120" align="center" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="主导产品及业务概览" name="business">
        <div class="business-detail-container">
          <div class="business-content-card">
             <span :class="{ 'red-text': entData.businessOverview === '未检索到明确信息' }">
                {{ entData.businessOverview || '暂无详细业务信息' }}
             </span>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="`发展历程 (${companyTimeline.length})`" name="timeline" v-if="companyTimeline.length > 0">
        <div class="timeline-container">
          <el-timeline>
            <el-timeline-item v-for="(item, index) in companyTimeline" :key="index" :timestamp="item.displayDate || item.date" :color="item.color">
              {{ item.content }}
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-tab-pane>

    </el-tabs>
  </div>

  <div v-else class="loading-box">
    <el-skeleton :rows="10" animated />
  </div>

  <div id="pdf-report-layout" style="display: none;" v-if="entData">
    <div class="pdf-report-container">
      <div class="pdf-cover">
        <h1 class="pdf-title">{{ entData.name }}</h1>
        <p class="pdf-subtitle">企业核心数据调研报告</p>
        <p class="pdf-meta">生成日期：{{ new Date().toLocaleDateString() }}</p>
      </div>
      <div class="pdf-section">
        <h3 class="pdf-section-title">一、基本信息</h3>
        <table class="pdf-table">
          <tr><th>企业名称</th><td>{{ entData.name }}</td><th>信用代码</th><td>{{ entData.creditCode }}</td></tr>
          <tr><th>注册资本</th><td>{{ formatCapital(entData.regCapital) }}</td><th>成立日期</th><td>{{ entData.establishDate }}</td></tr>
          <tr><th>人员规模</th><td>{{ entData.staffNumber || '0' }} 人</td><th>所属行业</th><td>{{ entData.industry }}</td></tr>
          <tr><th>企业地址</th><td colspan="3">{{ entData.address || entData.registeredAddress || '暂无公布' }}</td></tr>
        </table>
      </div>
      <div class="pdf-section" v-if="entData.shareholderList?.length > 0">
        <h3 class="pdf-section-title">二、股东信息</h3>
        <table class="pdf-table">
          <thead><tr><th>股东名称</th><th>持股比例</th><th>股东类型</th></tr></thead>
          <tbody>
          <tr v-for="item in entData.shareholderList" :key="item.shareholderName">
            <td>{{ item.shareholderName }}</td>
            <td align="center">{{ item.ownershipProportion }}%</td>
            <td align="center">{{ item.shareholderType === '10' ? '企业法人' : '其他' }}</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="pdf-section" v-if="entData.executiveList?.length > 0">
        <h3 class="pdf-section-title">三、主要人员</h3>
        <table class="pdf-table">
          <thead><tr><th>序号</th><th>姓名</th><th>职务</th></tr></thead>
          <tbody>
          <tr v-for="(item, index) in entData.executiveList" :key="index">
            <td align="center">{{ index + 1 }}</td>
            <td>{{ item.executiveName }}</td>
            <td>{{ item.position }}</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="pdf-section" v-if="entData.patentList?.length > 0">
        <h3 class="pdf-section-title">四、专利信息</h3>
        <table class="pdf-table">
          <thead><tr><th>专利名称</th><th>专利类型</th><th>申请日期</th><th>公开(公告)号</th></tr></thead>
          <tbody>
          <tr v-for="(item, index) in entData.patentList" :key="index">
            <td>{{ item.patentName }}</td>
            <td align="center">{{ item.type }}</td>
            <td align="center">{{ item.applicationDate }}</td>
            <td align="center">{{ item.openNumber }}</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="pdf-section" v-if="entData.qualificationList?.length > 0">
        <h3 class="pdf-section-title">五、企业资质</h3>
        <table class="pdf-table">
          <thead><tr><th>资质名称</th><th>认定年份</th><th>批次</th></tr></thead>
          <tbody>
          <tr v-for="(item, index) in entData.qualificationList" :key="index">
            <td>{{ item.category }}</td>
            <td align="center">{{ item.year }}</td>
            <td align="center">{{ item.batch }}</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="pdf-section">
        <h3 class="pdf-section-title">六、主导产品及业务概览</h3>
        <table class="pdf-table">
          <tbody>
          <tr>
            <td :class="{ 'red-text': entData.businessOverview === '未检索到明确信息' }" style="padding: 20px; line-height: 1.8;">
              {{ entData.businessOverview || '暂无详细业务信息' }}
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { Refresh, Close, Phone, Message, Location, Link, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import html2pdf from 'html2pdf.js'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const activeTab = ref('basic')
const entData = ref(null)

const isExporting = ref(false)

const exportToPDF = () => {
  if (isExporting.value) return
  isExporting.value = true
  ElMessage.success('正在后台生成报告，页面请勿关闭...')

  setTimeout(() => {
    const original = document.getElementById('pdf-report-layout')
    const clone = original.cloneNode(true)

    const wrapper = document.createElement('div')
    wrapper.style.position = 'absolute'
    wrapper.style.left = '-9999px'
    wrapper.style.top = '0'
    wrapper.style.width = '800px'
    wrapper.appendChild(clone)
    document.body.appendChild(wrapper)

    clone.style.display = 'block'

    const opt = {
      margin:       [10, 10],
      filename:     `${entData.value.name}_核心数据调研报告.pdf`,
      image:        { type: 'jpeg', quality: 0.98 },
      html2canvas:  {
        scale: 2,
        useCORS: true,
        logging: false,
        scrollX: 0,
        scrollY: 0
      },
      jsPDF:        { unit: 'mm', format: 'a4', orientation: 'portrait' },
      pagebreak:    { mode: ['avoid-all', 'css', 'legacy'] }
    }

    html2pdf().set(opt).from(clone).save().then(() => {
      document.body.removeChild(wrapper)
      isExporting.value = false
      ElMessage.success('数据报告导出成功！')
    }).catch(err => {
      console.error(err)
      document.body.removeChild(wrapper)
      isExporting.value = false
      ElMessage.error('导出失败，请重试')
    })
  }, 100)
}

const ensureAbsoluteUrl = (url) => (!url || /^(http|https):\/\//i.test(url)) ? url : `http://${url}`
const formatCapital = (val) => val ? (parseFloat(val) / 10000).toFixed(2) + ' 万人民币' : '未公示'

// AI 诊断逻辑
const aiState = ref({ started: false, isThinking: false, text: '' })
let summaryEs = null
const formattedAiText = computed(() => aiState.value.text.replace(/\n/g, '<br/>').replace(/\*\*/g, ''))

const startAiSummary = () => {
  aiState.value.started = true; aiState.value.isThinking = true; aiState.value.text = ''
  summaryEs = new EventSource(`/api/ai/summary/company?id=${route.params.id}`)
  summaryEs.onmessage = (event) => { aiState.value.isThinking = false; aiState.value.text += event.data }
  summaryEs.onerror = () => { if (summaryEs) { summaryEs.close(); summaryEs = null; }; aiState.value.isThinking = false }
}
const closeAiSummary = () => { if (summaryEs) { summaryEs.close(); summaryEs = null; }; aiState.value.started = false; aiState.value.text = '' }
const regenerateAiSummary = () => { closeAiSummary(); startAiSummary(); }

const handleDeepSearch = (keyword) => {
  if (!keyword) return;
  router.push({ path: '/list', query: { keyword: keyword } })
}

// 自动生成企业发展时间轴（✅ 核心修复：移除了专利展示）
const companyTimeline = computed(() => {
  if (!entData.value) return []
  let events = []

  // 1. 添加成立日期
  if (entData.value.establishDate) {
    events.push({ date: entData.value.establishDate, content: '企业正式成立', color: '#7c4dff' })
  }

  // 2. 添加企业资质
  entData.value.qualificationList?.forEach(q => {
    if(q.year) events.push({ date: `${q.year}-01-01`, displayDate: `${q.year}年`, content: `荣获企业资质：${q.category}`, color: '#67c23a' })
  })

  // 按时间倒序排列 (最新的在最上面)
  return events.sort((a, b) => new Date(b.date) - new Date(a.date))
})

// ECharts 饼图逻辑
const pieChartRef = ref(null)
let myChart = null

const initPieChart = () => {
  if (!pieChartRef.value || !entData.value?.shareholderList) return
  if (myChart) { myChart.dispose() }
  myChart = echarts.init(pieChartRef.value)
  const chartData = entData.value.shareholderList.map(item => ({
    name: item.shareholderName,
    value: parseFloat(item.ownershipProportion || 0)
  }))
  myChart.setOption({
    title: { text: '股权结构占比', left: 'center', top: '10' },
    tooltip: { trigger: 'item', formatter: '{b}: {c}% ({d}%)' },
    legend: {
      selectedMode: false,
      orient: 'vertical', left: '5%', top: 'middle', type: 'scroll',
      textStyle: { width: 180, overflow: 'truncate' }
    },
    color: ['#7c4dff', '#9e6fff', '#b388ff', '#d1c4e9', '#ede7f6', '#6366f1'],
    series: [{
      name: '持股比例', type: 'pie', center: ['65%', '55%'], radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false }, emphasis: { label: { show: false } },
      data: chartData
    }]
  })
}

// 新增专利趋势图逻辑（✅ 核心修复：使用了高级渐变色和数值标签）
const patentChartRef = ref(null)
let patentChart = null

const initPatentChart = () => {
  if (!patentChartRef.value || !entData.value?.patentList) return
  if (patentChart) { patentChart.dispose() }
  patentChart = echarts.init(patentChartRef.value)

  const yearCount = {}
  entData.value.patentList.forEach(p => {
    if (p.applicationDate) {
      const year = p.applicationDate.substring(0, 4)
      yearCount[year] = (yearCount[year] || 0) + 1
    }
  })

  const years = Object.keys(yearCount).sort()
  const counts = years.map(y => yearCount[y])

  patentChart.setOption({
    title: {
      text: '年度专利申请趋势',
      left: 'center',
      top: '15',
      textStyle: { fontSize: 16, fontWeight: '600', color: '#1a1a1a' }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: { left: '4%', right: '4%', bottom: '5%', top: '25%', containLabel: true },
    xAxis: {
      type: 'category',
      data: years,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e5e6eb' } },
      axisLabel: { color: '#86909c', margin: 12 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: {
        lineStyle: { type: 'dashed', color: '#f2f3f5' }
      },
      axisLabel: { color: '#86909c' }
    },
    series: [{
      data: counts,
      type: 'bar',
      barMaxWidth: 35,
      label: {
        show: true,
        position: 'top',
        color: '#7c4dff',
        fontWeight: 'bold',
        fontSize: 14,
        distance: 10
      },
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#9e6fff' },
          { offset: 1, color: '#6366f1' }
        ]),
        borderRadius: [6, 6, 0, 0],
        shadowColor: 'rgba(124, 77, 255, 0.2)',
        shadowBlur: 10,
        shadowOffsetY: 2
      }
    }]
  })
}

watch(activeTab, (newTab) => {
  if (newTab === 'shareholders') { setTimeout(() => { initPieChart(); myChart?.resize(); }, 200); }
  if (newTab === 'patents') { setTimeout(() => { initPatentChart(); patentChart?.resize(); }, 200); }
})

watch(entData, (newData) => {
  if (newData && activeTab.value === 'shareholders') { setTimeout(() => { initPieChart(); myChart?.resize(); }, 200); }
  if (newData && activeTab.value === 'patents') { setTimeout(() => { initPatentChart(); patentChart?.resize(); }, 200); }
}, { deep: true })

const handleResize = () => {
  myChart?.resize();
  patentChart?.resize();
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  try {
    const res = await axios.get(`/api/enterprise/detail/${route.params.id}`)
    if (res.data.code === 0) {
      entData.value = res.data.data
    }
  } catch (error) { console.error("详情加载失败", error) }
})

onUnmounted(() => {
  if (summaryEs) summaryEs.close()
  window.removeEventListener('resize', handleResize)
  if (myChart) myChart.dispose()
  if (patentChart) patentChart.dispose()
})
</script>

<style scoped>
/* ================= 网页原有 UI 样式 ================= */
.detail-page { max-width: 1200px; margin: 30px auto; padding: 0 20px; position: relative; }
.page-header-actions { display: flex; justify-content: flex-end; margin-bottom: 15px; }
.ent-header { display: flex; background: white; padding: 30px; border-radius: 12px; margin-bottom: 20px; border: 1px solid #ecefff; box-shadow: 0 4px 20px rgba(124, 77, 255, 0.05); }
.logo-box { width: 80px; height: 80px; background: linear-gradient(135deg, #7c4dff 0%, #9e6fff 100%); color: white; font-size: 28px; font-weight: bold; display: flex; align-items: center; justify-content: center; border-radius: 12px; margin-right: 25px; box-shadow: 0 4px 12px rgba(124, 77, 255, 0.3); }
.ent-info-main { flex: 1; }
.ent-info-main h1 { margin: 0 0 10px 0; font-size: 24px; color: #1a1a1a; font-weight: 700; }
.header-sub-info { margin-top: 15px; font-size: 13px; color: #86909c; }
.divider { margin: 0 15px; color: #e5e6eb; }
.ml-2 { margin-left: 10px; }
.purple-tag { background-color: #f3f0ff; border-color: #dcd1ff; color: #7c4dff; }
.purple-plain-tag { color: #7c4dff !important; border-color: #7c4dff !important; }
.contact-card { display: flex; flex-wrap: wrap; gap: 20px; margin: 15px 0; padding: 15px 20px; background: #fcfbfe; border-radius: 8px; border: 1px dashed #dcd1ff; width: fit-content; }
.contact-item { display: flex; align-items: center; font-size: 14px; color: #4e5969; }
.contact-item .el-icon { color: #7c4dff; margin-right: 6px; font-size: 16px; }
.contact-item .label { color: #86909c; }
.contact-item .value { font-weight: 500; color: #1a1a1a; }
.contact-link { color: #7c4dff; text-decoration: none; transition: 0.3s; }
.contact-link:hover { text-decoration: underline; opacity: 0.8; }
.address-item { width: 100%; margin-top: -5px; }
.ai-summary-card { background: linear-gradient(to right, #f8f6ff, #ffffff); border: 1px solid #ebdfff; border-radius: 12px; padding: 20px 25px; margin-bottom: 25px; box-shadow: 0 4px 15px rgba(124, 77, 255, 0.08); }
.ai-header { display: flex; justify-content: space-between; align-items: center; }
.ai-title { font-size: 18px; font-weight: bold; color: #5b21b6; display: flex; align-items: center; }
.ai-actions { display: flex; gap: 12px; align-items: center; }
.ai-content-box { margin-top: 20px; padding-top: 20px; border-top: 1px dashed #dcd1ff; }
.typewriter-text { font-size: 15px; line-height: 1.8; color: #333; text-align: justify; }
.detail-tabs { border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
:deep(.el-tabs--border-card) { border: 1px solid #ecefff; }
.shareholder-chart { width: 100%; height: 380px; margin-bottom: 20px; background: #fbfaff; border-radius: 8px; border: 1px solid #f0edff; }
.red-text { color: #f53f3f; }

/* 兼容样式 */
.drill-link { font-weight: 500; color: #7c4dff; }
.drill-link:hover { opacity: 0.8; }
.patent-chart { width: 100%; height: 300px; margin-bottom: 20px; background: #fcfbfe; border-radius: 8px; border: 1px solid #f0edff; }
.timeline-container { padding: 20px 40px; }

/* ================= PDF 专属纯净排版样式 ================= */
.pdf-report-container { width: 100%; box-sizing: border-box; padding: 40px; background: #ffffff; color: #333; font-family: sans-serif; }
.pdf-cover { text-align: center; margin-bottom: 40px; padding-bottom: 30px; border-bottom: 3px solid #7c4dff; }
.pdf-title { font-size: 32px; color: #1a1a1a; margin-bottom: 15px; font-weight: bold; }
.pdf-subtitle { font-size: 22px; color: #7c4dff; margin-bottom: 10px; }
.pdf-meta { font-size: 14px; color: #86909c; }
.pdf-section { margin-bottom: 30px; page-break-inside: avoid; }
.pdf-section-title { font-size: 18px; color: #1a1a1a; font-weight: bold; border-left: 4px solid #7c4dff; padding-left: 10px; margin-bottom: 15px; }
.pdf-table { width: 100%; border-collapse: collapse; margin-bottom: 10px; font-size: 13px; }
.pdf-table th, .pdf-table td { border: 1px solid #e5e6eb; padding: 10px 12px; text-align: left; }
.pdf-table th { background-color: #f7f8fa; color: #4e5969; font-weight: 600; width: 15%; }
</style>