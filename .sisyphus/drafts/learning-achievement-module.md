# Draft: Learning Achievement Module (Iteration 1)

## User's Request Summary
Implement Learning Achievement Module with 4 features:
1. Quiz score trend chart (line chart)
2. Knowledge point mastery distribution (radar or pie chart)
3. Achievement badge system
4. Learning progress visualization

## Key Requirements Provided

### Technical Stack
- Backend: Java 17+, Spring Boot 3.x, JPA, MySQL 8.0+
- Frontend: Vue 3, Vite, Pinia, Element Plus, ECharts 5.6.0, echarts-wordcloud 2.1.0
- Design: Apple macOS style + Modern Glass effect

### Existing Data Models
- QuizRecord: {id, userId, collectionId, totalScore, status, createdTime}
- QuizQuestion: {id, quizId, relatedAnalysisId, questionText, optionsJson, correctAnswer, explanation, userAnswer, isCorrect}
- QuizRecordRepository: countByUserIdAndStatus, findTop2ByUserIdAndStatusOrderByCreatedTimeDesc
- QuizQuestionRepository: findByQuizId
- AnalysisResult: {id, fileId, userId, problemStatement, solution, createdTime}
- AnalysisTag: {id, analysisId, tagId, subTagId, recommends, confidenceScore}
- MainTag: {id, userId, name}
- SubTag: {id, userId, name}
- TagRelation: {id, userId, mainTagId, subTagId}

### Current ProfilePage.vue Structure
- Hero Section: Welcome area (avatar, username, learning days)
- Recent Activity Timeline
- Statistics Grid: 6 stat cards
- Quick Actions Grid: 8 shortcut buttons
- Uses CustomScroll component
- Data source: api.getUserStats() → UserStatsVo

### Design Requirements
- Use CustomScroll component for scrolling
- Modern Glass effect
- Borderless design
- Dark/Light mode support
- Reference WordCloudPage.vue ECharts patterns

### Achievement System Suggestions
- Basic achievement types (first sync, first quiz, 7-day streak, 10 collections, etc.)
- Each achievement: id, name, description, icon, unlock condition, unlock status
- Two states: unlocked/locked
- Support progress display (5/10 quizzes completed)

### Knowledge Point Mastery
- Based on quiz correctness: group by main tag, calculate correctness rate
- Or based on tag frequency: count AnalysisResults per tag
- Suggestion: prioritize quiz correctness

## Research Findings

### Frontend Patterns (from ProfilePage.vue)
- File: `frontend/src/pages/ProfilePage.vue` (1,142 lines)
- Layout: Single-page vertical scroll with CustomScroll wrapper
- No tabs or sections - just sequential sections
- Data fetching: `api.getUserStats()` returns UserStatsVo
- State: Vue 3 Composition API with ref()
- Animation: 1.5s easeOutQuart for number transitions
- Quick Actions: Grid of 8 navigation shortcuts to other pages

### Backend Patterns (from statistics modules)
- VO Classes: UserStatsVo, StatisticVo, QuizVo with nested static classes
- Service Layer: UserService.getUserStats() aggregates data from multiple repositories
- Repository Patterns: countBy*, findTop*By*, findAllBy* (simple counts, Top-N, date-range)
- Query Patterns: @Query for custom SQL, batch queries with in clause
- Controller Pattern: @RestController, @RequestMapping, @GetMapping/PostMapping

### ECharts Integration (pending WordCloudPage analysis)
- Awaiting results from background task

## Key Decisions Needed from User
