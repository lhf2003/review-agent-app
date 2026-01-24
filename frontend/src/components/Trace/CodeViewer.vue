<script setup>
import { computed, ref, watch, nextTick } from 'vue'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  sessions: {
    type: Array,
    default: () => []
  },
  activeSessionIndex: {
    type: Number,
    default: -1
  },
  fileName: {
    type: String,
    default: 'Source Code'
  }
})

const emit = defineEmits(['select-session'])

// Process content into chunks: { text, isHighlight, sessionIndex }
const chunks = computed(() => {
  if (!props.content) return []
  
  const sortedSessions = props.sessions
    .map((s, i) => ({ ...s, originalIndex: i }))
    .sort((a, b) => a.startIndex - b.startIndex)

  const result = []
  let currentIndex = 0

  for (const session of sortedSessions) {
    if (session.startIndex > currentIndex) {
      result.push({
        text: props.content.slice(currentIndex, session.startIndex),
        isHighlight: false
      })
    }
    
    result.push({
      text: props.content.slice(session.startIndex, session.endIndex),
      isHighlight: true,
      sessionIndex: session.originalIndex
    })
    
    currentIndex = session.endIndex
  }

  if (currentIndex < props.content.length) {
    result.push({
      text: props.content.slice(currentIndex),
      isHighlight: false
    })
  }

  return result
})

function handleChunkClick(chunk) {
  if (chunk.isHighlight) {
    emit('select-session', chunk.sessionIndex)
  }
}

// Scroll to active session
const viewerRef = ref(null)

watch(() => props.activeSessionIndex, async (newVal) => {
  if (newVal > -1) {
    await nextTick()
    const activeEl = viewerRef.value.querySelector(`.session-highlight[data-index="${newVal}"]`)
    if (activeEl) {
      activeEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }
})
</script>

<template>
  <div class="code-viewer-container">
    <div class="editor-header">
      <div class="window-controls">
        <span class="control red"></span>
        <span class="control yellow"></span>
        <span class="control green"></span>
      </div>
      <div class="file-name">{{ fileName }}</div>
      <div class="header-spacer"></div>
    </div>
    
    <div class="code-viewer custom-scrollbar" ref="viewerRef">
      <div class="content-wrapper">
        <div v-if="activeSessionIndex > -1" class="mask-layer"></div>
        <pre><code><template v-for="(chunk, idx) in chunks" :key="idx"><span 
            v-if="chunk.isHighlight" 
            class="session-highlight" 
            :class="{ active: chunk.sessionIndex === activeSessionIndex }"
            :data-index="chunk.sessionIndex"
            @click="handleChunkClick(chunk)"
          >{{ chunk.text }}</span><span v-else>{{ chunk.text }}</span></template></code></pre>
      </div>
    </div>
  </div>
</template>

<style scoped>
.code-viewer-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color);
  position: relative;
  /* Specific Code Viewer Colors */
  --cv-bg-light: #ffffff;
  --cv-text-light: #24292e; /* GitHub Dark Gray */
  --cv-bg-dark: #0d1117;   /* GitHub Dark Bg */
  --cv-text-dark: #c9d1d9; /* GitHub Dark Text */
  
  --cv-highlight-bg-light: #fff8c5; /* Light Yellow */
  --cv-highlight-text-light: #24292e;
  
  --cv-highlight-bg-dark: rgba(187, 128, 9, 0.15); /* Dark Gold Low Opacity */
  --cv-highlight-text-dark: #e3b341; /* Gold Text */
  
  --cv-active-bg-light: #fffbdd;
  --cv-active-border-light: #d29922;
  
  --cv-active-bg-dark: rgba(187, 128, 9, 0.3);
  --cv-active-border-dark: #e3b341;
}

.editor-header {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background-color: var(--el-bg-color-overlay);
  border-bottom: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
}

.window-controls {
  display: flex;
  gap: 8px;
  width: 60px;
}

.control {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.control.red { background-color: #FF5F56; border: 1px solid #E0443E; }
.control.yellow { background-color: #FFBD2E; border: 1px solid #DEA123; }
.control.green { background-color: #27C93F; border: 1px solid #1AAB29; }

.file-name {
  font-family: 'JetBrains Mono', 'IBM Plex Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 13px;
  color: var(--el-text-color-regular);
  font-weight: 500;
}

.header-spacer {
  width: 60px;
}

.code-viewer {
  flex: 1;
  overflow: auto;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 14px; /* Increased for readability */
  line-height: 1.6;
  white-space: pre-wrap;
  position: relative;
  background-color: var(--cv-bg-light);
  color: var(--cv-text-light);
  transition: background-color 0.3s, color 0.3s;
}

:global(html.dark) .code-viewer {
  background-color: var(--cv-bg-dark);
  color: var(--cv-text-dark);
}

.content-wrapper {
  position: relative;
  min-height: 100%;
  width: fit-content;
  min-width: 100%;
  padding: 20px 24px;
  box-sizing: border-box;
}

.mask-layer {
  position: absolute;
  inset: 0;
  background-color: var(--cv-bg-light);
  opacity: 0.7; /* Increased opacity for better focus */
  backdrop-filter: blur(1px);
  z-index: 1;
  pointer-events: none;
  transition: opacity 0.3s;
}

:global(html.dark) .mask-layer {
  background-color: var(--cv-bg-dark);
  opacity: 0.7;
}

pre {
  margin: 0;
  position: relative;
  z-index: 0;
}

.session-highlight {
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px dashed var(--el-border-color);
  padding: 2px 0;
  background-color: transparent;
}

.session-highlight:hover {
  background-color: var(--el-fill-color);
  border-radius: 2px;
}

.session-highlight.active {
  position: relative;
  z-index: 2;
  background-color: var(--cv-highlight-bg-light);
  box-shadow: 0 0 0 2px var(--cv-highlight-bg-light); /* Softer highlight */
  border-radius: 4px;
  color: var(--cv-highlight-text-light);
  border-bottom: 2px solid var(--cv-active-border-light);
  box-decoration-break: clone;
  -webkit-box-decoration-break: clone;
  font-weight: 600;
}

:global(html.dark) .session-highlight.active {
  background-color: var(--cv-highlight-bg-dark);
  box-shadow: 0 0 0 2px var(--cv-highlight-bg-dark);
  color: var(--cv-highlight-text-dark);
  border-bottom: 2px solid var(--cv-active-border-dark);
}

/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 5px;
  border: 2px solid transparent;
  background-clip: content-box;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.4);
}

:global(html.dark) .custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.2);
}

:global(html.dark) .custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.4);
}

.custom-scrollbar::-webkit-scrollbar-corner {
  background: transparent;
}
</style>
