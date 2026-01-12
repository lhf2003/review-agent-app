<template>
  <div ref="containerRef" class="animated-list-container">
    <CustomScroll
      ref="listRef"
      class="animated-list-scroll"
      :hide-scrollbar="!displayScrollbar"
      @scroll="handleScroll"
    >
      <Motion
        v-for="(item, index) in items"
        :key="item[itemKey] || index"
        tag="div"
        :data-index="index"
        class="animated-list-item-wrapper"
        :initial="{ opacity: 0, x: 50 }"
        :in-view="{ opacity: 1, x: 0 }"
        :in-view-options="{ once: false, amount: 0.1 }"
        :transition="{ duration: 0.5, delay: 0.1 }"
      >
        <div 
          @mouseenter="setSelectedIndex(index)"
          @click="handleItemClick(item, index)"
        >
          <slot :item="item" :index="index" :is-selected="selectedIndex === index">
            <div class="default-item" :class="{ 'is-selected': selectedIndex === index }">
              {{ item }}
            </div>
          </slot>
        </div>
      </Motion>
    </CustomScroll>
    
    <div
      v-if="showGradients"
      class="list-gradient top"
      :style="{ opacity: topGradientOpacity }"
    />
    <div
      v-if="showGradients"
      class="list-gradient bottom"
      :style="{ opacity: bottomGradientOpacity }"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, useTemplateRef } from 'vue';
import { Motion } from 'motion-v';
import CustomScroll from './CustomScroll.vue';

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  itemKey: {
    type: String,
    default: 'id'
  },
  showGradients: {
    type: Boolean,
    default: true
  },
  enableArrowNavigation: {
    type: Boolean,
    default: true
  },
  displayScrollbar: {
    type: Boolean,
    default: true
  },
  initialSelectedIndex: {
    type: Number,
    default: -1
  }
});

const emit = defineEmits(['itemSelected']);

const containerRef = useTemplateRef('containerRef');
const listRef = useTemplateRef('listRef');
const selectedIndex = ref(props.initialSelectedIndex);
const keyboardNav = ref(false);
const topGradientOpacity = ref(0);
const bottomGradientOpacity = ref(1);

const setSelectedIndex = (index) => {
  selectedIndex.value = index;
};

const handleItemClick = (item, index) => {
  setSelectedIndex(index);
  emit('itemSelected', item, index);
};

const handleScroll = (e) => {
  const target = e.target;
  const { scrollTop, scrollHeight, clientHeight } = target;
  topGradientOpacity.value = Math.min(scrollTop / 50, 1);
  const bottomDistance = scrollHeight - (scrollTop + clientHeight);
  bottomGradientOpacity.value = scrollHeight <= clientHeight ? 0 : Math.min(bottomDistance / 50, 1);
};

const handleKeyDown = (e) => {
  if (!props.items.length) return;
  
  if (e.key === 'ArrowDown' || (e.key === 'Tab' && !e.shiftKey)) {
    e.preventDefault();
    keyboardNav.value = true;
    setSelectedIndex(Math.min(selectedIndex.value + 1, props.items.length - 1));
  } else if (e.key === 'ArrowUp' || (e.key === 'Tab' && e.shiftKey)) {
    e.preventDefault();
    keyboardNav.value = true;
    setSelectedIndex(Math.max(selectedIndex.value - 1, 0));
  } else if (e.key === 'Enter') {
    if (selectedIndex.value >= 0 && selectedIndex.value < props.items.length) {
      e.preventDefault();
      emit('itemSelected', props.items[selectedIndex.value], selectedIndex.value);
    }
  }
};

watch([selectedIndex, keyboardNav], () => {
  if (!keyboardNav.value || selectedIndex.value < 0 || !listRef.value) return;
  // CustomScroll component exposes the underlying div element via $el usually, 
  // but if we want to access scrollTop, we need to access the DOM element.
  // Vue components by default expose their instance. 
  // Let's assume CustomScroll renders the div as root.
  const container = listRef.value.$el || listRef.value;
  // Use data-index to find the motion wrapper
  const selectedItem = container.querySelector(`[data-index="${selectedIndex.value}"]`);
  
  if (selectedItem) {
    const extraMargin = 50;
    const containerScrollTop = container.scrollTop;
    const containerHeight = container.clientHeight;
    const itemTop = selectedItem.offsetTop;
    const itemBottom = itemTop + selectedItem.offsetHeight;
    
    if (itemTop < containerScrollTop + extraMargin) {
      container.scrollTo({ top: itemTop - extraMargin, behavior: 'smooth' });
    } else if (itemBottom > containerScrollTop + containerHeight - extraMargin) {
      container.scrollTo({
        top: itemBottom - containerHeight + extraMargin,
        behavior: 'smooth'
      });
    }
  }
  keyboardNav.value = false;
});

onMounted(() => {
  if (props.enableArrowNavigation) {
    window.addEventListener('keydown', handleKeyDown);
  }
  // Initialize gradients
  if (listRef.value) {
    handleScroll({ target: listRef.value });
  }
});

onUnmounted(() => {
  if (props.enableArrowNavigation) {
    window.removeEventListener('keydown', handleKeyDown);
  }
});
</script>

<style scoped lang="scss">
.animated-list-container {
  position: relative;
  height: 100%;
  overflow: hidden;
}

.animated-list-scroll {
  height: 100%;
  /* overflow properties handled by CustomScroll */
  padding-right: 12px; /* Increase padding to account for scrollbar width */
  margin-right: -4px; /* Slight negative margin to balance padding */
  
  /* Scrollbar styles removed as they are now in CustomScroll */
}

.animated-list-item-wrapper {
  margin-bottom: 16px;
  cursor: pointer;
  margin-right: 4px; /* Prevent content from touching the scrollbar edge */
}

.list-gradient {
  position: absolute;
  left: 0;
  right: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
  z-index: 5;

  &.top {
    top: 0;
    height: 50px;
    background: linear-gradient(to bottom, var(--el-bg-color-page) 0%, transparent 100%);
  }

  &.bottom {
    bottom: 0;
    height: 100px;
    background: linear-gradient(to top, var(--el-bg-color-page) 0%, transparent 100%);
  }
}

.default-item {
  padding: 16px;
  background: var(--el-bg-color);
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
  
  &.is-selected {
    border-color: var(--el-color-primary);
    background-color: var(--el-color-primary-light-9);
  }
}
</style>
