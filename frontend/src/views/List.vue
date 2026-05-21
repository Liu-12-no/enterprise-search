<template>
  <div class="list-container">
    <!-- AI 检索状态条 -->
    <div class="ai-status-bar" v-if="route.query.sentence">
      <div class="ai-icon">✨</div>
      <div class="ai-msg">
        AI 正在为您检索：<span class="query-text">"{{ route.query.sentence }}"</span>
      </div>
    </div>

    <!-- 结果统计 -->
    <div class="result-info" v-if="totalCount > 0">
      共找到 <span class="count">{{ totalCount }}</span> 家企业
    </div>

    <!-- 企业列表卡片 -->
    <div v-for="item in companyList" :key="item.id" class="company-card">
      <div class="card-left">
        <div class="logo-box">{{ getCleanLogoText(item.name) }}</div>
      </div>

      <div class="card-right">
        <h3 class="name">
          <span class="clickable-name" @click="goDetail(item.id)" v-html="item.name"></span>
          <el-tag size="small" type="success" effect="plain" class="status-tag">存续</el-tag>
        </h3>

        <div class="detail-row">
          <span>法定代表人：
            <span class="highlight" v-html="item.legalPerson || '未公示'"></span>
          </span>
          <span class="divider">|</span>
          <span>注册资本：{{ formatCapital(item.regCapital) }}</span>
          <span class="divider">|</span>
          <span>信用代码：
            <span v-html="item.creditCode || '-'"></span>
          </span>
        </div>

        <div class="address">地址：{{ item.address || '暂无详细地址' }}</div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="totalCount === 0 && !loading" description="未找到匹配的企业" />

    <!-- 分页区域 -->
    <div class="pagination-box" v-if="totalCount > 0">
      <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          background
          layout="prev, pager, next"
          :total="totalCount"
          @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const companyList = ref([])
const totalCount = ref(0)
const loading = ref(false)
const pageSize = ref(10)

// 初始页码从路由解析
const pageNum = ref(parseInt(route.query.pageNum) || 1)

// 金额格式化
const formatCapital = (val) => {
  if(!val) return '未公示'
  return (parseFloat(val) / 10000).toFixed(2) + ' 万人民币'
}

// 安全提取纯文本 Logo
const getCleanLogoText = (htmlString) => {
  if (!htmlString) return '企'
  const plainText = htmlString.replace(/<[^>]+>/g, '')
  return plainText.substring(0, 2)
}

const goDetail = (id) => router.push('/detail/' + id)

/**
 * 🌟 修复后的分页处理逻辑
 * 确保修改路由时，同时更新外部 pageNum 和 advancedData 内部的 pageNum
 */
const handleCurrentChange = (val) => {
  let newQuery = { ...route.query, pageNum: val }

  if (newQuery.advancedData) {
    try {
      const adv = JSON.parse(newQuery.advancedData)
      adv.pageNum = val // 同步修改 JSON 内部页码
      newQuery.advancedData = JSON.stringify(adv)
    } catch (e) {
      console.error("处理高级搜索分页参数失败", e)
    }
  }

  router.push({
    path: route.path,
    query: newQuery
  })
}

/**
 * 🌟 修复后的核心搜索逻辑
 * 解决 JSON 内部参数覆盖外部页码的问题
 */
const doSearch = async () => {
  loading.value = true

  // 1. 同步当前路由中的页码
  const currentPage = parseInt(route.query.pageNum) || 1
  pageNum.value = currentPage

  let url = ''
  let method = ''
  let finalPayload = {
    pageSize: pageSize.value,
    pageNum: currentPage // 默认优先级
  }

  // 2. 路由参数分拣
  if (route.query.sentence) {
    url = '/api/enterprise/ai-search'
    method = 'get'
    finalPayload.sentence = route.query.sentence
  } else if (route.query.advancedData) {
    url = '/api/enterprise/advancedSearch'
    method = 'post'
    try {
      const advParams = JSON.parse(route.query.advancedData)
      // 🌟 核心：合并高级搜索参数后，再次强制覆盖最新的 pageNum
      finalPayload = { ...finalPayload, ...advParams, pageNum: currentPage }
    } catch (e) {
      console.error("高级搜索参数解析失败", e)
    }
  } else {
    url = '/api/enterprise/search'
    method = 'post'
    finalPayload.keyword = route.query.keyword || ''
  }

  try {
    const res = await axios({
      method: method,
      url: url,
      params: method === 'get' ? finalPayload : null,
      data: method === 'post' ? finalPayload : null
    })

    if (res.data.code === 0) {
      companyList.value = res.data.data.records || []
      totalCount.value = res.data.data.total || 0
      window.scrollTo(0, 0) // 翻页后置顶
    }
  } catch (e) {
    console.error("请求发生异常", e)
  } finally {
    loading.value = false
  }
}

// 深度监听所有搜索相关参数的变化
watch(
    () => [route.query.keyword, route.query.sentence, route.query.advancedData, route.query.pageNum],
    (newVal, oldVal) => {
      // 逻辑判断：如果搜索关键词改变了，则重置回第一页
      const searchTermsChanged = newVal[0] !== oldVal[0] || newVal[1] !== oldVal[1] || newVal[2] !== oldVal[2]
      if (searchTermsChanged && route.query.pageNum && route.query.pageNum !== '1') {
        router.push({ path: route.path, query: { ...route.query, pageNum: 1 } })
        return
      }
      doSearch()
    }
)

onMounted(doSearch)
</script>

<style scoped>
.list-container { max-width: 900px; margin: 30px auto; }
.ai-status-bar {
  display: flex; align-items: center; gap: 12px; padding: 15px 20px;
  background: #f3f0ff; border-radius: 12px; margin-bottom: 20px; border: 1px solid #dcd1ff;
}
.query-text { color: #7c4dff; font-weight: bold; }
.result-info { margin-bottom: 15px; font-size: 14px; color: #666; }
.count { color: #f56c6c; font-weight: bold; }

.company-card {
  background: #fff; border: 1px solid #eee; padding: 22px; display: flex;
  margin-bottom: 16px; border-radius: 10px; transition: 0.3s;
}
.company-card:hover { border-color: #7c4dff; box-shadow: 0 5px 15px rgba(124, 77, 255, 0.1); }

.logo-box {
  width: 64px; height: 64px; background: #f0f2f5; color: #7c4dff;
  display: flex; align-items: center; justify-content: center;
  font-weight: bold; font-size: 22px; border-radius: 8px; margin-right: 20px;
  flex-shrink: 0;
}

.card-right { flex: 1; }
.name { font-size: 19px; margin-bottom: 10px; }
.clickable-name { color: #333; cursor: pointer; transition: color 0.2s;}
.clickable-name:hover { color: #7c4dff; }

.detail-row { font-size: 13px; color: #777; margin-bottom: 8px; }
.highlight { color: #7c4dff; }
.divider { margin: 0 10px; color: #ddd; }
.address { font-size: 13px; color: #999; margin-top: 10px; }
.pagination-box { margin-top: 30px; display: flex; justify-content: center; }
</style>