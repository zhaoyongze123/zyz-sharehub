<template>
  <div class="interaction-bar glass-panel">
    <BaseButton size="sm" variant="secondary" :disabled="disableLike" @click="$emit('like')">
      {{ likeText }}
    </BaseButton>
    <BaseButton size="sm" variant="secondary" :disabled="disableFavorite" @click="$emit('favorite')">
      {{ favoriteText }}
    </BaseButton>
    <BaseButton size="sm" variant="secondary" @click="$emit('report')">举报</BaseButton>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import BaseButton from '@/components/base/BaseButton.vue'

const props = withDefaults(defineProps<{
  likes: number
  favorites: number
  disableLike?: boolean
  disableFavorite?: boolean
  likeLabel?: string
  favoriteLabel?: string
}>(), {
  disableLike: false,
  disableFavorite: false,
  likeLabel: '',
  favoriteLabel: ''
})

defineEmits<{
  like: []
  favorite: []
  report: []
}>()

const likeText = computed(() => props.likeLabel || `点赞 ${props.likes}`)
const favoriteText = computed(() => props.favoriteLabel || `收藏 ${props.favorites}`)
</script>

<style scoped lang="scss">
.interaction-bar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  padding: var(--space-4);
}
</style>
