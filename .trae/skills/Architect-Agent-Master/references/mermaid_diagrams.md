# Mermaid 图表规范

本文档定义项目中使用 Mermaid 图表的标准规范，确保所有可视化图表风格统一、可读性强、易于维护。

---

## 核心原则

1. **一致性**：同类图表使用相同的样式和命名约定
2. **简洁性**：避免过度复杂的图表，突出核心逻辑
3. **可读性**：节点命名清晰、注释完整、布局合理
4. **可维护性**：模块化设计，易于扩展和修改

---

## 图表类型选择指南

| 图表类型 | 适用场景 | 示例 |
|----------|----------|------|
| **Sequence Diagram**（时序图） | 跨模块交互、API 调用流程、消息传递 | 用户登录、订单支付流程 |
| **Flowchart**（流程图） | 业务逻辑流转、数据处理流程、决策分支 | 订单状态流转、数据清洗流程 |
| **Class Diagram**（类图） | 实体关系、继承结构、接口定义 | 用户-订单-商品关系 |
| **Entity Relationship Diagram**（ER图） | 数据库表关系、外键约束、索引设计 | 核心数据表关联 |
| **State Diagram**（状态机） | 有限状态流转、生命周期管理 | 订单状态、设备在线状态 |

---

## 样式规范

### 通用配置

所有 Mermaid 图表应包含通用样式配置：

```mermaid
%%{init: {
  'theme': 'base',
  'themeVariables': {
    'primaryColor': '#e3f2fd',
    'primaryTextColor': '#1565c0',
    'primaryBorderColor': '#1565c0',
    'lineColor': '#42a5f5',
    'secondaryColor': '#f3e5f5',
    'tertiaryColor': '#fff'
  }
}}%%
```

### 命名约定

- **节点命名**：使用小写字母 + 下划线，如 `user_service`、`order_controller`
- **方法命名**：驼峰命名，如 `processOrder()`、`validateUser()`
- **参数命名**：简短清晰，如 `userId`、`orderId`

### 注释规范

```mermaid
sequenceDiagram
    participant User as 用户
    participant Frontend as 前端
    participant Backend as 后端

    %% 用户发起登录请求
    User->>Frontend: 输入用户名/密码
    Frontend->>Backend: POST /api/auth/login

    %% 后端验证凭证
    Backend->>Backend: 验证用户名/密码
    alt 验证成功
        Backend-->>Frontend: 返回 JWT Token
    else 验证失败
        Backend-->>Frontend: 返回 401 错误
    end
```

---

## Sequence Diagram 规范（时序图）

### 基本结构

```mermaid
sequenceDiagram
    %% 参与者定义
    participant Client as 客户端
    participant API as API 网关
    participant Service as 业务服务
    participant DB as 数据库

    %% 交互流程
    Client->>API: 请求
    API->>Service: 转发请求
    Service->>DB: 查询数据
    DB-->>Service: 返回结果
    Service-->>API: 返回响应
    API-->>Client: 返回结果
```

### 最佳实践

1. **参与者顺序**：从左到右按调用层次排列（客户端 → 前端 → 后端 → 数据库）
2. **消息清晰**：每个消息都应有清晰的描述（HTTP 方法 + URL 或业务动作）
3. **分组使用**：使用 `alt`、`loop`、`opt` 表示条件分支和循环
4. **异步调用**：使用 `-x` 表示异步消息

### 复杂场景示例

```mermaid
sequenceDiagram
    autonumber
    participant User as 用户
    participant UI as 订单页面
    participant OrderService as 订单服务
    participant InventoryService as 库存服务
    participant PaymentService as 支付服务
    participant DB as 数据库

    User->>UI: 点击"下单"按钮
    UI->>OrderService: POST /api/orders/create

    par 创建订单
        OrderService->>DB: INSERT INTO orders
        DB-->>OrderService: 返回订单 ID
    and 扣减库存
        OrderService->>InventoryService: POST /inventory/deduct
        InventoryService->>InventoryService: 校验库存充足性
        alt 库存不足
            InventoryService-->>OrderService: 返回库存不足错误
            OrderService-->>UI: 400 Bad Request
        else 库存充足
            InventoryService->>DB: UPDATE inventory SET qty = qty - 1
            DB-->>InventoryService: 扣减成功
            InventoryService-->>OrderService: 返回成功
        end
    end

    alt 库存扣减成功
        OrderService->>PaymentService: POST /payments/create
        PaymentService->>PaymentService: 调用第三方支付网关
        PaymentService-->>OrderService: 返回支付 URL
        OrderService-->>UI: 201 Created + 支付 URL
        UI->>User: 跳转至支付页面
    else 库存扣减失败
        OrderService->>DB: ROLLBACK 事务
        OrderService-->>UI: 返回错误信息
        UI->>User: 显示错误提示
    end
```

---

## Flowchart 规范（流程图）

### 基本结构

```mermaid
flowchart TD
    Start([开始]) --> Process[处理逻辑]
    Process --> Decision{判断条件}
    Decision -->|是| Action1[执行动作 1]
    Decision -->|否| Action2[执行动作 2]
    Action1 --> End([结束])
    Action2 --> End
```

### 节点形状规范

| 形状 | 语法 | 用途 |
|------|------|------|
| 圆角矩形 | `[节点名]` | 处理步骤、操作 |
| 菱形 | `{条件名}` | 判断、决策 |
| 胶囊形 | `([开始/结束])` | 流程起点、终点 |
| 平行四边形 | `[/输入/输出/])` | 数据输入、输出 |
| 数据库 | `[(数据源)]` | 数据库操作 |

### 复杂业务流程示例

```mermaid
flowchart TD
    Start([用户点击下单]) --> CheckLogin{用户已登录?}
    CheckLogin -->|否| Login[跳转登录页]
    CheckLogin -->|是| ValidateCart{购物车不为空?}

    Login --> Start

    ValidateCart -->|否| ShowEmpty[显示购物车为空提示]
    ValidateCart -->|是| CheckStock{检查库存}

    ShowEmpty --> End([流程结束])

    CheckStock -->|不足| ShowStockError[显示库存不足]
    CheckStock -->|充足| CalculatePrice[计算订单金额]

    ShowStockError --> End

    CalculatePrice --> ApplyDiscount{应用优惠券?}
    ApplyDiscount -->|是| ApplyCoupon[扣减优惠券]
    ApplyDiscount -->|否| CreateOrder[创建订单]

    ApplyCoupon --> CreateOrder

    CreateOrder --> DeductInventory[扣减库存]
    DeductInventory --> CreatePayment[创建支付记录]
    CreatePayment --> RedirectPayment[跳转支付页]
    RedirectPayment --> End

    style Start fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    style End fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    style CheckLogin fill:#fff3e0,stroke:#ef6c00
    style ValidateCart fill:#fff3e0,stroke:#ef6c00
    style CheckStock fill:#fff3e0,stroke:#ef6c00
    style ApplyDiscount fill:#fff3e0,stroke:#ef6c00
```

---

## Class Diagram 规范（类图）

### 基本结构

```mermaid
classDiagram
    class UserController {
        +login(username, password) JWT
        +register(userDTO) User
        +getProfile(userId) UserVO
    }

    class UserService {
        +validateCredentials(username, password) boolean
        +encryptPassword(password) String
    }

    class UserRepository {
        +findByUsername(username) User
        +save(user) User
    }

    UserController --> UserService : 依赖
    UserService --> UserRepository : 调用
```

### 可见性符号

| 符号 | 含义 | 示例 |
|------|------|------|
| `+` | public | `+login()` |
| `-` | private | `-hashPassword()` |
| `#` | protected | `#validateToken()` |
| `~` | package-private | `~logRequest()` |

### 复杂实体关系示例

```mermaid
classDiagram
    class User {
        -Long id
        -String username
        -String password
        -String email
        -LocalDateTime createdAt
        +login() JWT
        +updateProfile() void
    }

    class Order {
        -Long id
        -Long userId
        -BigDecimal totalAmount
        -OrderStatus status
        -LocalDateTime createdAt
        +createOrder() Order
        +cancelOrder() void
        +payOrder() void
    }

    class OrderItem {
        -Long id
        -Long orderId
        -Long productId
        -Integer quantity
        -BigDecimal price
        +calculateSubtotal() BigDecimal
    }

    class Product {
        -Long id
        -String name
        -BigDecimal price
        -Integer stock
        +reduceStock() void
    }

    User "1" --> "*" Order : 下单
    Order "1" --> "*" OrderItem : 包含
    Product "1" --> "*" OrderItem : 关联
```

---

## Entity Relationship Diagram 规范（ER 图）

### 基本结构

```mermaid
erDiagram
    USER {
        bigint id PK
        varchar username UK
        varchar password
        varchar email
        datetime created_at
    }

    ORDER {
        bigint id PK
        bigint user_id FK
        decimal total_amount
        varchar status
        datetime created_at
    }

    USER ||--o{ ORDER : "1:N"
```

### 符号规范

| 符号 | 关系类型 | 说明 |
|------|----------|------|
| `\|\|` | 1 | 唯一或强制 |
| `o{` | 0 或 N | 可选或多个 |
| `\|o{` | 1 或 N | 强制或多个 |
| `o\|` | 0 或 1 | 可选或单个 |

### 复杂数据库关系示例

```mermaid
erDiagram
    USER {
        bigint id PK
        varchar username UK
        varchar password
        varchar email
        tinyint status
        datetime created_at
        datetime updated_at
        tinyint is_deleted
    }

    ORDER {
        bigint id PK
        bigint user_id FK
        varchar order_no UK
        decimal total_amount
        tinyint status
        datetime created_at
        datetime updated_at
        tinyint is_deleted
    }

    ORDER_ITEM {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        decimal price
        datetime created_at
    }

    PRODUCT {
        bigint id PK
        varchar name
        varchar description
        decimal price
        int stock
        datetime created_at
        datetime updated_at
        tinyint is_deleted
    }

    PAYMENT {
        bigint id PK
        bigint order_id FK
        varchar payment_no UK
        decimal amount
        tinyint status
        datetime created_at
        datetime updated_at
    }

    USER ||--o{ ORDER : "用户下单"
    ORDER ||--|{ ORDER_ITEM : "订单包含商品"
    PRODUCT ||--o{ ORDER_ITEM : "商品关联订单项"
    ORDER ||--\| PAYMENT : "订单对应支付"
```

---

## State Diagram 规范（状态机）

### 基本结构

```mermaid
stateDiagram-v2
    [*] --> Pending
    Pending --> Processing : 开始处理
    Processing --> Completed : 处理完成
    Processing --> Failed : 处理失败
    Completed --> [*]
    Failed --> [*]
```

### 复杂状态流转示例

```mermaid
stateDiagram-v2
    [*] --> Unpaid : 创建订单

    Unpaid --> Paid : 支付成功
    Unpaid --> Cancelled : 取消订单
    Unpaid --> Expired : 超时未支付

    Paid --> Shipped : 发货
    Paid --> Refunding : 申请退款

    Shipped --> Delivered : 确认收货
    Shipped --> Refunding : 拒收退货

    Delivered --> Completed : 评价完成
    Delivered --> Refunding : 申请售后

    Refunding --> Refunded : 退款成功
    Refunding --> Paid : 退款失败

    Cancelled --> [*]
    Expired --> [*]
    Refunded --> [*]
    Completed --> [*]

    note right of Unpaid
        未支付状态
        30分钟内可支付
    end note

    note right of Paid
        已支付状态
        可发货或退款
    end note
```

---

## 图表布局优化

### 自动布局方向

```mermaid
flowchart TD  %% TD: Top-Down (从上到下)
%% flowchart LR  %% LR: Left-Right (从左到右)
%% flowchart BT  %% BT: Bottom-Top (从下到上)
```

### 子图（Subgraph）使用

```mermaid
flowchart TD
    subgraph Frontend [前端层]
        A[用户界面]
        B[数据展示]
    end

    subgraph Backend [后端层]
        C[业务逻辑]
        D[数据访问]
    end

    A --> C
    B --> D
    C --> D
```

### 样式自定义

```mermaid
flowchart TD
    Start([开始]) --> Process[处理]
    Process --> End([结束])

    classDef default fill:#f9f9f9,stroke:#333,stroke-width:1px
    classDef startEnd fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    classDef process fill:#fff3e0,stroke:#ef6c00,stroke-width:1px

    class Start,End startEnd
    class Process process
```

---

## 图表维护清单

每次更新图表时，请检查：

- [ ] 图表是否与实际代码/数据库一致
- [ ] 是否包含必要的注释说明
- [ ] 节点命名是否清晰、符合规范
- [ ] 流程逻辑是否完整，无遗漏分支
- [ ] 样式是否统一（颜色、字体、线宽）
- [ ] 复杂图表是否使用了分组（subgraph）
- [ ] 是否标注了关键的业务规则或约束
- [ ] 是否在 Markdown 编辑器中可以正确渲染

---

## 常见问题

### Q1: 图表太大无法查看怎么办？

**A**: 拆分为多个子图表，或使用 `click` 事件实现跳转（部分 Markdown 编辑器支持）。

### Q2: 如何处理循环引用？

**A**: 使用 `loop` 关键字（Sequence Diagram）或明确的箭头指向（Flowchart）。

### Q3: 图表渲染失败怎么办？

**A**: 检查语法是否正确，尤其是特殊字符（如 `|`, `{`, `}`）是否需要转义。
