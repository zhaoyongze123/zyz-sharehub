import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../pages/HomePage.vue'
import ResourceListPage from '../pages/ResourceListPage.vue'
import ResourceDetailPage from '../pages/ResourceDetailPage.vue'
import PublishPage from '../pages/PublishPage.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomePage },
    { path: '/resources', component: ResourceListPage },
    { path: '/resources/:id', component: ResourceDetailPage },
    { path: '/publish', component: PublishPage }
  ]
})
