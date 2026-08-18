<template>
  <div>
    <el-card>
      <template #header>
        <span style="font-size:18px;font-weight:bold;color:var(--primary)">煤层瓦斯介绍</span>
      </template>
      <div style="line-height:2;color:var(--text-primary);font-size:15px;white-space:pre-line">
        {{ introText }}
      </div>
    </el-card>

    <el-card style="margin-top:20px">
      <template #header>
        <span style="font-size:18px;font-weight:bold;color:var(--primary)">相关图示</span>
      </template>
      <el-row :gutter="18" class="related-image-grid">
        <el-col
          v-for="(image, index) in imageList"
          :key="image"
          :xs="24"
          :sm="8"
          class="related-image-grid__item"
        >
          <el-image
            :src="image"
            fit="cover"
            class="related-image"
            :preview-src-list="imageList"
            :initial-index="index"
            preview-teleported
          >
            <template #error>
              <div class="related-image__error">
                图片加载失败
              </div>
            </template>
          </el-image>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import mineWorkingFaceImage from '@/assets/images/mine-working-face.jpg'
import mineHydraulicSupportImage from '@/assets/images/mine-hydraulic-support.jpg'
import mineConveyorImage from '@/assets/images/mine-conveyor.jpg'

const imageList = [mineWorkingFaceImage, mineHydraulicSupportImage, mineConveyorImage]

const introText = `煤层瓦斯（又称煤层气、矿井瓦斯）是赋存于煤层及其围岩中的以甲烷（CH₄）为主的混合气体，是在煤化作用过程中生成的天然气体资源。

煤层瓦斯的危害主要体现在以下几个方面：一是瓦斯爆炸，当空气中甲烷浓度处于5%～16%时，遇明火可引发爆炸，是煤矿最严重的安全威胁之一；二是瓦斯突出，在高瓦斯压力和地应力作用下，煤岩体瞬间大量喷出瓦斯和煤岩，可造成重大人员伤亡；三是瓦斯窒息，高浓度瓦斯可使井下氧气浓度降低，导致人员缺氧窒息。

同时，煤层瓦斯也是一种宝贵的清洁能源。每立方米甲烷的发热量约35.9MJ，相当于1.2kg标准煤。

目前我国煤矿瓦斯防治主要采用"先抽后采、监测监控、以风定产"的方针，通过本煤层抽采、邻近层抽采、采空区抽采等综合技术手段，有效降低瓦斯涌出量，保障矿井安全生产。`
</script>

<style scoped>
.related-image-grid__item {
  margin-bottom: 18px;
}

.related-image {
  display: block;
  width: 100%;
  height: 280px;
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
}

.related-image__error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 280px;
  color: var(--text-secondary);
  background: var(--bg);
}

@media (max-width: 767px) {
  .related-image,
  .related-image__error {
    height: 220px;
  }
}
</style>
