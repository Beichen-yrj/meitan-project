<template>
  <div class="placeholder-chart">
    <div class="placeholder-chart__title">{{ title }}</div>
    <div class="placeholder-chart__plot">
      <span class="placeholder-chart__tick placeholder-chart__tick--top">1.0</span>
      <span class="placeholder-chart__tick placeholder-chart__tick--bottom">0.0</span>
      <span class="placeholder-chart__watermark">{{ hint }}</span>
      <span class="placeholder-chart__x-label">{{ xLabel }}</span>
      <span class="placeholder-chart__y-label">{{ yLabel }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  xLabel: { type: String, required: true },
  yLabel: { type: String, required: true },
  hint: { type: String, required: true },
})
</script>

<style scoped>
.placeholder-chart {
  width: 100%;
  height: 100%;
  min-height: 380px;
  display: flex;
  flex-direction: column;
  padding: 34px 54px 34px 68px;
  background: #fff;
}

.placeholder-chart__title {
  flex: 0 0 auto;
  margin-bottom: 24px;
  color: #147be5;
  text-align: center;
  font-size: 23px;
  font-weight: 800;
}

.placeholder-chart__plot {
  position: relative;
  flex: 1;
  min-height: 280px;
  border-left: 2px solid #80c8f7;
  border-bottom: 2px solid #80c8f7;
  background-image:
    linear-gradient(to right, rgba(144, 202, 249, 0.32) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(144, 202, 249, 0.32) 1px, transparent 1px);
  background-size: 20% 20%;
}

.placeholder-chart__tick {
  position: absolute;
  left: -38px;
  color: #455a64;
  font-size: 13px;
}

.placeholder-chart__tick--top { top: -8px; }
.placeholder-chart__tick--bottom { bottom: -8px; }

.placeholder-chart__watermark {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 90%;
  color: rgba(126, 190, 237, 0.28);
  text-align: center;
  font-size: clamp(18px, 2vw, 28px);
  transform: translate(-50%, -50%) rotate(-26deg);
}

.placeholder-chart__x-label {
  position: absolute;
  left: 50%;
  bottom: -38px;
  color: #27469a;
  font-size: 17px;
  font-weight: 700;
  transform: translateX(-50%);
}

.placeholder-chart__y-label {
  position: absolute;
  left: -62px;
  top: 50%;
  color: #27469a;
  font-size: 17px;
  font-weight: 700;
  white-space: nowrap;
  transform: translate(-50%, -50%) rotate(-90deg);
}
</style>
