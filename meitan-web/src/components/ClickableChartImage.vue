<template>
  <div class="draft-chart-image-wrap chart-preview-trigger" title="点击放大查看图表">
    <el-image
      class="draft-chart-image chart-preview-image"
      :src="imageUrl"
      :preview-src-list="[imageUrl]"
      :initial-index="0"
      fit="contain"
      preview-teleported
      hide-on-click-modal
      :alt="alt"
    >
      <template #error>
        <div class="chart-preview-error">图表加载失败</div>
      </template>
    </el-image>

    <div class="chart-preview-hint">
      <el-icon><ZoomIn /></el-icon>
      <span>点击放大查看</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  image: {
    type: String,
    required: true,
  },
  alt: {
    type: String,
    default: '分析图表',
  },
})

const imageUrl = computed(() => `data:image/png;base64,${props.image}`)
</script>

<style scoped>
.chart-preview-trigger {
  position: relative;
  cursor: zoom-in;
}

.chart-preview-image {
  width: 100%;
  height: 100%;
  cursor: zoom-in;
}

.chart-preview-image :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: contain;
  cursor: zoom-in;
  transition: filter 0.18s ease;
}

.chart-preview-trigger:hover .chart-preview-image :deep(.el-image__inner) {
  filter: brightness(0.97);
}

.chart-preview-hint {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 11px;
  color: #fff;
  font-size: 13px;
  line-height: 1;
  background: rgba(13, 71, 161, 0.76);
  border-radius: 5px;
  box-shadow: 0 3px 10px rgba(13, 71, 161, 0.2);
  opacity: 0.72;
  pointer-events: none;
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.chart-preview-trigger:hover .chart-preview-hint {
  opacity: 1;
  transform: translateY(-1px);
}

.chart-preview-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #f5f7fa;
}
</style>
