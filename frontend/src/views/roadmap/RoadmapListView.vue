<template>
  <div class="roadmap-paths">
    <header class="paths-hero">
      <div class="hero-dock" aria-hidden="true">
        <div
          class="dock-track dock-track--primary"
          @mouseleave="clearDockFocus"
        >
          <div
            v-for="(app, index) in dockIconsLoop"
            :key="`primary-${app.name}-${index}`"
            class="dock-icon"
            :style="dockIconStyle(app, 'primary', index)"
            @mouseenter="setDockFocus('primary', index)"
          >
            <span class="dock-icon__halo"></span>
            <img class="dock-icon__logo" :src="app.src" :alt="`${app.name} 官方 logo`" />
            <span class="dock-icon__mirror">
              <img class="dock-icon__logo dock-icon__logo--mirror" :src="app.src" :alt="''" />
            </span>
          </div>
        </div>
        <div
          class="dock-track dock-track--secondary"
          @mouseleave="clearDockFocus"
        >
        <div
          v-for="(app, index) in dockIconsLoop"
          :key="`secondary-${app.name}-${index}`"
          class="dock-icon"
          :style="dockIconStyle(app, 'secondary', index)"
          @mouseenter="setDockFocus('secondary', index)"
        >
            <span class="dock-icon__halo"></span>
            <img class="dock-icon__logo" :src="app.src" :alt="`${app.name} 官方 logo`" />
            <span class="dock-icon__mirror">
              <img class="dock-icon__logo dock-icon__logo--mirror" :src="app.src" :alt="''" />
            </span>
          </div>
        </div>
        <div class="dock-reflection"></div>
      </div>
      <div class="hero-content">
        <h1 class="hero-title">学习路线图</h1>
        <p class="hero-desc">精心设计的系统化成长路径，从零到一掌握核心技术栈。</p>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-num">{{ roadmaps.length }}</span>
            <span class="stat-label">开源路线</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">体系化</span>
            <span class="stat-label">知识结构</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">实战派</span>
            <span class="stat-label">工业级项目</span>
          </div>
        </div>
      </div>
    </header>

    <div class="paths-filter">
      <div class="filter-tabs">
        <button v-for="cat in categoryOptions" :key="cat.value"
                class="tab-btn"
                :class="{ active: category === cat.value }"
                @click="category = cat.value">
          {{ cat.label }}
        </button>
      </div>
      <div class="search-box">
        <div class="i-carbon-search search-icon"></div>
        <input type="text" v-model="keyword" placeholder="搜索阶段或技术..." class="search-input" />
      </div>
    </div>

    <main class="paths-content">
      <BaseErrorState v-if="status === 'error'" />
      <BaseEmpty v-else-if="status === 'empty'" title="没有匹配的路线图" description="尝试调整搜索关键词或分类。" />
      <div v-else-if="status === 'loading'" class="paths-grid">
        <BaseSkeleton v-for="item in 4" :key="item" height="24rem" />
      </div>
      <div v-else class="paths-grid">
        <RoadmapCard v-for="item in filteredRoadmaps" :key="item.id" :item="item" class="roadmap-feature-card" />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import RoadmapCard from '@/components/business/RoadmapCard.vue'
import { fetchRoadmaps, type RoadmapItem } from '@/api/roadmaps'
import cursorLogo from '@/assets/brand-icons/cursor.svg'
import figmaLogo from '@/assets/brand-icons/figma.svg'
import geminiLogo from '@/assets/brand-icons/gemini.svg'
import githubLogo from '@/assets/brand-icons/github.svg'
import googleLogo from '@/assets/brand-icons/google.svg'
import octocatLogo from '@/assets/brand-icons/octocat.svg'
import chromeLogo from '@/assets/brand-icons/chrome.svg'
import claudeLogo from '../../../../icon/claude-ai.svg'
import dockerLogo from '../../../../icon/docker.svg'
import grokLogo from '../../../../icon/grok.svg'
import openClawLogo from '../../../../icon/openclaw.svg'
import openAiLogo from '../../../../icon/openai.svg'
import vscodeLogo from '../../../../icon/vscode.svg'

const route = useRoute()
const keyword = ref(String(route.query.keyword || ''))
const category = ref('全部')
const roadmaps = ref<RoadmapItem[]>([])
const loading = ref(true)
const loadError = ref(false)

interface DockIcon {
  name: string
  src: string
  glow: string
}

const dockIcons: DockIcon[] = [
  { name: 'OpenAI', src: openAiLogo, glow: 'rgba(16, 185, 129, 0.22)' },
  { name: 'Cursor', src: cursorLogo, glow: 'rgba(255, 255, 255, 0.18)' },
  { name: 'Google', src: googleLogo, glow: 'rgba(59, 130, 246, 0.2)' },
  { name: 'Gemini', src: geminiLogo, glow: 'rgba(129, 140, 248, 0.3)' },
  { name: 'Claude', src: claudeLogo, glow: 'rgba(249, 115, 22, 0.24)' },
  { name: 'Grok', src: grokLogo, glow: 'rgba(99, 102, 241, 0.24)' },
  { name: 'Figma', src: figmaLogo, glow: 'rgba(244, 114, 182, 0.28)' },
  { name: 'GitHub', src: githubLogo, glow: 'rgba(148, 163, 184, 0.22)' },
  { name: 'OpenClaw', src: openClawLogo, glow: 'rgba(34, 197, 94, 0.2)' },
  { name: 'VS Code', src: vscodeLogo, glow: 'rgba(59, 130, 246, 0.26)' },
  { name: 'Docker', src: dockerLogo, glow: 'rgba(14, 165, 233, 0.24)' },
  { name: 'Chrome', src: chromeLogo, glow: 'rgba(250, 204, 21, 0.2)' },
  { name: 'Octocat', src: octocatLogo, glow: 'rgba(148, 163, 184, 0.24)' }
]

const dockIconsLoop = [...dockIcons, ...dockIcons]
const activeDockIcon = ref<{ track: 'primary' | 'secondary'; index: number } | null>(null)

const categoryOptions = [
  { label: '全部路线', value: '全部' },
  { label: '后端架构', value: '后端' },
  { label: '前端工程', value: '前端' },
  { label: '基础底座', value: '基础' }
]

const filteredRoadmaps = computed(() => {
  return roadmaps.value.filter((item) => {
    const matchKeyword = !keyword.value || `${item.title}${item.summary}`.toLowerCase().includes(keyword.value.toLowerCase())
    const matchCategory = category.value === '全部' || item.category === category.value
    return matchKeyword && matchCategory
  })
})

const status = computed(() => {
  if (loading.value) return 'loading'
  if (loadError.value) return 'error'
  if (!filteredRoadmaps.value.length) return 'empty'
  return 'ready'
})

async function loadRoadmaps() {
  loading.value = true
  loadError.value = false

  try {
    const response = await fetchRoadmaps({ page: 1, pageSize: 50 })
    roadmaps.value = response.items
  } catch {
    roadmaps.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
}

watch(() => route.query.keyword, (value) => {
  keyword.value = String(value || '')
})

void loadRoadmaps()

function dockIconStyle(app: DockIcon, track: 'primary' | 'secondary', index: number) {
  const midpoint = (dockIconsLoop.length - 1) / 2
  const centerDistance = Math.abs(index - midpoint)
  const centerInfluence = 1 - Math.min(centerDistance / Math.max(midpoint, 1), 1)
  const baseScale = 0.84 + centerInfluence * 0.28
  const focus = activeDockIcon.value?.track === track ? Math.abs(activeDockIcon.value.index - index) : Infinity
  const hoverBoost = focus === 0 ? 0.3 : focus === 1 ? 0.16 : focus === 2 ? 0.07 : 0
  const lift = focus === 0 ? -16 : focus === 1 ? -10 : focus === 2 ? -5 : -(centerInfluence * 4.5)
  const depth = focus === 0 ? 26 : focus === 1 ? 22 : 18
  const zIndex = focus === 0 ? 24 : focus === 1 ? 18 : Math.round(8 + centerInfluence * 8)
  return {
    '--dock-glow': app.glow,
    '--dock-scale': String((baseScale + hoverBoost).toFixed(3)),
    '--dock-lift': `${lift}px`,
    '--dock-depth': `${depth + hoverBoost * 32}px`,
    '--dock-opacity': String((0.78 + centerInfluence * 0.2 + hoverBoost * 0.08).toFixed(3)),
    '--dock-refract': String((0.08 + centerInfluence * 0.12 + hoverBoost * 0.18).toFixed(3)),
    zIndex: String(zIndex)
  }
}

function setDockFocus(track: 'primary' | 'secondary', index: number) {
  activeDockIcon.value = { track, index }
}

function clearDockFocus() {
  activeDockIcon.value = null
}
</script>

<style scoped lang="scss">
.roadmap-paths {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.paths-hero {
  background:
    radial-gradient(circle at 12% 18%, rgba(125, 211, 252, 0.16), transparent 34%),
    radial-gradient(circle at 88% 20%, rgba(59, 130, 246, 0.14), transparent 28%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(241, 245, 249, 0.88));
  border-radius: 28px;
  padding: 60px 48px;
  box-shadow:
    0 20px 55px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.7);
  position: relative;
  overflow: hidden;
  min-height: 330px;
}

.paths-hero::after {
  content: '';
  position: absolute;
  inset: auto 0 0 0;
  height: 110px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.72));
  pointer-events: none;
}

.paths-hero::before {
  content: '';
  position: absolute;
  top: -30%;
  right: -12%;
  width: 44%;
  height: 100%;
  background: radial-gradient(circle at center, rgba(37, 99, 235, 0.14), transparent 68%);
  filter: blur(12px);
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 580px;
}

.hero-title {
  font-size: 40px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 16px 0;
  letter-spacing: -0.02em;
}

.hero-desc {
  font-size: 18px;
  color: #4b5563;
  margin: 0 0 40px 0;
  max-width: 600px;
  line-height: 1.6;
}

.hero-stats {
  display: flex;
  gap: 48px;
}

.hero-dock {
  position: absolute;
  right: -32px;
  bottom: 22px;
  width: min(650px, 62%);
  display: grid;
  gap: 14px;
  z-index: 0;
  transform: perspective(1200px) rotateX(8deg) rotateY(-7deg);
  transform-origin: bottom right;
  opacity: 0.95;
}

.dock-track {
  position: relative;
  display: flex;
  gap: 16px;
  width: max-content;
  padding: 18px 20px;
  border-radius: 32px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.66), rgba(226, 232, 240, 0.36)),
    rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 18px 42px rgba(15, 23, 42, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    inset 0 -10px 18px rgba(148, 163, 184, 0.12);
  backdrop-filter: blur(22px) saturate(145%);
  -webkit-backdrop-filter: blur(22px) saturate(145%);
  transform: translate3d(0, 0, 0);
  will-change: transform;
  backface-visibility: hidden;
  contain: paint;
}

.dock-track::before {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: 30px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.52), rgba(255, 255, 255, 0.04) 42%, rgba(148, 163, 184, 0.12) 70%, rgba(255, 255, 255, 0.18)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.24), transparent 40%);
  mix-blend-mode: screen;
  pointer-events: none;
}

.dock-track::after {
  content: '';
  position: absolute;
  left: 9%;
  right: 9%;
  bottom: -10px;
  height: 26px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.32), rgba(255, 255, 255, 0));
  filter: blur(12px);
  pointer-events: none;
}

.dock-track--primary {
  animation: dock-scroll-left 30s linear infinite;
}

.dock-track--secondary {
  margin-left: 48px;
  animation: dock-scroll-right 36s linear infinite;
}

.dock-icon {
  --dock-glow: rgba(148, 163, 184, 0.3);
  --dock-scale: 1;
  --dock-lift: -4px;
  --dock-depth: 18px;
  --dock-opacity: 1;
  flex: 0 0 auto;
  width: 82px;
  height: 86px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 8px 20px;
  position: relative;
  overflow: visible;
  transform: translate3d(0, var(--dock-lift), 0) scale3d(var(--dock-scale), var(--dock-scale), 1);
  transform-origin: center bottom;
  transition:
    transform 150ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 150ms ease;
  opacity: var(--dock-opacity);
  will-change: transform, opacity;
  backface-visibility: hidden;
  contain: paint;
}

.dock-icon::before {
  content: '';
  position: absolute;
  inset: 8px 10px 26px;
  border-radius: 24px;
  background:
    radial-gradient(circle at 50% 42%, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.18) 38%, rgba(255, 255, 255, 0) 68%);
  filter: blur(5px);
  pointer-events: none;
}

.dock-icon::after {
  content: '';
  position: absolute;
  inset: 14px 14px 28px;
  border-radius: 999px;
  background:
    radial-gradient(circle at 50% 50%, var(--dock-glow), rgba(255, 255, 255, 0) 72%);
  filter: blur(8px);
  opacity: calc(0.48 + var(--dock-refract));
  pointer-events: none;
}

.dock-icon__halo,
.dock-icon__mirror,
.dock-icon__logo {
  position: relative;
  z-index: 1;
}

.dock-icon__halo {
  position: absolute;
  inset: 14px 14px 28px;
  border-radius: 999px;
  background:
    radial-gradient(circle at 34% 28%, rgba(255, 255, 255, calc(0.4 + var(--dock-refract))), rgba(255, 255, 255, 0) 52%),
    linear-gradient(120deg, rgba(255, 255, 255, calc(0.08 + var(--dock-refract))) 0%, rgba(255, 255, 255, 0.02) 42%, rgba(255, 255, 255, calc(0.12 + var(--dock-refract))) 64%, rgba(255, 255, 255, 0) 100%);
  mix-blend-mode: screen;
  filter: blur(2.5px);
  opacity: calc(0.72 + var(--dock-refract));
}

.dock-icon__logo {
  width: 54px;
  height: 54px;
  object-fit: contain;
  filter:
    drop-shadow(0 8px 14px rgba(15, 23, 42, 0.14))
    drop-shadow(0 1px 0 rgba(255, 255, 255, 0.45));
  will-change: transform;
  backface-visibility: hidden;
}

.dock-icon__mirror {
  position: absolute;
  left: 50%;
  top: calc(100% + 2px);
  width: 52px;
  height: 26px;
  transform: translateX(-50%);
  overflow: hidden;
  opacity: calc(0.16 + var(--dock-refract) * 0.6);
  filter: blur(1.4px);
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.6), rgba(0, 0, 0, 0.1) 56%, transparent);
  -webkit-mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.6), rgba(0, 0, 0, 0.1) 56%, transparent);
  pointer-events: none;
}

.dock-icon__logo--mirror {
  width: 54px;
  height: 54px;
  transform: scaleY(-1) translateY(-6px);
  opacity: 0.72;
}

.dock-reflection {
  position: absolute;
  inset: auto 22px -18px 60px;
  height: 54px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0)),
    linear-gradient(180deg, rgba(148, 163, 184, 0.32), rgba(255, 255, 255, 0));
  filter: blur(14px);
  border-radius: 999px;
  pointer-events: none;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: #2563eb;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

.paths-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  padding: 12px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #f3f4f6;
  position: sticky;
  top: 80px;
  z-index: 10;
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: #111827;
  background: #f9fafb;
}

.tab-btn.active {
  background: #111827;
  color: white;
}

.search-box {
  position: relative;
  width: 300px;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
}

.search-input {
  width: 100%;
  height: 40px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 0 16px 0 40px;
  font-size: 14px;
  color: #111827;
  outline: none;
  transition: all 0.2s;
  background: #f9fafb;
}

.search-input:focus {
  background: white;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.paths-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 32px;
}

@keyframes dock-scroll-left {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}

@keyframes dock-scroll-right {
  from {
    transform: translateX(-28%);
  }
  to {
    transform: translateX(0);
  }
}

/* Make roadmap cards look more like feature cards in this view */
:deep(.roadmap-feature-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s;
}

:deep(.roadmap-feature-card:hover) {
  transform: translateY(-4px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

@media (max-width: 768px) {
  .paths-hero {
    padding: 38px 24px 132px;
    min-height: auto;
  }

  .hero-title {
    font-size: 34px;
  }

  .hero-desc {
    font-size: 16px;
    margin-bottom: 28px;
  }

  .hero-stats {
    gap: 24px;
    flex-wrap: wrap;
  }

  .hero-dock {
    width: 100%;
    right: 0;
    left: 0;
    bottom: 18px;
    transform: none;
    opacity: 0.9;
  }

  .dock-track {
    padding: 14px 16px;
    gap: 12px;
  }

  .dock-track--secondary {
    margin-left: 18px;
  }

  .dock-icon {
    width: 64px;
    height: 72px;
    padding-bottom: 12px;
  }

  .dock-icon__logo {
    width: 42px;
    height: 42px;
  }

  .dock-icon__mirror {
    width: 40px;
    height: 18px;
  }

  .paths-filter {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  .filter-tabs {
    flex-wrap: wrap;
  }
  .search-box {
    width: 100%;
  }
}
</style>
