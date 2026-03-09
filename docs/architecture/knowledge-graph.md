

```
<html lang="en" vid="0"><head vid="1">
    <meta charset="UTF-8" vid="2">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" vid="3">
    <title vid="4">Review Agent | Knowledge Management</title>
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
            
            --chart-line: rgba(255, 255, 255, 0.8);
            --chart-fill: rgba(255, 255, 255, 0.1);
            
            
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
            box-shadow: 0 4px 24px -1px rgba(0, 0, 0, 0.2);
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
            transition: color 0.2s;
        }

        nav a:hover, nav a.active {
            color: var(--text-primary);
        }

        .user-actions {
            display: flex;
            gap: var(--space-s);
            align-items: center;
        }

        .btn-text {
            color: var(--text-primary);
            background: transparent;
            border: 1px solid rgba(255,255,255,0.2);
            padding: 8px 16px;
            border-radius: var(--radius-pill);
            font-size: 13px;
            cursor: pointer;
            transition: all 0.2s;
        }

        .btn-text:hover {
            background: rgba(255,255,255,0.05);
            border-color: rgba(255,255,255,0.4);
        }

        .btn-primary {
            background: var(--accent-primary);
            color: var(--accent-text);
            border: none;
            padding: 8px 20px;
            border-radius: var(--radius-pill);
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            transition: transform 0.2s;
        }

        .btn-primary:hover {
            transform: scale(1.02);
        }

        
        main {
            flex: 1;
            padding: var(--space-s) var(--space-m) var(--space-m);
            display: grid;
            grid-template-columns: 280px 1fr 320px;
            grid-template-rows: 1fr;
            gap: var(--space-m);
            overflow: hidden;
        }

        
        .col-left, .col-right {
            display: flex;
            flex-direction: column;
            gap: var(--space-m);
            overflow-y: auto;
            scrollbar-width: none; 
        }
        
        .col-center {
            display: grid;
            grid-template-rows: auto 1fr auto;
            gap: var(--space-m);
            overflow-y: auto;
        }

        
        .card {
            border-radius: var(--radius-card);
            padding: var(--space-m);
            display: flex;
            flex-direction: column;
            position: relative;
            overflow: hidden;
        }

        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: var(--space-m);
        }

        .card-title {
            font-size: 14px;
            font-weight: 500;
            color: var(--text-secondary);
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        .card-action {
            color: var(--text-tertiary);
            font-size: 12px;
            cursor: pointer;
        }

        
        
        
        .profile-summary {
            display: flex;
            align-items: center;
            gap: var(--space-s);
            margin-bottom: var(--space-s);
        }
        
        .avatar {
            width: 48px;
            height: 48px;
            border-radius: 50%;
            background: linear-gradient(135deg, #e0e7ff 0%, #6366f1 100%);
            border: 2px solid rgba(255,255,255,0.2);
        }

        .profile-name {
            font-size: 16px;
            font-weight: 600;
            display: block;
        }
        
        .profile-level {
            font-size: 12px;
            color: var(--text-secondary);
        }

        .badges-row {
            display: flex;
            gap: 8px;
            margin-top: 12px;
        }
        
        .badge {
            width: 28px;
            height: 28px;
            border-radius: 50%;
            background: rgba(255,255,255,0.05);
            border: 1px solid rgba(255,255,255,0.1);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 10px;
        }

        
        .file-list {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .file-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 12px;
            border-radius: 12px;
            background: rgba(255,255,255,0.02);
            border: 1px solid rgba(255,255,255,0.05);
            transition: background 0.2s;
        }

        .file-item:hover {
            background: rgba(255,255,255,0.05);
        }

        .file-info {
            display: flex;
            flex-direction: column;
        }

        .file-name {
            font-size: 13px;
            color: var(--text-primary);
        }

        .file-date {
            font-size: 11px;
            color: var(--text-tertiary);
            margin-top: 2px;
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
        }
        
        .status-processing { background-color: var(--mastery-med); box-shadow: 0 0 8px rgba(234, 179, 8, 0.4); }
        .status-done { background-color: var(--bg-glow); box-shadow: 0 0 8px rgba(76, 110, 245, 0.4); }

        
        .vis-container {
            flex: 1;
            position: relative;
            min-height: 400px;
            border-radius: var(--radius-card);
            overflow: hidden;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .graph-controls {
            position: absolute;
            bottom: 24px;
            right: 24px;
            display: flex;
            gap: 8px;
        }

        .graph-btn {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: rgba(0,0,0,0.3);
            border: 1px solid rgba(255,255,255,0.1);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
        }
        
        
        .node {
            position: absolute;
            border-radius: 50%;
            border: 1px solid rgba(255,255,255,0.3);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 10px;
            color: rgba(255,255,255,0.8);
            backdrop-filter: blur(4px);
            cursor: pointer;
            transition: transform 0.3s;
        }
        
        .node:hover {
            transform: scale(1.1);
            z-index: 5;
            border-color: white;
        }

        .node-lg { width: 80px; height: 80px; background: radial-gradient(circle, rgba(76,110,245,0.4) 0%, rgba(76,110,245,0.1) 100%); }
        .node-md { width: 50px; height: 50px; background: radial-gradient(circle, rgba(124,58,237,0.4) 0%, rgba(124,58,237,0.1) 100%); }
        .node-sm { width: 30px; height: 30px; background: rgba(255,255,255,0.05); }

        .connection {
            position: absolute;
            height: 1px;
            background: linear-gradient(90deg, rgba(255,255,255,0) 0%, rgba(255,255,255,0.2) 50%, rgba(255,255,255,0) 100%);
            transform-origin: left center;
        }

        
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: var(--space-m);
        }

        .stat-value {
            font-size: 32px;
            font-weight: 600;
            margin-bottom: 4px;
            background: linear-gradient(180deg, #fff 0%, rgba(255,255,255,0.7) 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        
        .stat-label {
            color: var(--text-secondary);
            font-size: 13px;
        }

        
        .tag-cloud {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }

        .tag {
            padding: 6px 12px;
            border-radius: var(--radius-pill);
            background: rgba(255,255,255,0.03);
            border: 1px solid rgba(255,255,255,0.1);
            color: var(--text-secondary);
            font-size: 12px;
            transition: all 0.2s;
            cursor: default;
        }

        .tag:hover {
            background: rgba(255,255,255,0.1);
            color: var(--text-primary);
            border-color: rgba(255,255,255,0.3);
        }

        
        .heatmap-grid {
            display: grid;
            grid-template-columns: repeat(12, 1fr);
            gap: 4px;
        }

        .heat-cell {
            aspect-ratio: 1;
            border-radius: 2px;
            background: rgba(255,255,255,0.03);
        }

        .heat-l1 { background: rgba(76, 110, 245, 0.2); }
        .heat-l2 { background: rgba(76, 110, 245, 0.4); }
        .heat-l3 { background: rgba(76, 110, 245, 0.7); }
        .heat-l4 { background: rgba(76, 110, 245, 1.0); box-shadow: 0 0 8px rgba(76,110,245,0.5); }

        
        .quiz-option {
            padding: 12px 16px;
            border-radius: 12px;
            border: 1px solid rgba(255,255,255,0.1);
            margin-bottom: 8px;
            font-size: 13px;
            cursor: pointer;
            transition: all 0.2s;
            display: flex;
            justify-content: space-between;
        }

        .quiz-option:hover {
            background: rgba(255,255,255,0.05);
            border-color: rgba(255,255,255,0.3);
        }
        
        .quiz-option.selected {
            background: rgba(76, 110, 245, 0.15);
            border-color: rgba(76, 110, 245, 0.5);
            color: white;
        }

        .quiz-progress {
            height: 4px;
            background: rgba(255,255,255,0.1);
            border-radius: 2px;
            margin-top: 16px;
            overflow: hidden;
        }

        .quiz-bar {
            width: 65%;
            height: 100%;
            background: white;
            border-radius: 2px;
        }

        
        .radar-container {
            display: flex;
            justify-content: center;
            align-items: center;
            height: 160px;
            position: relative;
        }
        
        .radar-bg {
            position: absolute;
            width: 120px;
            height: 120px;
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 50%;
        }
        .radar-bg::before {
            content: '';
            position: absolute;
            top: 50%; left: 50%;
            transform: translate(-50%, -50%);
            width: 60px; height: 60px;
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 50%;
        }
        
        .radar-shape {
            width: 80px; height: 80px;
            background: rgba(124, 58, 237, 0.3);
            border: 1px solid rgba(124, 58, 237, 0.8);
            transform: rotate(45deg) skew(10deg);
            box-shadow: 0 0 20px rgba(124, 58, 237, 0.2);
        }

        
        .trend-chart {
            height: 60px;
            display: flex;
            align-items: flex-end;
            gap: 4px;
            padding-top: 10px;
        }
        
        .bar {
            flex: 1;
            background: rgba(255,255,255,0.1);
            border-radius: 2px 2px 0 0;
            transition: height 0.5s;
        }
        
        .bar:hover {
            background: rgba(255,255,255,0.3);
        }

        
        ::-webkit-scrollbar {
            width: 6px;
        }
        ::-webkit-scrollbar-track {
            background: transparent;
        }
        ::-webkit-scrollbar-thumb {
            background: rgba(255,255,255,0.1);
            border-radius: 3px;
        }
        ::-webkit-scrollbar-thumb:hover {
            background: rgba(255,255,255,0.2);
        }

    </style>
</head>
<body vid="6">

    
    <header class="glass" vid="7">
        <div class="brand" vid="8">
            <div class="brand-icon" vid="9"></div>
            <span vid="10">Review Agent</span>
        </div>
        
        <nav vid="11">
            <a href="#" class="active" vid="12">Dashboard</a>
            <a href="#" vid="13">Knowledge Graph</a>
            <a href="#" vid="14">Collections</a>
            <a href="#" vid="15">Settings</a>
        </nav>
        
        <div class="user-actions" vid="16">
            <button class="btn-text" vid="17">Import Log</button>
            <button class="btn-primary" vid="18">Start Quiz</button>
        </div>
    </header>

    <main vid="19">
        
        
        <div class="col-left" vid="20">
            
            <section class="card glass" vid="21">
                <div class="profile-summary" vid="22">
                    <div class="avatar" vid="23"></div>
                    <div vid="24">
                        <span class="profile-name" vid="25">Alex Dev</span>
                        <span class="profile-level" vid="26">Architect Lvl 4</span>
                    </div>
                </div>
                <div class="badges-row" vid="27">
                    <div class="badge" title="Fast Learner" vid="28">⚡</div>
                    <div class="badge" title="Deep Diver" vid="29">🌊</div>
                    <div class="badge" title="Consistent" vid="30">🔥</div>
                    <div class="badge" title="Master" vid="31">🏆</div>
                </div>
            </section>

            
            <section class="card glass" style="flex: 1;" vid="32">
                <div class="card-header" vid="33">
                    <div class="card-title" vid="34">Data Source</div>
                    <div class="card-action" vid="35">+</div>
                </div>
                <div class="file-list" vid="36">
                    <div class="file-item" vid="37">
                        <div class="file-info" vid="38">
                            <span class="file-name" vid="39">chat_log_v2.json</span>
                            <span class="file-date" vid="40">Just now</span>
                        </div>
                        <div class="status-dot status-processing" vid="41"></div>
                    </div>
                    <div class="file-item" vid="42">
                        <div class="file-info" vid="43">
                            <span class="file-name" vid="44">react_hooks.md</span>
                            <span class="file-date" vid="45">2h ago</span>
                        </div>
                        <div class="status-dot status-done" vid="46"></div>
                    </div>
                    <div class="file-item" vid="47">
                        <div class="file-info" vid="48">
                            <span class="file-name" vid="49">system_design.pdf</span>
                            <span class="file-date" vid="50">Yesterday</span>
                        </div>
                        <div class="status-dot status-done" vid="51"></div>
                    </div>
                    <div class="file-item" vid="52">
                        <div class="file-info" vid="53">
                            <span class="file-name" vid="54">docker_compose.yml</span>
                            <span class="file-date" vid="55">Yesterday</span>
                        </div>
                        <div class="status-dot status-done" vid="56"></div>
                    </div>
                </div>
            </section>
        </div>

        
        <div class="col-center" vid="57">
            
            
            <div class="stats-grid" vid="58">
                <section class="card glass" vid="59">
                    <div class="stat-value" vid="60">89%</div>
                    <div class="stat-label" vid="61">Mastery Score</div>
                </section>
                <section class="card glass" vid="62">
                    <div class="stat-value" vid="63">124</div>
                    <div class="stat-label" vid="64">Knowledge Points</div>
                </section>
                <section class="card glass" vid="65">
                    <div class="stat-value" vid="66">12h</div>
                    <div class="stat-label" vid="67">Learning Time</div>
                </section>
            </div>

            
            <section class="card glass vis-container" vid="68">
                <div style="position:absolute; top: 20px; left: 24px; z-index: 2;" vid="69">
                    <div class="card-title" vid="70">Knowledge Graph</div>
                    <div class="card-action" style="margin-top:4px; opacity: 0.6;" vid="71">Force Directed Layout</div>
                </div>
                
                
                <div class="connection" style="width: 120px; top: 45%; left: 35%; transform: rotate(15deg);" vid="72"></div>
                <div class="connection" style="width: 150px; top: 55%; left: 45%; transform: rotate(-25deg);" vid="73"></div>
                <div class="connection" style="width: 80px; top: 35%; left: 55%; transform: rotate(45deg);" vid="74"></div>

                
                <div class="node node-lg" style="top: 40%; left: 45%;" vid="75">React</div>
                <div class="node node-md" style="top: 25%; left: 60%;" vid="76">Hooks</div>
                <div class="node node-md" style="top: 60%; left: 30%;" vid="77">State</div>
                <div class="node node-sm" style="top: 20%; left: 30%;" vid="78">Redux</div>
                <div class="node node-sm" style="top: 70%; left: 60%;" vid="79">Context</div>
                <div class="node node-sm" style="top: 50%; left: 70%;" vid="80">Ref</div>

                <div class="graph-controls" vid="81">
                    <div class="graph-btn" vid="82">+</div>
                    <div class="graph-btn" vid="83">-</div>
                </div>
            </section>

            
            <section class="card glass" vid="84">
                <div class="card-header" vid="85">
                    <div class="card-title" vid="86">Learning Activity</div>
                    <div class="card-action" vid="87">Year View</div>
                </div>
                <div class="heatmap-grid" vid="88">
                    
                    <div class="heat-cell heat-l1" vid="89"></div> <div class="heat-cell heat-l2" vid="90"></div> <div class="heat-cell" vid="91"></div> <div class="heat-cell heat-l1" vid="92"></div>
                    <div class="heat-cell heat-l3" vid="93"></div> <div class="heat-cell heat-l4" vid="94"></div> <div class="heat-cell heat-l2" vid="95"></div> <div class="heat-cell heat-l1" vid="96"></div>
                    <div class="heat-cell" vid="97"></div> <div class="heat-cell heat-l1" vid="98"></div> <div class="heat-cell heat-l3" vid="99"></div> <div class="heat-cell heat-l2" vid="100"></div>
                    <div class="heat-cell heat-l1" vid="101"></div> <div class="heat-cell heat-l2" vid="102"></div> <div class="heat-cell" vid="103"></div> <div class="heat-cell heat-l1" vid="104"></div>
                    <div class="heat-cell heat-l2" vid="105"></div> <div class="heat-cell heat-l4" vid="106"></div> <div class="heat-cell heat-l3" vid="107"></div> <div class="heat-cell heat-l2" vid="108"></div>
                    <div class="heat-cell heat-l1" vid="109"></div> <div class="heat-cell" vid="110"></div> <div class="heat-cell heat-l1" vid="111"></div> <div class="heat-cell heat-l2" vid="112"></div>
                    <div class="heat-cell heat-l3" vid="113"></div> <div class="heat-cell heat-l2" vid="114"></div> <div class="heat-cell heat-l4" vid="115"></div> <div class="heat-cell heat-l3" vid="116"></div>
                    <div class="heat-cell heat-l1" vid="117"></div> <div class="heat-cell heat-l2" vid="118"></div> <div class="heat-cell" vid="119"></div> <div class="heat-cell heat-l1" vid="120"></div>
                    <div class="heat-cell heat-l2" vid="121"></div> <div class="heat-cell heat-l3" vid="122"></div> <div class="heat-cell heat-l1" vid="123"></div> <div class="heat-cell" vid="124"></div>
                    <div class="heat-cell heat-l1" vid="125"></div> <div class="heat-cell heat-l2" vid="126"></div> <div class="heat-cell heat-l4" vid="127"></div> <div class="heat-cell heat-l3" vid="128"></div>
                    <div class="heat-cell heat-l2" vid="129"></div> <div class="heat-cell heat-l1" vid="130"></div> <div class="heat-cell" vid="131"></div> <div class="heat-cell heat-l1" vid="132"></div>
                    <div class="heat-cell heat-l3" vid="133"></div> <div class="heat-cell heat-l4" vid="134"></div> <div class="heat-cell heat-l2" vid="135"></div> <div class="heat-cell heat-l1" vid="136"></div>
                </div>
            </section>
        </div>

        
        <div class="col-right" vid="137">
            
            
            <section class="card glass" vid="138">
                <div class="card-header" vid="139">
                    <div class="card-title" vid="140">Daily Quiz</div>
                    <div class="card-action" vid="141">Skip</div>
                </div>
                <div style="font-size: 14px; margin-bottom: 16px; line-height: 1.4;" vid="142">
                    What is the primary purpose of the `useEffect` hook dependency array?
                </div>
                <div class="quiz-option" vid="143">Control execution timing</div>
                <div class="quiz-option selected" vid="144">Optimize re-renders <span vid="145">✓</span></div>
                <div class="quiz-option" vid="146">Define state types</div>
                
                <div class="quiz-progress" vid="147">
                    <div class="quiz-bar" vid="148"></div>
                </div>
                <div style="margin-top: 8px; font-size: 11px; color: var(--text-tertiary); text-align: right;" vid="149">Question 3/5</div>
            </section>

            
            <section class="card glass" vid="150">
                <div class="card-header" vid="151">
                    <div class="card-title" vid="152">Skill Radar</div>
                </div>
                <div class="radar-container" vid="153">
                    <div class="radar-bg" vid="154"></div>
                    <div class="radar-shape" vid="155"></div>
                </div>
                <div style="display:flex; justify-content:space-between; margin-top:8px;" vid="156">
                    <span class="file-date" vid="157">Frontend</span>
                    <span class="file-date" vid="158">Backend</span>
                    <span class="file-date" vid="159">DevOps</span>
                </div>
            </section>

            
            <section class="card glass" style="flex:1" vid="160">
                <div class="card-header" vid="161">
                    <div class="card-title" vid="162">Top Concepts</div>
                </div>
                <div class="tag-cloud" vid="163">
                    <div class="tag" vid="164">System Design</div>
                    <div class="tag" vid="165">Microservices</div>
                    <div class="tag" vid="166">Kubernetes</div>
                    <div class="tag" vid="167">LLM</div>
                    <div class="tag" vid="168">Transformers</div>
                    <div class="tag" vid="169">Graph RAG</div>
                    <div class="tag" vid="170">Vector DB</div>
                    <div class="tag" vid="171">Prompt Eng</div>
                    <div class="tag" vid="172">CI/CD</div>
                </div>
                
                <div class="card-header" style="margin-top: 24px; margin-bottom: 12px;" vid="173">
                    <div class="card-title" vid="174">Trend</div>
                </div>
                <div class="trend-chart" vid="175">
                    <div class="bar" style="height: 30%" vid="176"></div>
                    <div class="bar" style="height: 50%" vid="177"></div>
                    <div class="bar" style="height: 40%" vid="178"></div>
                    <div class="bar" style="height: 80%" vid="179"></div>
                    <div class="bar" style="height: 60%" vid="180"></div>
                    <div class="bar" style="height: 90%" vid="181"></div>
                    <div class="bar" style="height: 70%" vid="182"></div>
                </div>
            </section>

        </div>
    </main>

</body></html>
```
