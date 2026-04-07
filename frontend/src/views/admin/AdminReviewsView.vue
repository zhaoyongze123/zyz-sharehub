<template>
  <div class="admin-dashboard-shell pb-8 h-full flex flex-col min-h-0">
    <div class="mb-4 flex justify-between items-end shrink-0">
      <div>
        <h1 class="text-2xl font-bold text-gray-800 tracking-tight">内容审核</h1>
        <p class="text-gray-500 text-sm mt-1">负责审核社区新发布的笔记与上传资料，保障社区环境。</p>
      </div>
      <div class="flex gap-2">
        <button class="bg-blue-50 text-blue-600 border border-blue-200 rounded-lg px-3 py-1.5 text-sm font-medium hover:bg-blue-100 transition">
          批量通过
        </button>
        <button class="bg-red-50 text-red-600 border border-red-200 rounded-lg px-3 py-1.5 text-sm font-medium hover:bg-red-100 transition">
          批量驳回
        </button>
      </div>
    </div>

    <!-- 解决撑开父元素，使用 min-h-0 和 h-full -->
    <div class="flex gap-6 flex-1 min-h-0 overflow-hidden">
      
      <!-- 左侧：待审列表列 -->
      <div class="w-1/2 flex flex-col bg-white border shadow-sm rounded-xl overflow-hidden shrink-0">
        <!-- 列表头部风控筛选 -->
        <div class="px-4 py-3 border-b flex items-center justify-between bg-gray-50 shrink-0">
          <div class="flex gap-2">
            <button class="px-2 py-1 text-xs rounded bg-white border border-gray-300 font-medium text-gray-700 shadow-sm">全部待审 12</button>
            <button class="px-2 py-1 text-xs rounded text-gray-500 hover:bg-gray-200 transition">机审高危 2</button>
            <button class="px-2 py-1 text-xs rounded text-gray-500 hover:bg-gray-200 transition">用户举报转审 1</button>
          </div>
          <span class="i-carbon-filter text-gray-500 cursor-pointer hover:text-gray-800"></span>
        </div>

        <!-- 列表区域可以滚动 -->
        <div class="flex-1 overflow-y-auto divide-y custom-scrollbar">
          <div 
            v-for="item in reviews" 
            :key="item.id"
            @click="selectedItem = item"
            class="px-5 py-4 cursor-pointer transition relative group"
            :class="selectedItem?.id === item.id ? 'bg-blue-50/50' : 'hover:bg-gray-50'">
            
            <div v-if="selectedItem?.id === item.id" class="absolute left-0 top-0 bottom-0 w-1 bg-blue-500"></div>

            <div class="flex items-start justify-between gap-4">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-1">
                  <!-- 类型角标 -->
                  <span class="px-1.5 py-0.5 rounded text-[10px] font-bold"
                        :class="item.type === '笔记' ? 'bg-indigo-100 text-indigo-700' : 'bg-orange-100 text-orange-700'">
                    {{ item.type }}
                  </span>
                  
                  <!-- 标题 -->
                  <h3 class="font-medium text-gray-800 text-sm truncate" :title="item.title">{{ item.title }}</h3>
                  
                  <!-- 风控告警 -->
                  <span v-if="item.riskLevel === 'high'" class="i-carbon-warning-alt text-red-500 text-sm shrink-0" title="风控高危"></span>
                </div>
                
                <!-- 摘要内容 -->
                <p class="text-xs text-gray-500 line-clamp-2 mb-2">
                  {{ item.summary }}
                </p>
                
                <!-- 底部元信息 -->
                <div class="flex items-center gap-4 text-[11px] text-gray-400">
                  <span class="flex items-center gap-1">
                    <img :src="item.authorAvatar" class="w-3.5 h-3.5 rounded-full" alt="">
                    {{ item.authorName }}
                  </span>
                  <span class="flex items-center gap-1">
                    <span class="i-carbon-time"></span>
                    {{ item.time }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：详情与操作列 -->
      <div class="flex-1 flex flex-col bg-white border shadow-sm rounded-xl overflow-hidden min-w-0 relative">
        
        <!-- 有选中内容时的视图 -->
        <template v-if="selectedItem">
          <!-- 审核控制台：粘性在顶部 -->
          <div class="p-4 border-b bg-gray-50 flex flex-wrap items-center justify-between shrink-0 shadow-sm z-10 w-full relative">
            <div class="flex items-center gap-3">
              <span class="w-2 h-2 rounded-full animate-pulse" :class="selectedItem.riskLevel === 'high' ? 'bg-red-500' : 'bg-yellow-400'"></span>
              <span class="text-sm font-bold text-gray-700">内容审查台</span>
            </div>
            <div class="flex gap-2">
              <button 
                @click="appStore.showToast('自动驳回', '内容存在违规', 'error'); selectedItem = null"
                class="px-4 py-1.5 rounded-lg bg-white border-2 border-red-200 text-red-600 text-sm font-medium hover:bg-red-50 hover:border-red-300 transition shadow-sm whitespace-nowrap">
                判定违规
              </button>
              <button 
                @click="appStore.showToast('审核通过', '内容已加入正常流'); selectedItem = null"
                class="px-4 py-1.5 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 transition shadow-sm whitespace-nowrap">
                通过合规
              </button>
            </div>
          </div>

          <!-- 内容详情区可以滚动 -->
          <div class="flex-1 overflow-y-auto p-6 custom-scrollbar">
            <div class="prose prose-sm prose-slate max-w-none">
              <h1 class="text-2xl font-bold mb-4 font-sans text-gray-900 leading-snug">{{ selectedItem.title }}</h1>
              
              <div class="flex flex-wrap gap-4 items-center mb-6 not-prose border-y py-3 text-sm text-gray-500">
                <div class="flex items-center gap-2">
                  <img :src="selectedItem.authorAvatar" class="w-6 h-6 rounded-full border" alt="">
                  <span class="text-gray-800 font-medium">{{ selectedItem.authorName }}</span> 
                  <span class="text-xs bg-gray-100 px-1.5 py-0.5 rounded text-gray-500 font-mono">UID: 10086</span>
                </div>
                <div class="w-px h-4 bg-gray-300 hidden sm:block"></div>
                <div class="flex items-center gap-1.5 text-xs"><span class="i-carbon-time"></span> 当前发布时间: {{ selectedItem.time }}</div>
              </div>

              <!-- 模拟风控结果 -->
              <div v-if="selectedItem.riskLevel === 'high'" class="mb-6 p-4 bg-red-50/50 border border-red-100 rounded-lg not-prose flex items-start gap-3">
                <span class="i-carbon-warning-alt text-xl text-red-600 mt-0.5 shrink-0"></span>
                <div>
                  <h4 class="text-red-800 text-sm font-bold mb-1">系统机器拦截规则触发</h4>
                  <ul class="list-disc pl-4 text-xs text-red-700 space-y-1">
                    <li>疑似包含站外引流微信号/QQ群号</li>
                    <li>包含黑灰产破解软件相关敏感词汇</li>
                  </ul>
                </div>
              </div>

              <!-- 内容正文 -->
              <div class="text-gray-700 text-sm md:text-base leading-loose break-words whitespace-pre-wrap font-sans mt-2">
                <p v-for="(paragraph, index) in selectedItem.content.split('\n\n')" :key="index" class="mb-4">
                  {{ paragraph }}
                </p>
              </div>
            </div>
          </div>
        </template>
        
        <!-- 空状态 -->
        <div v-else class="flex-1 flex flex-col items-center justify-center text-gray-400 bg-gray-50/50 absolute inset-0">
          <div class="w-20 h-20 rounded-full bg-gray-100 flex items-center justify-center mb-4 shadow-inner">
            <span class="i-carbon-view text-3xl text-gray-300"></span>
          </div>
          <h3 class="text-lg font-medium text-gray-500">从左侧选择记录进行审核预览</h3>
          <p class="text-sm mt-2 max-w-xs text-center text-gray-400">在此处完整审查用户发帖、配图及敏感词命中情况，保障业务安全。</p>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

interface ReviewItem {
  id: number
  type: string
  title: string
  summary: string
  content: string
  authorName: string
  authorAvatar: string
  time: string
  riskLevel: 'normal' | 'high'
}

const selectedItem = ref<ReviewItem | null>(null)

const reviews = ref<ReviewItem[]>([
  {
    id: 101,
    type: '笔记',
    title: '手把手教你破解最新的商业版WebStorm',
    summary: '本文将详细介绍如何通过修改vmoptions文件并载入特殊JAR包从而永久试用JB系列软件...',
    content: '这篇文章因为版权合规要求禁止发布。大家可以直接加我QQ群获取激活补丁和注册码。\n\n群号：987654321。里面有各种大厂资料库。每天稳定更新破戒版软件和游戏包。\n\n注意这只是技术交流并不构成违法。由于最近风控比较严，大家请尽快加群保存。如果你需要开发教程，也可以在我的博客找：http://spam-site.com',
    authorName: '匿名黑客',
    authorAvatar: 'https://api.dicebear.com/7.x/notionists/svg?seed=Hack',
    time: '15分钟前',
    riskLevel: 'high'
  },
  {
    id: 102,
    type: '资料',
    title: '2025架构师全景脑图总结',
    summary: '总结了我在阿里大厂多年的架构摸爬滚打经验，这是一份价值连城的进阶书...',
    content: '第一章：分布式理论基础。\n讨论微服务下的CAP定理和Base理论的取舍，如何在业务中平衡强一致性与最终一致性。\n\n第二章：中间件实战运用技巧。\nRedis的穿透雪崩击穿终极解决方案与布隆过滤器的实际落地代码。消息队列Kafka和RocketMQ的高可用集群搭建。ES底层倒排索引实现原理剖析。\n\n第三章：DDD领域驱动设计。如何拆分聚合根与实体限界上下文。',
    authorName: 'Java打工人',
    authorAvatar: 'https://api.dicebear.com/7.x/notionists/svg?seed=Jack',
    time: '2小时前',
    riskLevel: 'normal'
  }
])
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(156, 163, 175, 0.3);
  border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(156, 163, 175, 0.5);
}
</style>
