# Backend Implementation Plan

## 1. Create Data Transfer Objects (DTOs)
Create `LlmModelDTO` in `com.review.agent.entity.dto` (or `pojo`) to structure the model information returned to the frontend.
- Fields: `id` (model name), `object`, `owned_by`, `capabilities` (List of strings, e.g., "chat", "tools").

## 2. Update `LlmModelService`
- Inject `RestClient.Builder` to perform HTTP requests.
- Implement `connectLlmProvider(LlmProvider provider)`:
    - Determine the API endpoint based on the provider name (DeepSeek, Bailian, Ollama).
    - Make a test request (e.g., list models) using the provided API Key and URL.
    - Return `true` if successful, throw exception otherwise.
- Implement `getLlmModels(LlmProvider provider)`:
    - Call the provider's model list endpoint.
    - **DeepSeek**: `https://api.deepseek.com/models` (OpenAI compatible).
    - **Bailian**: `https://dashscope.aliyuncs.com/compatible-mode/v1/models` (OpenAI compatible).
    - **Ollama**: `http://localhost:11434/api/tags`.
    - Parse the response and map it to `List<LlmModelDTO>`.
    - Enrich with capabilities (e.g., check if model name contains "coder" or "chat").

## 3. Update `LlmModelController`
- Update `/connect` endpoint to call `service.connectLlmProvider`.
- Add `@PostMapping("/list")` endpoint to call `service.getLlmModels`.

# Frontend Implementation Plan

## 1. Update `ConfigPage.vue` Logic
- **State Management**: Add `modelList` ref to store fetched models. Add `isVerified` ref to track connection status.
- **API Integration**:
    - `checkConnection()`: Calls `/llm/model/connect`. On success, calls `fetchModels()`.
    - `fetchModels()`: Calls `/llm/model/list`. Updates `modelList`.
- **Manual Addition**: Add logic to push a manually entered model to `modelList`.

## 2. Update `ConfigPage.vue` UI (per `ui-ux-pro-max`)
- **Layout**: Use a card-based layout for provider configuration.
- **Inputs**:
    - **API Key**: Password input with toggle visibility.
    - **URL**: Input field (read-only for Cloud providers, editable for Ollama).
- **Actions**:
    - "Check Connection" button (loading state supported).
    - "Add Model" button (opens a dialog or inline form).
- **Model List**:
    - Grid or List view of models.
    - Capability icons (Chat, Tools/Function Calling).
    - Selection mechanism (Radio or Card selection).
- **Styling**: Apply Apple-style aesthetics (rounded corners, subtle shadows, consistent padding) as per existing `ConfigPage` style.

## 3. Verification
- Test connection with invalid/valid keys.
- Verify model list rendering.
- Verify manual model addition.
