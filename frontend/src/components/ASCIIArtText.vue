<script setup>
/**
 * ASCII艺术字体组件
 * 预定义的ASCII艺术字，用于标题展示
 */
import { computed } from 'vue'

const props = defineProps({
  text: {
    type: String,
    required: true
  },
  font: {
    type: String,
    default: 'block',
    validator: (value) => ['block', 'banner', 'small', 'tiny'].includes(value)
  },
  animate: {
    type: Boolean,
    default: true
  },
  speed: {
    type: Number,
    default: 30 // ms per line
  }
})

// 预定义的ASCII艺术字体
const asciiFonts = {
  block: {
    'R': [
      '██████╗ ',
      '██╔══██╗',
      '██████╔╝',
      '██╔══██╗',
      '██║  ██║',
      '╚═╝  ╚═╝'
    ],
    'A': [
      ' █████╗ ',
      '██╔══██╗',
      '███████║',
      '██╔══██║',
      '██║  ██║',
      '╚═╝  ╚═╝'
    ],
    'V': [
      '██╗   ██╗',
      '██║   ██║',
      '██║   ██║',
      '╚██╗ ██╔╝',
      ' ╚████╔╝ ',
      '  ╚═══╝  '
    ],
    'G': [
      ' ██████╗ ',
      '██╔════╝ ',
      '██║  ███╗',
      '██║   ██║',
      '╚██████╔╝',
      ' ╚═════╝ '
    ],
    'E': [
      '███████╗',
      '██╔════╝',
      '█████╗  ',
      '██╔══╝  ',
      '███████╗',
      '╚══════╝'
    ],
    'N': [
      '███╗   ██╗',
      '████╗  ██║',
      '██╔██╗ ██║',
      '██║╚██╗██║',
      '██║ ╚████║',
      '╚═╝  ╚═══╝'
    ],
    'T': [
      '████████╗',
      '╚══██╔══╝',
      '   ██║   ',
      '   ██║   ',
      '   ██║   ',
      '   ╚═╝   '
    ],
    ' ': [
      '   ',
      '   ',
      '   ',
      '   ',
      '   ',
      '   '
    ]
  },
  banner: {
    'R': [
      '######  ',
      '#     # ',
      '######  ',
      '#   #   ',
      '#    #  ',
      '#     # '
    ],
    'A': [
      '     #   ',
      '    # #  ',
      '   ##### ',
      '  #     #',
      ' #       #',
      '#         #'
    ],
    'V': [
      '#       #',
      ' #     # ',
      '  #   #  ',
      '   # #   ',
      '    #    ',
      '         '
    ],
    'G': [
      '  #####  ',
      ' #     # ',
      ' #       ',
      ' #   ### ',
      ' #     # ',
      '  #####  '
    ],
    'E': [
      '#######',
      '#      ',
      '#####  ',
      '#      ',
      '#######',
      '       '
    ],
    'N': [
      '#      #',
      '##     #',
      '# #    #',
      '#  #   #',
      '#   #  #',
      '#    # #'
    ],
    'T': [
      '#########',
      '    #    ',
      '    #    ',
      '    #    ',
      '    #    ',
      '         '
    ],
    ' ': [
      '    ',
      '    ',
      '    ',
      '    ',
      '    ',
      '    '
    ]
  },
  small: {
      'R': ['██████╗ ', '██╔══██╗', '██████╔╝', '██╔══██╗', '██║  ██║'],
      'A': [' █████╗ ', '██╔══██╗', '███████║', '██╔══██║', '██║  ██║'],
      'V': ['██╗   ██╗', '██║   ██║', '██║   ██║', '╚██╗ ██╔╝', ' ╚████╔╝ '],
      'G': [' ██████╗ ', '██╔════╝ ', '██║  ███╗', '██║   ██║', '╚██████╔╝'],
      'E': ['███████╗', '██╔════╝', '█████╗  ', '██╔══╝  ', '███████╗'],
      'N': ['███╗   ██╗', '████╗  ██║', '██╔██╗ ██║', '██║╚██╗██║', '██║ ╚████║'],
      'T': ['████████╗', '╚══██╔══╝', '   ██║   ', '   ██║   ', '   ██║   '],
      ' ': ['   ', '   ', '   ', '   ', '   ']
  },
  tiny: {
      'R': ['╔═╗', '╠╣', '║║', '   '],
      'A': ['┌─┐', '├─┤', '┴ ┴', '   '],
      'V': ['┬  ┬', '┴┐┌┘', ' └┘ ', '    '],
      'G': ['┌─┐', '│ ┬', '└─┘', '   '],
      'E': ['┌─┐', '├┤ ', '└─┘', '   '],
      'N': ['┌┐┌', '│││', '┘└┘', '   '],
      'T': ['┌┬┐', ' │ ', ' ┴ ', '   '],
      ' ': ['  ', '  ', '  ', '  ']
  }
}

const artLines = computed(() => {
  const font = asciiFonts[props.font] || asciiFonts.block
  const lines = []
  const maxLines = font['R'].length

  for (let i = 0; i < maxLines; i++) {
    let line = ''
    for (const char of props.text.toUpperCase()) {
      line += (font[char] || font[' '])[i] || '    '
    }
    lines.push(line)
  }

  return lines
})

const animationDelay = computed(() => {
  return artLines.value.map((_, index) => index * props.speed)
})
</script>

<template>
  <pre class="ascii-art-text" :class="`ascii-art-text--${font}`">
    <code
      v-for="(line, index) in artLines"
      :key="index"
      class="ascii-art-line"
      :class="{ 'ascii-art-line--animate': animate }"
      :style="animate ? { animationDelay: `${animationDelay[index]}ms` } : {}"
    >{{ line }}</code>
  </pre>
</template>

<style scoped>
.ascii-art-text {
  font-family: var(--ascii-font-mono);
  margin: 0;
  padding: 0;
  line-height: 1.2;
  display: flex;
  flex-direction: column;
}

.ascii-art-line {
  display: block;
  color: var(--ascii-text-primary);
  white-space: pre;
  overflow: hidden;
}

.ascii-art-text--block .ascii-art-line {
  font-size: 0.7rem;
}

.ascii-art-text--banner .ascii-art-line {
  font-size: 0.8rem;
}

.ascii-art-text--small .ascii-art-line {
  font-size: 0.6rem;
}

.ascii-art-text--tiny .ascii-art-line {
  font-size: 1rem;
}

/* 打字机动画 */
.ascii-art-line--animate {
  animation: ascii-type 0.3s ease-out forwards;
  opacity: 0;
  transform: translateY(-10px);
}

@keyframes ascii-type {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 发光效果 */
.ascii-art-text {
  text-shadow: var(--ascii-glow-text);
}
</style>
