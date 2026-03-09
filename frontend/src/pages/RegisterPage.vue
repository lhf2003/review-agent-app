<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, CircleCheck } from '@element-plus/icons-vue'

const form = ref({ username: '', password: '', confirm: '' })
const loading = ref(false)
const auth = useAuthStore()

// Canvas 动画相关
const canvasRef = ref(null)
let canvas = null
let ctx = null
let animationId = null
let particles = []
let width, height

let mouse = { x: null, y: null, radius: 150 }

const handleMouseMove = (e) => {
    mouse.x = e.clientX
    mouse.y = e.clientY
}

const handleMouseOut = () => {
    mouse.x = null
    mouse.y = null
}

function resize() {
    if (!canvas) return
    width = canvas.width = window.innerWidth
    height = canvas.height = window.innerHeight
    initParticles()
}

class Particle {
    constructor() {
        this.x = Math.random() * width
        this.y = Math.random() * height
        this.vx = (Math.random() - 0.5) * 0.5
        this.vy = (Math.random() - 0.5) * 0.5
        this.baseRadius = Math.random() * 1.5 + 0.5
        this.radius = this.baseRadius
    }

    update() {
        this.x += this.vx
        this.y += this.vy

        if (this.x < 0 || this.x > width) this.vx = -this.vx
        if (this.y < 0 || this.y > height) this.vy = -this.vy

        if (mouse.x != null) {
            let dx = mouse.x - this.x
            let dy = mouse.y - this.y
            let distance = Math.sqrt(dx * dx + dy * dy)

            if (distance < mouse.radius) {
                const forceDirectionX = dx / distance
                const forceDirectionY = dy / distance
                const force = (mouse.radius - distance) / mouse.radius

                this.x += forceDirectionX * force * 1.5
                this.y += forceDirectionY * force * 1.5

                this.radius = this.baseRadius + (force * 1.5)
            } else {
                this.radius = this.baseRadius
            }
        } else {
            this.radius = this.baseRadius
        }
    }

    draw() {
        ctx.beginPath()
        ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2)
        ctx.fillStyle = `rgba(240, 149, 98, ${this.radius > this.baseRadius ? 0.8 : 0.4})`
        ctx.fill()

        if (this.radius > 1) {
            ctx.shadowBlur = 10
            ctx.shadowColor = '#E07B47'
        } else {
            ctx.shadowBlur = 0
        }
    }
}

function initParticles() {
    particles = []
    const numParticles = Math.floor((width * height) / 12000)
    const limit = Math.min(numParticles, 150)

    for (let i = 0; i < limit; i++) {
        particles.push(new Particle())
    }
}

function animate() {
    if (!ctx) return
    ctx.clearRect(0, 0, width, height)

    const connectionDistance = 120

    for (let i = 0; i < particles.length; i++) {
        particles[i].update()
        particles[i].draw()

        for (let j = i; j < particles.length; j++) {
            const dx = particles[i].x - particles[j].x
            const dy = particles[i].y - particles[j].y
            const distance = Math.sqrt(dx * dx + dy * dy)

            if (distance < connectionDistance) {
                const opacity = 1 - (distance / connectionDistance)
                ctx.beginPath()
                ctx.strokeStyle = `rgba(224, 123, 71, ${opacity * 0.3})`
                ctx.lineWidth = 0.5
                ctx.moveTo(particles[i].x, particles[i].y)
                ctx.lineTo(particles[j].x, particles[j].y)
                ctx.stroke()
            }
        }
    }

    animationId = requestAnimationFrame(animate)
}

onMounted(() => {
    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseout', handleMouseOut)
    window.addEventListener('resize', resize)
    document.body.style.overflow = 'hidden'

    // 初始化 Canvas
    canvas = canvasRef.value
    if (canvas) {
        ctx = canvas.getContext('2d')
        resize()
        animate()
    }
})

onUnmounted(() => {
    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseout', handleMouseOut)
    window.removeEventListener('resize', resize)
    document.body.style.overflow = ''

    if (animationId) {
        cancelAnimationFrame(animationId)
    }
})

async function onSubmit() {
    if (!form.value.username || !form.value.password) {
        ElMessage.warning('请输入用户名和密码')
        return
    }
    if (form.value.password !== form.value.confirm) {
        ElMessage.warning('两次密码不一致')
        return
    }
    try {
        loading.value = true
        await auth.register(form.value.username, form.value.password)
        ElMessage.success('注册成功，请登录')
        auth.routerPushLogin()
    } catch (e) {
        ElMessage.error(`注册失败: ${e.message}`)
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <div class="register-page">
        <!-- Canvas 粒子背景 -->
        <canvas ref="canvasRef" class="neural-canvas"></canvas>

        <!-- 环境光晕 -->
        <div class="ambient-core"></div>

        <!-- 几何装饰层 -->
        <div class="geometry-layer">
            <div class="geo-diamond"></div>
            <div class="geo-circle"></div>
        </div>

        <!-- 注册表单 -->
        <main class="register-wrapper">
            <div class="glass-panel">
                <div class="header">
                    <h1>创建账号</h1>
                    <p>加入 Synapse，开始构建您的知识库</p>
                </div>

                <form @submit.prevent="onSubmit">
                    <div class="input-group">
                        <label>用户名</label>
                        <input
                            v-model="form.username"
                            type="text"
                            class="input-field"
                            placeholder="请输入用户名"
                            required
                        >
                        <svg class="input-icon" viewBox="0 0 24 24">
                            <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                        </svg>
                    </div>

                    <div class="input-group">
                        <label>密码</label>
                        <input
                            v-model="form.password"
                            type="password"
                            class="input-field"
                            placeholder="设置密码"
                            required
                        >
                        <svg class="input-icon" viewBox="0 0 24 24">
                            <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM9 6c0-1.66 1.34-3 3-3s3 1.34 3 3v2H9V6z"/>
                        </svg>
                    </div>

                    <div class="input-group">
                        <label>确认密码</label>
                        <input
                            v-model="form.confirm"
                            type="password"
                            class="input-field"
                            placeholder="再次输入密码"
                            required
                        >
                        <svg class="input-icon" viewBox="0 0 24 24">
                            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                        </svg>
                    </div>

                    <button
                        type="submit"
                        class="submit-btn"
                        :disabled="loading"
                    >
                        {{ loading ? '创建中...' : '立即加入' }}
                    </button>
                </form>

                <div class="footer">
                    已有账号？<a @click="$router.push('/login')">返回登录</a>
                </div>
            </div>
        </main>
    </div>
</template>

<style scoped>
/* ====================================
 * CSS 变量
 * ==================================== */
.register-page {
    --bg-deep: #0a0502;
    --bg-base: #1a0f08;
    --amber-deep: #CC6633;
    --amber-main: #E07B47;
    --amber-light: #F09562;
    --text-primary: #fdf6f2;
    --text-secondary: #b89c8a;
    --glass-bg: rgba(26, 15, 8, 0.45);
    --glass-border: rgba(224, 123, 71, 0.15);
    --glass-highlight: rgba(240, 149, 98, 0.1);
}

/* ====================================
 * 基础布局
 * ==================================== */
.register-page {
    background-color: var(--bg-deep);
    color: var(--text-primary);
    font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    min-height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
    position: relative;
    -webkit-font-smoothing: antialiased;
}

/* ====================================
 * Canvas 背景
 * ==================================== */
.neural-canvas {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
    pointer-events: none;
}

/* ====================================
 * 环境光晕
 * ==================================== */
.ambient-core {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 80vw;
    height: 80vw;
    max-width: 1000px;
    max-height: 1000px;
    background: radial-gradient(circle, rgba(204, 102, 51, 0.15) 0%, rgba(26, 15, 8, 0) 70%);
    z-index: 0;
    pointer-events: none;
}

/* ====================================
 * 几何装饰层
 * ==================================== */
.geometry-layer {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 100%;
    height: 100%;
    z-index: 2;
    pointer-events: none;
    display: flex;
    justify-content: center;
    align-items: center;
    opacity: 0.6;
}

.geo-diamond {
    position: absolute;
    width: 600px;
    height: 600px;
    border: 1px solid rgba(224, 123, 71, 0.08);
    transform: rotate(45deg);
    border-radius: 100px;
    box-shadow: inset 0 0 40px rgba(224, 123, 71, 0.02);
}

.geo-circle {
    position: absolute;
    width: 700px;
    height: 700px;
    border-top: 1px solid rgba(224, 123, 71, 0.15);
    border-bottom: 1px solid rgba(224, 123, 71, 0.05);
    border-radius: 50%;
    mask-image: linear-gradient(to right, transparent, black 20%, black 80%, transparent);
    -webkit-mask-image: linear-gradient(to right, transparent, black 20%, black 80%, transparent);
}

/* ====================================
 * 注册卡片
 * ==================================== */
.register-wrapper {
    position: relative;
    z-index: 10;
    width: 100%;
    max-width: 420px;
    padding: 2rem;
    animation: etherealEntry 1.2s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    opacity: 0;
    transform: translateY(20px) scale(0.98);
}

@keyframes etherealEntry {
    to {
        opacity: 1;
        transform: translateY(0) scale(1);
    }
}

.glass-panel {
    background: linear-gradient(145deg, rgba(30, 18, 10, 0.6) 0%, rgba(15, 8, 4, 0.8) 100%);
    backdrop-filter: blur(24px);
    -webkit-backdrop-filter: blur(24px);
    border: 1px solid var(--glass-border);
    border-top-color: rgba(240, 149, 98, 0.3);
    border-radius: 24px;
    padding: 40px;
    box-shadow:
        0 30px 60px rgba(0, 0, 0, 0.6),
        0 0 40px rgba(204, 102, 51, 0.1),
        inset 0 1px 0 rgba(255, 255, 255, 0.05);
    position: relative;
    overflow: hidden;
}

/* 扫光动画 */
.glass-panel::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 50%;
    height: 100%;
    background: linear-gradient(to right, transparent, rgba(255, 255, 255, 0.03), transparent);
    transform: skewX(-20deg);
    animation: sweep 8s infinite linear;
    pointer-events: none;
}

@keyframes sweep {
    0% { left: -100%; }
    20% { left: 200%; }
    100% { left: 200%; }
}

/* ====================================
 * 头部
 * ==================================== */
.header {
    margin-bottom: 32px;
}

.header h1 {
    font-size: 28px;
    font-weight: 300;
    letter-spacing: -0.5px;
    margin-bottom: 8px;
    background: linear-gradient(180deg, #FFFFFF 0%, #E07B47 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}

.header p {
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.5;
    font-weight: 300;
}

/* ====================================
 * 输入框
 * ==================================== */
.input-group {
    margin-bottom: 20px;
    position: relative;
}

.input-group label {
    display: block;
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 1px;
    color: var(--text-secondary);
    margin-bottom: 8px;
    padding-left: 2px;
}

.input-field {
    width: 100%;
    background: rgba(0, 0, 0, 0.3);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-bottom: 1px solid rgba(224, 123, 71, 0.3);
    border-radius: 12px;
    padding: 14px 16px 14px 44px;
    color: var(--text-primary);
    font-family: inherit;
    font-size: 14px;
    transition: all 0.3s ease;
    outline: none;
}

.input-field::placeholder {
    color: rgba(184, 156, 138, 0.5);
}

.input-field:focus {
    background: rgba(26, 15, 8, 0.6);
    border-bottom-color: var(--amber-light);
    box-shadow: 0 10px 20px -10px rgba(224, 123, 71, 0.2), inset 0 0 10px rgba(224, 123, 71, 0.05);
}

.input-icon {
    position: absolute;
    left: 16px;
    bottom: 14px;
    width: 16px;
    height: 16px;
    fill: var(--text-secondary);
    transition: fill 0.3s ease;
    pointer-events: none;
}

.input-field:focus + .input-icon {
    fill: var(--amber-light);
}

/* ====================================
 * 提交按钮
 * ==================================== */
.submit-btn {
    width: 100%;
    padding: 16px;
    margin-top: 12px;
    background: linear-gradient(90deg, var(--amber-deep), var(--amber-main), var(--amber-light));
    background-size: 200% auto;
    border: none;
    border-radius: 12px;
    color: #0a0502;
    font-family: inherit;
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 0.5px;
    cursor: pointer;
    transition: all 0.4s ease;
    box-shadow: 0 4px 15px rgba(204, 102, 51, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.submit-btn:hover:not(:disabled) {
    background-position: right center;
    box-shadow: 0 8px 25px rgba(204, 102, 51, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.4);
    transform: translateY(-1px);
}

.submit-btn:disabled {
    opacity: 0.7;
    cursor: not-allowed;
}

/* ====================================
 * 底部链接
 * ==================================== */
.footer {
    margin-top: 32px;
    text-align: center;
    font-size: 13px;
    color: var(--text-secondary);
}

.footer a {
    color: var(--amber-main);
    text-decoration: none;
    font-weight: 500;
    transition: color 0.2s;
    cursor: pointer;
}

.footer a:hover {
    color: var(--amber-light);
    text-shadow: 0 0 8px rgba(240, 149, 98, 0.4);
}

/* ====================================
 * 响应式适配
 * ==================================== */
@media (max-width: 480px) {
    .register-wrapper {
        padding: 1rem;
    }

    .glass-panel {
        padding: 30px 24px;
    }

    .header h1 {
        font-size: 24px;
    }

    .geo-diamond {
        width: 400px;
        height: 400px;
    }

    .geo-circle {
        width: 450px;
        height: 450px;
    }
}
</style>
