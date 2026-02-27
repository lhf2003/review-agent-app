<script setup>
import { ref, defineProps, defineEmits, computed } from 'vue'
import { Check, Plus, QuestionFilled, Tools } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  modelValue: {
    type: String,
    required: true
  },
  providers: {
    type: Array,
    required: true
  },
  providerForms: {
    type: Object,
    required: true
  },
  verifiedProviders: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['update:modelValue', 'update:providerForms', 'update:verifiedProviders'])

const auth = useAuthStore()
const testingModel = ref('')
const expandedProvider = ref('')
const addModelDialog = ref(false)
const manualModelForm = ref({ providerId: '', id: '', name: '' })

// 默认模型配置
const defaultModels = ref({
  SESSION_SPLIT: null,
  TAG_CLASSIFICATION: null,
  SMART_ANALYSIS: null
})
const loadingDefaultModels = ref(false)
const savingDefaultModels = ref(false)

import { watch } from 'vue'

watch(() => props.providers, (newVal) => {
  if (newVal.length > 0 && !expandedProvider.value) {
    expandedProvider.value = props.modelValue || newVal[0].id
  }
}, { immediate: true })

watch(() => props.modelValue, (newVal) => {
    if (newVal) {
        expandedProvider.value = newVal
    }
})

watch(expandedProvider, (newVal) => {
    if (newVal) {
        loadSelectedModels(newVal)
    }
})

function getProviderIcon(url) {
  if (!url) return '/icons/cpu.svg' // Default icon
  if (url.includes('deepseek')) return '/icons/deepseek-color.svg'
  if (url.includes('aliyuncs')) return '/icons/bailian-color.svg'
  if (url.includes('localhost') || url.includes('ollama')) return '/icons/ollama.svg'
  return '/icons/cpu.svg'
}

function getProviderName(provider) {
  if (provider.name === 'deepseek') return 'DeepSeek'
  if (provider.name === 'bailian') return '阿里云百炼'
  if (provider.name === 'ollama') return 'Ollama'
  return provider.name
}

async function checkConnection(providerId) {
  testingModel.value = providerId
  try {
    const config = props.providerForms[providerId]
    const payload = {
      userId: Number(auth.userId),
      name: providerId,
      apiKey: config.apiKey,
      url: config.requestUrl
    }

    const connected = await api.connectLlmProvider(payload)

    // Update local status
    const newVerified = { ...props.verifiedProviders, [providerId]: !!connected }
    emit('update:verifiedProviders', newVerified)

    if (connected) {
      ElMessage.success(`${providerId} 连接成功`)
      // Fetch models
      await fetchModels(providerId)
    } else {
      ElMessage.error(`${providerId} 连接失败，请检查配置`)
    }
    
    // Trigger save in parent if needed? 
    // The parent saveConfig calls updateUserModelConfig which relies on verifiedProviders.
    // We might need to emit an event to tell parent to save, or just let the user click save.
    // The original code called `await saveConfig()` here. 
    // I should probably emit an event 'save' or similar.
    emit('save')

  } catch (e) {
    ElMessage.error(`连接失败: ${e.message}`)
    const newVerified = { ...props.verifiedProviders, [providerId]: false }
    emit('update:verifiedProviders', newVerified)
  } finally {
    testingModel.value = ''
  }
}

async function fetchModels(providerId) {
  try {
    const config = props.providerForms[providerId]
    const payload = {
      userId: Number(auth.userId),
      name: providerId,
      apiKey: config.apiKey,
      url: config.requestUrl
    }
    const models = await api.getLlmModels(payload)
    if (models && Array.isArray(models)) {
      props.providerForms[providerId].models = models
    }
  } catch {
  }
}

function openAddManualModelDialog(providerId) {
  manualModelForm.value = { providerId, id: '', name: '' }
  addModelDialog.value = true
}

function addManualModel() {
  if (!manualModelForm.value.id) return
  const providerId = manualModelForm.value.providerId
  const newModel = {
    id: manualModelForm.value.id,
    object: 'model',
    ownedBy: 'manual',
    capabilities: ['chat'] // Default assumption
  }

  // Add to list if not exists
  const exists = props.providerForms[providerId].models.some(m => m.id === newModel.id)
  if (!exists) {
    props.providerForms[providerId].models.push(newModel)
    ElMessage.success('模型添加成功')
  } else {
    ElMessage.warning('模型已存在')
  }
  addModelDialog.value = false
}

async function loadSelectedModels(providerId) {
  try {
    const provider = props.providers.find(p => p.id === providerId)
    if (!provider || !provider.dbId) return

    const res = await api.getSelectedModelList(provider.dbId)
    const list = res?.data || res
    
    if (Array.isArray(list) && list.length > 0) {
        // Update active models list
        props.providerForms[providerId].activeModels = list.map(m => m.modelName)
        
        // Also ensure these models are in the display list if not already
        const existingIds = new Set(props.providerForms[providerId].models.map(m => m.id))
        const newModels = list
            .filter(m => !existingIds.has(m.modelName))
            .map(m => ({
                id: m.modelName,
                object: 'model',
                ownedBy: 'system',
                capabilities: ['chat'] 
            }))
            
        if (newModels.length > 0) {
            props.providerForms[providerId].models = [...props.providerForms[providerId].models, ...newModels]
        }
    } else {
        props.providerForms[providerId].activeModels = []
    }
  } catch {
  }
}

async function selectModel(providerId, modelId) {
    const form = props.providerForms[providerId]
    if (!form.activeModels) form.activeModels = []
    
    const idx = form.activeModels.indexOf(modelId)
    const isActive = idx > -1
    
    // Optimistic UI update
    if (isActive) {
        form.activeModels.splice(idx, 1)
    } else {
        form.activeModels.push(modelId)
    }

    // Also select the provider if not selected
    if (props.modelValue !== providerId) {
        emit('update:modelValue', providerId)
    }
    
    try {
        const provider = props.providers.find(p => p.id === providerId)
        if (provider && provider.dbId) {
            const payload = {
                providerId: provider.dbId,
                modelName: modelId
            }
            if (isActive) {
                await api.deactiveSelectedModel(payload)
                // ElMessage.success(`已取消 ${modelId}`)
            } else {
                await api.activeSelectedModel(payload)
                // ElMessage.success(`已激活 ${modelId}`)
            }
        }
    } catch (e) {
        // Revert UI on error
        if (isActive) {
            form.activeModels.push(modelId)
        } else {
            const revertIdx = form.activeModels.indexOf(modelId)
            if (revertIdx > -1) form.activeModels.splice(revertIdx, 1)
        }
        ElMessage.error(`操作失败: ${e.message}`)
    }
}

function toggleProvider(providerId) {
    expandedProvider.value = providerId
    emit('update:modelValue', providerId)
}

// 默认模型配置相关方法
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
    } catch {
    } finally {
        loadingDefaultModels.value = false
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
        emit('save')
    } catch (e) {
        ElMessage.error('保存失败: ' + e.message)
    } finally {
        savingDefaultModels.value = false
    }
}

// 获取用于分组下拉框的数据
const groupedModelOptions = computed(() => {
    const groups = []

    Object.keys(props.providerForms).forEach(providerId => {
        const form = props.providerForms[providerId]
        if (props.verifiedProviders[providerId] && form.activeModels && form.activeModels.length > 0) {
            const provider = props.providers.find(p => p.id === providerId)
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

// 监听 providerForms 变化，当有新模型激活时重新加载默认模型
watch(() => props.verifiedProviders, (newVal) => {
    // 当有至少一个提供商被连接时，加载默认模型配置
    const hasConnected = Object.values(newVal).some(v => v === true)
    if (hasConnected && Object.keys(defaultModels.value).every(key => defaultModels.value[key] === null)) {
        loadDefaultModels()
    }
}, { immediate: true })
</script>

<template>
  <div class="model-config-container">
    <div class="group-header">
      <div style="display:flex;align-items:center;gap:8px;">
        <h3>模型配置</h3>
        <el-tag type="warning" size="small" effect="plain" round class="beta-tag">Beta</el-tag>
      </div>
      <p>选择并配置用于分析的 AI 模型</p>
    </div>

    <!-- 默认模型配置区域 -->
    <div class="default-models-section">
      <div class="section-title">
        <h4>默认模型</h4>
        <p>为不同的分析场景配置默认使用的模型</p>
      </div>

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
          :loading="loadingDefaultModels"
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
          :loading="loadingDefaultModels"
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
          :loading="loadingDefaultModels"
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

      <div class="default-model-actions">
        <el-button
          type="primary"
          :loading="savingDefaultModels"
          @click="saveDefaultModels"
          class="save-default-btn"
        >
          保存默认模型配置
        </el-button>
      </div>
    </div>

    <el-divider />

    <div class="provider-list">
      <div v-for="provider in providers" :key="provider.id" class="provider-card"
        :class="{ active: modelValue === provider.id, expanded: expandedProvider === provider.id }">
        <!-- Provider Header -->
        <div class="provider-header" @click="toggleProvider(provider.id)">
          <div class="header-left">
            <div class="provider-icon">
              <img :src="getProviderIcon(providerForms[provider.id]?.requestUrl || provider.defaultUrl)" alt="" />
            </div>
            <span class="provider-name">{{ getProviderName(provider) }}</span>
          </div>
          <div class="header-right">
            <el-tag v-if="verifiedProviders[provider.id]" type="success" size="small" effect="light">
              <el-icon>
                <Check />
              </el-icon> 已连接
            </el-tag>
            <el-tag v-else type="info" size="small" effect="plain">未连接</el-tag>
            <div class="radio-indicator">
              <div class="radio-inner" v-if="modelValue === provider.id"></div>
            </div>
          </div>
        </div>

        <!-- Provider Content (Collapsible) -->
        <div class="provider-body" v-show="expandedProvider === provider.id">
          <div class="config-row">
            <div class="config-item url-item">
              <label>请求地址</label>
              <el-input v-model="providerForms[provider.id].requestUrl" :placeholder="provider.defaultUrl"
                class="apple-input" :disabled="provider.id !== 'ollama'" />
            </div>
            <div class="config-item key-item" v-if="provider.id !== 'ollama'">
              <label>API Key</label>
              <el-input v-model="providerForms[provider.id].apiKey" type="password" show-password
                placeholder="请输入 API Key" class="apple-input" />
            </div>
            <el-button type="primary" plain class="check-btn" :loading="testingModel === provider.id"
              @click="checkConnection(provider.id)">
              检测
            </el-button>
          </div>

          <!-- Model List -->
          <div class="model-selection"
            v-if="verifiedProviders[provider.id] || providerForms[provider.id].models.length > 0">
            <div class="selection-header">
              <div class="header-title-row">
                <span>可用模型</span>
                <el-tooltip content="点击下方模型激活" placement="top">
                    <el-icon class="help-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <el-button link type="primary" size="small" @click="openAddManualModelDialog(provider.id)">
                <el-icon>
                  <Plus />
                </el-icon> 手动添加
              </el-button>
            </div>

            <div class="model-grid" v-if="providerForms[provider.id].models.length > 0">
              <div v-for="model in providerForms[provider.id].models" 
                   :key="model.id" 
                   class="model-chip"
                   :class="{ 'is-active': providerForms[provider.id].activeModels?.includes(model.id) }"
                   @click="selectModel(provider.id, model.id)">
                <div class="chip-content">
                  <span class="chip-name">{{ model.id }}</span>
                  <div class="chip-tags">
                    <el-tag v-if="model.capabilities && model.capabilities.includes('tools')" size="small"
                      type="warning" effect="plain"><el-icon><Tools /></el-icon></el-tag>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-models">
              暂未绑定模型，请点击检测或手动添加
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Add Manual Model Dialog -->
  <el-dialog v-model="addModelDialog" title="手动添加模型" width="400px" class="apple-dialog" align-center append-to-body>
    <el-form label-position="top">
      <el-form-item label="模型 ID">
        <el-input v-model="manualModelForm.id" placeholder="如 deepseek-chat" class="apple-input" />
      </el-form-item>
      <el-form-item label="显示名称 (可选)">
        <el-input v-model="manualModelForm.name" placeholder="自定义显示名称" class="apple-input" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="addModelDialog = false">取消</el-button>
        <el-button type="primary" @click="addManualModel">添加</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.model-config-container {
    width: 100%;
}

.group-header {
  margin-bottom: 16px;
  padding-left: 4px;
}

.group-header h3 {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--el-text-color-primary);
}

.group-header p {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.provider-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.provider-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
}

.provider-card.active {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-5);
}

.provider-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  background: var(--el-fill-color-lighter);
}

.provider-header:hover {
  background: var(--el-fill-color-light);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.provider-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.provider-icon img {
  width: 20px;
  height: 20px;
}

.provider-name {
  font-weight: 600;
  font-size: 16px;
  color: var(--el-text-color-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.radio-indicator {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--el-border-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.provider-card.active .radio-indicator {
  border-color: var(--el-color-primary);
}

.radio-inner {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--el-color-primary);
}

.provider-body {
  padding: 20px;
  border-top: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
}

.config-row {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  margin-bottom: 20px;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.config-item label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.url-item {
  flex: 1;
}

.key-item {
  flex: 1.5;
}

.check-btn {
  height: 32px;
  margin-bottom: 2px;
}

.model-selection {
  margin-top: 20px;
}

.selection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.header-title-row {
    display: flex;
    align-items: center;
    gap: 6px;
}

.help-icon {
    color: var(--el-text-color-secondary);
    cursor: help;
    font-size: 14px;
}

.model-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.model-chip {
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.model-chip:hover {
    border-color: var(--el-color-primary);
    background-color: var(--el-color-primary-light-9);
}

.model-chip.is-active {
    background-color: var(--el-color-success-light-9);
    border-color: var(--el-color-success);
    color: var(--el-color-success);
}

.chip-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chip-name {
  font-weight: 500;
}

.empty-models {
  padding: 20px;
  text-align: center;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  border: 1px dashed var(--el-border-color-light);
}

.apple-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background-color: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 4px 12px;
  transition: all 0.2s;
}

.apple-input :deep(.el-input__wrapper.is-focus) {
  background-color: var(--el-bg-color);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-5);
}

.apple-dialog :deep(.el-dialog) {
    border-radius: 12px;
}

/* 默认模型配置样式 */
.default-models-section {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.section-title {
  margin-bottom: 20px;
  padding-left: 4px;
}

.section-title h4 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--el-text-color-primary);
}

.section-title p {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.default-model-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.model-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
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

.default-model-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-light);
}

.save-default-btn {
  padding: 10px 24px;
  font-weight: 600;
  border-radius: 8px;
}

.apple-select.full-width {
  width: 100%;
}
</style>
