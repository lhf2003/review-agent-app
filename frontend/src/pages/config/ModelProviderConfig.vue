<template>
  <CustomScroll class="model-config-scroll">
    <div class="model-config-page">
    <div class="page-header">
      <h2>模型提供商配置</h2>
      <p>管理您的 AI 模型提供商和激活模型</p>
    </div>

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

    <div class="floating-save-bar">
      <el-button :loading="loading" type="primary" size="large" @click="saveConfig"
        class="save-btn">保存更改</el-button>
    </div>
    </div>
  </CustomScroll>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { Check, Plus, QuestionFilled, Tools } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import CustomScroll from '../../components/CustomScroll.vue'

const auth = useAuthStore()
const loading = ref(false)
const modelValue = ref('')
const providers = ref([])
const providerForms = ref({})
const verifiedProviders = ref({})

const testingModel = ref('')
const expandedProvider = ref('')
const addModelDialog = ref(false)
const manualModelForm = ref({ providerId: '', id: '', name: '' })

onMounted(() => {
    loadData()
})

async function loadData() {
    try {
        loading.value = true
        // Load main config to get current llmProvider
        const configResp = await api.getConfig()
        const cfg = configResp?.data || configResp
        modelValue.value = cfg?.llmProvider || 'deepseek'

        // Load Model Config
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
                    apiKey: m.apiKey || '',
                    requestUrl: m.url || '',
                    models: []
                }

                if (m.isConnected) {
                    verifiedProviders.value[m.name] = true
                    // If connected, fetch models? Or just load selected models?
                    // We need to load selected models for this provider
                    // But first we might need to know if models are already populated?
                    // The backend getUserModelConfig might not return models list.
                    // Let's call fetchModels if needed, but fetchModels hits API.
                    // Better to rely on user action or just load selected models which are stored in DB.
                    loadSelectedModels(m.name)
                }
            })
        }
    } catch (e) {
        
        ElMessage.error('加载配置失败')
    } finally {
        loading.value = false
    }
}

watch(providers, (newVal) => {
  if (newVal.length > 0 && !expandedProvider.value) {
    expandedProvider.value = modelValue.value || newVal[0].id
  }
}, { immediate: true })

watch(modelValue, (newVal) => {
    if (newVal) {
        expandedProvider.value = newVal
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
    const config = providerForms.value[providerId]
    const payload = {
      userId: Number(auth.userId),
      name: providerId,
      apiKey: config.apiKey,
      url: config.requestUrl
    }

    const connected = await api.connectLlmProvider(payload)

    verifiedProviders.value[providerId] = !!connected

    if (connected) {
      ElMessage.success(`${providerId} 连接成功`)
      await fetchModels(providerId)
    } else {
      ElMessage.error(`${providerId} 连接失败，请检查配置`)
    }
    
    // Auto save on successful connection? Or just wait for manual save?
    // User might expect save.
    
  } catch (e) {
    ElMessage.error(`连接失败: ${e.message}`)
    verifiedProviders.value[providerId] = false
  } finally {
    testingModel.value = ''
  }
}

async function fetchModels(providerId) {
  try {
    const config = providerForms.value[providerId]
    const payload = {
      userId: Number(auth.userId),
      name: providerId,
      apiKey: config.apiKey,
      url: config.requestUrl
    }
    const models = await api.getLlmModels(payload)
    if (models && Array.isArray(models)) {
      providerForms.value[providerId].models = models
    }
  } catch (e) {
    
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

  const exists = providerForms.value[providerId].models.some(m => m.id === newModel.id)
  if (!exists) {
    providerForms.value[providerId].models.push(newModel)
    ElMessage.success('模型添加成功')
  } else {
    ElMessage.warning('模型已存在')
  }
  addModelDialog.value = false
}

async function loadSelectedModels(providerId) {
  try {
    const provider = providers.value.find(p => p.id === providerId)
    if (!provider || !provider.dbId) return

    const res = await api.getSelectedModelList(provider.dbId)
    const list = res?.data || res

    if (Array.isArray(list) && list.length > 0) {
        providerForms.value[providerId].activeModels = list.map(m => m.modelName)

        const existingIds = new Set(providerForms.value[providerId].models.map(m => m.id))
        const newModels = list
            .filter(m => !existingIds.has(m.modelName))
            .map(m => ({
                id: m.modelName,
                object: 'model',
                ownedBy: 'system',
                capabilities: ['chat']
            }))

        if (newModels.length > 0) {
            providerForms.value[providerId].models = [...providerForms.value[providerId].models, ...newModels]
        }
    } else {
        providerForms.value[providerId].activeModels = []
    }
  } catch (e) {
    
  }
}

async function selectModel(providerId, modelId) {
    const form = providerForms.value[providerId]
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
    if (modelValue.value !== providerId) {
        modelValue.value = providerId
    }

    try {
        const provider = providers.value.find(p => p.id === providerId)
        if (provider && provider.dbId) {
            const payload = {
                providerId: provider.dbId,
                modelName: modelId
            }
            if (isActive) {
                await api.deactiveSelectedModel(payload)
            } else {
                await api.activeSelectedModel(payload)
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
    modelValue.value = providerId
}

watch(expandedProvider, (newVal) => {
    if (newVal) {
        loadSelectedModels(newVal)
    }
})

async function saveConfig() {
  try {
    loading.value = true
    
    // Save current provider selection
    // We need to update llmProvider in main config
    // But api.updateConfig requires all fields? 
    // Usually updateConfig is partial or full update. 
    // Let's try to update only llmProvider if backend supports it.
    // If backend requires full object, we are in trouble because we don't have other fields here.
    // Assuming we need to fetch config first, merge, and save.
    
    const resp = await api.getConfig()
    const currentConfig = resp?.data || resp
    
    const body = {
      ...currentConfig,
      userId: Number(auth.userId),
      llmProvider: modelValue.value
    }
    await api.updateConfig(body)

    // Save Model Details
    const modelList = []
    for (const [key, value] of Object.entries(providerForms.value)) {
      if (value.apiKey || value.requestUrl) {
        modelList.push({
          id: value.id,
          userId: Number(auth.userId),
          name: key,
          apiKey: value.apiKey,
          url: value.requestUrl,
          isConnected: !!verifiedProviders.value[key]
        })
      }
    }

    await api.updateUserModelConfig(Number(auth.userId), modelList)

    ElMessage.success('配置已保存')
  } catch (e) {
    ElMessage.error(`保存失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.model-config-scroll {
  height: 100%;
}

.model-config-page {
  width: 100%;
  padding-bottom: 80px;
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

.help-icon:hover {
    color: var(--el-color-primary);
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

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
}

.floating-save-bar {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  width: fit-content;
  z-index: 100;
}

.save-btn {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-radius: 24px;
  padding: 12px 32px;
  font-weight: 600;
  transition: all 0.3s;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.16);
}

@media (max-width: 768px) {
  .provider-config-container {
    padding: 16px 20px;
  }
}
</style>