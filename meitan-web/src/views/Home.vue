<template>
  <div class="home-page" :style="{ backgroundImage: `url(${homeBackground})` }">
    <HomeTypewriterTitle />

    <!-- 首页轮播图 -->
    <section class="home-carousel-shell">
      <el-carousel class="home-carousel" :interval="4000" arrow="always" indicator-position="outside">
        <el-carousel-item v-for="(image, index) in carouselImages" :key="image">
          <img :src="image" :alt="`煤矿瓦斯图示${index + 1}`" class="home-carousel-image" />
        </el-carousel-item>
      </el-carousel>
    </section>

    <!-- 功能导航卡片 -->
    <el-row :gutter="20">
      <el-col :span="8" v-for="card in navCards" :key="card.path">
        <el-card shadow="hover" class="nav-card" @click="$router.push(card.path)">
          <div class="nav-card-content">
            <el-icon :size="40" :color="card.color"><component :is="card.icon" /></el-icon>
            <h3>{{ card.title }}</h3>
            <p>{{ card.desc }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新闻资讯 -->
    <el-card class="news-card">
      <template #header>
        <span class="news-card-title">行业新闻资讯</span>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="news in newsList"
          :key="news.id"
          :timestamp="news.source"
          placement="top"
        >
          <el-link :href="news.url" target="_blank" type="primary" :underline="false">
            {{ news.title }}
          </el-link>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getNews } from '@/api'
import HomeTypewriterTitle from '@/components/HomeTypewriterTitle.vue'
import image1 from '@/assets/images/1.jpg'
import image2 from '@/assets/images/2.jpg'
import image3 from '@/assets/images/3.jpg'
import image4 from '@/assets/images/4.jpg'
import image5 from '@/assets/images/5.jpg'
import image6 from '@/assets/images/6.jpg'
import homeBackground from '@/assets/images/7.jpg'

const carouselImages = [image1, image2, image3, image4, image5, image6]

const navCards = [
  { title: '瓦斯吸附含量计算', desc: 'Langmuir吸附模型计算与多曲线对比分析', path: '/analysis', icon: 'TrendCharts', color: '#1A73E8' },
  { title: '参数统计分析', desc: '煤样瓦斯吸附参数的多维度统计图表', path: '/statistics', icon: 'DataAnalysis', color: '#42A5F5' },
  { title: '突出危险性检测', desc: '双重临界值判定与危险区域标注', path: '/detection', icon: 'WarningFilled', color: '#FF6B6B' },
  { title: '数据文件管理', desc: '上传与管理您的数据文件', path: '/files', icon: 'FolderOpened', color: '#27AE60' },
  { title: '报告导出', desc: '生成XLSX/HTML综合安全评价报告', path: '/reports', icon: 'Document', color: '#F39C12' },
  { title: '用户反馈', desc: '提交使用体验与改进建议', path: '/feedback', icon: 'ChatLineSquare', color: '#3498DB' },
]

const newsList = ref([])

onMounted(async () => {
  try {
    const res = await getNews()
    newsList.value = res.data || []
  } catch (e) {
    // 默认新闻
    newsList.value = [
      { id: 1, title: '国家矿山安全监察局：加强煤矿瓦斯防治工作', source: '政策法规', url: '#' },
      { id: 2, title: '智能化瓦斯监测预警系统在多个矿区推广应用', source: '科技前沿', url: '#' },
    ]
  }
})
</script>

<style scoped>
.home-page {
  min-height: calc(100vh - 50px);
  margin: -20px;
  padding: 20px;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}
.home-carousel-shell {
  width: min(76%, 1000px);
  margin: 0 auto 22px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.30);
  box-shadow: 0 10px 28px rgba(7, 57, 104, 0.20);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
}
.home-carousel :deep(.el-carousel__container) {
  height: auto;
  aspect-ratio: 16 / 9;
}
.home-carousel-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center;
}
.home-carousel-shell :deep(.el-carousel__indicators--outside) {
  padding: 6px 0 4px;
  background: rgba(255, 255, 255, 0.66);
}
.home-carousel-shell :deep(.el-carousel__button) {
  width: 26px;
  background-color: #0d47a1;
}
.home-carousel-shell :deep(.el-carousel__arrow) {
  width: 42px;
  height: 42px;
  color: #0d47a1;
  background: rgba(255, 255, 255, 0.86);
}
.home-page :deep(.el-card) {
  --el-card-bg-color: rgba(255, 255, 255, 0.76);
  background-color: rgba(255, 255, 255, 0.76);
  border-color: rgba(255, 255, 255, 0.58) !important;
  box-shadow: 0 8px 24px rgba(7, 57, 104, 0.14);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}
.home-page :deep(.el-card__header) {
  background-color: rgba(255, 255, 255, 0.14);
  border-bottom-color: rgba(144, 202, 249, 0.62);
}
.nav-card {
  cursor: pointer;
  transition: transform .2s, box-shadow .2s, background-color .2s;
  margin-bottom: 16px;
}
.nav-card:hover {
  background-color: rgba(255, 255, 255, 0.88);
  transform: translateY(-4px);
}
.nav-card-content {
  text-align: center;
  padding: 16px 0;
}
.nav-card-content h3 {
  margin: 12px 0 8px;
  color: var(--text-primary);
  font-size: 16px;
}
.nav-card-content p {
  color: var(--text-secondary);
  font-size: 13px;
}
.news-card {
  width: min(96%, 1560px);
  margin: 14px auto 0;
}
.news-card-title {
  color: #0b3f73;
  font-size: 18px;
  font-weight: 700;
}
.news-card :deep(.el-card__header) {
  padding: 13px 22px;
}
.news-card :deep(.el-card__body) {
  padding: 17px 30px 9px;
}
.news-card :deep(.el-timeline) {
  max-width: 1320px;
  padding-left: 10px;
}
.news-card :deep(.el-timeline-item) {
  padding-bottom: 14px;
}
.news-card :deep(.el-timeline-item:last-child) {
  padding-bottom: 2px;
}
.news-card :deep(.el-timeline-item__timestamp) {
  margin-bottom: 4px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}
.news-card :deep(.el-link) {
  --el-link-text-color: #0b3f73;
  --el-link-hover-text-color: #f39c12;
  color: #0b3f73;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.45;
  transition: color .18s ease, transform .18s ease;
}
.news-card :deep(.el-link:hover) {
  --el-link-text-color: #f39c12;
  color: #f39c12 !important;
  transform: translateX(6px);
}
@media (max-width: 1100px) {
  .home-carousel-shell {
    width: 88%;
  }
}
</style>
