<template>
  <div>
    <el-upload :show-file-list="false" accept="image/*" :auto-upload="false" :on-change="onFile">
      <el-button size="small" :icon="Plus">添加照片</el-button>
    </el-upload>
    <div class="photos" v-if="modelValue.length">
      <div v-for="(p, i) in modelValue" :key="i" class="photo-item">
        <el-image :src="p" :preview-src-list="modelValue" :initial-index="i" fit="cover" class="photo" />
        <el-icon class="del" @click="remove(i)"><Close /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({ modelValue: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])

// 压缩为 base64 data URL，便于随 JSON 提交
const onFile = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const max = 800
      const scale = Math.min(1, max / Math.max(img.width, img.height))
      canvas.width = img.width * scale
      canvas.height = img.height * scale
      canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height)
      const url = canvas.toDataURL('image/jpeg', 0.7)
      emit('update:modelValue', [...props.modelValue, url])
    }
    img.onerror = () => ElMessage.error('图片读取失败')
    img.src = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

const remove = (i) => {
  const arr = [...props.modelValue]
  arr.splice(i, 1)
  emit('update:modelValue', arr)
}
</script>

<style scoped>
.photos {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.photo-item {
  position: relative;
  width: 90px;
  height: 90px;
}
.photo {
  width: 90px;
  height: 90px;
  border-radius: 4px;
}
.del {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #f56c6c;
  color: #fff;
  border-radius: 50%;
  padding: 2px;
  cursor: pointer;
}
</style>
