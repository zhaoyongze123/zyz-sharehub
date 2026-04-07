<template>
  <div class="admin-dashboard-shell pb-8">
    <div class="mb-8 flex justify-between items-end">
      <div>
        <h1 class="text-2xl font-bold text-gray-800 tracking-tight">标签与分类管理</h1>
        <p class="text-gray-500 text-sm mt-1">定制全局资源分类结构与笔记标签库，优化用户的检索体验。</p>
      </div>
      <button class="bg-blue-600 text-white rounded-lg px-4 py-2 text-sm font-medium hover:bg-blue-700 flex items-center gap-2 shadow-sm transition">
        <span class="i-carbon-add text-lg"></span> 新建分类/标签
      </button>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <!-- 资源大分类管理 -->
      <div class="bg-white rounded-xl shadow-sm border flex flex-col h-[600px]">
        <div class="px-5 py-4 border-b bg-gray-50 flex items-center justify-between">
          <h2 class="font-bold text-gray-800 flex items-center gap-2">
            <span class="i-carbon-folder text-blue-500 text-lg"></span> 资源架构树 (Categories)
          </h2>
          <span class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded font-medium">8 个大类</span>
        </div>
        
        <div class="p-4 flex-1 overflow-auto">
          <div class="space-y-3">
            <!-- 模拟树形结构 -->
            <div v-for="category in categories" :key="category.id" class="border rounded-lg overflow-hidden group">
              <div class="bg-white flex items-center justify-between p-3 hover:bg-gray-50 transition cursor-pointer">
                <div class="flex items-center gap-3">
                  <span class="i-carbon-chevron-down text-gray-400"></span>
                  <div class="font-medium text-gray-800">{{ category.name }}</div>
                  <span class="text-xs text-gray-400">{{ category.count }} 份资源</span>
                </div>
                <div class="flex gap-2 opacity-0 group-hover:opacity-100 transition">
                  <button class="text-gray-400 hover:text-blue-500"><span class="i-carbon-edit"></span></button>
                  <button class="text-gray-400 hover:text-red-500"><span class="i-carbon-trash-can"></span></button>
                </div>
              </div>
              
              <!-- 子分类 -->
              <div v-if="category.children && category.children.length > 0" class="bg-gray-50 border-t py-1 pl-10 pr-3">
                <div v-for="sub in category.children" :key="sub.id" class="flex items-center justify-between py-2 group/sub">
                  <div class="flex items-center gap-2 text-sm text-gray-600">
                    <span class="w-1.5 h-1.5 rounded-full bg-gray-300"></span>
                    {{ sub.name }}
                  </div>
                  <div class="flex gap-2 opacity-0 group-hover/sub:opacity-100 transition">
                    <button class="text-gray-400 hover:text-blue-500 text-xs"><span class="i-carbon-edit"></span></button>
                  </div>
                </div>
                <button class="text-xs text-blue-500 hover:text-blue-700 py-1 flex items-center gap-1 mt-1">
                  <span class="i-carbon-add"></span> 添加子分类
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 笔记标签群管理 -->
      <div class="bg-white rounded-xl shadow-sm border flex flex-col h-[600px]">
        <div class="px-5 py-4 border-b bg-gray-50 flex items-center justify-between">
          <h2 class="font-bold text-gray-800 flex items-center gap-2">
            <span class="i-carbon-tag text-purple-500 text-lg"></span> 热门标签库 (Tags)
          </h2>
          <div class="relative">
            <span class="i-carbon-search absolute left-2 top-1/2 -translate-y-1/2 text-gray-400 text-xs"></span>
            <input type="text" placeholder="搜索标签..." class="pl-7 pr-2 py-1 text-xs border rounded bg-white w-32 focus:w-40 transition-all outline-none focus:border-purple-400">
          </div>
        </div>
        
        <div class="p-5 flex-1 overflow-auto">
          <div class="flex flex-wrap gap-2.5">
            <div v-for="tag in tags" :key="tag.id" 
                 class="group inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-sm border bg-white hover:border-purple-300 hover:shadow-sm transition cursor-pointer">
              <span class="text-gray-700">{{ tag.name }}</span>
              <span class="text-[10px] text-gray-400 bg-gray-100 px-1 rounded">{{ tag.useCount }}</span>
              <button class="w-4 h-4 rounded-full flex items-center justify-center bg-gray-100 text-gray-400 hover:bg-red-100 hover:text-red-500 opacity-0 group-hover:opacity-100 transition ml-1">
                <span class="i-carbon-close text-[10px]"></span>
              </button>
            </div>
          </div>
          
          <div class="mt-8 pt-6 border-t border-dashed relative">
            <span class="absolute -top-3 left-4 bg-white px-2 text-xs font-semibold text-gray-400">近期系统屏蔽的违规标签</span>
            <div class="flex flex-wrap gap-2">
              <span v-for="tag in bannedTags" :key="tag" class="inline-flex items-center px-2.5 py-1 rounded text-xs border border-red-200 bg-red-50 text-red-600 line-through">
                {{ tag }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const categories = ref([
  {
    id: 1, name: '前端开发', count: 1245,
    children: [
      { id: 11, name: 'Vue.js 相关' },
      { id: 12, name: 'React 生态' },
      { id: 13, name: 'CSS 架构与动画' }
    ]
  },
  {
    id: 2, name: '后端开发', count: 3042,
    children: [
      { id: 21, name: 'Java 并发编程' },
      { id: 22, name: 'Go 语言微服务' },
      { id: 23, name: '数据库设计' }
    ]
  },
  {
    id: 3, name: '人工智能', count: 856,
    children: [
      { id: 31, name: '机器学习理论' },
      { id: 32, name: '大模型应用开发' }
    ]
  },
  {
    id: 4, name: '面试面经', count: 4230,
    children: []
  }
])

const tags = ref([
  { id: 1, name: '面经', useCount: '12k' },
  { id: 2, name: '源码分析', useCount: '8k' },
  { id: 3, name: 'Spring Boot', useCount: '5.4k' },
  { id: 4, name: '大厂晋升', useCount: '3.2k' },
  { id: 5, name: '性能优化', useCount: '2.8k' },
  { id: 6, name: 'Redis', useCount: '2.1k' },
  { id: 7, name: 'K8s 实战', useCount: '1.9k' },
  { id: 8, name: 'Vite', useCount: '1.5k' },
  { id: 9, name: '避坑指南', useCount: '1.2k' },
  { id: 10, name: '全栈路线', useCount: '980' }
])

const bannedTags = ref(['代练', '翻墙教程', '内部泄漏', '黑产工具', '菠菜引流'])
</script>
