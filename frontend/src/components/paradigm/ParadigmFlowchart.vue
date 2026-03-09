<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { Graph } from '@antv/g6'
import { Loading } from '@element-plus/icons-vue'

/**
 * 思维范式流程图组件
 * 使用AntV G6渲染不同类型的范式可视化
 */
const props = defineProps({
  flowchartId: {
    type: Number,
    default: null
  },
  paradigmCode: {
    type: String,
    default: ''
  },
  paradigmName: {
    type: String,
    default: ''
  },
  paradigmType: {
    type: String,
    validator: (value) => ['FLOW_CHART', 'DECISION_TREE', 'DECOMPOSITION_TREE', 'DERIVATION_CHAIN'].includes(value),
    default: 'FLOW_CHART'
  },
  data: {
    type: Object,
    default: () => ({ nodes: [], edges: [] })
  },
  height: {
    type: String,
    default: '400px'
  },
  interactive: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['nodeClick', 'nodeHover'])

const graphContainer = ref(null)
let graph = null
const loading = ref(false)

// 根据paradigmType获取不同的布局配置
const getLayoutConfig = (type) => {
  const configs = {
    FLOW_CHART: {
      type: 'dagre',
      rankdir: 'TB',
      align: 'DL',
      nodesep: 40,
      ranksep: 60
    },
    DECISION_TREE: {
      type: 'mindmap',
      direction: 'H',
      getHeight: () => 40,
      getWidth: () => 120,
      getVGap: () => 20,
      getHGap: () => 50
    },
    DECOMPOSITION_TREE: {
      type: 'indented',
      direction: 'LR',
      indent: 40,
      getHeight: () => 40,
      getWidth: () => 120
    },
    DERIVATION_CHAIN: {
      type: 'dagre',
      rankdir: 'LR',
      nodesep: 40,
      ranksep: 80
    }
  }
  return configs[type] || configs.FLOW_CHART
}

// 节点样式根据类型 - 适配深色主题的暖色调
const getNodeStyle = (type) => {
  const styles = {
    START: { fill: '#22c55e', stroke: '#16a34a' },
    PROCESS: { fill: '#CC6633', stroke: '#A8552A' },
    DECISION: { fill: '#f59e0b', stroke: '#d97706' },
    END: { fill: '#ef4444', stroke: '#dc2626' }
  }
  return styles[type] || styles.PROCESS
}

// 获取节点形状
const getNodeShape = (type) => {
  const shapes = {
    START: 'circle',
    PROCESS: 'rect',
    DECISION: 'diamond',
    END: 'circle'
  }
  return shapes[type] || 'rect'
}

// 获取节点大小
const getNodeSize = (type) => {
  const sizes = {
    START: [60, 60],
    PROCESS: [120, 40],
    DECISION: [100, 60],
    END: [60, 60]
  }
  return sizes[type] || [120, 40]
}

const initGraph = () => {
  if (!graphContainer.value) return

  const width = graphContainer.value.clientWidth
  const height = graphContainer.value.clientHeight

  graph = new Graph({
    container: graphContainer.value,
    width,
    height,
    modes: {
      default: props.interactive ? ['drag-canvas', 'zoom-canvas', 'drag-node', 'click-select'] : []
    },
    layout: getLayoutConfig(props.paradigmType),
    defaultNode: {
      type: 'rect',
      size: [120, 40],
      style: {
        radius: 4,
        fill: '#CC6633',
        stroke: '#A8552A',
        lineWidth: 1
      },
      labelCfg: {
        style: {
          fill: '#ffffff',
          fontSize: 12
        }
      }
    },
    defaultEdge: {
      type: 'polyline',
      style: {
        stroke: 'rgba(255, 248, 245, 0.4)',
        lineWidth: 1,
        endArrow: true
      },
      labelCfg: {
        style: {
          fill: 'rgba(255, 255, 255, 0.6)',
          fontSize: 11
        }
      }
    },
    fitView: true,
    fitViewPadding: [20, 20, 20, 20]
  })

  // 事件监听
  if (props.interactive) {
    graph.on('node:click', (evt) => {
      emit('nodeClick', evt.item.getModel())
    })
    graph.on('node:mouseenter', (evt) => {
      emit('nodeHover', evt.item.getModel())
    })
  }
}

const renderData = () => {
  if (!graph || !props.data) return

  const nodes = (props.data.nodes || []).map(node => ({
    id: node.id,
    label: node.label,
    type: getNodeShape(node.type),
    size: getNodeSize(node.type),
    style: {
      ...getNodeStyle(node.type),
      radius: node.type === 'DECISION' ? 0 : 4,
      cursor: props.interactive ? 'pointer' : 'default'
    },
    labelCfg: {
      style: {
        fill: '#ffffff',
        fontSize: 12,
        fontWeight: 500
      }
    },
    // 存储额外数据
    description: node.description,
    codeSnippet: node.codeSnippet,
    nodeType: node.type
  }))

  const edges = (props.data.edges || []).map(edge => ({
    source: edge.source,
    target: edge.target,
    label: edge.label,
    style: {
      stroke: 'rgba(255, 248, 245, 0.4)',
      lineWidth: 1.5,
      endArrow: {
        path: 'M 0,0 L 8,4 L 8,-4 Z',
        fill: 'rgba(255, 248, 245, 0.4)'
      }
    },
    labelCfg: {
      style: {
        fill: 'rgba(255, 255, 255, 0.7)',
        fontSize: 11,
        background: {
          fill: 'rgba(26, 15, 8, 0.8)',
          padding: [2, 4],
          radius: 2
        }
      },
      autoRotate: true
    }
  }))

  graph.data({ nodes, edges })
  graph.render()
}

// ResizeObserver 处理容器大小变化
let resizeObserver = null

const handleResize = () => {
  if (graph && graphContainer.value) {
    const width = graphContainer.value.clientWidth
    const height = graphContainer.value.clientHeight
    graph.changeSize(width, height)
    graph.fitView()
  }
}

onMounted(() => {
  initGraph()
  renderData()

  // 使用ResizeObserver监听容器大小变化
  if (graphContainer.value) {
    resizeObserver = new ResizeObserver(handleResize)
    resizeObserver.observe(graphContainer.value)
  }
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (graph) {
    graph.destroy()
    graph = null
  }
})

// 监听数据变化
watch(() => props.data, renderData, { deep: true })

// 监听类型变化，重新初始化
watch(() => props.paradigmType, () => {
  if (graph) {
    graph.destroy()
    graph = null
  }
  initGraph()
  renderData()
})

// 暴露方法供父组件调用
defineExpose({
  fitView: () => graph?.fitView(),
  zoomIn: () => graph?.zoom(1.2),
  zoomOut: () => graph?.zoom(0.8),
  resetZoom: () => {
    graph?.zoomTo(1)
    graph?.fitView()
  }
})
</script>

<template>
  <div ref="graphContainer" class="paradigm-flowchart" :style="{ height }">
    <div v-if="loading" class="loading-overlay">
      <ElIcon class="is-loading"><Loading /></ElIcon>
      <span>加载中...</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.paradigm-flowchart {
  width: 100%;
  position: relative;
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-radius: var(--radius-card);
  border: 1px solid var(--glass-border);
  overflow: hidden;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(26, 15, 8, 0.85);
  backdrop-filter: blur(4px);
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  z-index: 10;
}

</style>
