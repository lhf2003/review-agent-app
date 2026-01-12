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
  <div class="code-viewer" ref="viewerRef">
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
</template>

<style scoped>
.code-viewer {
  height: 100%;
  overflow: auto;
  background-color: #282c34; /* Atom One Dark bg */
  color: #abb2bf;
  font-family: 'Fira Code', monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap; /* Wrap long lines */
}

.content-wrapper {
  position: relative;
  min-height: 100%;
  width: fit-content;
  min-width: 100%;
  padding: 16px;
  box-sizing: border-box;
}

.mask-layer {
  position: absolute;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.3);
  z-index: 1;
  pointer-events: none;
  transition: opacity 0.3s;
}

pre {
  margin: 0;
  position: relative;
  z-index: 0;
}

.session-highlight {
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px dashed rgba(171, 178, 191, 0.3);
}

.session-highlight:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.session-highlight.active {
  position: relative;
  z-index: 2;
  background-color: #3e4451;
  box-shadow: 0 0 0 4px #3e4451;
  border-radius: 4px;
  color: #fff;
  border-bottom: none;
  box-decoration-break: clone;
  -webkit-box-decoration-break: clone;
}
</style>
