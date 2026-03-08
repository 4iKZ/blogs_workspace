<template>
  <div class="website-statistics">
    <!-- 统计卡片 -->
    <div class="stats-overview">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-info">
            <p class="stat-value">{{ formatNumber(overview.totalPageViews) }}</p>
            <p class="stat-label">总访问量 (PV)</p>
          </div>
          <div class="stat-icon pv-icon">
            <el-icon size="32"><View /></el-icon>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-info">
            <p class="stat-value">{{ formatNumber(overview.totalUniqueVisitors) }}</p>
            <p class="stat-label">总访客数 (UV)</p>
          </div>
          <div class="stat-icon uv-icon">
            <el-icon size="32"><User /></el-icon>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-info">
            <p class="stat-value">{{ formatNumber(overview.todayPageViews) }}</p>
            <p class="stat-label">今日访问量</p>
            <p class="stat-compare" :class="trendClass.today">
              <el-icon v-if="overview.todayPageViews > overview.yesterdayPageViews"><CaretTop /></el-icon>
              <el-icon v-else-if="overview.todayPageViews < overview.yesterdayPageViews"><CaretBottom /></el-icon>
              较昨日 {{ getTrendText(overview.todayPageViews, overview.yesterdayPageViews) }}
            </p>
          </div>
          <div class="stat-icon today-icon">
            <el-icon size="32"><Calendar /></el-icon>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-info">
            <p class="stat-value">{{ formatNumber(overview.todayUniqueVisitors) }}</p>
            <p class="stat-label">今日访客数</p>
            <p class="stat-compare" :class="trendClass.uv">
              <el-icon v-if="overview.todayUniqueVisitors > overview.yesterdayUniqueVisitors"><CaretTop /></el-icon>
              <el-icon v-else-if="overview.todayUniqueVisitors < overview.yesterdayUniqueVisitors"><CaretBottom /></el-icon>
              较昨日 {{ getTrendText(overview.todayUniqueVisitors, overview.yesterdayUniqueVisitors) }}
            </p>
          </div>
          <div class="stat-icon visitor-icon">
            <el-icon size="32"><Avatar /></el-icon>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 访问趋势图表 -->
    <el-card class="chart-card">
      <template #header>
        <div class="chart-header">
          <h3>访问趋势</h3>
          <el-radio-group v-model="trendRange" size="small" @change="loadTrendData">
            <el-radio-button label="7">近7天</el-radio-button>
            <el-radio-button label="14">近14天</el-radio-button>
            <el-radio-button label="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendChartRef" class="chart-container" v-loading="trendLoading"></div>
    </el-card>

    <!-- 热门页面和访问来源 -->
    <div class="charts-row">
      <el-card class="chart-card half-width">
        <template #header>
          <h3>热门页面排行</h3>
        </template>
        <div class="top-pages-list" v-loading="topPagesLoading">
          <el-table :data="topPages" stripe style="width: 100%">
            <el-table-column prop="page_url" label="页面路径" min-width="200">
              <template #default="{ row }">
                <el-tooltip :content="row.page_url" placement="top">
                  <span class="page-url">{{ truncateUrl(row.page_url) }}</span>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="visit_count" label="访问次数" width="100" align="center" />
            <el-table-column prop="unique_visitor" label="独立访客" width="100" align="center" />
          </el-table>
        </div>
      </el-card>

      <el-card class="chart-card half-width">
        <template #header>
          <h3>访问来源分布</h3>
        </template>
        <div ref="sourceChartRef" class="chart-container" v-loading="sourceLoading"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import * as echarts from 'echarts'
import { statisticsService } from '../../services/statisticsService'
import type { WebsiteStatisticsDTO, VisitTrendDTO, TopPageItem, TrafficSourceItem } from '../../types/statistics'
import { View, User, Calendar, Avatar, CaretTop, CaretBottom } from '@element-plus/icons-vue'

// 数据状态
const overview = ref<WebsiteStatisticsDTO>({
  totalPageViews: 0,
  totalUniqueVisitors: 0,
  todayPageViews: 0,
  todayUniqueVisitors: 0,
  yesterdayPageViews: 0,
  yesterdayUniqueVisitors: 0,
  statisticsDate: ''
})

const trendData = ref<VisitTrendDTO[]>([])
const topPages = ref<TopPageItem[]>([])
const trafficSources = ref<TrafficSourceItem[]>([])

// 加载状态
const overviewLoading = ref(false)
const trendLoading = ref(false)
const topPagesLoading = ref(false)
const sourceLoading = ref(false)

// 图表引用
const trendChartRef = ref<HTMLElement | null>(null)
const sourceChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let sourceChart: echarts.ECharts | null = null

// 时间范围
const trendRange = ref('7')

// 计算趋势样式
const trendClass = computed(() => ({
  today: getTrendClass(overview.value.todayPageViews, overview.value.yesterdayPageViews),
  uv: getTrendClass(overview.value.todayUniqueVisitors, overview.value.yesterdayUniqueVisitors)
}))

// 格式化数字
function formatNumber(num: number): string {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

// 获取趋势文本
function getTrendText(current: number, previous: number): string {
  if (previous === 0) return current > 0 ? '新增' : '持平'
  const percent = ((current - previous) / previous * 100).toFixed(1)
  if (current > previous) return `+${percent}%`
  if (current < previous) return `${percent}%`
  return '持平'
}

// 获取趋势样式类
function getTrendClass(current: number, previous: number): string {
  if (current > previous) return 'trend-up'
  if (current < previous) return 'trend-down'
  return 'trend-flat'
}

// 截断URL
function truncateUrl(url: string): string {
  if (url.length > 40) {
    return url.substring(0, 40) + '...'
  }
  return url
}

// 加载概览数据
async function loadOverview() {
  overviewLoading.value = true
  try {
    const response = await statisticsService.getWebsiteOverview()
    if (response.data) {
      overview.value = response.data
    }
  } catch (error) {
    console.error('加载概览数据失败:', error)
  } finally {
    overviewLoading.value = false
  }
}

// 加载趋势数据
async function loadTrendData() {
  trendLoading.value = true
  try {
    const days = parseInt(trendRange.value)
    const endDate = new Date()
    const startDate = new Date()
    startDate.setDate(startDate.getDate() - days)
    
    const response = await statisticsService.getVisitTrend(
      formatDate(startDate),
      formatDate(endDate)
    )
    if (response.data) {
      trendData.value = response.data
      renderTrendChart()
    }
  } catch (error) {
    console.error('加载趋势数据失败:', error)
  } finally {
    trendLoading.value = false
  }
}

// 格式化日期
function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 渲染趋势图表
function renderTrendChart() {
  if (!trendChartRef.value || trendData.value.length === 0) return
  
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  
  const dates = trendData.value.map(item => item.date)
  const pvData = trendData.value.map(item => item.pageViews)
  const uvData = trendData.value.map(item => item.uniqueVisitors)
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['访问量 (PV)', '访客数 (UV)'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '访问量 (PV)',
        type: 'line',
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
          ])
        },
        lineStyle: {
          color: '#409EFF',
          width: 2
        },
        itemStyle: {
          color: '#409EFF'
        },
        data: pvData
      },
      {
        name: '访客数 (UV)',
        type: 'line',
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103, 194, 58, 0.5)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.1)' }
          ])
        },
        lineStyle: {
          color: '#67C23A',
          width: 2
        },
        itemStyle: {
          color: '#67C23A'
        },
        data: uvData
      }
    ]
  }
  
  trendChart.setOption(option)
}

// 加载热门页面
async function loadTopPages() {
  topPagesLoading.value = true
  try {
    const response = await statisticsService.getTopPages(1, 10)
    if (response.data && response.data.records) {
      topPages.value = response.data.records
    }
  } catch (error) {
    console.error('加载热门页面失败:', error)
  } finally {
    topPagesLoading.value = false
  }
}

// 加载访问来源
async function loadTrafficSources() {
  sourceLoading.value = true
  try {
    const response = await statisticsService.getTrafficSources()
    if (response.data) {
      trafficSources.value = response.data
      renderSourceChart()
    }
  } catch (error) {
    console.error('加载访问来源失败:', error)
  } finally {
    sourceLoading.value = false
  }
}

// 渲染来源饼图
function renderSourceChart() {
  if (!sourceChartRef.value || trafficSources.value.length === 0) return
  
  if (!sourceChart) {
    sourceChart = echarts.init(sourceChartRef.value)
  }
  
  const sourceTypeMap: Record<string, string> = {
    'direct': '直接访问',
    'search': '搜索引擎',
    'social': '社交媒体',
    'referral': '外部链接'
  }
  
  const colorMap: Record<string, string> = {
    'direct': '#409EFF',
    'search': '#67C23A',
    'social': '#E6A23C',
    'referral': '#F56C6C'
  }
  
  const data = trafficSources.value.map(item => ({
    name: sourceTypeMap[item.source_type] || item.source_type,
    value: item.visit_count,
    itemStyle: {
      color: colorMap[item.source_type]
    }
  }))
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center'
    },
    series: [
      {
        name: '访问来源',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: data
      }
    ]
  }
  
  sourceChart.setOption(option)
}

// 窗口大小变化时重新渲染图表
function handleResize() {
  trendChart?.resize()
  sourceChart?.resize()
}

onMounted(() => {
  loadOverview()
  loadTrendData()
  loadTopPages()
  loadTrafficSources()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  sourceChart?.dispose()
})
</script>

<style scoped>
.website-statistics {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .stats-overview {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-overview {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary, #303133);
  margin: 0;
}

.stat-label {
  font-size: 14px;
  color: var(--text-tertiary, #909399);
  margin: 4px 0 0 0;
}

.stat-compare {
  font-size: 12px;
  margin: 4px 0 0 0;
  display: flex;
  align-items: center;
  gap: 2px;
}

.stat-compare.trend-up {
  color: #67C23A;
}

.stat-compare.trend-down {
  color: #F56C6C;
}

.stat-compare.trend-flat {
  color: #909399;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pv-icon {
  background: rgba(64, 158, 255, 0.1);
  color: #409EFF;
}

.uv-icon {
  background: rgba(103, 194, 58, 0.1);
  color: #67C23A;
}

.today-icon {
  background: rgba(230, 162, 60, 0.1);
  color: #E6A23C;
}

.visitor-icon {
  background: rgba(245, 108, 108, 0.1);
  color: #F56C6C;
}

.chart-card {
  border-radius: 8px;
}

.chart-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color-lighter, #EBEEF5);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #303133);
}

.chart-container {
  height: 300px;
  width: 100%;
}

.charts-row {
  display: flex;
  gap: 20px;
}

.charts-row .half-width {
  flex: 1;
}

@media (max-width: 992px) {
  .charts-row {
    flex-direction: column;
  }
}

.top-pages-list {
  max-height: 300px;
  overflow-y: auto;
}

.page-url {
  color: var(--text-primary, #303133);
  font-size: 13px;
}
</style>