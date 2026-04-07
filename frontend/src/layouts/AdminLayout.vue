<template>
  <div class="h-screen bg-gray-50 flex overflow-hidden font-sans">
    <!-- 左侧侧边栏 -->
    <aside class="w-64 bg-slate-900 border-r border-slate-800 flex flex-col transition-all duration-300 z-20 shrink-0">
      <!-- Logo 区域 -->
      <div class="h-16 flex items-center px-6 border-b border-slate-800/60 shadow-sm shrink-0">
        <RouterLink to="/" class="flex items-center gap-2.5 group">
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-md shadow-blue-500/20 group-hover:shadow-blue-500/40 transition">
            <span class="i-carbon-cube text-white text-lg"></span>
          </div>
          <span class="text-slate-100 font-bold tracking-tight text-lg">ShareHub<span class="text-blue-400 font-medium ml-1 text-xs px-1 border border-blue-400/30 rounded">ADMIN</span></span>
        </RouterLink>
      </div>

      <!-- 导航菜单 -->
      <div class="flex-1 overflow-y-auto py-5 px-3 custom-scrollbar">
        <div class="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-3 px-3">核心看板</div>
        <nav class="space-y-1 mb-8">
          <RouterLink to="/admin" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-slate-300 hover:bg-slate-800 hover:text-white transition group focus:outline-none" active-class="bg-blue-600/10 text-blue-400 font-medium">
            <span class="i-carbon-dashboard text-lg group-hover:scale-110 transition-transform"></span>
            工作台概览
          </RouterLink>
        </nav>

        <div class="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-3 px-3">业务管理</div>
        <nav class="space-y-1 mb-8">
          <RouterLink to="/admin/reviews" class="flex items-center justify-between px-3 py-2.5 rounded-lg text-slate-300 hover:bg-slate-800 hover:text-white transition group" active-class="bg-blue-600/10 text-blue-400 font-medium">
            <div class="flex items-center gap-3">
              <span class="i-carbon-document-tasks text-lg group-hover:scale-110 transition-transform"></span>
              内容审核
            </div>
            <span class="bg-blue-500 text-white text-[10px] px-1.5 py-0.5 rounded-full font-bold">12</span>
          </RouterLink>
          <RouterLink to="/admin/reports" class="flex items-center justify-between px-3 py-2.5 rounded-lg text-slate-300 hover:bg-slate-800 hover:text-white transition group" active-class="bg-blue-600/10 text-blue-400 font-medium">
            <div class="flex items-center gap-3">
              <span class="i-carbon-warning text-lg group-hover:scale-110 transition-transform"></span>
              违规举报
            </div>
            <span class="bg-red-500 text-white text-[10px] px-1.5 py-0.5 rounded-full font-bold">3</span>
          </RouterLink>
        </nav>

        <div class="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-3 px-3">系统基础</div>
        <nav class="space-y-1">
          <RouterLink to="/admin/users" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-slate-300 hover:bg-slate-800 hover:text-white transition group" active-class="bg-blue-600/10 text-blue-400 font-medium">
            <span class="i-carbon-user-multiple text-lg group-hover:scale-110 transition-transform"></span>
            用户管理
          </RouterLink>
          <RouterLink to="/admin/taxonomy" class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-slate-300 hover:bg-slate-800 hover:text-white transition group" active-class="bg-blue-600/10 text-blue-400 font-medium">
            <span class="i-carbon-tag-group text-lg group-hover:scale-110 transition-transform"></span>
            标签与分类
          </RouterLink>
        </nav>
      </div>

      <!-- 底部用户信息 -->
      <div class="p-4 border-t border-slate-800/80 bg-slate-900/50 shrink-0">
        <div class="flex items-center gap-3 group cursor-pointer p-2 rounded-lg hover:bg-slate-800 transition">
          <img src="https://api.dicebear.com/7.x/notionists/svg?seed=Admin" class="w-9 h-9 rounded-full bg-slate-800 border border-slate-700" alt="Admin Avatar">
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium text-slate-200 truncate group-hover:text-white transition">Admin System</div>
            <div class="text-xs text-slate-500 truncate mt-0.5">超级管理员</div>
          </div>
          <span class="i-carbon-logout text-slate-500 hover:text-red-400 transition" title="退出登录"></span>
        </div>
      </div>
    </aside>

    <!-- 右侧主体内容区 -->
    <div class="flex-1 flex flex-col min-w-0 overflow-hidden bg-gray-50/50">
      <!-- 顶部 Header -->
      <header class="h-16 bg-white border-b flex items-center justify-between px-6 shadow-sm z-10 shrink-0">
        <div class="flex items-center gap-4">
          <button class="text-gray-400 hover:text-gray-700 transition lg:hidden">
            <span class="i-carbon-menu text-2xl"></span>
          </button>
          
          <!-- 面包屑辅助显示 -->
          <div class="hidden sm:flex items-center text-sm text-gray-500">
            <span class="i-carbon-home mr-2"></span>
            <span class="mx-2 text-gray-300">/</span>
            <span class="font-medium text-gray-800">{{ currentRouteName }}</span>
          </div>
        </div>

        <div class="flex items-center gap-5">
          <!-- 全局搜索 -->
          <div class="relative hidden md:block">
            <span class="i-carbon-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"></span>
            <input type="text" placeholder="快捷搜索 (Cmd+K)" class="pl-9 pr-4 py-1.5 bg-gray-100 border-transparent rounded-full text-sm w-64 focus:bg-white focus:border-blue-500 focus:ring-2 focus:ring-blue-200 transition outline-none">
          </div>

          <!-- 通知中心 -->
          <button class="relative text-gray-400 hover:text-blue-600 transition">
            <span class="i-carbon-notification text-xl block"></span>
            <span class="absolute top-0 right-0 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
          </button>
          
          <!-- 前台首页入口 -->
          <RouterLink to="/" class="text-sm font-medium text-blue-600 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-full transition flex items-center gap-1.5">
            <span class="i-carbon-launch block text-base"></span>
            返回前台
          </RouterLink>
        </div>
      </header>

      <!-- 主要内容区域 (路由出口带滚动条) -->
      <main class="flex-1 overflow-y-auto p-6 md:p-8 custom-scrollbar relative">
        <!-- 路由过渡动画 -->
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()

// 简单的路由名映射 (也可以从 route.meta 读取)
const routeNameMap: Record<string, string> = {
  'AdminDashboard': '工作台概览',
  'AdminReviews': '内容审核',
  'AdminReports': '违规举报处理',
  'AdminUsers': '系统用户管理',
  'AdminTaxonomy': '系统标签与分类'
}

const currentRouteName = computed(() => {
  return route.name ? routeNameMap[route.name as string] || '管理中心' : '管理中心'
})
</script>

<style scoped>
/* 自定义滚动条风格 */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.3);
  border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.5);
}

/* 简单的路由渐变动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
