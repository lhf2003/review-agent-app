<template>
  <div class="default-model-page">
    <div class="page-header">
      <h2>默认模型配置</h2>
      <p>为不同的分析场景配置默认使用的模型</p>
    </div>

    <el-divider />

    <div v-loading="loadingDefaultModels" element-loading-text="加载中...">
      <div class="default-model-item">
        <div class="model-label">
          <span>会话拆分模型</span>
          <el-tooltip content="用于将AI对话按主题分割成独立的会话片段" placement="top">
            <el-icon class="help-icon"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
        <el-select
          v-model="defaultModels.SESSION_SPLIT"
          placeholder="选择模型"
          class="apple-select full-width"
          clearable
        >
          <el-option-group
            v-for="group in groupedModelOptions"
            :key="group.label"
            :label="group.label"
          >
            <el-option
              v-for="item in group.options"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-option-group>
          <template v-if="groupedModelOptions.length === 0" #empty>
            <span>请先连接并激活模型</span>
          </template>
        </el-select>
      </div>

      <div class="default-model-item">
        <div class="model-label">
          <span>标签分类模型</span>
          <el-tooltip content="用于为分析结果自动生成主标签和子标签" placement="top">
            <el-icon class="help-icon"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
        <el-select
          v-model="defaultModels.TAG_CLASSIFICATION"
          placeholder="选择模型"
          class="apple-select full-width"
          clearable
        >
          <el-option-group
            v-for="group in groupedModelOptions"
            :key="group.label"
            :label="group.label"
          >
            <el-option
              v-for="item in group.options"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-option-group>
          <template v-if="groupedModelOptions.length === 0" #empty>
            <span>请先连接并激活模型</span>
          </template>
        </el-select>
      </div>

      <div class="default-model-item">
        <div class="model-label">
          <span>智能分析模型</span>
          <el-tooltip content="用于对AI对话内容进行深度分析，提取关键信息" placement="top">
            <el-icon class="help-icon"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
        <el-select
          v-model="defaultModels.SMART_ANALYSIS"
          placeholder="选择模型"
          class="apple-select full-width"
          clearable
        >
          <el-option-group
            v-for="group in groupedModelOptions"
            :key="group.label"
            :label="group.label"
          >
            <el-option
              v-for="item in group.options"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-option-group>
          <template v-if="groupedModelOptions.length === 0" #empty>
            <span>请先连接并激活模型</span>
          </template>
        </el-select>
      </div>
    </div>

    <div class="save-section">
      <el-button
        type="primary"
        :loading="savingDefaultModels"
        @click="saveDefaultModels"
        class="save-btn"
        size="large"
      >
        保存默认模型配置
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/http'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()

// 默认模型配置
const defaultModels = ref({
  SESSION_SPLIT: null,
  TAG_CLASSIFICATION: null,
  SMART_ANALYSIS: null
})
const loadingDefaultModels = ref(false)
const savingDefaultModels = ref(false)

// Data for options
const providers = ref([])
const providerForms = ref({})
const verifiedProviders = ref({})

// 获取用于分组下拉框的数据
const groupedModelOptions = computed(() => {
    const groups = []

    Object.keys(providerForms.value).forEach(providerId => {
        const form = providerForms.value[providerId]
        if (verifiedProviders.value[providerId] && form.activeModels && form.activeModels.length > 0) {
            const provider = providers.value.find(p => p.id === providerId)
            if (provider) {
                groups.push({
                    label: getProviderName(provider),
                    options: form.activeModels.map(modelName => ({
                        label: modelName,
                        value: modelName,
                        providerId: providerId
                    }))
                })
            }
        }
    })

    return groups
})

function getProviderName(provider) {
  if (provider.name === 'deepseek') return 'DeepSeek'
  if (provider.name === 'bailian') return '阿里云百炼'
  if (provider.name === 'ollama') return 'Ollama'
  return provider.name
}

onMounted(() => {
    loadDefaultModels()
    loadModelConfig()
})

async function loadDefaultModels() {
    try {
        loadingDefaultModels.value = true
        const resp = await api.getUserDefaultModels()
        const modelList = resp?.data || resp || []

        modelList.forEach(m => {
            if (m.modelType === 'SESSION_SPLIT') {
                defaultModels.value.SESSION_SPLIT = m.modelName
            } else if (m.modelType === 'TAG_CLASSIFICATION') {
                defaultModels.value.TAG_CLASSIFICATION = m.modelName
            } else if (m.modelType === 'SMART_ANALYSIS') {
                defaultModels.value.SMART_ANALYSIS = m.modelName
            }
        })
    } catch (e) {
        
    } finally {
        loadingDefaultModels.value = false
    }
}

async function loadModelConfig() {
    try {
        // Load Model Config to get available models
        const modelResp = await api.getUserModelConfig(auth.userId)
        const modelList = modelResp?.data || modelResp

        if (Array.isArray(modelList)) {
            providers.value = modelList.map(m => ({
                id: m.name,
                name: m.name,
                defaultUrl: m.url,
                isCustom: false,
                dbId: m.id
            }))

            modelList.forEach(m => {
                providerForms.value[m.name] = {
                    id: m.id || '',
                    activeModels: [],
                    // apiKey, url not needed here unless we want to test
                }

                if (m.isConnected) {
                    verifiedProviders.value[m.name] = true
                    loadSelectedModels(m.name)
                }
            })
        }
    } catch (e) {
        
    }
}

async function loadSelectedModels(providerId) {
  try {
    const provider = providers.value.find(p => p.id === providerId)
    if (!provider || !provider.dbId) return

    const res = await api.getSelectedModelList(provider.dbId)
    const list = res?.data || res

    if (Array.isArray(list) && list.length > 0) {
        providerForms.value[providerId].activeModels = list.map(m => m.modelName)
    } else {
        providerForms.value[providerId].activeModels = []
    }
  } catch (e) {
    
  }
}

async function saveDefaultModels() {
    try {
        savingDefaultModels.value = true

        const modelConfigs = []
        if (defaultModels.value.SESSION_SPLIT) {
            modelConfigs.push({
                modelType: 'SESSION_SPLIT',
                modelName: defaultModels.value.SESSION_SPLIT
            })
        }
        if (defaultModels.value.TAG_CLASSIFICATION) {
            modelConfigs.push({
                modelType: 'TAG_CLASSIFICATION',
                modelName: defaultModels.value.TAG_CLASSIFICATION
            })
        }
        if (defaultModels.value.SMART_ANALYSIS) {
            modelConfigs.push({
                modelType: 'SMART_ANALYSIS',
                modelName: defaultModels.value.SMART_ANALYSIS
            })
        }

        await api.updateUserDefaultModels(modelConfigs)
        ElMessage.success('默认模型配置已保存')
    } catch (e) {
        
        ElMessage.error('保存失败: ' + e.message)
    } finally {
        savingDefaultModels.value = false
    }
}
</script>

<style scoped>
.default-model-page {
  width: 100%;
}

.page-header {
  margin-bottom: 24px;
  padding-left: 4px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--el-text-color-primary);
}

.page-header p {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.default-model-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 32px;
  padding: 24px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
}

.model-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 12px;
}

.model-label .help-icon {
  color: var(--el-text-color-secondary);
  cursor: help;
  font-size: 14px;
  transition: all 0.2s;
}

.model-label .help-icon:hover {
  color: var(--el-color-primary);
}

.apple-select.full-width {
  width: 100%;
}

.save-section {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--el-border-color-light);
}

.save-btn {
  padding: 12px 32px;
  font-weight: 600;
  border-radius: 8px;
}

@media (max-width: 768px) {
  .config-container {
    padding: 16px;
  }

  .save-section {
    padding-top: 16px;
  }
}
</style>