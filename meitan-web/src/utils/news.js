const NEWS_URLS_BY_ORDER = [
  'https://www.chinamine-safety.gov.cn/zfxxgk/fdzdgknr/tzgg/202605/t20260524_604733.shtml',
  'https://mp.weixin.qq.com/s/1kFBskZki1rZdsHIClnh5g',
  'https://paper.people.com.cn/zgnyb/pc/content/202502/17/content_30058300.html',
  'https://www.bijie.gov.cn/bm/bjsnyj/dt/bmdt/202608/t20260805_90698536.html',
  'http://dsj.guizhou.gov.cn/ztzl/rdzt/jdal/202507/t20250709_88261060.html',
  'https://zrzyt.shanxi.gov.cn/mobile/zwgk/ghjh/202407/t20240710_9607008.shtml',
]

export const DEFAULT_NEWS = [
  { id: 1, title: '国家矿山安全监察局：加强煤矿瓦斯防治工作', source: '国家矿山安全监察局' },
  { id: 2, title: '2024年全国煤矿瓦斯抽采利用率稳步提升', source: '国家矿山安全监察局' },
  { id: 3, title: '深部煤层瓦斯抽采技术取得新突破', source: '行业动态' },
  { id: 4, title: '煤矿瓦斯综合治理方案优化研究进展', source: '行业动态' },
  { id: 5, title: '智能化瓦斯监测预警系统在多个矿区推广应用', source: '科技前沿' },
  { id: 6, title: '煤层气开发利用十四五规划发布', source: '政策法规' },
].map((news, index) => ({ ...news, url: NEWS_URLS_BY_ORDER[index] }))

export function applyNewsUrls(newsList) {
  return newsList.map((news, index) => {
    const order = Number(news.sortOrder)
    const urlIndex = Number.isInteger(order) && order > 0 ? order - 1 : index
    return { ...news, url: NEWS_URLS_BY_ORDER[urlIndex] || news.url }
  })
}
