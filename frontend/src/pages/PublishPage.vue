<template>
  <BaseCard>
    <h2>发布资料</h2>
    <form class="form-grid">
      <label>标题<input placeholder="请输入标题" /></label>
      <label>摘要<textarea placeholder="请输入摘要"></textarea></label>
      <button type="button">提交（示例）</button>
    </form>
  </BaseCard>
</template>

<script setup>
import BaseCard from '../components/BaseCard.vue'
</script>

<style scoped>
.form-grid {
  display: grid;
  gap: var(--space-4);
}

label {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-secondary);
}

input,
textarea {
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: var(--color-bg-elevated);
  color: var(--color-text-primary);
}

button {
  width: fit-content;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-brand-600);
  color: var(--color-text-inverse);
  padding: var(--space-3) var(--space-4);
}
</style>
