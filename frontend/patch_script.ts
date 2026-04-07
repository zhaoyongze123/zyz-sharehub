<script setup lang="ts">
import { computed, ref, defineEmits, defineProps, defineExpose } from 'vue'

const emit = defineEmits(['focus-field', 'select-module'])

const props = defineProps<{
  modules: any[]
}>()

const sortedModules = computed(() => {
  return [...props.modules].filter(m => m.visible)
})

const resumeRef = ref<HTMLElement | null>(null)
defineExpose({ resumeRef })
</script>
