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
    <pre><code><template v-for="(chunk, idx) in chunks" :key="idx"><span 
        v-if="chunk.isHighlight" 
        class="session-highlight" 
        :class="{ active: chunk.sessionIndex === activeSessionIndex }"
        :data-index="chunk.sessionIndex"
        @click="handleChunkClick(chunk)"
      >{{ chunk.text }}</span><span v-else>{{ chunk.text }}</span></template></code></pre>
  </div>
</template>

<style scoped>
.code-viewer {
  height: 100%;
  overflow: auto;
  background-color: #282c34; /* Atom One Dark bg */
  color: #abb2bf;
  padding: 16px;
  font-family: 'Fira Code', monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap; /* Wrap long lines */
}

pre {
  margin: 0;
}

.session-highlight {
  background-color: rgba(255, 215, 0, 0.15); /* Subtle yellow */
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px dashed rgba(255, 215, 0, 0.5);
}

.session-highlight:hover {
  background-color: rgba(255, 215, 0, 0.3);
}

.session-highlight.active {
  background-color: rgba(255, 215, 0, 0.4);
  border-bottom: 2px solid #ffd700;
}
</style>
