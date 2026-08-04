<template>
  <section class="home-title" aria-label="煤层瓦斯智能分析中心">
    <div class="home-title__line">
      <h1>{{ typedTitle }}</h1>
      <span class="home-title__cursor" aria-hidden="true"></span>
    </div>
    <p>瓦斯吸附数据分析&nbsp;&nbsp;·&nbsp;&nbsp;参数统计&nbsp;&nbsp;·&nbsp;&nbsp;突出危险性检测</p>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const fullTitle = '煤层瓦斯 · 智能分析中心'
const typedTitle = ref('')
let titleIndex = 0
let deleting = false
let typeTimer = null

function runTypewriter() {
  if (!deleting) {
    titleIndex += 1
    typedTitle.value = fullTitle.slice(0, titleIndex)

    if (titleIndex === fullTitle.length) {
      deleting = true
      typeTimer = window.setTimeout(runTypewriter, 2400)
      return
    }
    typeTimer = window.setTimeout(runTypewriter, 135)
    return
  }

  titleIndex -= 1
  typedTitle.value = fullTitle.slice(0, titleIndex)
  if (titleIndex === 0) {
    deleting = false
    typeTimer = window.setTimeout(runTypewriter, 650)
    return
  }
  typeTimer = window.setTimeout(runTypewriter, 65)
}

onMounted(() => {
  typeTimer = window.setTimeout(runTypewriter, 350)
})

onBeforeUnmount(() => {
  if (typeTimer) window.clearTimeout(typeTimer)
})
</script>

<style scoped>
.home-title {
  min-height: 94px;
  margin: 0 auto 14px;
  text-align: center;
  text-shadow: 0 2px 10px rgba(255, 255, 255, 0.88);
}
.home-title__line {
  min-height: 53px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.home-title h1 {
  margin: 0;
  color: #0d47a1;
  font-size: clamp(30px, 2.5vw, 43px);
  font-weight: 800;
  letter-spacing: 4px;
  line-height: 1.2;
}
.home-title__cursor {
  width: 4px;
  height: 38px;
  margin-left: 7px;
  border-radius: 2px;
  background: #1a73e8;
  box-shadow: 0 0 10px rgba(26, 115, 232, 0.42);
  animation: cursor-blink 0.78s steps(1) infinite;
}
.home-title p {
  margin: 5px 0 0;
  color: #315b78;
  font-size: clamp(14px, 1.08vw, 18px);
  font-weight: 600;
  letter-spacing: 2px;
}
@keyframes cursor-blink {
  0%, 48% { opacity: 1; }
  49%, 100% { opacity: 0; }
}
@media (prefers-reduced-motion: reduce) {
  .home-title__cursor { animation: none; }
}
</style>
