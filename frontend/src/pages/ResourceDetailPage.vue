<template>
  <BaseCard>
    <h2>资料详情 #{{ $route.params.id }}</h2>
    <p>这里展示标签、摘要、下载与互动区。</p>
  </BaseCard>
</template>

<script setup>
import BaseCard from '../components/BaseCard.vue'
</script>
