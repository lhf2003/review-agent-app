---
name: frontend-specifications
description: Frontend Development Specifications
---

# Frontend Development Specifications

## Skill Description

Comprehensive Vue 3 + Vite development standards and best practices for the Review Agent application. Covers component architecture, state management, styling, API integration, and code organization patterns.

## Trigger Phrases

- "Create a new Vue component"
- "Add a new page"
- "Implement frontend feature"
- "Write Vue code"
- "Build frontend component"
- "Add API integration"
- "Create state management"
- "Style Vue component"

---

## Technology Stack

**Core Framework**:
- **Vue**: 3.5.24 (Composition API + `<script setup>`)
- **Build Tool**: Vite 7.2.4
- **UI Library**: Element Plus 2.11.9
- **State Management**: Pinia 3.0.4
- **Router**: Vue Router 4.6.3

**Additional Libraries**:
- **Charts**: ECharts 5.6.0 + echarts-wordcloud 2.1.0
- **Markdown**: markdown-it 14.1.0 + highlight.js 11.11.1
- **Animations**: motion-v 1.9.0
- **Security**: DOMPurify 3.3.0
- **Cryptography**: node-forge 1.3.3 + Web Crypto API

---

## MUST DO

1. **ALWAYS use Composition API + `<script setup>`** - This is the only accepted pattern in this codebase
2. **Props MUST have type validation** - Every prop must define type and default value
3. **Use scoped styles** - Component styles should be scoped unless global utility
4. **Follow naming conventions** - PascalCase for components, camelCase for variables
5. **Handle lifecycle cleanup** - Use onUnmounted to remove event listeners and timers
6. **Import in correct order** - Vue API → Third-party → Router → Store → API → Local components
7. **Emit events with clear naming** - Use verb prefixes like `handleClick`, `onSubmit`, `openDialog`
8. **Use CSS variables** - Priority on Element Plus variables (--el-*) for theme compatibility
9. **Handle async errors** - All async operations must have try-catch with user feedback
10. **Extract common components** - Reuse existing components like AnimatedList, CustomScroll, MarkdownRenderer

## MUST NOT DO

1. **NEVER use Options API** - No `data()`, `methods`, `computed` objects in component body
2. **NEVER suppress type errors** - No `as any`, `@ts-ignore`, `@ts-expect-error`
3. **NEVER hardcode colors** - Use CSS variables (--el-*) for theme switching support
4. **NEVER call useStore inside functions** - Store hooks must be called at component top level
5. **NEVER skip cleanup in onUnmounted** - Always remove event listeners and clear timers
6. **NEVER use deep watch unless necessary** - Prefer single source or array watch for performance
7. **NEVER skip error handling** - All async operations must catch and display errors
8. **NEVER use global styles unnecessarily** - Only utility components should have non-scoped styles

---