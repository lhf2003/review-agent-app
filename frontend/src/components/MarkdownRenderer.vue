<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import markdownit from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import { ElMessage } from 'element-plus'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const md = new markdownit({
  breaks: true,
  html: true, // Enable HTML tags in source
  linkify: true, // Autoconvert URL-like text to links
  highlight: function (str, lang) {
    const trimmed = str.trimEnd()
    let highlighted = ''
    const lg = (lang || '').trim().toLowerCase()
    if (lg && hljs.getLanguage(lg)) {
      try {
        highlighted = hljs.highlight(trimmed, { language: lg, ignoreIllegals: true }).value
      } catch (__) {}
    } else {
      highlighted = md.utils.escapeHtml(trimmed)
    }
    
    return `<div class="code-block-wrapper"><div class="code-block-header"><span class="code-lang">${lg || 'text'}</span><button class="copy-btn">复制</button></div><div class="code-block-body"><pre><code class="hljs ${lg}">${highlighted}</code></pre></div></div>`
  }
})

function toMdInput(v) {
  if (typeof v === 'string') return v
  if (v == null) return ''
  try { return '```json\n' + JSON.stringify(v, null, 2) + '\n```' } catch { return String(v) }
}

const renderedContent = computed(() => {
  return DOMPurify.sanitize(md.render(toMdInput(props.content)))
})

function handleClick(e) {
  if (e.target.classList.contains('copy-btn')) {
    const btn = e.target
    const wrapper = btn.closest('.code-block-wrapper')
    if (wrapper) {
      const codeEl = wrapper.querySelector('pre code')
      if (codeEl) {
        const text = codeEl.textContent
        navigator.clipboard.writeText(text).then(() => {
          const originalText = btn.textContent
          btn.textContent = '已复制'
          btn.classList.add('copied')
          setTimeout(() => {
            btn.textContent = originalText
            btn.classList.remove('copied')
          }, 2000)
        }).catch(err => {
          ElMessage.error('复制失败')
          console.error(err)
        })
      }
    }
  }
}
</script>

<template>
  <div class="markdown-renderer" v-html="renderedContent" @click="handleClick"></div>
</template>

<style scoped>
.markdown-renderer {
  line-height: 1.6;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.markdown-renderer :deep(h1),
.markdown-renderer :deep(h2),
.markdown-renderer :deep(h3),
.markdown-renderer :deep(h4),
.markdown-renderer :deep(h5),
.markdown-renderer :deep(h6) {
  margin-top: 1.5em;
  margin-bottom: 0.8em;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-renderer :deep(p) {
  margin-bottom: 1em;
}

.markdown-renderer :deep(ul),
.markdown-renderer :deep(ol) {
  padding-left: 2em;
  margin-bottom: 1em;
}

.markdown-renderer :deep(li) {
  margin-bottom: 0.25em;
}

.markdown-renderer :deep(blockquote) {
  margin: 0 0 1em;
  padding: 0 1em;
  color: var(--el-text-color-secondary);
  border-left: 4px solid var(--el-border-color);
}

.markdown-renderer :deep(a) {
  color: var(--el-color-primary);
  text-decoration: none;
}

.markdown-renderer :deep(a:hover) {
  text-decoration: underline;
}

.markdown-renderer :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}

/* Code Block Styles */
.markdown-renderer :deep(.code-block-wrapper) {
  margin: 1em 0;
  border-radius: 8px;
  background-color: #282c34;
  overflow: hidden;
  border: 1px solid #3e4451;
}

.markdown-renderer :deep(.code-block-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: transparent;
  border-bottom: 1px solid #3e4451;
  color: #abb2bf;
  font-size: 12px;
  user-select: none;
}

.markdown-renderer :deep(.code-block-body) {
  display: flex;
  background-color: #282c34;
  padding: 12px 0;
}

.markdown-renderer :deep(.code-lang) {
  font-weight: 600;
  text-transform: uppercase;
}

.markdown-renderer :deep(.copy-btn) {
  cursor: pointer;
  background: transparent;
  border: 1px solid #5c6370;
  color: #abb2bf;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 12px;
  transition: all 0.2s;
}

.markdown-renderer :deep(.copy-btn:hover) {
  background-color: #3e4451;
  border-color: #abb2bf;
  color: #fff;
}

.markdown-renderer :deep(.copy-btn.copied) {
  border-color: #98c379;
  color: #98c379;
}

.markdown-renderer :deep(pre) {
  margin: 0;
  padding: 0 12px;
  overflow: auto;
  background-color: transparent;
  border: none;
  border-radius: 0;
  flex: 1;
  line-height: 1.5;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 14px;
}

.markdown-renderer :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  color: #abb2bf;
  font-size: 14px;
  line-height: 1.5;
}

/* Inline code style */
.markdown-renderer :deep(:not(pre) > code) {
  background-color: var(--el-fill-color);
  color: var(--el-color-danger);
  padding: 2px 4px;
  border-radius: 4px;
}

.markdown-renderer :deep(.hljs) {
  display: block;
  overflow-x: auto;
  padding: 0;
  background: transparent;
}
</style>
