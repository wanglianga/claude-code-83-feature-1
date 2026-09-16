<template>
  <div class="photos" v-if="list.length">
    <el-image v-for="(p, i) in list" :key="i" :src="p" :preview-src-list="list" :initial-index="i"
      fit="cover" class="photo" />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ photos: { type: [String, Array], default: null } })

const list = computed(() => {
  if (!props.photos) return []
  if (Array.isArray(props.photos)) return props.photos
  try {
    return JSON.parse(props.photos)
  } catch (e) {
    return []
  }
})
</script>

<style scoped>
.photos {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.photo {
  width: 90px;
  height: 90px;
  border-radius: 4px;
}
</style>
