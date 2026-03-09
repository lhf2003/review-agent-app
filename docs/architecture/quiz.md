```
<html lang="en" vid="0"><head vid="1">
    <meta charset="UTF-8" vid="2">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" vid="3">
    <title vid="4">Active Quiz | Review Agent</title>
    <style vid="5">
        :root {
            --bg-deep: #0a0a16;
            --bg-nebula-1: #1a1b4b; 
            --bg-nebula-2: #2e1065; 
            --bg-glow: #4c6ef5;     
            
            --glass-surface: rgba(255, 255, 255, 0.03);
            --glass-border: rgba(255, 255, 255, 0.08);
            --glass-highlight: rgba(255, 255, 255, 0.15);
            --glass-blur: 24px;
            
            --text-primary: #ffffff;
            --text-secondary: rgba(255, 255, 255, 0.65);
            --text-tertiary: rgba(255, 255, 255, 0.4);
            
            --accent-primary: #ffffff;
            --accent-text: #0a0a16;
            
            --correct: #22c55e;
            --incorrect: #ef4444;
            --streak: #f59e0b;
            
            --radius-card: 24px;
            --radius-pill: 999px;
            --font-main: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: var(--font-main);
            background-color: var(--bg-deep);
            color: var(--text-primary);
            height: 100vh;
            width: 1440px;
            height: 960px;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            background-image: 
                radial-gradient(circle at 10% 20%, rgba(76, 110, 245, 0.12) 0%, transparent 40%),
                radial-gradient(circle at 90% 10%, rgba(124, 58, 237, 0.12) 0%, transparent 40%),
                radial-gradient(circle at 50% 50%, rgba(26, 27, 75, 1) 0%, var(--bg-deep) 100%);
        }

        .glass {
            background: var(--glass-surface);
            backdrop-filter: blur(var(--glass-blur));
            -webkit-backdrop-filter: blur(var(--glass-blur));
            border: 1px solid var(--glass-border);
            border-top: 1px solid var(--glass-highlight);
        }

        header {
            height: 80px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 60px;
            flex-shrink: 0;
        }

        .brand {
            font-weight: 500;
            font-size: 18px;
            display: flex;
            align-items: center;
            gap: 12px;
            opacity: 0.9;
        }

        .brand-icon {
            width: 28px;
            height: 28px;
            background: rgba(255,255,255,0.1);
            border-radius: 8px;
            border: 1px solid rgba(255,255,255,0.2);
        }

        .quiz-meta {
            display: flex;
            align-items: center;
            gap: 40px;
        }

        .meta-item {
            display: flex;
            flex-direction: column;
            align-items: center;
        }

        .meta-label {
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.1em;
            color: var(--text-tertiary);
            margin-bottom: 4px;
        }

        .meta-value {
            font-size: 18px;
            font-weight: 600;
        }

        .streak-counter {
            color: var(--streak);
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .btn-close {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            border: 1px solid rgba(255,255,255,0.1);
            background: rgba(255,255,255,0.05);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            font-size: 20px;
        }

        main {
            flex: 1;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 40px;
            position: relative;
        }

        .question-container {
            width: 840px;
            padding: 60px;
            border-radius: 40px;
            position: relative;
            z-index: 2;
        }

        .timer-ring {
            position: absolute;
            top: -40px;
            left: 50%;
            transform: translateX(-50%);
            width: 80px;
            height: 80px;
            background: var(--bg-deep);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            border: 4px solid rgba(255,255,255,0.05);
        }

        .timer-progress {
            position: absolute;
            width: 100%;
            height: 100%;
            border-radius: 50%;
            border: 4px solid var(--bg-glow);
            border-top-color: transparent;
            transform: rotate(-45deg);
        }

        .timer-text {
            font-size: 20px;
            font-weight: 700;
            z-index: 2;
        }

        .question-category {
            font-size: 14px;
            color: var(--bg-glow);
            text-transform: uppercase;
            letter-spacing: 0.2em;
            margin-bottom: 24px;
            text-align: center;
        }

        .question-text {
            font-size: 32px;
            font-weight: 500;
            line-height: 1.4;
            text-align: center;
            margin-bottom: 60px;
        }

        .options-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
        }

        .option-card {
            padding: 24px 32px;
            border-radius: 20px;
            background: rgba(255,255,255,0.02);
            border: 1px solid rgba(255,255,255,0.08);
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 18px;
            position: relative;
            overflow: hidden;
        }

        .option-card:hover {
            background: rgba(255,255,255,0.06);
            border-color: rgba(255,255,255,0.2);
            transform: translateY(-2px);
        }

        .option-card.correct {
            background: rgba(34, 197, 94, 0.1);
            border-color: var(--correct);
            box-shadow: 0 0 30px rgba(34, 197, 94, 0.15);
        }

        .option-card.incorrect {
            background: rgba(239, 68, 68, 0.1);
            border-color: var(--incorrect);
            opacity: 0.8;
        }

        .option-indicator {
            width: 24px;
            height: 24px;
            border-radius: 50%;
            border: 2px solid rgba(255,255,255,0.2);
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .correct .option-indicator {
            background: var(--correct);
            border-color: var(--correct);
        }

        .incorrect .option-indicator {
            background: var(--incorrect);
            border-color: var(--incorrect);
        }

        .progress-stepper {
            position: absolute;
            bottom: 60px;
            display: flex;
            gap: 12px;
            align-items: center;
        }

        .step-dot {
            width: 12px;
            height: 4px;
            border-radius: 2px;
            background: rgba(255,255,255,0.1);
            transition: all 0.3s;
        }

        .step-dot.completed {
            background: var(--bg-glow);
            width: 24px;
        }

        .step-dot.active {
            background: white;
            width: 40px;
            box-shadow: 0 0 10px rgba(255,255,255,0.3);
        }

        .feedback-overlay {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            font-size: 120px;
            font-weight: 800;
            opacity: 0;
            pointer-events: none;
            color: var(--correct);
            text-transform: uppercase;
            letter-spacing: -0.05em;
            z-index: 10;
            animation: feedbackPop 0.8s ease-out forwards;
        }

        @keyframes feedbackPop {
            0% { transform: translate(-50%, -40%) scale(0.8); opacity: 0; filter: blur(10px); }
            30% { transform: translate(-50%, -50%) scale(1.1); opacity: 0.2; filter: blur(0); }
            100% { transform: translate(-50%, -60%) scale(1); opacity: 0; }
        }

        .bottom-actions {
            position: absolute;
            bottom: 120px;
            display: flex;
            gap: 20px;
        }

        .btn-secondary {
            background: rgba(255,255,255,0.05);
            border: 1px solid rgba(255,255,255,0.1);
            padding: 12px 32px;
            border-radius: var(--radius-pill);
            color: var(--text-secondary);
            font-size: 15px;
            cursor: pointer;
        }

        .btn-primary-lg {
            background: white;
            color: var(--bg-deep);
            padding: 12px 48px;
            border-radius: var(--radius-pill);
            font-weight: 600;
            font-size: 15px;
            border: none;
            cursor: pointer;
            box-shadow: 0 10px 20px rgba(0,0,0,0.2);
        }

    </style>
</head>
<body vid="6">

    <header vid="7">
        <div class="brand" vid="8">
            <div class="brand-icon" vid="9"></div>
            <span vid="10">Review Agent</span>
        </div>

        <div class="quiz-meta" vid="11">
            <div class="meta-item" vid="12">
                <span class="meta-label" vid="13">Accuracy</span>
                <span class="meta-value" vid="14">100%</span>
            </div>
            <div class="meta-item" vid="15">
                <span class="meta-label" vid="16">Points</span>
                <span class="meta-value" vid="17">1,240</span>
            </div>
            <div class="meta-item" vid="18">
                <span class="meta-label" vid="19">Streak</span>
                <span class="meta-value streak-counter" vid="20">🔥 7</span>
            </div>
        </div>

        <button class="btn-close" vid="21">×</button>
    </header>

    <main vid="22">
        <div class="feedback-overlay" vid="23">Perfect!</div>

        <section class="card glass question-container" vid="24">
            <div class="timer-ring" vid="25">
                <div class="timer-progress" vid="26"></div>
                <span class="timer-text" vid="27">14</span>
            </div>

            <div class="question-category" vid="28">React Architecture • Advanced</div>
            <h1 class="question-text" vid="29">Which mechanism does React use to reconcile the virtual DOM with the real DOM effectively?</h1>

            <div class="options-grid" vid="30">
                <div class="option-card" vid="31">
                    <span vid="32">Shadow DOM Diffing</span>
                    <div class="option-indicator" vid="33"></div>
                </div>
                <div class="option-card correct" vid="34">
                    <span vid="35">Fiber Reconciler</span>
                    <div class="option-indicator" vid="36">
                        <svg width="12" height="10" viewBox="0 0 12 10" fill="none" vid="37">
                            <path d="M1 5L4.5 8.5L11 1.5" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" vid="38"></path>
                        </svg>
                    </div>
                </div>
                <div class="option-card incorrect" vid="39">
                    <span vid="40">Immediate Mutation</span>
                    <div class="option-indicator" vid="41">
                        <svg width="10" height="10" viewBox="0 0 10 10" fill="none" vid="42">
                            <path d="M1 1L9 9M9 1L1 9" stroke="white" stroke-width="2" stroke-linecap="round" vid="43"></path>
                        </svg>
                    </div>
                </div>
                <div class="option-card" vid="44">
                    <span vid="45">Global State Injection</span>
                    <div class="option-indicator" vid="46"></div>
                </div>
            </div>
        </section>

        <div class="bottom-actions" vid="47">
            <button class="btn-secondary" vid="48">Explain Concept</button>
            <button class="btn-primary-lg" vid="49">Next Question</button>
        </div>

        <div class="progress-stepper" vid="50">
            <div class="step-dot completed" vid="51"></div>
            <div class="step-dot completed" vid="52"></div>
            <div class="step-dot completed" vid="53"></div>
            <div class="step-dot completed" vid="54"></div>
            <div class="step-dot completed" vid="55"></div>
            <div class="step-dot completed" vid="56"></div>
            <div class="step-dot completed" vid="57"></div>
            <div class="step-dot active" vid="58"></div>
            <div class="step-dot" vid="59"></div>
            <div class="step-dot" vid="60"></div>
        </div>
    </main>

</body></html>
```
