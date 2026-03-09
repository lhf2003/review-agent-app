<script setup>
import { ref } from 'vue'
import { InfoFilled, Star, Link, Document } from '@element-plus/icons-vue'

const version = ref('1.0.0')
const features = ref([
  {
    icon: '📁',
    title: '自动/手动同步',
    description: '自动扫描指定目录，新增AI聊天记录文件即时入库（仅桌面版）'
  },
  {
    icon: '🏷️',
    title: '智能标签体系',
    description: '支持主/子标签，避免重复；可手动编辑与合并'
  },
  {
    icon: '🧩',
    title: '结构化知识提取',
    description: '调用 LLM 提取「问题描述」「根本原因」「解决方案」等字段'
  },
  {
    icon: '🔄',
    title: '状态机管理',
    description: '分析状态明确：未分析 / 已完成 / 失败（支持重试）'
  },
  {
    icon: '🔎',
    title: '智能问答',
    description: '基于历史数据与AI聊天问答，巩固之前的学习成果'
  },
  {
    icon: '📅',
    title: '日/周报生成',
    description: '自动生成学习报告，并通过邮件推送（可选）'
  }
])

const links = ref([
  {
    name: 'GitHub 仓库',
    url: 'https://github.com/lhf2003/review-agent-app',
    icon: Link
  },
  {
    name: '项目文档',
    url: 'https://github.com/lhf2003/review-agent-app/tree/master/image',
    icon: Document
  }
])
</script>

<template>
  <div class="about-page">
    <div class="about-container">
      <!-- Header Section -->
      <div class="header-section">
        <div class="logo-area">
          <div class="logo-icon">🧠</div>
          <h1 class="app-title">Review Agent</h1>
        </div>
        <p class="app-subtitle">让每一次向 AI 求助，都成为你技术成长的一块砖</p>
        <div class="version-badge">
          <el-icon><InfoFilled /></el-icon>
          <span>版本 {{ version }}</span>
        </div>
      </div>

      <!-- Description Section -->
      <div class="content-card">
        <div class="card-header">
          <h3>关于项目</h3>
        </div>
        <div class="card-content">
          <p class="description">
            许多开发者已经习惯在遇到问题时第一时间问 AI："帮我修复这个 Bug"、"分析下报错日志"……
            但很少有人回头复盘：<strong>我为什么总在这类问题上卡住？下次还会不会犯同样的错？</strong>
          </p>
          <p class="description">
            Review Agent 正是为此而生 —— 借助 review-mcp 将你与AI的聊天请求保存，然后通过本系统智能分析构建专属的「技术认知知识库」，帮你从"用完即忘"走向"持续沉淀"。
          </p>
          <div class="highlight-box">
            <p class="highlight-text">📌 核心理念：不是替代思考，而是辅助反思。</p>
          </div>
        </div>
      </div>

      <!-- Features Section -->
      <div class="content-card">
        <div class="card-header">
          <h3>功能特性</h3>
        </div>
        <div class="card-content">
          <div class="features-grid">
            <div v-for="(feature, index) in features" :key="index" class="feature-item">
              <div class="feature-icon">{{ feature.icon }}</div>
              <div class="feature-info">
                <h4 class="feature-title">{{ feature.title }}</h4>
                <p class="feature-desc">{{ feature.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Target Users Section -->
      <div class="content-card">
        <div class="card-header">
          <h3>适用场景</h3>
        </div>
        <div class="card-content">
          <ul class="target-list">
            <li>想系统整理自己常问的 AI 问题</li>
            <li>发现自身技术盲区和薄弱环节</li>
            <li>建立个人可检索的技术笔记库</li>
          </ul>
        </div>
      </div>

      <!-- Links Section -->
      <div class="content-card">
        <div class="card-header">
          <h3>相关链接</h3>
        </div>
        <div class="card-content">
          <div class="links-list">
            <a v-for="(link, index) in links" :key="index" :href="link.url" target="_blank" class="link-item">
              <el-icon class="link-icon">
                <component :is="link.icon" />
              </el-icon>
              <span>{{ link.name }}</span>
              <el-icon class="external-icon">
                <Link />
              </el-icon>
            </a>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="footer-section">
        <p class="footer-text">
          如果你觉得这个项目对你有启发，请点个 ⭐ Star 支持一下 ❤️
        </p>
      </div>
      </div>
    </div>
</template>

<style scoped>
.about-page {
  display: flex;
  justify-content: center;
  padding: 20px; /* Reduced padding for embedded view */
  /* min-height removed to fit in config page */
  background: transparent; /* Transparent to blend with config page */
  padding-bottom: 40px;
}

.about-container {
  width: 100%;
  max-width: 800px; /* Slightly smaller max-width */
}


/* Header Section */
.header-section {
  text-align: center;
  margin-bottom: 40px;
  padding: 40px 0;
}

.logo-area {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.logo-icon {
  font-size: 64px;
  line-height: 1;
}

.app-title {
  font-size: 48px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, var(--accent-primary) 0%, var(--accent-secondary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.app-subtitle {
  font-size: 18px;
  color: var(--text-secondary);
  margin: 0 0 24px 0;
  line-height: 1.6;
}

.version-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--glass-surface);
  border: 1px solid var(--glass-border);
  border-radius: 20px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
}

/* Content Cards */
.content-card {
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-radius: var(--radius-card);
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
  padding: 24px;
  margin-bottom: 24px;
  transition: var(--transition-base);
}

.card-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--glass-border);
}

.card-header h3 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.card-content {
  color: var(--text-secondary);
  line-height: 1.8;
}

.description {
  margin: 0 0 16px 0;
  font-size: 15px;
  line-height: 1.8;
}

.description strong {
  color: var(--text-primary);
  font-weight: 600;
}

.highlight-box {
  background: linear-gradient(135deg, rgba(204, 102, 51, 0.1) 0%, rgba(224, 123, 71, 0.08) 100%);
  border-radius: 12px;
  padding: 16px 20px;
  margin-top: 20px;
  border-left: 4px solid var(--accent-primary);
}

.highlight-text {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

/* Features Grid */
.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.feature-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 248, 245, 0.03);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
}

.feature-item:hover {
  background: var(--glass-surface-hover);
  border-color: var(--glass-border-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.feature-icon {
  font-size: 32px;
  line-height: 1;
  flex-shrink: 0;
}

.feature-info {
  flex: 1;
  min-width: 0;
}

.feature-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--text-primary);
}

.feature-desc {
  font-size: 14px;
  margin: 0;
  color: var(--text-tertiary);
  line-height: 1.6;
}

/* Target List */
.target-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.target-list li {
  position: relative;
  padding-left: 28px;
  margin-bottom: 12px;
  font-size: 15px;
  color: var(--text-secondary);
}

.target-list li:last-child {
  margin-bottom: 0;
}

.target-list li::before {
  content: '✓';
  position: absolute;
  left: 0;
  top: 0;
  color: var(--accent-primary);
  font-weight: 700;
  font-size: 18px;
}

/* Links List */
.links-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.link-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: rgba(255, 248, 245, 0.03);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  text-decoration: none;
  transition: all var(--transition-base);
  font-size: 15px;
  font-weight: 500;
}

.link-item:hover {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
  color: #fff;
  transform: translateX(4px);
  box-shadow: 0 4px 16px var(--accent-glow-soft);
}

.link-icon {
  font-size: 20px;
}

.external-icon {
  margin-left: auto;
  font-size: 16px;
  opacity: 0.6;
}

/* Footer */
.footer-section {
  text-align: center;
  padding: 32px 0;
  margin-top: 40px;
  border-top: 1px solid var(--glass-border);
}

.footer-text {
  margin: 0;
  font-size: 15px;
  color: var(--text-tertiary);
}

/* Responsive */
@media (max-width: 768px) {
  .about-page {
    padding: 20px 16px;
  }

  .logo-icon {
    font-size: 48px;
  }

  .app-title {
    font-size: 36px;
  }

  .app-subtitle {
    font-size: 16px;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .content-card {
    padding: 20px;
  }
}
</style>
