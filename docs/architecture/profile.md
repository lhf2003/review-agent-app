个人信息页面，提供返回到dashboard的按钮
```
<html lang="en" vid="0"><head vid="1">
    <meta charset="UTF-8" vid="2">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" vid="3">
    <title vid="4">Profile &amp; Achievements | Review Agent</title>
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
            
            --mastery-low: #ef4444;    
            --mastery-med: #eab308;    
            --mastery-high: #22c55e;   
            
            --space-xs: 8px;
            --space-s: 16px;
            --space-m: 24px;
            --space-l: 32px;
            
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
            width: 100vw;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            background-image: 
                radial-gradient(circle at 10% 20%, rgba(76, 110, 245, 0.15) 0%, transparent 40%),
                radial-gradient(circle at 90% 60%, rgba(124, 58, 237, 0.15) 0%, transparent 40%),
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
            height: 64px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 var(--space-m);
            flex-shrink: 0;
            z-index: 10;
        }

        .brand {
            font-weight: 500;
            font-size: 16px;
            display: flex;
            align-items: center;
            gap: 12px;
            opacity: 0.9;
        }

        .brand-icon {
            width: 24px;
            height: 24px;
            background: rgba(255,255,255,0.1);
            border-radius: 6px;
            border: 1px solid rgba(255,255,255,0.2);
        }

        nav {
            display: flex;
            gap: 32px;
        }

        nav a {
            color: var(--text-secondary);
            text-decoration: none;
            font-size: 14px;
        }

        nav a.active {
            color: var(--text-primary);
            font-weight: 500;
        }

        main {
            flex: 1;
            padding: var(--space-m);
            display: grid;
            grid-template-columns: 360px 1fr;
            grid-template-rows: 1fr;
            gap: var(--space-m);
            overflow: hidden;
        }

        .card {
            border-radius: var(--radius-card);
            padding: var(--space-m);
            display: flex;
            flex-direction: column;
        }

        .card-title {
            font-size: 14px;
            font-weight: 500;
            color: var(--text-secondary);
            text-transform: uppercase;
            letter-spacing: 0.05em;
            margin-bottom: var(--space-m);
        }

        
        .profile-hero {
            text-align: center;
            padding: var(--space-m) 0;
        }

        .avatar-lg {
            width: 120px;
            height: 120px;
            border-radius: 50%;
            background: linear-gradient(135deg, #e0e7ff 0%, #6366f1 100%);
            border: 4px solid rgba(255,255,255,0.1);
            margin: 0 auto 16px;
            box-shadow: 0 0 40px rgba(99, 102, 241, 0.3);
        }

        .user-name {
            font-size: 24px;
            font-weight: 700;
            margin-bottom: 4px;
        }

        .user-title {
            color: var(--text-secondary);
            font-size: 14px;
            margin-bottom: 24px;
        }

        .level-container {
            margin-top: 8px;
        }

        .level-labels {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: var(--text-secondary);
            margin-bottom: 8px;
        }

        .level-bar-bg {
            height: 8px;
            background: rgba(255,255,255,0.05);
            border-radius: 4px;
            overflow: hidden;
        }

        .level-bar-fill {
            height: 100%;
            width: 72%;
            background: linear-gradient(90deg, #6366f1, #a855f7);
            box-shadow: 0 0 12px rgba(168, 85, 247, 0.5);
        }

        
        .badge-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 16px;
        }

        .badge-item {
            aspect-ratio: 1;
            background: rgba(255,255,255,0.02);
            border: 1px solid var(--glass-border);
            border-radius: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            transition: all 0.3s ease;
            position: relative;
        }

        .badge-item:hover {
            background: rgba(255,255,255,0.08);
            transform: translateY(-4px);
        }

        .badge-locked {
            filter: grayscale(1) opacity(0.3);
        }

        
        .streak-container {
            display: grid;
            grid-template-columns: repeat(7, 1fr);
            gap: 8px;
        }

        .day-label {
            font-size: 10px;
            color: var(--text-tertiary);
            text-align: center;
            margin-bottom: 4px;
        }

        .day-cell {
            aspect-ratio: 1;
            border-radius: 6px;
            background: rgba(255,255,255,0.03);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 11px;
            color: var(--text-tertiary);
        }

        .day-active {
            background: rgba(76, 110, 245, 0.3);
            border: 1px solid rgba(76, 110, 245, 0.5);
            color: white;
            box-shadow: 0 0 10px rgba(76, 110, 245, 0.2);
        }

        .day-streak {
            background: var(--bg-glow);
            color: white;
            box-shadow: 0 0 15px rgba(76, 110, 245, 0.6);
        }

        
        .skill-list {
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        .skill-group-title {
            font-size: 12px;
            color: var(--text-tertiary);
            margin-bottom: 12px;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .skill-group-title::after {
            content: '';
            flex: 1;
            height: 1px;
            background: rgba(255,255,255,0.05);
        }

        .skill-row {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .skill-info {
            width: 140px;
            flex-shrink: 0;
        }

        .skill-name {
            font-size: 14px;
            font-weight: 500;
            display: block;
        }

        .skill-meta {
            font-size: 11px;
            color: var(--text-tertiary);
        }

        .skill-track {
            flex: 1;
            height: 6px;
            background: rgba(255,255,255,0.05);
            border-radius: 3px;
            position: relative;
        }

        .skill-progress {
            position: absolute;
            left: 0; top: 0; height: 100%;
            border-radius: 3px;
        }

        .skill-val {
            width: 40px;
            text-align: right;
            font-size: 13px;
            font-variant-numeric: tabular-nums;
            color: var(--text-secondary);
        }

        .col-scroll {
            display: flex;
            flex-direction: column;
            gap: var(--space-m);
            overflow-y: auto;
            padding-right: 4px;
        }

        ::-webkit-scrollbar { width: 4px; }
        ::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 2px; }

    </style>
</head>
<body vid="6">

    <header class="glass" vid="7">
        <div class="brand" vid="8">
            <div class="brand-icon" vid="9"></div>
            <span vid="10">Review Agent</span>
        </div>
        <nav vid="11">
            <a href="#" vid="12">Dashboard</a>
            <a href="#" vid="13">Knowledge Graph</a>
            <a href="#" vid="14">Collections</a>
            <a href="#" class="active" vid="15">Profile</a>
        </nav>
        <div class="user-actions" vid="16">
            <button style="background: transparent; border: 1px solid rgba(255,255,255,0.2); color: white; padding: 8px 16px; border-radius: 20px; font-size: 13px; cursor: pointer;" vid="17">Edit Profile</button>
        </div>
    </header>

    <main vid="18">
        
        <div class="col-left" vid="19">
            <section class="card glass" style="margin-bottom: 24px;" vid="20">
                <div class="profile-hero" vid="21">
                    <div class="avatar-lg" vid="22"></div>
                    <h1 class="user-name" vid="23">Alex Dev</h1>
                    <p class="user-title" vid="24">Senior Software Architect</p>
                    
                    <div class="level-container" vid="25">
                        <div class="level-labels" vid="26">
                            <span vid="27">Level 4</span>
                            <span vid="28">2,450 / 3,000 XP</span>
                        </div>
                        <div class="level-bar-bg" vid="29">
                            <div class="level-bar-fill" vid="30"></div>
                        </div>
                    </div>
                </div>
            </section>

            <section class="card glass" style="flex: 1;" vid="31">
                <div class="card-title" vid="32">Learning Streak</div>
                <div class="streak-container" vid="33">
                    
                    <div class="day-label" vid="34">M</div><div class="day-label" vid="35">T</div><div class="day-label" vid="36">W</div><div class="day-label" vid="37">T</div><div class="day-label" vid="38">F</div><div class="day-label" vid="39">S</div><div class="day-label" vid="40">S</div>
                    
                    <div class="day-cell day-active" vid="41">1</div><div class="day-cell day-active" vid="42">2</div><div class="day-cell day-active" vid="43">3</div><div class="day-cell" vid="44">4</div><div class="day-cell day-active" vid="45">5</div><div class="day-cell day-active" vid="46">6</div><div class="day-cell day-active" vid="47">7</div>
                    <div class="day-cell day-active" vid="48">8</div><div class="day-cell day-active" vid="49">9</div><div class="day-cell day-active" vid="50">10</div><div class="day-cell day-active" vid="51">11</div><div class="day-cell day-active" vid="52">12</div><div class="day-cell day-active" vid="53">13</div><div class="day-cell day-active" vid="54">14</div>
                    <div class="day-cell day-streak" vid="55">15</div><div class="day-cell day-streak" vid="56">16</div><div class="day-cell day-streak" vid="57">17</div><div class="day-cell day-streak" vid="58">18</div><div class="day-cell day-streak" vid="59">19</div><div class="day-cell day-streak" vid="60">20</div><div class="day-cell day-streak" vid="61">21</div>
                    <div class="day-cell day-streak" vid="62">22</div><div class="day-cell day-streak" vid="63">23</div><div class="day-cell day-streak" vid="64">24</div><div class="day-cell" style="border: 1px dashed rgba(255,255,255,0.2)" vid="65">25</div><div class="day-cell" vid="66"></div><div class="day-cell" vid="67"></div><div class="day-cell" vid="68"></div>
                </div>
                <div style="margin-top: 24px; text-align: center;" vid="69">
                    <span style="font-size: 32px; font-weight: 700; display: block;" vid="70">12</span>
                    <span style="font-size: 12px; color: var(--text-secondary); text-transform: uppercase;" vid="71">Day Current Streak</span>
                </div>
            </section>
        </div>

        
        <div class="col-scroll" vid="72">
            
            <section class="card glass" vid="73">
                <div class="card-title" vid="74">Achievement Gallery</div>
                <div class="badge-grid" vid="75">
                    <div class="badge-item" title="Fast Learner" vid="76">⚡</div>
                    <div class="badge-item" title="Deep Diver" vid="77">🌊</div>
                    <div class="badge-item" title="Night Owl" vid="78">🦉</div>
                    <div class="badge-item" title="Problem Solver" vid="79">🧩</div>
                    <div class="badge-item" title="Architect" vid="80">🏗️</div>
                    <div class="badge-item" title="consistent" vid="81">🔥</div>
                    <div class="badge-item" title="Scholar" vid="82">📜</div>
                    <div class="badge-item" title="Master" vid="83">🏆</div>
                    <div class="badge-item badge-locked" vid="84">🔒</div>
                    <div class="badge-item badge-locked" vid="85">🔒</div>
                    <div class="badge-item badge-locked" vid="86">🔒</div>
                    <div class="badge-item badge-locked" vid="87">🔒</div>
                </div>
            </section>

            
            <section class="card glass" vid="88">
                <div class="card-title" vid="89">Skill Proficiency</div>
                <div class="skill-list" vid="90">
                    
                    <div vid="91">
                        <div class="skill-group-title" vid="92">Frontend Engineering</div>
                        <div class="skill-row" vid="93">
                            <div class="skill-info" vid="94">
                                <span class="skill-name" vid="95">React &amp; Ecosystem</span>
                                <span class="skill-meta" vid="96">Expert • 24 Concepts</span>
                            </div>
                            <div class="skill-track" vid="97">
                                <div class="skill-progress" style="width: 92%; background: #6366f1;" vid="98"></div>
                            </div>
                            <div class="skill-val" vid="99">92%</div>
                        </div>
                        <div class="skill-row" style="margin-top: 12px;" vid="100">
                            <div class="skill-info" vid="101">
                                <span class="skill-name" vid="102">TypeScript</span>
                                <span class="skill-meta" vid="103">Advanced • 18 Concepts</span>
                            </div>
                            <div class="skill-track" vid="104">
                                <div class="skill-progress" style="width: 78%; background: #6366f1;" vid="105"></div>
                            </div>
                            <div class="skill-val" vid="106">78%</div>
                        </div>
                    </div>

                    <div vid="107">
                        <div class="skill-group-title" vid="108">System Architecture</div>
                        <div class="skill-row" vid="109">
                            <div class="skill-info" vid="110">
                                <span class="skill-name" vid="111">Microservices</span>
                                <span class="skill-meta" vid="112">Advanced • 12 Concepts</span>
                            </div>
                            <div class="skill-track" vid="113">
                                <div class="skill-progress" style="width: 85%; background: #a855f7;" vid="114"></div>
                            </div>
                            <div class="skill-val" vid="115">85%</div>
                        </div>
                        <div class="skill-row" style="margin-top: 12px;" vid="116">
                            <div class="skill-info" vid="117">
                                <span class="skill-name" vid="118">Distributed Systems</span>
                                <span class="skill-meta" vid="119">Intermediate • 9 Concepts</span>
                            </div>
                            <div class="skill-track" vid="120">
                                <div class="skill-progress" style="width: 64%; background: #a855f7;" vid="121"></div>
                            </div>
                            <div class="skill-val" vid="122">64%</div>
                        </div>
                    </div>

                    <div vid="123">
                        <div class="skill-group-title" vid="124">Intelligence &amp; Data</div>
                        <div class="skill-row" vid="125">
                            <div class="skill-info" vid="126">
                                <span class="skill-name" vid="127">LLM Orchestration</span>
                                <span class="skill-meta" vid="128">Learning • 15 Concepts</span>
                            </div>
                            <div class="skill-track" vid="129">
                                <div class="skill-progress" style="width: 45%; background: #ec4899;" vid="130"></div>
                            </div>
                            <div class="skill-val" vid="131">45%</div>
                        </div>
                    </div>

                </div>
            </section>
        </div>
    </main>

</body></html>
```
