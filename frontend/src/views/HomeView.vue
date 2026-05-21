<template>
  <div class="home-container">
    <header class="header">
      <div class="logo">企智搜</div>
    </header>

    <main class="search-main">
      <h1 class="slogan">都在用的商业查询平台</h1>

      <div class="search-flex-row">
        <div class="input-capsule">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
              v-model="keyword"
              placeholder="输入公司名称、法人、社会统一信用代码等..."
              class="inner-input"
              @keyup.enter="handleSearch"
          />
        </div>
        <button class="btn-capsule btn-orange" @click="handleSearch">搜索</button>
        <button class="btn-capsule btn-purple" @click="goToAiSearch">AI 搜索</button>
      </div>

      <div class="search-extras">
        <div class="hot-search">
          <span class="label">热门搜索：</span>
          <span
              v-for="(word, index) in hotWords"
              :key="word"
              class="hot-item"
              :style="{ animationDelay: `${0.4 + index * 0.1}s` }"
              @click="clickHotWord(word)"
          >
            {{ word }}
          </span>
        </div>

        <div class="divider"></div>

        <el-link
            class="advanced-link"
            :underline="false"
            @click="goToAdvanced"
            style="animation-delay: 0.8s"
        >
          高级搜索
        </el-link>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import axios from 'axios'

const keyword = ref('')
const router = useRouter()
const hotWords = ref(['京东方', '青岛海尔', '宁德时代'])

const fetchHotWords = async () => {
  try {
    const response = await axios.get('/api/enterprise/hot-keywords')
    // 假设 BaseResponse 成功状态码是 200，如果是其他数字请自行修改
    if (response.data.code === 0 && response.data.data) {
      hotWords.value = response.data.data
    }
  } catch (error) {
    console.error('获取热搜词失败:', error)
  }
}

onMounted(() => {
  fetchHotWords()
})

const handleSearch = () => {
  if (!keyword.value.trim()) return
  router.push({ path: '/list', query: { keyword: keyword.value } })
}

const clickHotWord = (word) => {
  keyword.value = word
  handleSearch()
}

const goToAdvanced = () => {
  router.push('/advanced')
}

const goToAiSearch = () => {
  router.push('/ai-search-page')
}
</script>

<style scoped>
.home-container {
  height: 100vh;
  background: radial-gradient(circle at 50% 50%, #ffffff 0%, #f4f7fa 100%);
  display: flex;
  flex-direction: column;
}

.header { padding: 30px 60px; }
.logo { font-size: 22px; font-weight: 800; color: #333; }
.search-main { flex: 1; display: flex; flex-direction: column; justify-content: center; align-items: center; margin-top: -120px; }
.slogan { font-size: 42px; color: #1a1a1a; margin-bottom: 45px; font-weight: 700; }
.search-flex-row { display: flex; align-items: center; gap: 15px; margin-bottom: 25px; }

.input-capsule {
  display: flex; align-items: center; width: 500px; height: 60px; background: #fff;
  border-radius: 30px; padding: 0 25px; border: 1px solid #e0e0e0;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05); transition: all 0.3s;
}
.input-capsule:focus-within { border-color: #7c4dff; box-shadow: 0 4px 25px rgba(124, 77, 255, 0.12); }
.inner-input { flex: 1; border: none; outline: none; font-size: 17px; margin-left: 12px; }

.btn-capsule {
  height: 60px; padding: 0 35px; border-radius: 30px; border: none;
  color: white; font-size: 18px; font-weight: 600; cursor: pointer;
  transition: all 0.3s; box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.btn-orange { background: linear-gradient(135deg, #ff6b35 0%, #ff8e53 100%); }
.btn-purple { background: linear-gradient(135deg, #7c4dff 0%, #9e6fff 100%); }
.btn-capsule:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,0.12); }

.search-extras { display: flex; align-items: center; gap: 20px; }
.hot-search { font-size: 14px; color: #666; }
.hot-search .label { color: #999; }

.hot-item {
  display: inline-block; margin-right: 15px; cursor: pointer; opacity: 0;
  animation: jumpIn 0.8s cubic-bezier(0.22, 1, 0.36, 1) forwards; transition: all 0.3s ease;
}
.hot-item:hover { color: #ff6b35; transform: translateY(-3px) scale(1.05); }

.divider { width: 1px; height: 14px; background-color: #ddd; opacity: 0; animation: fadeIn 0.5s ease forwards 0.7s; }

.advanced-link {
  font-size: 14px; color: #ff6b35; font-weight: 600; opacity: 0; display: inline-block;
  animation: jumpIn 0.8s cubic-bezier(0.22, 1, 0.36, 1) forwards; transition: all 0.3s ease;
}
.advanced-link:hover { color: #7c4dff; transform: translateY(-2px); }

@keyframes jumpIn {
  0% { opacity: 0; transform: translateY(30px) scale(0.9); }
  70% { transform: translateY(-5px) scale(1.02); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
</style>