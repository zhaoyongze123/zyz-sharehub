import type { RouteRecordRaw } from 'vue-router'

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/EmptyLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/public/HomeView.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/public/LoginView.vue'),
        meta: { title: '登录' }
      }
    ]
  },
  {
    path: '/',
    component: () => import('@/layouts/ConsoleLayout.vue'),
    children: [
      {
        path: 'auth/callback',
        name: 'auth-callback',
        component: () => import('@/views/public/AuthCallbackView.vue'),
        meta: { title: '登录回调' }
      },
      {
        path: 'resources',
        name: 'resources',
        component: () => import('@/views/resource/ResourceListView.vue'),
        meta: { title: '资料广场' }
      },
      {
        path: 'resources/:id',
        name: 'resource-detail',
        component: () => import('@/views/resource/ResourceDetailView.vue'),
        meta: { title: '资料详情' }
      },
      {
        path: 'roadmaps',
        name: 'roadmaps',
        component: () => import('@/views/roadmap/RoadmapListView.vue'),
        meta: { title: '路线广场' }
      },
      {
        path: 'roadmaps/:id',
        name: 'roadmap-detail',
        component: () => import('@/views/roadmap/RoadmapDetailView.vue'),
        meta: { title: '路线详情' }
      },
      {
        path: 'community',
        name: 'notes',
        component: () => import('@/views/note/NoteListView.vue'),
        meta: { title: '社区' }
      },
      {
        path: 'notes/settings',
        name: 'note-settings',
        component: () => import('@/views/note/LocalNoteSettingsView.vue'),
        meta: { title: '本地库设置' }
      },
      {
        path: 'notes/:id',
        name: 'note-detail',
        component: () => import('@/views/note/NoteDetailView.vue'),
        meta: { title: '笔记详情' }
      }
    ]
  },
  {
    path: '/',
    component: () => import('@/layouts/ConsoleLayout.vue'),
    children: [
      {
        path: 'publish/resource',
        name: 'publish-resource',
        component: () => import('@/views/resource/PublishResourceView.vue'),
        meta: { title: '发布资料' }
      },
      {
        path: 'publish/roadmap',
        name: 'publish-roadmap',
        component: () => import('@/views/roadmap/PublishRoadmapView.vue'),
        meta: { title: '创建路线' }
      },
      {
        path: 'editor/note/:id?',
        name: 'note-editor',
        component: () => import('@/views/note/NoteEditorView.vue'),
        meta: { title: '笔记编辑' }
      },
      {
        path: 'resume',
        name: 'resume',
        component: () => import('@/views/resume/ResumeWorkbenchView.vue'),
        meta: { title: '简历工作台' }
      },
      {
        path: 'me',
        name: 'me',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: { title: '个人帐户' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { auth: true, admin: true },
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { title: '后台首页' }
      },
      {
        path: 'reviews',
        name: 'admin-reviews',
        component: () => import('@/views/admin/AdminReviewsView.vue'),
        meta: { title: '内容审核' }
      },
      {
        path: 'reports',
        name: 'admin-reports',
        component: () => import('@/views/admin/AdminReportsView.vue'),
        meta: { title: '举报处理' }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/AdminUsersView.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'taxonomy',
        name: 'admin-taxonomy',
        component: () => import('@/views/admin/AdminTaxonomyView.vue'),
        meta: { title: '标签分类管理' }
      }
    ]
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/public/ForbiddenView.vue'),
    meta: { title: '无权限' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/public/NotFoundView.vue'),
    meta: { title: '页面不存在' }
  }
]
