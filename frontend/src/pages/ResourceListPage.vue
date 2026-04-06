<template>
  <BaseCard>
    <h2>资料列表</h2>
    <p>此页对应 page-contract: resource-list。</p>
    <ul>
      <li><RouterLink to="/resources/1001">Spring Boot 实战资料包</RouterLink></li>
    </ul>
  </BaseCard>
</template>

<script setup>
import BaseCard from '../components/BaseCard.vue'
</script>
