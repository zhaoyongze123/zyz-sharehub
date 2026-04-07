<template>
  <div class="admin-dashboard-shell pb-8">
    <div class="mb-8 flex justify-between items-end">
      <div>
        <h1 class="text-2xl font-bold text-gray-800 tracking-tight">用户管理</h1>
        <p class="text-gray-500 text-sm mt-1">网站全量用户检索、状态控制与权限下发。</p>
      </div>
      <div class="flex gap-3">
        <button class="bg-white border rounded-lg px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2 shadow-sm transition">
          <span class="i-carbon-export text-lg"></span> 导出数据
        </button>
      </div>
    </div>

    <div class="bg-white rounded-xl shadow-sm border mb-6 p-4">
      <div class="flex flex-wrap gap-4 items-center">
        <div class="flex-1 min-w-[200px] relative">
          <span class="i-carbon-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-lg"></span>
          <input type="text" placeholder="搜索 用户ID / 昵称 / 邮箱 / 手机号..." class="w-full pl-9 pr-4 py-2 bg-gray-50 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition">
        </div>
        
        <div class="flex items-center gap-2">
          <select class="bg-gray-50 border rounded-lg px-3 py-2 text-sm text-gray-700 focus:outline-none shrink-0">
            <option value="">全部状态</option>
            <option value="active">状态: 正常</option>
            <option value="banned">状态: 封禁</option>
            <option value="muted">状态: 禁言</option>
          </select>

          <select class="bg-gray-50 border rounded-lg px-3 py-2 text-sm text-gray-700 focus:outline-none shrink-0">
            <option value="">全部角色</option>
            <option value="user">角色: 普通用户</option>
            <option value="author">角色: 认证创作者</option>
            <option value="admin">角色: 系统管理</option>
          </select>
        </div>
        
        <button class="bg-blue-600 text-white rounded-lg px-6 py-2 text-sm hover:bg-blue-700 shadow-sm transition shrink-0">
          筛选
        </button>
      </div>
    </div>

    <div class="bg-white rounded-xl shadow-sm border overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-gray-50 border-b text-sm text-gray-500">
              <th class="py-3 px-5 font-medium w-16 text-center">UID</th>
              <th class="py-3 px-4 font-medium">用户信息</th>
              <th class="py-3 px-4 font-medium">角色与等级</th>
              <th class="py-3 px-4 font-medium">账户状态</th>
              <th class="py-3 px-4 font-medium">注册时间</th>
              <th class="py-3 px-5 font-medium text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y text-sm">
            <tr v-for="user in users" :key="user.id" class="hover:bg-gray-50 transition">
              <td class="py-3 px-5 text-gray-500 text-center font-mono text-xs">{{ user.id }}</td>
              <td class="py-3 px-4">
                <div class="flex items-center gap-3">
                  <img :src="user.avatar" class="w-10 h-10 rounded-full border bg-white object-cover" alt="avatar">
                  <div>
                    <div class="font-medium text-gray-800">{{ user.nickname }}</div>
                    <div class="text-xs text-gray-500 mt-0.5">{{ user.email }}</div>
                  </div>
                </div>
              </td>
              <td class="py-3 px-4">
                <div class="flex flex-col gap-1.5 items-start">
                  <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium"
                        :class="[
                          user.role === 'admin' ? 'bg-purple-100 text-purple-700' : '',
                          user.role === 'author' ? 'bg-orange-100 text-orange-700' : '',
                          user.role === 'user' ? 'bg-gray-100 text-gray-600' : ''
                        ]">
                    <span :class="[
                          user.role === 'admin' ? 'i-carbon-security' : '',
                          user.role === 'author' ? 'i-carbon-certificate-check' : '',
                          user.role === 'user' ? 'i-carbon-user' : ''
                    ]"></span>
                    {{ user.roleName }}
                  </span>
                  <span class="text-xs text-blue-600 font-medium bg-blue-50 px-2 py-0.5 rounded">Lv.{{ user.level }}</span>
                </div>
              </td>
              <td class="py-3 px-4">
                <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium"
                      :class="user.status === 'active' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'">
                  <span class="w-1.5 h-1.5 rounded-full" :class="user.status === 'active' ? 'bg-green-500' : 'bg-red-500'"></span>
                  {{ user.status === 'active' ? '正常' : '已封禁' }}
                </span>
                <div v-if="user.status !== 'active'" class="text-[10px] text-red-400 mt-1">发帖违规惩罚中</div>
              </td>
              <td class="py-3 px-4 text-gray-500 text-xs">
                <div>{{ user.regDate }}</div>
                <div class="mt-0.5 text-gray-400">IP: {{ user.regIp }}</div>
              </td>
              <td class="py-3 px-5 text-right">
                <div class="flex items-center justify-end gap-2">
                  <button class="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded transition" title="编辑信息">
                    <span class="i-carbon-edit text-lg block"></span>
                  </button>
                  <button 
                    class="p-1.5 hover:bg-red-50 rounded transition" 
                    :class="user.status === 'active' ? 'text-gray-400 hover:text-red-600' : 'text-green-500 hover:bg-green-50'"
                    :title="user.status === 'active' ? '封禁账号' : '解封账号'"
                    @click="toggleStatus(user)">
                    <span class="text-lg block" :class="user.status === 'active' ? 'i-carbon-locked' : 'i-carbon-unlocked'"></span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 分页 -->
      <div class="px-5 py-4 border-t flex items-center justify-between">
        <div class="text-sm text-gray-500">共 <span class="font-medium text-gray-800">12,450</span> 名用户</div>
        <div class="flex gap-1">
          <button class="w-8 h-8 flex items-center justify-center rounded border bg-gray-50 text-gray-400 cursor-not-allowed">
            <span class="i-carbon-chevron-left"></span>
          </button>
          <button class="w-8 h-8 flex items-center justify-center rounded border bg-blue-600 text-white font-medium">1</button>
          <button class="w-8 h-8 flex items-center justify-center rounded border hover:bg-gray-50 text-gray-600">2</button>
          <button class="w-8 h-8 flex items-center justify-center rounded border hover:bg-gray-50 text-gray-600">3</button>
          <button class="w-8 h-8 flex items-center justify-center rounded border hover:bg-gray-50 text-gray-600">
            <span class="i-carbon-chevron-right"></span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const users = ref([
  {
    id: 10086,
    nickname: '代码艺术家',
    email: 'coder_art@example.com',
    avatar: 'https://api.dicebear.com/7.x/notionists/svg?seed=Felix',
    role: 'author',
    roleName: '认证作者',
    level: 12,
    status: 'active',
    regDate: '2023-05-12 14:30',
    regIp: '114.212.**.**'
  },
  {
    id: 10087,
    nickname: 'Java打工人',
    email: 'java_worker@test.com',
    avatar: 'https://api.dicebear.com/7.x/notionists/svg?seed=Jack',
    role: 'user',
    roleName: '普通用户',
    level: 3,
    status: 'active',
    regDate: '2023-08-01 09:15',
    regIp: '121.22.**.**'
  },
  {
    id: 10088,
    nickname: '神秘黑客',
    email: 'hack_t00l@spam.net',
    avatar: 'https://api.dicebear.com/7.x/notionists/svg?seed=Hacker',
    role: 'user',
    roleName: '普通用户',
    level: 1,
    status: 'banned',
    regDate: '2024-01-20 22:10',
    regIp: '8.8.**.**'
  },
  {
    id: 1,
    nickname: 'AdminRoot',
    email: 'admin@sharehub.com',
    avatar: 'https://api.dicebear.com/7.x/notionists/svg?seed=Admin',
    role: 'admin',
    roleName: '超级管理',
    level: 99,
    status: 'active',
    regDate: '2022-01-01 00:00',
    regIp: '127.0.0.1'
  }
])

function toggleStatus(user: any) {
  if(confirm(`确定要${user.status === 'active' ? '封禁' : '解封'}用户 ${user.nickname} 吗？`)) {
    user.status = user.status === 'active' ? 'banned' : 'active'
    // 这里调用接口
  }
}
</script>

<style scoped>
.admin-dashboard-shell {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
