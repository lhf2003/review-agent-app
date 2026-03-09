The code below contains a design. This design should be used to create a new app or be added to an existing one.

Look at the current open project to determine if a project exists. If no project is open, create a new Vite project then create this view in React after componentizing it.

If a project does exist, determine the framework being used and implement the design within that framework. Identify whether reusable components already exist that can be used to implement the design faithfully and if so use them, otherwise create new components. If other views already exist in the project, make sure to place the view in a sensible route and connect it to the other views.

Ensure the visual characteristics, layout, and interactions in the design are preserved with perfect fidelity.

Run the dev command so the user can see the app once finished.

```
<html lang="en" vid="0"><head vid="1">
    <meta charset="UTF-8" vid="2">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" vid="3">
    <title vid="4">Review Agent | Collections</title>
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
            overflow: hidden;
            display: flex;
            flex-direction: column;
            
            background-image: 
                radial-gradient(circle at 10% 20%, rgba(76, 110, 245, 0.1) 0%, transparent 40%),
                radial-gradient(circle at 90% 60%, rgba(124, 58, 237, 0.1) 0%, transparent 40%),
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

        main {
            flex: 1;
            padding: var(--space-l);
            overflow-y: auto;
            max-width: 1440px;
            margin: 0 auto;
            width: 100%;
        }

        .page-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-end;
            margin-bottom: var(--space-l);
        }

        .title-group h1 {
            font-size: 28px;
            font-weight: 600;
            margin-bottom: 8px;
        }

        .title-group p {
            color: var(--text-secondary);
            font-size: 14px;
        }

        .actions-group {
            display: flex;
            gap: 12px;
        }

        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
            gap: var(--space-m);
        }

        .collection-card {
            border-radius: var(--radius-card);
            padding: var(--space-m);
            transition: transform 0.2s, background 0.2s;
            cursor: pointer;
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        .collection-card:hover {
            transform: translateY(-4px);
            background: rgba(255, 255, 255, 0.05);
        }

        .card-top {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
        }

        .folder-icon {
            width: 44px;
            height: 44px;
            background: rgba(255,255,255,0.05);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
        }

        .mastery-badge {
            font-size: 12px;
            padding: 4px 10px;
            border-radius: var(--radius-pill);
            background: rgba(0,0,0,0.2);
            border: 1px solid rgba(255,255,255,0.1);
        }

        .collection-name {
            font-size: 18px;
            font-weight: 600;
            margin-bottom: 4px;
        }

        .collection-meta {
            font-size: 13px;
            color: var(--text-tertiary);
            display: flex;
            gap: 12px;
        }

        .topic-tags {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }

        .tag-small {
            font-size: 11px;
            padding: 4px 8px;
            border-radius: 6px;
            background: rgba(255,255,255,0.05);
            color: var(--text-secondary);
            border: 1px solid rgba(255,255,255,0.05);
        }

        .mastery-progress-container {
            margin-top: auto;
        }

        .progress-label {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: var(--text-secondary);
            margin-bottom: 8px;
        }

        .progress-bar-bg {
            height: 6px;
            background: rgba(255,255,255,0.05);
            border-radius: 3px;
            overflow: hidden;
        }

        .progress-bar-fill {
            height: 100%;
            background: var(--bg-glow);
            border-radius: 3px;
            box-shadow: 0 0 10px rgba(76, 110, 245, 0.4);
        }

        .options-dots {
            color: var(--text-tertiary);
            cursor: pointer;
            padding: 4px;
        }

        .new-collection-card {
            border-radius: var(--radius-card);
            border: 2px dashed rgba(255,255,255,0.1);
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 12px;
            color: var(--text-secondary);
            transition: all 0.2s;
            cursor: pointer;
            min-height: 220px;
        }

        .new-collection-card:hover {
            border-color: rgba(255,255,255,0.3);
            background: rgba(255,255,255,0.02);
            color: white;
        }

        .plus-icon {
            font-size: 32px;
            font-weight: 300;
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
            <a href="#" vid="12">Dashboard</a>
            <a href="#" vid="13">Knowledge Graph</a>
            <a href="#" class="active" vid="14">Collections</a>
            <a href="#" vid="15">Settings</a>
        </nav>
        
        <div class="user-actions" vid="16">
            <button class="btn-text" vid="17">Import Log</button>
            <button class="btn-primary" vid="18">Start Quiz</button>
        </div>
    </header>

    <main vid="19">
        <div class="page-header" vid="20">
            <div class="title-group" vid="21">
                <h1 vid="22">Knowledge Collections</h1>
                <p vid="23">Organize and master your knowledge across specialized domains</p>
            </div>
            <div class="actions-group" vid="24">
                <button class="btn-text" vid="25">Merge Selected</button>
                <button class="btn-primary" vid="26">+ New Collection</button>
            </div>
        </div>

        <div class="grid" vid="27">
            
            <div class="new-collection-card" vid="28">
                <div class="plus-icon" vid="29">+</div>
                <div style="font-size: 14px;" vid="30">Create Collection</div>
            </div>

            
            <div class="collection-card glass" vid="31">
                <div class="card-top" vid="32">
                    <div class="folder-icon" vid="33">⚛️</div>
                    <div class="options-dots" vid="34">•••</div>
                </div>
                <div vid="35">
                    <div class="collection-name" vid="36">React Ecosystem</div>
                    <div class="collection-meta" vid="37">
                        <span vid="38">14 Files</span>
                        <span vid="39">Updated 2h ago</span>
                    </div>
                </div>
                <div class="topic-tags" vid="40">
                    <span class="tag-small" vid="41">Hooks</span>
                    <span class="tag-small" vid="42">Redux</span>
                    <span class="tag-small" vid="43">Next.js</span>
                    <span class="tag-small" vid="44">Context</span>
                </div>
                <div class="mastery-progress-container" vid="45">
                    <div class="progress-label" vid="46">
                        <span vid="47">Mastery level</span>
                        <span style="color: #22c55e;" vid="48">89%</span>
                    </div>
                    <div class="progress-bar-bg" vid="49">
                        <div class="progress-bar-fill" style="width: 89%;" vid="50"></div>
                    </div>
                </div>
            </div>

            
            <div class="collection-card glass" vid="51">
                <div class="card-top" vid="52">
                    <div class="folder-icon" vid="53">🏗️</div>
                    <div class="options-dots" vid="54">•••</div>
                </div>
                <div vid="55">
                    <div class="collection-name" vid="56">System Architecture</div>
                    <div class="collection-meta" vid="57">
                        <span vid="58">8 Files</span>
                        <span vid="59">Updated 1d ago</span>
                    </div>
                </div>
                <div class="topic-tags" vid="60">
                    <span class="tag-small" vid="61">Microservices</span>
                    <span class="tag-small" vid="62">Scalability</span>
                    <span class="tag-small" vid="63">CAP Theorem</span>
                </div>
                <div class="mastery-progress-container" vid="64">
                    <div class="progress-label" vid="65">
                        <span vid="66">Mastery level</span>
                        <span style="color: #eab308;" vid="67">64%</span>
                    </div>
                    <div class="progress-bar-bg" vid="68">
                        <div class="progress-bar-fill" style="width: 64%; background: #eab308; box-shadow: 0 0 10px rgba(234, 179, 8, 0.4);" vid="69"></div>
                    </div>
                </div>
            </div>

            
            <div class="collection-card glass" vid="70">
                <div class="card-top" vid="71">
                    <div class="folder-icon" vid="72">🤖</div>
                    <div class="options-dots" vid="73">•••</div>
                </div>
                <div vid="74">
                    <div class="collection-name" vid="75">AI &amp; Machine Learning</div>
                    <div class="collection-meta" vid="76">
                        <span vid="77">22 Files</span>
                        <span vid="78">Updated 4h ago</span>
                    </div>
                </div>
                <div class="topic-tags" vid="79">
                    <span class="tag-small" vid="80">LLMs</span>
                    <span class="tag-small" vid="81">Transformers</span>
                    <span class="tag-small" vid="82">PyTorch</span>
                    <span class="tag-small" vid="83">Fine-tuning</span>
                </div>
                <div class="mastery-progress-container" vid="84">
                    <div class="progress-label" vid="85">
                        <span vid="86">Mastery level</span>
                        <span style="color: #ef4444;" vid="87">31%</span>
                    </div>
                    <div class="progress-bar-bg" vid="88">
                        <div class="progress-bar-fill" style="width: 31%; background: #ef4444; box-shadow: 0 0 10px rgba(239, 68, 68, 0.4);" vid="89"></div>
                    </div>
                </div>
            </div>

            
            <div class="collection-card glass" vid="90">
                <div class="card-top" vid="91">
                    <div class="folder-icon" vid="92">🚢</div>
                    <div class="options-dots" vid="93">•••</div>
                </div>
                <div vid="94">
                    <div class="collection-name" vid="95">DevOps &amp; Cloud</div>
                    <div class="collection-meta" vid="96">
                        <span vid="97">6 Files</span>
                        <span vid="98">Updated 3d ago</span>
                    </div>
                </div>
                <div class="topic-tags" vid="99">
                    <span class="tag-small" vid="100">Docker</span>
                    <span class="tag-small" vid="101">Kubernetes</span>
                    <span class="tag-small" vid="102">CI/CD</span>
                </div>
                <div class="mastery-progress-container" vid="103">
                    <div class="progress-label" vid="104">
                        <span vid="105">Mastery level</span>
                        <span style="color: #22c55e;" vid="106">92%</span>
                    </div>
                    <div class="progress-bar-bg" vid="107">
                        <div class="progress-bar-fill" style="width: 92%;" vid="108"></div>
                    </div>
                </div>
            </div>

            
            <div class="collection-card glass" vid="109">
                <div class="card-top" vid="110">
                    <div class="folder-icon" vid="111">🛡️</div>
                    <div class="options-dots" vid="112">•••</div>
                </div>
                <div vid="113">
                    <div class="collection-name" vid="114">Web Security</div>
                    <div class="collection-meta" vid="115">
                        <span vid="116">4 Files</span>
                        <span vid="117">Updated 1w ago</span>
                    </div>
                </div>
                <div class="topic-tags" vid="118">
                    <span class="tag-small" vid="119">OWASP</span>
                    <span class="tag-small" vid="120">OAuth2</span>
                    <span class="tag-small" vid="121">JWT</span>
                </div>
                <div class="mastery-progress-container" vid="122">
                    <div class="progress-label" vid="123">
                        <span vid="124">Mastery level</span>
                        <span style="color: #eab308;" vid="125">51%</span>
                    </div>
                    <div class="progress-bar-bg" vid="126">
                        <div class="progress-bar-fill" style="width: 51%; background: #eab308; box-shadow: 0 0 10px rgba(234, 179, 8, 0.4);" vid="127"></div>
                    </div>
                </div>
            </div>

            
            <div class="collection-card glass" vid="128">
                <div class="card-top" vid="129">
                    <div class="folder-icon" vid="130">📝</div>
                    <div class="options-dots" vid="131">•••</div>
                </div>
                <div vid="132">
                    <div class="collection-name" vid="133">Technical Writing</div>
                    <div class="collection-meta" vid="134">
                        <span vid="135">11 Files</span>
                        <span vid="136">Updated 2d ago</span>
                    </div>
                </div>
                <div class="topic-tags" vid="137">
                    <span class="tag-small" vid="138">Documentation</span>
                    <span class="tag-small" vid="139">Markdown</span>
                    <span class="tag-small" vid="140">API Spec</span>
                </div>
                <div class="mastery-progress-container" vid="141">
                    <div class="progress-label" vid="142">
                        <span vid="143">Mastery level</span>
                        <span style="color: #22c55e;" vid="144">78%</span>
                    </div>
                    <div class="progress-bar-bg" vid="145">
                        <div class="progress-bar-fill" style="width: 78%;" vid="146"></div>
                    </div>
                </div>
            </div>
        </div>
    </main>

</body></html>
```
