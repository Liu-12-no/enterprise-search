import { createRouter, createWebHistory } from 'vue-router'

// 1. 修改引入路径：让根路径指向 HomeView 而不是 App.vue
import HomeView from '../views/HomeView.vue'
import List from '../views/List.vue'
import Detail from '../views/Detail.vue'
import AdvancedSearch from '../views/AdvancedSearch.vue'
import AiSearchPage from '../views/AiSearchPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      // 访问 http://localhost:5173/ 时展示首页
      path: '/',
      name: 'home',
      component: HomeView // 👈 确保这里是 HomeView
    },
    {
      // 访问 http://localhost:5173/list 时展示搜索结果列表
      path: '/list',
      name: 'list',
      component: List
    },
    {
      // 访问 http://localhost:5173/detail/2 时展示企业详情
      path: '/detail/:id',
      name: 'detail',
      component: Detail
    },
    {
      path: '/advanced',
          name: 'advanced',
        component: AdvancedSearch
    },
    {
      path: '/ai-search-page',
        name: 'ai-search-page',
      component: AiSearchPage
    }
  ],
})

export default router