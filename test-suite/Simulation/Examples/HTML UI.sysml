package 'HTML UI' {
    item FlashingLightUI_HTML {
        language "HTML"
        /* <!DOCTYPE html>
         * <html lang="en">
         * <head>
         * <meta charset="UTF-8">
         * <meta name="viewport" content="width=device-width, initial-scale=1.0">
         * <title>Flashing Light — Control Panel</title>
         * <link rel="preconnect" href="https://fonts.googleapis.com">
         * <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=IBM+Plex+Mono:wght@400;500;600&display=swap" rel="stylesheet">
         * <style>
         * :root{
         * --bg: #10131a;
         * --bg-grain: #14171f;
         * --panel: #1a1e28;
         * --panel-raised: #212632;
         * --plate: #1e2330;
         * --line: #2b3140;
         * --line-soft: #242938;
         * --text: #e9e7e1;
         * --text-dim: #8b93a5;
         * --text-faint: #565e6f;
         * --amber: #ffb238;
         * --amber-hot: #ffd27a;
         * --amber-glow: rgba(255, 178, 56, 0.55);
         * --off-grey: #3c4250;
         * --green: #5bd68a;
         * --red: #ea5c4f;
         * --font-display: 'Space Grotesk', 'Segoe UI', sans-serif;
         * --font-mono: 'IBM Plex Mono', 'SFMono-Regular', Consolas, monospace;
         * --radius: 14px;
         * }
         *
         * { box-sizing:border-box; }
         *
         * html, body{
         * height:100%;
         * margin:0;
         * }
         *
         * body{
         * background:
         * radial-gradient(circle at 50% 0%, #1a1f2b 0%, var(--bg) 55%),
         * repeating-linear-gradient(0deg, rgba(255,255,255,0.012) 0px, rgba(255,255,255,0.012) 1px, transparent 1px, transparent 3px);
         * color: var(--text);
         * font-family: var(--font-mono);
         * min-height:100vh;
         * display:flex;
         * align-items:center;
         * justify-content:center;
         * padding: 32px 16px;
         * }
         *
         * .panel{
         * width:100%;
         * max-width: 420px;
         * background: linear-gradient(180deg, var(--panel-raised) 0%, var(--panel) 100%);
         * border: 1px solid var(--line);
         * border-radius: 22px;
         * box-shadow:
         * 0 30px 60px -20px rgba(0,0,0,0.6),
         * inset 0 1px 0 rgba(255,255,255,0.04);
         * overflow:hidden;
         * }
         *
         * .plaque{
         * padding: 20px 22px 16px;
         * border-bottom: 1px solid var(--line-soft);
         * display:flex;
         * align-items:flex-start;
         * justify-content:space-between;
         * background: linear-gradient(180deg, rgba(255,255,255,0.02), transparent);
         * }
         *
         * .plaque h1{
         * font-family: var(--font-display);
         * font-size: 17px;
         * font-weight: 700;
         * letter-spacing: 0.04em;
         * margin: 0 0 4px;
         * text-transform: uppercase;
         * }
         *
         * .plaque .sub{
         * font-size: 11px;
         * color: var(--text-faint);
         * letter-spacing: 0.06em;
         * }
         *
         * .viewport{
         * position:relative;
         * padding: 34px 20px 26px;
         * display:flex;
         * flex-direction:column;
         * align-items:center;
         * background:
         * radial-gradient(ellipse 70% 60% at 50% 30%, rgba(255,255,255,0.03), transparent 70%),
         * var(--plate);
         * border-bottom: 1px solid var(--line-soft);
         * }
         *
         * .bulb-wrap{
         * position:relative;
         * width: 210px;
         * height: 210px;
         * display:flex;
         * align-items:center;
         * justify-content:center;
         * margin-bottom: 14px;
         * }
         *
         * .bulb-halo{
         * position:absolute;
         * width: 210px; height:210px;
         * border-radius:50%;
         * background: radial-gradient(circle, var(--amber-glow) 0%, transparent 68%);
         * opacity: 0;
         * transform: scale(0.6);
         * transition: opacity .35s ease, transform .35s ease;
         * filter: blur(2px);
         * }
         * .bulb-halo.on{
         * opacity: 1;
         * transform: scale(1);
         * }
         *
         * svg#bulb{
         * position:relative;
         * z-index:1;
         * width: 140px;
         * height: 140px;
         * transition: filter .35s ease;
         * }
         *
         * #bulb .glass{
         * fill: #232838;
         * stroke: #454d60;
         * stroke-width: 1.4;
         * transition: fill .35s ease, stroke .35s ease;
         * }
         * #bulb .filament{
         * stroke: #5a6377;
         * stroke-width: 2;
         * fill:none;
         * stroke-linecap: round;
         * transition: stroke .35s ease, filter .35s ease;
         * }
         * #bulb .base{
         * fill: #4a505f;
         * }
         * #bulb .base-line{
         * stroke: #2b2f3a;
         * stroke-width: 1;
         * }
         *
         * .bulb-wrap.on #bulb .glass{
         * fill: #3a2c14;
         * stroke: var(--amber-hot);
         * }
         * .bulb-wrap.on #bulb .filament{
         * stroke: var(--amber-hot);
         * filter: drop-shadow(0 0 5px var(--amber-hot));
         * }
         *
         * .bulb-caption{
         * font-family: var(--font-display);
         * font-size: 12px;
         * letter-spacing: 0.12em;
         * text-transform: uppercase;
         * color: var(--text-dim);
         * }
         * .bulb-caption b{
         * color: var(--amber);
         * font-weight: 600;
         * }
         * .bulb-wrap.on ~ .bulb-caption b{ color: var(--amber-hot); }
         *
         * .telemetry{
         * display:grid;
         * grid-template-columns: 1fr 1fr 1fr;
         * gap: 1px;
         * background: var(--line-soft);
         * border-bottom: 1px solid var(--line-soft);
         * }
         * .cell{
         * background: var(--panel);
         * padding: 12px 8px 13px;
         * text-align:center;
         * }
         * .cell .label{
         * font-size: 9px;
         * letter-spacing: 0.1em;
         * text-transform:uppercase;
         * color: var(--text-faint);
         * margin-bottom: 6px;
         * }
         * .cell .value{
         * font-family: var(--font-display);
         * font-size: 13px;
         * font-weight: 600;
         * color: var(--text);
         * letter-spacing: 0.02em;
         * }
         * .cell .led{
         * display:inline-block;
         * width:6px; height:6px;
         * border-radius:50%;
         * background: var(--off-grey);
         * margin-right:6px;
         * vertical-align:middle;
         * transition: all .2s ease;
         * }
         * .cell .led.active{
         * background: var(--amber);
         * box-shadow: 0 0 6px 1px var(--amber-glow);
         * }
         *
         * .button-bay{
         * display:flex;
         * flex-direction:column;
         * align-items:center;
         * padding: 26px 20px 24px;
         * border-bottom: 1px solid var(--line-soft);
         * background: var(--panel);
         * user-select:none;
         * }
         *
         * .pushbutton{
         * width: 88px;
         * height: 88px;
         * border-radius: 50%;
         * border: none;
         * cursor: pointer;
         * position: relative;
         * background:
         * radial-gradient(circle at 35% 30%, #e0655a 0%, #c23f34 55%, #9c2e26 100%);
         * box-shadow:
         * 0 7px 0 #6e1c17,
         * 0 10px 16px rgba(0,0,0,0.5),
         * inset 0 2px 3px rgba(255,255,255,0.25);
         * transition: transform .08s ease, box-shadow .08s ease;
         * outline-offset: 4px;
         * }
         * .pushbutton:focus-visible{
         * outline: 2px solid var(--amber);
         * }
         * .pushbutton.pressed{
         * transform: translateY(3px);
         * background:
         * radial-gradient(circle at 35% 30%, #ff8a7d 0%, #d94a3d 55%, #ad342a 100%);
         * box-shadow:
         * 0 4px 0 #6e1c17,
         * 0 6px 10px rgba(0,0,0,0.5),
         * inset 0 2px 3px rgba(255,255,255,0.3),
         * 0 0 0 5px rgba(255,178,56,0.22);
         * }
         * .pushbutton::after{
         * content:"";
         * position:absolute;
         * inset: 12px;
         * border-radius:50%;
         * background: radial-gradient(circle at 40% 30%, rgba(255,255,255,0.18), transparent 60%);
         * }
         *
         * .button-status{
         * margin-top: 12px;
         * display:flex;
         * align-items:center;
         * gap: 6px;
         * font-size: 10px;
         * letter-spacing: 0.1em;
         * text-transform: uppercase;
         * color: var(--text-dim);
         * }
         * .button-status .led{
         * width:7px; height:7px; border-radius:50%;
         * background: var(--off-grey);
         * transition: all .2s ease;
         * }
         * .button-status .led.active{
         * background: var(--amber);
         * box-shadow: 0 0 7px 1px var(--amber-glow);
         * }
         *
         * .button-caption{
         * margin-top: 14px;
         * font-size: 10px;
         * letter-spacing: 0.14em;
         * text-transform: uppercase;
         * color: var(--text-faint);
         * text-align:center;
         * }
         * .button-caption span{
         * display:block;
         * margin-top:2px;
         * color: var(--text-dim);
         * font-size: 9px;
         * letter-spacing: 0.08em;
         * }
         *
         * .control-rail{
         * padding: 18px 20px 22px;
         * }
         * .control-rail .rail-label{
         * font-size: 9px;
         * letter-spacing: 0.14em;
         * text-transform: uppercase;
         * color: var(--text-faint);
         * margin-bottom: 10px;
         * }
         *
         * .controls{
         * display:grid;
         * grid-template-columns: repeat(3, 1fr);
         * gap: 8px;
         * }
         *
         * .ctrl{
         * font-family: var(--font-mono);
         * font-size: 10.5px;
         * letter-spacing: 0.06em;
         * text-transform: uppercase;
         * color: var(--text);
         * background: var(--panel-raised);
         * border: 1px solid var(--line);
         * border-radius: 9px;
         * padding: 10px 4px;
         * cursor: pointer;
         * display:flex;
         * flex-direction:column;
         * align-items:center;
         * gap: 6px;
         * transition: background .15s ease, transform .08s ease, border-color .15s ease;
         * }
         * .ctrl svg{ width:15px; height:15px; opacity:0.85; }
         * .ctrl:hover{
         * background: #262c3a;
         * border-color: #3a4152;
         * }
         * .ctrl:active{
         * transform: translateY(1px);
         * }
         * .ctrl.danger{
         * color: #ffb3ac;
         * }
         * .ctrl.danger:hover{
         * background: rgba(234,92,79,0.12);
         * border-color: rgba(234,92,79,0.4);
         * }
         * .ctrl.primary svg{ color: var(--amber); }
         * .ctrl.primary:hover{ border-color: rgba(255,178,56,0.4); }
         *
         * @media (prefers-reduced-motion: reduce){
         * { transition: none !important; }
         * }
         * </style>
         * </head>
         * <body>
         *
         * <div class="panel">
         *
         * <!-- Header -->
         * <div class="plaque">
         * <div>
         * <h1>Flashing Light</h1>
         * <div class="sub">FlashingLightSystem · live panel</div>
         * </div>
         * </div>
         *
         * <!-- Bulb viewport -->
         * <div class="viewport">
         * <div class="bulb-wrap" id="bulb-wrap">
         * <div class="bulb-halo" id="bulb-halo"></div>
         * <svg id="bulb" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
         * <path class="glass" d="M32 6 C20 6 12 14 12 25 C12 33 16.5 37.5 20.5 41.5 C22.5 43.5 23.5 45.5 23.5 48 L40.5 48 C40.5 45.5 41.5 43.5 43.5 41.5 C47.5 37.5 52 33 52 25 C52 14 44 6 32 6 Z"/>
         * <path class="filament" d="M25 26 L28 20 L32 30 L36 20 L39 26"/>
         * <rect class="base" x="24" y="48" width="16" height="9" rx="1.5"/>
         * <line class="base-line" x1="24" y1="51.5" x2="40" y2="51.5"/>
         * <line class="base-line" x1="24" y1="54.5" x2="40" y2="54.5"/>
         * </svg>
         * </div>
         * <div class="bulb-caption">light.lightIn — <b id="light-caption">—</b></div>
         * </div>
         *
         * <!-- State telemetry -->
         * <div class="telemetry">
         * <div class="cell">
         * <div class="label">Button</div>
         * <div class="value"><span class="led" id="led-button"></span><span id="state-button">—</span></div>
         * </div>
         * <div class="cell">
         * <div class="label">Timer</div>
         * <div class="value"><span class="led" id="led-timer"></span><span id="state-timer">—</span></div>
         * </div>
         * <div class="cell">
         * <div class="label">Light</div>
         * <div class="value"><span class="led" id="led-light"></span><span id="state-light">—</span></div>
         * </div>
         * </div>
         *
         * <!-- Push button -->
         * <div class="button-bay">
         * <button class="pushbutton" id="push-button" aria-label="Button — click to toggle Push / Release" aria-pressed="false"></button>
         * <div class="button-status">
         * <span class="led" id="button-led"></span>
         * <span id="button-status-text">Released — click to Push</span>
         * </div>
         * </div>
         *
         * <!-- Simulation controls -->
         * <div class="control-rail">
         * <div class="rail-label">Simulation Control</div>
         * <div class="controls">
         * <button class="ctrl primary" id="ctrl-run">
         * <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="6 3 20 12 6 21 6 3"/></svg>
         * Run
         * </button>
         * <button class="ctrl" id="ctrl-toggle">
         * <svg id="toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><polygon points="10 8 16 12 10 16 10 8"/></svg>
         * <span id="toggle-label">Start</span>
         * </button>
         * <button class="ctrl danger" id="ctrl-terminate">
         * <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="5" width="14" height="14" rx="1.5"/></svg>
         * Terminate
         * </button>
         * </div>
         * </div>
         *
         * </div>
         *
         * <script>
         * (function(){
         * "use strict";
         *
         * // ---------- Structural paths (from FlashingLightSystemPkg::FlashingLightDomain) ----------
         * var PATH_ROOT   = "FlashingLightSystem";
         * var PATH_BUTTON = "FlashingLightSystem.button";
         * var PATH_TIMER  = "FlashingLightSystem.timer";
         * var PATH_LIGHT  = "FlashingLightSystem.light";
         *
         * // ---------- DOM refs ----------
         * var el = {
         * bulbWrap: document.getElementById("bulb-wrap"),
         * bulbHalo: document.getElementById("bulb-halo"),
         * lightCaption: document.getElementById("light-caption"),
         * stateButton: document.getElementById("state-button"),
         * stateTimer: document.getElementById("state-timer"),
         * stateLight: document.getElementById("state-light"),
         * ledButton: document.getElementById("led-button"),
         * ledTimer: document.getElementById("led-timer"),
         * ledLight: document.getElementById("led-light"),
         * pushButton: document.getElementById("push-button"),
         * buttonLed: document.getElementById("button-led"),
         * buttonStatusText: document.getElementById("button-status-text"),
         * ctrlRun: document.getElementById("ctrl-run"),
         * ctrlToggle: document.getElementById("ctrl-toggle"),
         * toggleIcon: document.getElementById("toggle-icon"),
         * toggleLabel: document.getElementById("toggle-label"),
         * ctrlTerminate: document.getElementById("ctrl-terminate")
         * };
         *
         * // ---------- Bridge helpers ----------
         * function hasApi(){
         * return typeof window.api !== "undefined" && window.api !== null;
         * }
         *
         * function safeCall(fnName){
         * if (!hasApi() || typeof window.api[fnName] !== "function") {
         * console.warn("[FlashingLightUI] window.api." + fnName + " unavailable");
         * return;
         * }
         * try{
         * window.api[fnName]();
         * }catch(err){
         * console.error("[FlashingLightUI] api." + fnName + " failed:", err);
         * }
         * }
         *
         * function getActiveStates(parentPath){
         * return new Promise(function(resolve){
         * if (!hasApi() || typeof window.api.getActiveStatesByDotNotation !== "function") {
         * resolve(null);
         * return;
         * }
         * window.api.getActiveStatesByDotNotation(window, resolve, parentPath);
         * });
         * }
         *
         * function fire(payloadName, receiverPath){
         * if (!hasApi() || typeof window.api.firePayloadEventByName !== "function") {
         * console.warn("[FlashingLightUI] cannot fire " + payloadName + " — engine offline");
         * return;
         * }
         * try{
         * window.api.firePayloadEventByName(payloadName, receiverPath);
         * }catch(err){
         * console.error("[FlashingLightUI] firePayloadEventByName failed:", err);
         * }
         * }
         *
         * // Walk to the deepest active leaf state and return its name.
         * function leafStateName(rawJson){
         * if (!rawJson) return null;
         * var nodes;
         * try{
         * nodes = JSON.parse(rawJson);
         * }catch(err){
         * return null;
         * }
         * if (!Array.isArray(nodes) || nodes.length === 0) return null;
         * var node = nodes[0];
         * while (node && node.children && node.children.length){
         * node = node.children[0];
         * }
         * return node ? node.name : null;
         * }
         *
         * // ---------- Rendering ----------
         * var bootChecked = false;
         * function renderStates(buttonName, timerName, lightName){
         * el.stateButton.textContent = buttonName || "—";
         * el.stateTimer.textContent = timerName || "—";
         * el.stateLight.textContent = lightName || "—";
         *
         * el.ledButton.classList.toggle("active", buttonName === "Pressed");
         * el.ledTimer.classList.toggle("active", timerName === "Ticking");
         * el.ledLight.classList.toggle("active", lightName === "On");
         *
         * var isOn = lightName === "On";
         * el.bulbWrap.classList.toggle("on", isOn);
         * el.bulbHalo.classList.toggle("on", isOn);
         * el.lightCaption.textContent = lightName || "—";
         * }
         *
         * function refresh(){
         * return Promise.all([
         * getActiveStates(PATH_BUTTON),
         * getActiveStates(PATH_TIMER),
         * getActiveStates(PATH_LIGHT)
         * ]).then(function(results){
         * var ok = results[0] !== null || results[1] !== null || results[2] !== null;
         * renderStates(
         * leafStateName(results[0]),
         * leafStateName(results[1]),
         * leafStateName(results[2])
         * );
         * // First time we see the engine respond, the simulation may already be
         * // running (e.g. SimConfig autoStart = true) before this UI attached.
         * // In that case the control should read "Pause", not "Start".
         * if (!bootChecked && ok){
         * bootChecked = true;
         * if (runState === "idle"){
         * runState = "running";
         * paintToggle();
         * }
         * }
         * });
         * }
         *
         * // ---------- Global engine hooks ----------
         * var updating = false;
         * window.update = function(){
         * if (updating) return;
         * updating = true;
         * return refresh().catch(function(err){
         * console.error("[FlashingLightUI] update failed:", err);
         * }).finally(function(){
         * updating = false;
         * });
         * };
         *
         * // ---------- Push button interaction (click to toggle Push / Release) ----------
         * var isPressed = false;
         *
         * function paintButton(){
         * el.pushButton.classList.toggle("pressed", isPressed);
         * el.pushButton.setAttribute("aria-pressed", isPressed ? "true" : "false");
         * el.buttonLed.classList.toggle("active", isPressed);
         * el.buttonStatusText.textContent = isPressed
         * ? "Pressed — click to Release"
         * : "Released — click to Push";
         * }
         *
         * function toggleButton(){
         * isPressed = !isPressed;
         * paintButton();
         * fire(isPressed ? "Push" : "Release", PATH_BUTTON);
         * setTimeout(refresh, 60);
         * }
         *
         * el.pushButton.addEventListener("click", toggleButton);
         * el.pushButton.addEventListener("keydown", function(e){
         * if (e.code === "Space" || e.code === "Enter"){
         * e.preventDefault();
         * if (!e.repeat) toggleButton();
         * }
         * });
         *
         * paintButton();
         *
         * // ---------- Simulation control rail ----------
         * var ICON_START  = '<circle cx="12" cy="12" r="9"/><polygon points="10 8 16 12 10 16 10 8"/>';
         * var ICON_PAUSE  = '<rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/>';
         * var ICON_RESUME = '<path d="M3 12a9 9 0 1 0 3-6.7"/><polyline points="3 4 3 9 8 9"/>';
         *
         * // Toggle cycles: idle -> Start -> running -> Pause -> paused -> Resume -> running ...
         * var runState = "idle"; // "idle" | "running" | "paused"
         *
         * function paintToggle(){
         * if (runState === "idle"){
         * el.toggleIcon.innerHTML = ICON_START;
         * el.toggleLabel.textContent = "Start";
         * } else if (runState === "running"){
         * el.toggleIcon.innerHTML = ICON_PAUSE;
         * el.toggleLabel.textContent = "Pause";
         * } else {
         * el.toggleIcon.innerHTML = ICON_RESUME;
         * el.toggleLabel.textContent = "Resume";
         * }
         * }
         * paintToggle();
         *
         * el.ctrlRun.addEventListener("click", function(){
         * safeCall("run");
         * setTimeout(refresh, 100);
         * });
         *
         * el.ctrlToggle.addEventListener("click", function(){
         * if (runState === "idle"){
         * safeCall("start");
         * runState = "running";
         * } else if (runState === "running"){
         * safeCall("pause");
         * runState = "paused";
         * } else {
         * safeCall("resume");
         * runState = "running";
         * }
         * paintToggle();
         * setTimeout(refresh, 100);
         * });
         *
         * el.ctrlTerminate.addEventListener("click", function(){
         * safeCall("terminate");
         * runState = "idle";
         * paintToggle();
         * setTimeout(refresh, 100);
         * });
         *
         * // ---------- Boot ----------
         * document.addEventListener("DOMContentLoaded", function(){
         * if (hasApi() && typeof window.api.run === "function"){
         * window.api.run();
         * }
         * refresh();
         * });
         *
         * })();
         * </script>
         *
         * </body>
         * </html>
         */
    }
    item WaterTankUI_HTML {
        language "HTML"
        /* <!DOCTYPE html>
         * <html lang="en" style="margin: 0; padding: 0; background: #0a0e13; color: #dbe6ef; font-family: 'IBM Plex Sans', sans-serif; min-height: 100vh; background-image: radial-gradient(circle at 15% 0%, rgba(53,208,196,0.05), transparent 40%), radial-gradient(circle at 85% 100%, rgba(255,180,84,0.04), transparent 40%); box-sizing: border-box;">
         * <head>
         * <meta charset="UTF-8" />
         * <meta name="viewport" content="width=device-width, initial-scale=1.0" />
         * <title>WaterTankSystem</title>
         * <link rel="preconnect" href="https://fonts.googleapis.com">
         * <link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500;600;700&family=IBM+Plex+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
         * <style>
         * @keyframes waveMove { from { transform: translateX(0); } to { transform: translateX(-50%); } }
         * @keyframes flowMove { from { background-position: 0 0; } to { background-position: 40px 0; } }
         *
         * .wave.w1 { animation: waveMove 6s linear infinite; }
         * .wave.w2 { animation: waveMove 9s linear infinite reverse; }
         * #valve-flow[data-open="true"] .pipe-flow { animation: flowMove 1s linear infinite; opacity: 1 !important; background: repeating-linear-gradient(90deg, #35d0c4 0 10px, transparent 10px 20px) !important; }
         * #valve-flow[data-open="true"] .valve-node { border-color: #4ade80 !important; color: #4ade80 !important; box-shadow: 0 0 12px rgba(74,222,128,0.4) !important; }
         * #valve-flow[data-open="false"] .valve-node { border-color: #ff5c5c !important; color: #ff5c5c !important; }
         *
         * .stm-node[data-active="true"] { color: #0a0e13 !important; background: #35d0c4 !important; border-color: #35d0c4 !important; box-shadow: 0 0 14px rgba(53,208,196,0.5) !important; }
         * #req-badge[data-ok="false"] { background: rgba(255,92,92,0.12) !important; color: #ff5c5c !important; }
         *
         * #sim-status[data-state="RUNNING"] { color: #4ade80 !important; border-color: rgba(74,222,128,0.35) !important; }
         * #sim-status[data-state="PAUSED"]  { color: #ffb454 !important; border-color: rgba(255,180,84,0.35) !important; }
         * #sim-status[data-state="STOPPED"] { color: #ff5c5c !important; border-color: rgba(255,92,92,0.35) !important; }
         * #sim-status[data-state="STEP"]    { color: #35d0c4 !important; border-color: rgba(53,208,196,0.35) !important; }
         *
         * button:hover { border-color: #35d0c4 !important; color: #35d0c4 !important; }
         * button:active { transform: translateY(1px); }
         * #btn-run:hover, #btn-start:hover { background: #35d0c4 !important; color: #0a0e13 !important; }
         * #btn-terminate:hover { background: rgba(255,92,92,0.12) !important; border-color: #ff5c5c !important; }
         * .override-row input:focus { outline: none; border-color: #35d0c4; }
         *
         * #console-log::-webkit-scrollbar { width: 8px; }
         * #console-log::-webkit-scrollbar-thumb { background: #22303f; border-radius: 4px; }
         * </style>
         * </head>
         * <body style="margin: 0; padding: 0; background: #0a0e13; color: #dbe6ef; font-family: 'IBM Plex Sans', sans-serif; min-height: 100vh; background-image: radial-gradient(circle at 15% 0%, rgba(53,208,196,0.05), transparent 40%), radial-gradient(circle at 85% 100%, rgba(255,180,84,0.04), transparent 40%); box-sizing: border-box;">
         *
         * <div class="frame" style="max-width: 1240px; margin: 0 auto; padding: 28px 24px 60px; box-sizing: border-box;">
         * <header style="display: flex; justify-content: space-between; align-items: flex-end; border-bottom: 1px solid #22303f; padding-bottom: 18px; margin-bottom: 24px; gap: 20px; flex-wrap: wrap;">
         * <div>
         * <div class="eyebrow" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.18em; color: #35d0c4; text-transform: uppercase; margin-bottom: 6px;">SYSML V2 SIMULATION CONTROL</div>
         * <h1 style="font-size: 26px; font-weight: 600; margin: 0; letter-spacing: -0.01em;">Water Tank System</h1>
         * <div class="model-path" style="font-family: 'IBM Plex Mono', monospace; font-size: 12px; color: #46586c; margin-top: 6px;">WaterTankSystem.waterTank &nbsp;·&nbsp; WaterTankSystem.controller</div>
         * </div>
         * <div class="status-cluster" style="display: flex; align-items: center; gap: 14px;">
         * <span id="sim-status" data-state="IDLE" style="font-family: 'IBM Plex Mono', monospace; font-size: 12px; letter-spacing: 0.1em; padding: 6px 14px; border-radius: 3px; border: 1px solid #22303f; background: #171f2b; color: #6d829a;">IDLE</span>
         * </div>
         * </header>
         *
         * <div class="grid" style="display: grid; grid-template-columns: 320px 1fr 300px; gap: 20px;">
         * <!-- ============ LEFT: TANK VISUAL ============ -->
         * <div class="panel tank-col" style="background: #121922; border: 1px solid #22303f; border-radius: 6px; padding: 18px 18px 20px; display: flex; flex-direction: column; gap: 20px; box-sizing: border-box;">
         * <div class="panel-title" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.14em; text-transform: uppercase; color: #6d829a; margin: 0 0 14px; display: flex; align-items: center; gap: 8px;">
         * <span style="width: 6px; height: 6px; background: #35d0c4; border-radius: 1px; display: inline-block;"></span>
         * Tank Level
         * </div>
         * <div class="tank-wrap" style="position: relative; height: 340px; display: flex; justify-content: center; align-items: flex-end;">
         * <div class="threshold-line" id="max-line" style="position: absolute; left: -2px; right: -2px; height: 0; border-top: 1px dashed #ffb454; opacity: 0.7; bottom:66%;">
         * <span style="position: absolute; right: -4px; top: -18px; font-family: 'IBM Plex Mono', monospace; font-size: 9px; color: #ffb454; background: #0a0e13; padding: 1px 4px; white-space: nowrap;">MAX 3.00</span>
         * </div>
         * <div class="threshold-line" id="min-line" style="position: absolute; left: -2px; right: -2px; height: 0; border-top: 1px dashed #6d829a; opacity: 0.7; bottom:22%;">
         * <span style="position: absolute; right: -4px; top: -18px; font-family: 'IBM Plex Mono', monospace; font-size: 9px; color: #6d829a; background: #0a0e13; padding: 1px 4px; white-space: nowrap;">MIN 1.00</span>
         * </div>
         * <div class="tank-body" style="position: relative; width: 180px; height: 340px; border: 2px solid #22303f; border-top: 2px dashed #46586c; border-radius: 4px 4px 10px 10px; background: linear-gradient(180deg, #0d131a 0%, #0a0f15 100%); overflow: hidden; box-shadow: inset 0 0 30px rgba(0,0,0,0.6); box-sizing: border-box;">
         * <div class="readout-block" style="position: absolute; top: 0; left: 50%; transform: translateX(-50%); text-align: center; padding-top: 8px; z-index: 2;">
         * <span id="level-readout" style="font-family: 'IBM Plex Mono', monospace; font-size: 15px; font-weight: 600; color: #dbe6ef; background: rgba(10,14,19,0.7); padding: 3px 10px; border-radius: 3px; border: 1px solid #22303f;">0.00 m</span>
         * </div>
         * <div class="liquid" id="liquid-level" style="position: absolute; bottom: 0; left: 0; width: 100%; height: 15%; transition: height 0.5s cubic-bezier(.4,0,.2,1); background: linear-gradient(180deg, rgba(53,208,196,0.55) 0%, rgba(24,120,113,0.85) 100%);">
         * <div class="wave w1" style="position: absolute; top: -14px; left: 0; width: 200%; height: 20px; opacity: 0.9;">
         * <svg viewBox="0 0 200 20" preserveAspectRatio="none" style="width: 100%; height: 100%; display: block;"><path d="M0 10 Q 25 0 50 10 T 100 10 T 150 10 T 200 10 V20 H0 Z" fill="rgba(53,208,196,0.5)"/></svg>
         * </div>
         * <div class="wave w2" style="position: absolute; top: -10px; left: 0; width: 200%; height: 20px; opacity: 0.55;">
         * <svg viewBox="0 0 200 20" preserveAspectRatio="none" style="width: 100%; height: 100%; display: block;"><path d="M0 10 Q 25 20 50 10 T 100 10 T 150 10 T 200 10 V20 H0 Z" fill="rgba(53,208,196,0.35)"/></svg>
         * </div>
         * </div>
         * </div>
         * </div>
         * <div class="req-row" style="display: flex; justify-content: space-between; align-items: center; font-family: 'IBM Plex Mono', monospace; font-size: 11px; padding-top: 10px; border-top: 1px solid #1a2531;">
         * <span>Requirement::waterLevel</span>
         * <span id="req-badge" data-ok="true" style="padding: 3px 8px; border-radius: 3px; letter-spacing: 0.06em; background: rgba(74,222,128,0.1); color: #4ade80;">WITHIN BOUNDS</span>
         * </div>
         * </div>
         *
         * <!-- ============ CENTER: STATES + VALVE ============ -->
         * <div class="panel tank-col" style="background: #121922; border: 1px solid #22303f; border-radius: 6px; padding: 18px 18px 20px; display: flex; flex-direction: column; gap: 20px; box-sizing: border-box;">
         * <div class="panel-title" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.14em; text-transform: uppercase; color: #6d829a; margin: 0 0 14px; display: flex; align-items: center; gap: 8px;">
         * <span style="width: 6px; height: 6px; background: #35d0c4; border-radius: 1px; display: inline-block;"></span>
         * State Machines
         * </div>
         *
         * <div class="stm-block" style="margin-bottom: 18px;">
         * <div class="stm-name" style="font-family: 'IBM Plex Mono', monospace; font-size: 10px; color: #46586c; letter-spacing: 0.08em; margin-bottom: 8px; text-transform: uppercase;">WaterTank.WaterTank_STM</div>
         * <div class="stm-nodes" style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
         * <div class="stm-node" data-tank-state="adjustingLevel" data-active="true" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; padding: 7px 12px; border-radius: 4px; border: 1px solid #22303f; color: #6d829a; background: #171f2b; transition: all 0.3s ease; position: relative;">adjustingLevel</div>
         * </div>
         * </div>
         *
         * <div class="stm-block" style="margin-bottom: 18px;">
         * <div class="stm-name" style="font-family: 'IBM Plex Mono', monospace; font-size: 10px; color: #46586c; letter-spacing: 0.08em; margin-bottom: 8px; text-transform: uppercase;">Controller.Controller_STM</div>
         * <div class="stm-nodes" style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
         * <div class="stm-node" data-controller-state="closeValve" data-active="true" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; padding: 7px 12px; border-radius: 4px; border: 1px solid #22303f; color: #6d829a; background: #171f2b; transition: all 0.3s ease; position: relative;">closeValve</div>
         * <span class="stm-arrow" style="color: #46586c; font-size: 12px;">⇄</span>
         * <div class="stm-node" data-controller-state="openValve" data-active="false" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; padding: 7px 12px; border-radius: 4px; border: 1px solid #22303f; color: #6d829a; background: #171f2b; transition: all 0.3s ease; position: relative;">openValve</div>
         * </div>
         * </div>
         *
         * <div class="panel-title" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.14em; text-transform: uppercase; color: #6d829a; margin: 22px 0 14px; display: flex; align-items: center; gap: 8px;">
         * <span style="width: 6px; height: 6px; background: #35d0c4; border-radius: 1px; display: inline-block;"></span>
         * Valve · Flow Path
         * </div>
         * <div class="valve-diagram" id="valve-flow" data-open="false" style="display: flex; align-items: center; gap: 14px; padding: 16px 4px 6px;">
         * <div class="pipe" style="flex: 1; height: 10px; border-radius: 5px; background: #171f2b; border: 1px solid #22303f; position: relative; overflow: hidden;">
         * <div class="pipe-flow" style="position: absolute; inset: 0; background: repeating-linear-gradient(90deg, #1b6e68 0 10px, transparent 10px 20px); opacity: 0.3;"></div>
         * </div>
         * <div class="valve-node" style="width: 46px; height: 46px; border-radius: 50%; border: 2px solid #22303f; display: flex; align-items: center; justify-content: center; font-family: 'IBM Plex Mono', monospace; font-size: 9px; letter-spacing: 0.04em; background: #171f2b; color: #6d829a; transition: all 0.3s ease; flex-shrink: 0;">
         * <span id="valve-label" style="font-weight: 600;">CLOSED</span>
         * </div>
         * <div class="pipe" style="flex: 1; height: 10px; border-radius: 5px; background: #171f2b; border: 1px solid #22303f; position: relative; overflow: hidden;">
         * <div class="pipe-flow" style="position: absolute; inset: 0; background: repeating-linear-gradient(90deg, #1b6e68 0 10px, transparent 10px 20px); opacity: 0.3;"></div>
         * </div>
         * </div>
         *
         * <div style="margin-top:8px;">
         * <div class="attr-row" style="display: flex; justify-content: space-between; font-family: 'IBM Plex Mono', monospace; font-size: 12px; padding: 7px 0; border-bottom: 1px solid #1a2531; color: #6d829a;">
         * <span>flowRate</span><span id="attr-flowrate" style="color: #dbe6ef;">0.20</span>
         * </div>
         * <div class="attr-row" style="display: flex; justify-content: space-between; font-family: 'IBM Plex Mono', monospace; font-size: 12px; padding: 7px 0; border-bottom: 1px solid #1a2531; color: #6d829a;">
         * <span>minlevel</span><span id="attr-minlevel" style="color: #dbe6ef;">1.00</span>
         * </div>
         * <div class="attr-row" style="display: flex; justify-content: space-between; font-family: 'IBM Plex Mono', monospace; font-size: 12px; padding: 7px 0; border-bottom: 1px solid #1a2531; color: #6d829a;">
         * <span>maxlevel</span><span id="attr-maxlevel" style="color: #dbe6ef;">3.00</span>
         * </div>
         * </div>
         * </div>
         *
         * <!-- ============ RIGHT: CONTROLS ============ -->
         * <div class="panel tank-col" style="background: #121922; border: 1px solid #22303f; border-radius: 6px; padding: 18px 18px 20px; display: flex; flex-direction: column; gap: 20px; box-sizing: border-box;">
         * <div class="panel-title" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.14em; text-transform: uppercase; color: #6d829a; margin: 0 0 14px; display: flex; align-items: center; gap: 8px;">
         * <span style="width: 6px; height: 6px; background: #35d0c4; border-radius: 1px; display: inline-block;"></span>
         * Simulation Controls
         * </div>
         * <div class="controls-grid" style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;">
         * <button id="btn-start" style="grid-column: span 3; background: #1b6e68; border: 1px solid #35d0c4; color: #eafffb; font-weight: 600; font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.06em; text-transform: uppercase; padding: 11px 6px; border-radius: 4px; cursor: pointer; transition: all 0.15s ease;">Start</button>
         * <button id="btn-run" style="grid-column: span 3; background: #1b6e68; border: 1px solid #35d0c4; color: #eafffb; font-weight: 600; font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.06em; text-transform: uppercase; padding: 11px 6px; border-radius: 4px; cursor: pointer; transition: all 0.15s ease;">Run</button>
         * <button id="btn-pause" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.06em; text-transform: uppercase; padding: 11px 6px; background: #171f2b; border: 1px solid #22303f; color: #dbe6ef; border-radius: 4px; cursor: pointer; transition: all 0.15s ease;">Pause</button>
         * <button id="btn-resume" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.06em; text-transform: uppercase; padding: 11px 6px; background: #171f2b; border: 1px solid #22303f; color: #dbe6ef; border-radius: 4px; cursor: pointer; transition: all 0.15s ease;">Resume</button>
         * <button id="btn-step" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.06em; text-transform: uppercase; padding: 11px 6px; background: #171f2b; border: 1px solid #22303f; color: #dbe6ef; border-radius: 4px; cursor: pointer; transition: all 0.15s ease;">Step</button>
         * <button id="btn-terminate" style="grid-column: span 3; border: 1px solid rgba(255,92,92,0.4); color: #ff5c5c; background: #171f2b; font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.06em; text-transform: uppercase; padding: 11px 6px; border-radius: 4px; cursor: pointer; transition: all 0.15s ease;">Terminate</button>
         * </div>
         *
         * <div class="override-row" style="margin-top: 16px; padding-top: 14px; border-top: 1px solid #1a2531; display: flex; flex-direction: column; gap: 8px;">
         * <div class="override-label" style="font-family: 'IBM Plex Mono', monospace; font-size: 10px; color: #46586c; letter-spacing: 0.06em; margin-bottom: 8px; text-transform: uppercase;">Manual Override · flowRate</div>
         * <div style="display:flex; gap:8px;">
         * <input type="number" id="flow-override" step="0.05" placeholder="0.20" style="flex: 1; min-width: 0; background: #171f2b; border: 1px solid #22303f; color: #dbe6ef; font-family: 'IBM Plex Mono', monospace; font-size: 12px; padding: 8px 10px; border-radius: 4px; box-sizing: border-box;" />
         * <button id="btn-apply-flow" style="white-space: nowrap; font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.06em; text-transform: uppercase; padding: 11px 6px; background: #171f2b; border: 1px solid #22303f; color: #dbe6ef; border-radius: 4px; cursor: pointer; transition: all 0.15s ease;">Apply</button>
         * </div>
         * </div>
         * </div>
         * </div>
         *
         * <!-- ============ CONSOLE ============ -->
         * <div class="panel console-panel" style="background: #121922; border: 1px solid #22303f; border-radius: 6px; padding: 18px 18px 20px; margin-top: 20px; box-sizing: border-box;">
         * <div class="panel-title" style="font-family: 'IBM Plex Mono', monospace; font-size: 11px; letter-spacing: 0.14em; text-transform: uppercase; color: #6d829a; margin: 0 0 14px; display: flex; align-items: center; gap: 8px;">
         * <span style="width: 6px; height: 6px; background: #35d0c4; border-radius: 1px; display: inline-block;"></span>
         * Engine Log
         * </div>
         * <div id="console-log" style="height: 160px; overflow-y: auto; background: #060a0e; border: 1px solid #22303f; border-radius: 4px; padding: 10px 12px; font-family: 'IBM Plex Mono', monospace; font-size: 11.5px; line-height: 1.6; box-sizing: border-box;"></div>
         * </div>
         * </div>
         *
         * <script>
         * const PATHS = {
         * waterTank: "WaterTankSystem.waterTank",
         * tankSTM: "WaterTankSystem.waterTank.WaterTankSTM",
         * controller: "WaterTankSystem.controller",
         * controllerSTM: "WaterTankSystem.controller.ControllerSTM",
         * valvePort: "WaterTankSystem.controller.valvePort",
         * };
         *
         * const SCALE_MAX = 4.5; // visual headroom above maxlevel (3.0)
         *
         * // ------------------------------------------------------------
         * // Promise wrappers around the async window.api bridge
         * // ------------------------------------------------------------
         * const hasRealApi = () => typeof window.api !== "undefined" && window.api !== null;
         *
         * const fetchState = (parentPath) => new Promise((resolve) => {
         * if (hasRealApi()) window.api.getActiveStatesByDotNotation(window, resolve, parentPath);
         * else resolve(null);
         * });
         *
         * const getFeature = (name, parentPath) => new Promise((resolve) => {
         * if (hasRealApi()) window.api.getFeatureValuesByDotNotation(window, resolve, name, parentPath);
         * else resolve(null);
         * });
         *
         * const updateFeature = (name, parentPath, value) => {
         * if (hasRealApi()) window.api.updateFeatureValuesByDotNotation(name, parentPath, value);
         * else MockEngine.updateFeature(name, parentPath, value);
         * };
         *
         * // ------------------------------------------------------------
         * // MOCK ENGINE — only used when there is no real window.api.
         * // Mirrors WaterTank / Controller STM logic from the .sysml file
         * // ------------------------------------------------------------
         * const MockEngine = (() => {
         * let level = 0.6;
         * let valve = 0; // 0 closed, 1 open
         * let flowRate = 0.2;
         * let minlevel = 1.0;
         * let maxlevel = 3.0;
         * let tankState = "adjustingLevel";
         * let controllerState = "closeValve";
         * let running = false;
         * let timer = null;
         * let listeners = [];
         *
         * function tick() {
         * // WaterTank.adjustingLevel entry action
         * level = level + flowRate - flowRate * valve * 1.2;
         * if (level < 0) level = 0;
         *
         * // Controller transitions
         * if (controllerState === "closeValve" && level >= maxlevel) {
         * controllerState = "openValve";
         * valve = 1;
         * } else if (controllerState === "openValve" && level <= minlevel) {
         * controllerState = "closeValve";
         * valve = 0;
         * }
         *
         * emitLog(`t+${(Date.now() % 100000)}ms  level=${level.toFixed(3)}  valve=${valve}  state=${controllerState}`);
         * listeners.forEach((fn) => fn());
         * }
         *
         * function emitLog(msg) {
         * if (typeof window.log === "function") window.log(msg);
         * }
         *
         * return {
         * onTick(fn) { listeners.push(fn); },
         * start() {
         * if (running) return;
         * running = true;
         * emitLog("MOCK ENGINE: simulation started");
         * timer = setInterval(tick, 1000);
         * },
         * pause() {
         * if (!running) return;
         * running = false;
         * clearInterval(timer);
         * emitLog("MOCK ENGINE: simulation paused");
         * },
         * resume() {
         * if (running) return;
         * running = true;
         * emitLog("MOCK ENGINE: simulation resumed");
         * timer = setInterval(tick, 1000);
         * },
         * step() { tick(); },
         * terminate() {
         * running = false;
         * clearInterval(timer);
         * level = 0.6;
         * valve = 0;
         * controllerState = "closeValve";
         * emitLog("MOCK ENGINE: simulation terminated & reset");
         * listeners.forEach((fn) => fn());
         * },
         * updateFeature(name, parentPath, value) {
         * if (name === "level" && parentPath === PATHS.waterTank) level = Number(value);
         * if (name === "flowRate" && parentPath === PATHS.waterTank) flowRate = Number(value);
         * if (name === "valve" && parentPath === PATHS.valvePort) valve = Number(value);
         * if (name === "minlevel" && parentPath === PATHS.controller) minlevel = Number(value);
         * if (name === "maxlevel" && parentPath === PATHS.controller) maxlevel = Number(value);
         * },
         * forceState(newControllerState) {
         * controllerState = newControllerState;
         * valve = newControllerState === "openValve" ? 1 : 0;
         * },
         * async mockFeature(name, parentPath) {
         * if (parentPath === PATHS.waterTank && name === "level") return String(level);
         * if (parentPath === PATHS.waterTank && name === "flowRate") return String(flowRate);
         * if (parentPath === PATHS.valvePort && name === "valve") return String(valve);
         * if (parentPath === PATHS.controller && name === "minlevel") return String(minlevel);
         * if (parentPath === PATHS.controller && name === "maxlevel") return String(maxlevel);
         * return null;
         * },
         * async mockState(parentPath) {
         * if (parentPath === PATHS.tankSTM) return JSON.stringify([{ name: tankState }]);
         * if (parentPath === PATHS.controllerSTM) return JSON.stringify([{ name: controllerState }]);
         * return null;
         * },
         * };
         * })();
         *
         * // Route feature/state calls through the mock when no real bridge exists
         * const _getFeature = getFeature;
         * const getFeatureSafe = async (name, parentPath) => hasRealApi() ? _getFeature(name, parentPath) : MockEngine.mockFeature(name, parentPath);
         *
         * const _fetchState = fetchState;
         * const fetchStateSafe = async (parentPath) => hasRealApi() ? _fetchState(parentPath) : MockEngine.mockState(parentPath);
         *
         * // ------------------------------------------------------------
         * // Control button bindings
         * // ------------------------------------------------------------
         * function bindControls() {
         * const runBtn = document.getElementById("btn-run");
         * if (runBtn) {
         * runBtn.onclick = () => {
         * if (hasRealApi() && window.api.run) {
         * window.api.run();
         * } else {
         * MockEngine.start();
         * }
         * setSimStatus("btn-run");
         * };
         * }
         *
         * const bind = (id, realFn, mockFn) => {
         * const el = document.getElementById(id);
         * if (!el) return;
         * el.addEventListener("click", () => {
         * if (hasRealApi() && window.api[realFn]) window.api[realFn]();
         * else mockFn();
         * setSimStatus(id);
         * });
         * };
         * bind("btn-start", "start", () => MockEngine.start());
         * bind("btn-pause", "pause", () => MockEngine.pause());
         * bind("btn-resume", "resume", () => MockEngine.resume());
         * bind("btn-step", "step", () => MockEngine.step());
         * bind("btn-terminate", "terminate", () => MockEngine.terminate());
         *
         * MockEngine.onTick(() => window.update && window.update());
         * }
         *
         * function setSimStatus(triggeredId) {
         * const map = {
         * "btn-run": "RUNNING",
         * "btn-start": "RUNNING",
         * "btn-pause": "PAUSED",
         * "btn-resume": "RUNNING",
         * "btn-step": "STEP",
         * "btn-terminate": "STOPPED",
         * };
         * const label = map[triggeredId] || "IDLE";
         * const badge = document.getElementById("sim-status");
         * if (!badge) return;
         * badge.textContent = label;
         * badge.dataset.state = label;
         * }
         *
         * // ------------------------------------------------------------
         * // Rendering — pure DOM/CSS updates driven by simulation data
         * // ------------------------------------------------------------
         * function renderLevel(level, minlevel, maxlevel) {
         * const pct = Math.max(0, Math.min(1, level / SCALE_MAX)) * 100;
         * const liquid = document.getElementById("liquid-level");
         * const readout = document.getElementById("level-readout");
         * const minLine = document.getElementById("min-line");
         * const maxLine = document.getElementById("max-line");
         * if (liquid) liquid.style.height = pct + "%";
         * if (readout) readout.textContent = level.toFixed(2) + " m";
         * if (minLine) minLine.style.bottom = (minlevel / SCALE_MAX) * 100 + "%";
         * if (maxLine) maxLine.style.bottom = (maxlevel / SCALE_MAX) * 100 + "%";
         *
         * const inRange = level >= minlevel && level <= maxlevel;
         * const req = document.getElementById("req-badge");
         * if (req) {
         * req.textContent = inRange ? "WITHIN BOUNDS" : "OUT OF BOUNDS";
         * req.dataset.ok = inRange ? "true" : "false";
         * }
         *
         * const minAttr = document.getElementById("attr-minlevel");
         * const maxAttr = document.getElementById("attr-maxlevel");
         * if (minAttr) minAttr.textContent = minlevel.toFixed(2);
         * if (maxAttr) maxAttr.textContent = maxlevel.toFixed(2);
         * }
         *
         * function renderValve(controllerState) {
         * const isOpen = controllerState === "openValve";
         * const pipe = document.getElementById("valve-flow");
         * const label = document.getElementById("valve-label");
         * if (pipe) pipe.dataset.open = isOpen ? "true" : "false";
         * if (label) label.textContent = isOpen ? "OPEN" : "CLOSED";
         * }
         *
         * function renderStates(tankState, controllerState) {
         * document.querySelectorAll("[data-tank-state]").forEach((el) => {
         * el.dataset.active = el.dataset.tankState === tankState ? "true" : "false";
         * });
         * document.querySelectorAll("[data-controller-state]").forEach((el) => {
         * el.dataset.active = el.dataset.controllerState === controllerState ? "true" : "false";
         * });
         * }
         *
         * function appendLog(message) {
         * const log = document.getElementById("console-log");
         * if (!log) return;
         * const line = document.createElement("div");
         * line.className = "log-line";
         * line.style.display = "flex";
         * line.style.gap = "10px";
         * line.style.color = "#6d829a";
         *
         * const time = new Date().toLocaleTimeString();
         * line.innerHTML = `<span class="log-time" style="color: #46586c; flex-shrink: 0;">${time}</span><span class="log-msg" style="color: #dbe6ef;">${message}</span>`;
         * log.appendChild(line);
         * log.scrollTop = log.scrollHeight;
         * while (log.children.length > 200) log.removeChild(log.firstChild);
         * }
         *
         * // ------------------------------------------------------------
         * // Global hooks required by the simulation engine
         * // ------------------------------------------------------------
         * let isUpdating = false;
         *
         * window.update = async () => {
         * if (isUpdating) return;
         * isUpdating = true;
         * try {
         * const [levelRaw, valveRaw, minRaw, maxRaw, tankStatesRaw, ctrlStatesRaw] = await Promise.all([
         * getFeatureSafe("level", PATHS.waterTank),
         * getFeatureSafe("valve", PATHS.valvePort),
         * getFeatureSafe("minlevel", PATHS.controller),
         * getFeatureSafe("maxlevel", PATHS.controller),
         * fetchStateSafe(PATHS.tankSTM),
         * fetchStateSafe(PATHS.controllerSTM),
         * ]);
         *
         * const level = levelRaw !== null ? parseFloat(levelRaw) : 0;
         * const valve = valveRaw !== null ? parseFloat(valveRaw) : 0;
         * const minlevel = minRaw !== null ? parseFloat(minRaw) : 1.0;
         * const maxlevel = maxRaw !== null ? parseFloat(maxRaw) : 3.0;
         * const tankState = safeParseStateName(tankStatesRaw) || "adjustingLevel";
         * const controllerState = safeParseStateName(ctrlStatesRaw) || "closeValve";
         *
         * renderLevel(level, minlevel, maxlevel);
         * renderValve(controllerState);
         * renderStates(tankState, controllerState);
         * } catch (err) {
         * // sync error suppressed
         * } finally {
         * isUpdating = false;
         * }
         * };
         *
         * function safeParseStateName(json) {
         * if (!json) return null;
         * try {
         * const arr = JSON.parse(json);
         * return Array.isArray(arr) && arr.length ? arr[0].name : null;
         * } catch {
         * return null;
         * }
         * }
         *
         * window.log = async (message) => {
         * appendLog(message);
         * };
         *
         * // ------------------------------------------------------------
         * // Optional: manual override controls
         * // ------------------------------------------------------------
         * function bindOverrides() {
         * const flowInput = document.getElementById("flow-override");
         * const applyBtn = document.getElementById("btn-apply-flow");
         * if (applyBtn && flowInput) {
         * applyBtn.addEventListener("click", () => {
         * const val = parseFloat(flowInput.value);
         * if (!Number.isNaN(val)) {
         * updateFeature("flowRate", PATHS.waterTank, val);
         * appendLog(`Manual override: flowRate -> ${val}`);
         * const flowAttr = document.getElementById("attr-flowrate");
         * if (flowAttr) flowAttr.textContent = val.toFixed(2);
         * }
         * });
         * }
         * }
         *
         * // ------------------------------------------------------------
         * // Boot
         * // ------------------------------------------------------------
         * document.addEventListener("DOMContentLoaded", () => {
         * bindControls();
         * bindOverrides();
         * if (hasRealApi() && window.api.run) window.api.run();
         * window.update();
         * });
         * </script>
         * </body>
         * </html>
         */
    }
    item TrafficLightUI_HTML {
        language "HTML"
        /* <!DOCTYPE html>
         * <html lang="en">
         * <head>
         * <meta charset="UTF-8">
         * <title>Signal Control — Traffic Light</title>
         * <link rel="preconnect" href="https://fonts.googleapis.com">
         * <link href="https://fonts.googleapis.com/css2?family=Oswald:wght@500;600;700&family=IBM+Plex+Mono:wght@400;500;600&display=swap" rel="stylesheet">
         * <style>
         * :root{
         * --asphalt: #16171a;
         * --asphalt-2: #0f1012;
         * --metal-1: #2b2c30;
         * --metal-2: #1c1d20;
         * --metal-edge: #3a3b40;
         * --hazard: #f4b400;
         * --hazard-dim: #8a6a12;
         * --red-lit: #ff3b30;
         * --red-glow: #ff6a5c;
         * --red-unlit: #3a1512;
         * --amber-lit: #ffb300;
         * --amber-glow: #ffd166;
         * --amber-unlit: #3a2c10;
         * --green-lit: #1fd88a;
         * --green-glow: #7cf0c0;
         * --green-unlit: #10301f;
         * --text-1: #ece9e2;
         * --text-2: #9a9ca3;
         * --text-3: #66686e;
         * --danger: #ff5147;
         * --ok: #2fbd77;
         * }
         *
         * {box-sizing:border-box;}
         * html,body{height:100%;}
         * body{
         * margin:0;
         * min-height:100vh;
         * background:
         * radial-gradient(1200px 600px at 50% -10%, #202126 0%, var(--asphalt) 55%, var(--asphalt-2) 100%);
         * color:var(--text-1);
         * font-family:'IBM Plex Mono', monospace;
         * display:flex;
         * align-items:flex-start;
         * justify-content:center;
         * padding:32px 16px 64px;
         * }
         *
         * .rig{
         * width:100%;
         * max-width:460px;
         * }
         *
         * .center{
         * display:flex;
         * flex-direction:column;
         * align-items:center;
         * }
         *
         * .hazard-bar{
         * height:10px;
         * border-radius:3px;
         * background: repeating-linear-gradient(135deg, var(--hazard) 0 14px, var(--asphalt-2) 14px 28px);
         * opacity:.9;
         * margin-bottom:14px;
         * }
         * header{
         * display:flex;
         * align-items:baseline;
         * justify-content:space-between;
         * margin-bottom:22px;
         * }
         * header h1{
         * font-family:'Oswald', sans-serif;
         * font-weight:700;
         * font-size:22px;
         * letter-spacing:.06em;
         * text-transform:uppercase;
         * margin:0;
         * color:var(--text-1);
         * }
         * header .unit{
         * font-size:11px;
         * color:var(--text-3);
         * letter-spacing:.08em;
         * }
         * #conn{
         * display:flex;
         * align-items:center;
         * gap:7px;
         * font-size:11px;
         * letter-spacing:.06em;
         * color:var(--text-2);
         * text-transform:uppercase;
         * }
         * #conn .dot{
         * width:7px;height:7px;border-radius:50%;
         * background:var(--text-3);
         * box-shadow:0 0 0 rgba(0,0,0,0);
         * transition:background .25s ease, box-shadow .25s ease;
         * }
         * #conn.up .dot{ background:var(--ok); box-shadow:0 0 8px var(--ok); }
         * #conn.down .dot{ background:var(--danger); box-shadow:0 0 8px var(--danger); }
         *
         * .mount{
         * display:flex;
         * flex-direction:column;
         * align-items:center;
         * margin-bottom:8px;
         * }
         * .arm{
         * width:6px;
         * height:34px;
         * background:linear-gradient(180deg, var(--metal-edge), var(--metal-2));
         * border-radius:2px;
         * }
         * .bolt-row{
         * display:flex;
         * gap:10px;
         * margin:2px 0 0;
         * }
         * .bolt{
         * width:6px;height:6px;border-radius:50%;
         * background:radial-gradient(circle at 35% 30%, #6b6c72, #212226);
         * }
         *
         * .head{
         * width:150px;
         * padding:16px 14px;
         * border-radius:20px;
         * background: linear-gradient(180deg, var(--metal-1), var(--metal-2) 60%, #17181b);
         * border:1px solid var(--metal-edge);
         * box-shadow:
         * 0 18px 40px rgba(0,0,0,.55),
         * inset 0 1px 0 rgba(255,255,255,.04),
         * inset 0 -14px 22px rgba(0,0,0,.5);
         * display:flex;
         * flex-direction:column;
         * gap:14px;
         * align-items:center;
         * }
         *
         * .lens-wrap{
         * position:relative;
         * width:104px;
         * height:104px;
         * }
         * .visor{
         * position:absolute;
         * top:-6px; left:-4px; right:-4px;
         * height:52px;
         * border-radius:52px 52px 0 0 / 30px 30px 0 0;
         * background:linear-gradient(180deg, #101114, #0000 70%);
         * opacity:.6;
         * pointer-events:none;
         * }
         * .lens{
         * width:100%;
         * height:100%;
         * border-radius:50%;
         * background: radial-gradient(circle at 38% 32%, #232226, #0c0d0f 72%);
         * box-shadow:
         * inset 0 0 0 6px #0a0a0c,
         * inset 0 10px 18px rgba(0,0,0,.7);
         * display:flex;
         * align-items:center;
         * justify-content:center;
         * transition: background .35s ease, box-shadow .35s ease;
         * }
         * .lens .glass{
         * width:74%;
         * height:74%;
         * border-radius:50%;
         * transition: background .35s ease, box-shadow .35s ease, opacity .35s ease;
         * }
         *
         * .lens.red .glass{ background:var(--red-unlit); }
         * .lens.amber .glass{ background:var(--amber-unlit); }
         * .lens.green .glass{ background:var(--green-unlit); }
         *
         * .lens.red.lit{ box-shadow: inset 0 0 0 6px #0a0a0c, 0 0 26px 6px rgba(255,59,48,.55); }
         * .lens.red.lit .glass{ background:radial-gradient(circle at 38% 32%, var(--red-glow), var(--red-lit) 70%); box-shadow:0 0 22px 4px var(--red-lit); }
         *
         * .lens.amber.lit{ box-shadow: inset 0 0 0 6px #0a0a0c, 0 0 26px 6px rgba(255,179,0,.55); }
         * .lens.amber.lit .glass{ background:radial-gradient(circle at 38% 32%, var(--amber-glow), var(--amber-lit) 70%); box-shadow:0 0 22px 4px var(--amber-lit); }
         *
         * .lens.green.lit{ box-shadow: inset 0 0 0 6px #0a0a0c, 0 0 26px 6px rgba(31,216,138,.5); }
         * .lens.green.lit .glass{ background:radial-gradient(circle at 38% 32%, var(--green-glow), var(--green-lit) 70%); box-shadow:0 0 22px 4px var(--green-lit); }
         *
         * .panel{
         * margin-top:22px;
         * background:linear-gradient(180deg, #1d1e22, #17181b);
         * border:1px solid var(--metal-edge);
         * border-radius:14px;
         * padding:16px 16px 18px;
         * }
         * .panel + .panel{ margin-top:14px; }
         * .panel h2{
         * font-family:'Oswald', sans-serif;
         * font-size:12px;
         * letter-spacing:.14em;
         * text-transform:uppercase;
         * color:var(--text-2);
         * margin:0 0 12px;
         * display:flex;
         * align-items:center;
         * gap:8px;
         * }
         * .panel h2::before{
         * content:"";
         * width:8px;height:8px;
         * background:var(--hazard);
         * border-radius:1px;
         * display:inline-block;
         * }
         *
         * .run-row{
         * display:grid;
         * grid-template-columns:repeat(5, 1fr);
         * gap:8px;
         * }
         * button{
         * font-family:'IBM Plex Mono', monospace;
         * font-size:10.5px;
         * letter-spacing:.04em;
         * text-transform:uppercase;
         * color:var(--text-1);
         * background:linear-gradient(180deg, #2a2b30, #202126);
         * border:1px solid var(--metal-edge);
         * border-radius:8px;
         * padding:12px 4px 10px;
         * cursor:pointer;
         * display:flex;
         * flex-direction:column;
         * align-items:center;
         * gap:6px;
         * transition:transform .08s ease, background .15s ease, border-color .15s ease;
         * }
         * button svg{
         * width:18px;
         * height:18px;
         * stroke:currentColor;
         * fill:none;
         * stroke-width:1.8;
         * stroke-linecap:round;
         * stroke-linejoin:round;
         * }
         * button:hover{ border-color:#57585f; }
         * button:active{ transform:translateY(1px) scale(.98); }
         * button:disabled{ opacity:.35; cursor:not-allowed; }
         *
         * button.danger:hover{ border-color:var(--danger); color:#ffb3ae; }
         * button.primary{ background:linear-gradient(180deg, #33352f, #24261f); border-color:var(--hazard-dim); }
         * button.primary:hover{ border-color:var(--hazard); color:#ffe9ad; }
         *
         * footer{
         * margin-top:18px;
         * text-align:center;
         * font-size:10px;
         * letter-spacing:.06em;
         * color:var(--text-3);
         * }
         *
         * @media (prefers-reduced-motion: reduce){
         * { transition-duration:.01ms !important; }
         * }
         * </style>
         * </head>
         * <body>
         *
         * <div class="rig">
         * <div class="hazard-bar"></div>
         * <header>
         * <div>
         * <h1>Signal Control</h1>
         * <div class="unit">TrafficLightSystem · SignalHead</div>
         * </div>
         * <div id="conn"><span class="dot"></span><span id="connLabel">Connecting</span></div>
         * </header>
         *
         * <div class="center">
         * <div class="mount">
         * <div class="arm"></div>
         * <div class="bolt-row"><div class="bolt"></div><div class="bolt"></div></div>
         * </div>
         *
         * <div class="head" role="img" aria-label="Traffic signal head">
         * <div class="lens-wrap">
         * <div class="visor"></div>
         * <div class="lens red" id="lensRed"><div class="glass"></div></div>
         * </div>
         * <div class="lens-wrap">
         * <div class="visor"></div>
         * <div class="lens amber" id="lensAmber"><div class="glass"></div></div>
         * </div>
         * <div class="lens-wrap">
         * <div class="visor"></div>
         * <div class="lens green" id="lensGreen"><div class="glass"></div></div>
         * </div>
         * </div>
         * </div>
         *
         * <div class="panel">
         * <h2>Simulation Controls</h2>
         * <div class="run-row">
         * <button class="primary" id="btnStart" aria-label="Start">
         * <svg viewBox="0 0 24 24"><polygon points="6 3 20 12 6 21 6 3"/></svg>
         * Start
         * </button>
         * <button id="btnPause" aria-label="Pause">
         * <svg viewBox="0 0 24 24"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>
         * Pause
         * </button>
         * <button id="btnResume" aria-label="Resume">
         * <svg viewBox="0 0 24 24"><path d="M3 12a9 9 0 1 1 3.2 6.9"/><polyline points="3 17 3 12 8 12"/></svg>
         * Resume
         * </button>
         * <button id="btnStep" aria-label="Step">
         * <svg viewBox="0 0 24 24"><polygon points="4 4 14 12 4 20 4 4"/><rect x="17" y="4" width="3" height="16"/></svg>
         * Step
         * </button>
         * <button class="danger" id="btnTerminate" aria-label="Stop">
         * <svg viewBox="0 0 24 24"><rect x="5" y="5" width="14" height="14" rx="1.5"/></svg>
         * Stop
         * </button>
         * </div>
         * </div>
         *
         *
         * </div>
         *
         * <script>
         * (function(){
         * "use strict";
         *
         * var ROOT = "TrafficLightSystem.signalHead";
         * var LAMPS = ["RED", "AMBER", "GREEN"];
         * var lensEls = { RED: document.getElementById("lensRed"), AMBER: document.getElementById("lensAmber"), GREEN: document.getElementById("lensGreen") };
         * var connEl = document.getElementById("conn");
         * var connLabel = document.getElementById("connLabel");
         *
         * function setConn(state){
         * connEl.classList.remove("up","down");
         * if(state === "up"){ connEl.classList.add("up"); connLabel.textContent = "Live"; }
         * else if(state === "down"){ connEl.classList.add("down"); connLabel.textContent = "No Sim"; }
         * else { connLabel.textContent = "Connecting"; }
         * }
         *
         * function apiReady(){ return !!(window.api); }
         *
         * // ---- Promise wrappers around the JxBrowser bridge ----
         * function fetchActiveStates(path){
         * return new Promise(function(resolve){
         * if(!apiReady()){ resolve(null); return; }
         * window.api.getActiveStatesByDotNotation(window, resolve, path);
         * });
         * }
         *
         * // Recursively search an ActiveStateNode tree for a leaf named "On" / "Off"
         * function findLampState(node){
         * if(!node) return null;
         * if(node.name === "On" || node.name === "Off") return node.name;
         * if(Array.isArray(node.children)){
         * for(var i = 0; i < node.children.length; i++){
         * var found = findLampState(node.children[i]);
         * if(found) return found;
         * }
         * }
         * return null;
         * }
         *
         * function applyLampState(lampId, isOn){
         * var el = lensEls[lampId];
         * if(!el) return;
         * el.classList.toggle("lit", !!isOn);
         * }
         *
         * // ---- Run control buttons ----
         * document.getElementById("btnStart").addEventListener("click", function(){
         * if(apiReady()){ if(window.api.run) window.api.run(); if(window.api.start) window.api.start(); }
         * });
         * document.getElementById("btnPause").addEventListener("click", function(){
         * if(apiReady() && window.api.pause) window.api.pause();
         * });
         * document.getElementById("btnResume").addEventListener("click", function(){
         * if(apiReady() && window.api.resume) window.api.resume();
         * });
         * document.getElementById("btnStep").addEventListener("click", function(){
         * if(apiReady() && window.api.step) window.api.step();
         * });
         * document.getElementById("btnTerminate").addEventListener("click", function(){
         * if(apiReady() && window.api.terminate) window.api.terminate();
         * });
         *
         * // ---- Simulation -> UI sync ----
         * var syncing = false;
         * window.update = async function(){
         * if(syncing || !apiReady()) { setConn("down"); return; }
         * syncing = true;
         * try{
         * var results = await Promise.all(LAMPS.map(function(id){
         * return fetchActiveStates(ROOT + "." + id);
         * }));
         * var anyData = false;
         * results.forEach(function(raw, i){
         * var lampId = LAMPS[i];
         * if(!raw) return;
         * anyData = true;
         * try{
         * var parsed = JSON.parse(raw);
         * var node = Array.isArray(parsed) ? parsed[0] : parsed;
         * var stateName = findLampState(node);
         * if(stateName) applyLampState(lampId, stateName === "On");
         * }catch(e){ }
         * });
         * setConn(anyData ? "up" : "down");
         * }catch(err){
         * setConn("down");
         * }finally{
         * syncing = false;
         * }
         * };
         *
         * window.log = async function(message){
         * // Reserved for future console panel; currently a no-op to keep the UI minimal.
         * };
         *
         * setConn("connecting");
         * document.addEventListener("DOMContentLoaded", function(){
         * if(apiReady() && window.api.run) window.api.run();
         * });
         * })();
         * </script>
         *
         * </body>
         * </html>
         */
    }
    item RollpupUI_HTML {
        language "HTML"
        /* <!DOCTYPE html>
         * <html lang="en">
         * <head>
         * <meta charset="UTF-8" />
         * <meta name="viewport" content="width=device-width, initial-scale=1.0" />
         * <title>DynamicPowerRollup — Mission Console</title>
         * <link rel="preconnect" href="https://fonts.googleapis.com">
         * <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=IBM+Plex+Mono:wght@400;500;600&display=swap" rel="stylesheet">
         * <style>
         * :root{
         * --bg-deep:#080c16;
         * --bg-panel:#111a2b;
         * --bg-panel-raised:#141f34;
         * --border:#223351;
         * --border-soft:#1a2740;
         * --text-hi:#e9eefb;
         * --text-mid:#93a2c2;
         * --text-dim:#5c6b8a;
         * --amber:#f2a65a;
         * --amber-dim:#5a4326;
         * --cyan:#4fd1d9;
         * --cyan-dim:#1c3a3d;
         * --danger:#e85d5d;
         * --ok:#6fd08c;
         * --font-display:'Space Grotesk', sans-serif;
         * --font-mono:'IBM Plex Mono', monospace;
         * }
         * {box-sizing:border-box;}
         * html,body{margin:0;padding:0;}
         * body{
         * background:
         * radial-gradient(ellipse 1200px 600px at 50% -10%, #10203a 0%, var(--bg-deep) 60%);
         * color:var(--text-hi);
         * font-family:var(--font-mono);
         * min-height:100vh;
         * padding:28px 20px 60px;
         * }
         * .wrap{max-width:1180px;margin:0 auto;}
         *
         * header{
         * display:flex;justify-content:space-between;align-items:flex-start;
         * flex-wrap:wrap;gap:16px;
         * border-bottom:1px solid var(--border-soft);
         * padding-bottom:18px;margin-bottom:26px;
         * }
         * .title-block .eyebrow{
         * font-family:var(--font-mono);font-size:11px;letter-spacing:.18em;
         * text-transform:uppercase;color:var(--cyan);margin:0 0 6px;
         * }
         * .title-block h1{
         * font-family:var(--font-display);font-weight:700;font-size:28px;margin:0;
         * letter-spacing:-0.01em;
         * }
         * .title-block .sub{color:var(--text-dim);font-size:12.5px;margin-top:6px;}
         *
         * .status-cluster{display:flex;gap:10px;align-items:center;flex-wrap:wrap;}
         * .pill{
         * display:flex;align-items:center;gap:7px;
         * background:var(--bg-panel);border:1px solid var(--border);
         * padding:7px 12px;border-radius:20px;font-size:12px;color:var(--text-mid);
         * }
         * .dot{width:7px;height:7px;border-radius:50%;background:var(--text-dim);}
         * .dot.live{background:var(--ok);box-shadow:0 0 8px var(--ok);animation:pulse 1.6s infinite;}
         * .dot.waiting{background:var(--amber);box-shadow:0 0 8px var(--amber);}
         * @keyframes pulse{0%,100%{opacity:1}50%{opacity:.35}}
         *
         * .controls{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:26px;}
         * button{
         * font-family:var(--font-mono);font-size:12px;letter-spacing:.03em;
         * background:var(--bg-panel);color:var(--text-hi);
         * border:1px solid var(--border);border-radius:8px;
         * padding:9px 15px;cursor:pointer;transition:.15s ease;
         * }
         * button:hover{border-color:var(--cyan);color:var(--cyan);}
         * button:active{transform:translateY(1px);}
         * button.primary{background:var(--amber);color:#20140a;border-color:var(--amber);font-weight:600;}
         * button.primary:hover{filter:brightness(1.08);color:#20140a;}
         * button.ghost{opacity:.75;}
         * button.danger:hover{border-color:var(--danger);color:var(--danger);}
         *
         * .grid{display:grid;grid-template-columns:340px 1fr;gap:20px;align-items:start;}
         * @media (max-width:920px){.grid{grid-template-columns:1fr;}}
         *
         * .panel{
         * background:var(--bg-panel);border:1px solid var(--border-soft);
         * border-radius:14px;padding:20px;
         * }
         * .panel h2{
         * font-family:var(--font-display);font-size:13px;font-weight:600;
         * text-transform:uppercase;letter-spacing:.1em;color:var(--text-mid);
         * margin:0 0 16px;
         * }
         *
         * .power-slider-wrap{display:flex;flex-direction:column;gap:16px;}
         * .slider-header{display:flex;justify-content:space-between;align-items:baseline;}
         * .slider-label{font-size:13px;color:var(--text-mid);}
         * .slider-value{font-family:var(--font-display);font-size:28px;font-weight:700;color:var(--text-hi);}
         * .slider-unit{font-size:11px;color:var(--text-dim);letter-spacing:.08em;}
         *
         * .slider-container{position:relative;padding:10px 0;}
         * input[type="range"]{
         * width:100%;height:8px;border-radius:4px;background:linear-gradient(to right, var(--border-soft) 0%, var(--border-soft) 100%);
         * outline:none;-webkit-appearance:none;appearance:none;
         * cursor:pointer;
         * }
         * input[type="range"]::-webkit-slider-thumb{
         * -webkit-appearance:none;appearance:none;
         * width:20px;height:20px;border-radius:50%;background:var(--amber);
         * border:2px solid #20140a;cursor:pointer;box-shadow:0 0 12px var(--amber);
         * transition:.1s ease;
         * }
         * input[type="range"]::-webkit-slider-thumb:hover{
         * transform:scale(1.15);box-shadow:0 0 16px var(--amber);
         * }
         * input[type="range"]::-moz-range-thumb{
         * width:20px;height:20px;border-radius:50%;background:var(--amber);
         * border:2px solid #20140a;cursor:pointer;box-shadow:0 0 12px var(--amber);
         * transition:.1s ease;
         * }
         * input[type="range"]::-moz-range-thumb:hover{
         * transform:scale(1.15);box-shadow:0 0 16px var(--amber);
         * }
         *
         * .slider-stats{display:flex;justify-content:space-between;font-size:11px;color:var(--text-dim);}
         *
         * .req-badge{
         * margin-top:16px;width:100%;text-align:center;
         * padding:9px 0;border-radius:8px;font-size:11.5px;letter-spacing:.09em;
         * text-transform:uppercase;font-weight:600;border:1px solid var(--border);
         * color:var(--text-dim);background:var(--bg-panel-raised);
         * }
         * .req-badge.pass{color:var(--ok);border-color:#1f4a34;background:#0e2119;}
         * .req-badge.fail{color:var(--danger);border-color:#4a1f1f;background:#241010;}
         *
         *
         * .tree-panel{padding-bottom:8px;}
         * svg{display:block;width:100%;height:auto;}
         * .node-box{fill:var(--bg-panel-raised);stroke:var(--border);stroke-width:1;}
         * .node-box.on{stroke:var(--amber);}
         * .node-label{font-family:var(--font-display);font-size:12.5px;font-weight:600;fill:var(--text-hi);}
         * .node-power{font-family:var(--font-mono);font-size:15px;font-weight:600;fill:var(--text-mid);}
         * .node-power.on{fill:var(--amber);}
         * .node-sub{font-family:var(--font-mono);font-size:9.5px;fill:var(--text-dim);letter-spacing:.06em;}
         * .edge{stroke:var(--border);stroke-width:1.5;fill:none;}
         * .edge.flowing{stroke:var(--amber);stroke-dasharray:5 4;animation:flow 0.7s linear infinite;}
         * @keyframes flow{to{stroke-dashoffset:-18;}}
         *
         * .leaf-controls{
         * display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));
         * gap:12px;margin-top:22px;
         * }
         * .leaf-card{
         * background:var(--bg-panel-raised);border:1px solid var(--border-soft);
         * border-radius:10px;padding:14px 14px 12px;
         * }
         * .leaf-card .lc-top{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:2px;}
         * .leaf-card .lc-name{font-family:var(--font-display);font-size:13.5px;font-weight:600;}
         * .leaf-card .lc-state{
         * font-size:10px;letter-spacing:.08em;text-transform:uppercase;
         * padding:2px 7px;border-radius:10px;background:var(--bg-panel);color:var(--text-dim);border:1px solid var(--border);
         * }
         * .leaf-card .lc-state.on{color:var(--amber);border-color:var(--amber-dim);background:#20160a;}
         * .leaf-card .lc-power{font-size:20px;color:var(--text-mid);margin:4px 0 10px;}
         * .leaf-card .lc-power.on{color:var(--amber);}
         * .leaf-card .lc-power .u{font-size:11px;color:var(--text-dim);}
         * .leaf-card .lc-btns{display:flex;gap:8px;}
         * .leaf-card .lc-btns button{flex:1;padding:7px 0;font-size:11px;}
         * </style>
         * </head>
         * <body>
         * <div class="wrap">
         *
         * <header>
         * <div class="title-block">
         * <p class="eyebrow">Dynamic Power Rollup · Mission Scenario</p>
         * <h1>Spacecraft Power Rollup</h1>
         * <div class="sub">Live telemetry from the SysML v2 simulation engine</div>
         * </div>
         * <div class="status-cluster">
         * <div class="pill"><span class="dot" id="conn-dot"></span><span id="conn-label">Awaiting engine…</span></div>
         * </div>
         * </header>
         *
         * <div class="controls">
         * <button id="btn-run">▶▶ Run</button>
         * <button id="btn-start">▶ Start</button>
         * <button id="btn-pause">⏸ Pause</button>
         * <button id="btn-resume">↻ Resume</button>
         * <button class="danger" id="btn-terminate">■ Terminate</button>
         * </div>
         *
         * <div class="grid">
         * <!-- LEFT COLUMN -->
         * <div>
         * <div class="panel">
         * <h2>Power Budget</h2>
         * <div class="power-slider-wrap">
         * <div class="slider-header">
         * <span class="slider-label">Current Draw</span>
         * <div>
         * <div class="slider-value" id="power-total">—</div>
         * <span class="slider-unit">WATTS</span>
         * </div>
         * </div>
         * <div class="slider-container">
         * <input type="range" id="power-slider" min="0" max="100" value="0" disabled>
         * </div>
         * <div class="slider-stats">
         * <span id="power-min">0 W</span>
         * <span id="power-max">— W</span>
         * </div>
         * </div>
         *
         * <div class="req-badge" id="req-badge">Requirement: maxPower — awaiting data</div>
         *
         *
         * </div>
         * </div>
         *
         * <!-- RIGHT COLUMN -->
         * <div>
         * <div class="panel tree-panel">
         * <h2>Rollup Tree</h2>
         * <svg viewBox="0 0 720 260" id="tree-svg">
         * <!-- edges -->
         * <path class="edge" id="e-sc-tel" d="M 360 40 C 300 70, 210 70, 175 100"/>
         * <path class="edge" id="e-sc-pro" d="M 360 40 C 420 70, 510 70, 545 100"/>
         * <path class="edge" id="e-tel-amp" d="M 175 130 C 150 155, 110 155, 90 190"/>
         * <path class="edge" id="e-tel-ant" d="M 175 130 C 190 155, 220 155, 235 190"/>
         * <path class="edge" id="e-pro-tank" d="M 545 130 C 510 155, 470 155, 450 190"/>
         * <path class="edge" id="e-pro-thr" d="M 545 130 C 570 155, 610 155, 625 190"/>
         *
         * <!-- spaceCraft -->
         * <g id="node-spaceCraft" transform="translate(290,10)">
         * <rect class="node-box" x="0" y="0" width="140" height="42" rx="8"/>
         * <text class="node-label" x="14" y="17">spaceCraft</text>
         * <text class="node-power" id="p-spaceCraft" x="14" y="34">0.0 W</text>
         * </g>
         * <!-- telecom -->
         * <g id="node-telecom" transform="translate(105,100)">
         * <rect class="node-box" x="0" y="0" width="140" height="42" rx="8"/>
         * <text class="node-label" x="14" y="17">telecom</text>
         * <text class="node-power" id="p-telecom" x="14" y="34">0.0 W</text>
         * </g>
         * <!-- propulsion -->
         * <g id="node-propulsion" transform="translate(475,100)">
         * <rect class="node-box" x="0" y="0" width="140" height="42" rx="8"/>
         * <text class="node-label" x="14" y="17">propulsion</text>
         * <text class="node-power" id="p-propulsion" x="14" y="34">0.0 W</text>
         * </g>
         * <!-- amplifier -->
         * <g id="node-amplifier" transform="translate(20,190)">
         * <rect class="node-box" x="0" y="0" width="132" height="42" rx="8"/>
         * <text class="node-label" x="12" y="17">amplifier</text>
         * <text class="node-power" id="p-amplifier" x="12" y="34">0.0 W</text>
         * <text class="node-sub" id="s-amplifier" x="118" y="17" text-anchor="end">OFF</text>
         * </g>
         * <!-- antenna -->
         * <g id="node-antenna" transform="translate(168,190)">
         * <rect class="node-box" x="0" y="0" width="132" height="42" rx="8"/>
         * <text class="node-label" x="12" y="17">antenna</text>
         * <text class="node-power" id="p-antenna" x="12" y="34">0.0 W</text>
         * <text class="node-sub" id="s-antenna" x="118" y="17" text-anchor="end">OFF</text>
         * </g>
         * <!-- tank -->
         * <g id="node-tank" transform="translate(384,190)">
         * <rect class="node-box" x="0" y="0" width="132" height="42" rx="8"/>
         * <text class="node-label" x="12" y="17">tank</text>
         * <text class="node-power" id="p-tank" x="12" y="34">0.0 W</text>
         * <text class="node-sub" id="s-tank" x="118" y="17" text-anchor="end">OFF</text>
         * </g>
         * <!-- thruster -->
         * <g id="node-thruster" transform="translate(532,190)">
         * <rect class="node-box" x="0" y="0" width="132" height="42" rx="8"/>
         * <text class="node-label" x="12" y="17">thruster</text>
         * <text class="node-power" id="p-thruster" x="12" y="34">0.0 W</text>
         * <text class="node-sub" id="s-thruster" x="118" y="17" text-anchor="end">OFF</text>
         * </g>
         * </svg>
         *
         * <div class="leaf-controls">
         * <div class="leaf-card" data-node="amplifier" data-path="MissionScenario.spaceCraft.telecom.amplifier">
         * <div class="lc-top"><span class="lc-name">Amplifier</span><span class="lc-state">OFF</span></div>
         * <div class="lc-power">— <span class="u">W</span></div>
         * <div class="lc-btns">
         * <button class="btn-on">turnOn</button>
         * <button class="btn-off">turnOff</button>
         * </div>
         * </div>
         * <div class="leaf-card" data-node="antenna" data-path="MissionScenario.spaceCraft.telecom.antenna">
         * <div class="lc-top"><span class="lc-name">Antenna</span><span class="lc-state">OFF</span></div>
         * <div class="lc-power">— <span class="u">W</span></div>
         * <div class="lc-btns">
         * <button class="btn-on">turnOn</button>
         * <button class="btn-off">turnOff</button>
         * </div>
         * </div>
         * <div class="leaf-card" data-node="tank" data-path="MissionScenario.spaceCraft.propulsion.tank">
         * <div class="lc-top"><span class="lc-name">Tank</span><span class="lc-state">OFF</span></div>
         * <div class="lc-power">— <span class="u">W</span></div>
         * <div class="lc-btns">
         * <button class="btn-on">turnOn</button>
         * <button class="btn-off">turnOff</button>
         * </div>
         * </div>
         * <div class="leaf-card" data-node="thruster" data-path="MissionScenario.spaceCraft.propulsion.thruster">
         * <div class="lc-top"><span class="lc-name">Thruster</span><span class="lc-state">OFF</span></div>
         * <div class="lc-power">— <span class="u">W</span></div>
         * <div class="lc-btns">
         * <button class="btn-on">turnOn</button>
         * <button class="btn-off">turnOff</button>
         * </div>
         * </div>
         * </div>
         * </div>
         * </div>
         *
         * </div>
         *
         * </div>
         *
         * <script>
         * (() => {
         * // ---------- Path constants (relative to executionTarget: MissionScenario) ----------
         * const PATHS = {
         * spaceCraft:   'MissionScenario.spaceCraft',
         * telecom:      'MissionScenario.spaceCraft.telecom',
         * propulsion:   'MissionScenario.spaceCraft.propulsion',
         * amplifier:    'MissionScenario.spaceCraft.telecom.amplifier',
         * antenna:      'MissionScenario.spaceCraft.telecom.antenna',
         * tank:         'MissionScenario.spaceCraft.propulsion.tank',
         * thruster:     'MissionScenario.spaceCraft.propulsion.thruster',
         * };
         * const ROOT = ''; // root-level attributes of MissionScenario
         *
         * // ---------- Promise wrappers around window.api ----------
         * const getFeature = (name, parentPath) => new Promise((resolve) => {
         * if (window.api && window.api.getFeatureValuesByDotNotation) {
         * window.api.getFeatureValuesByDotNotation(window, resolve, name, parentPath);
         * } else {
         * resolve(null);
         * }
         * });
         * const getStates = (parentPath) => new Promise((resolve) => {
         * if (window.api && window.api.getActiveStatesByDotNotation) {
         * window.api.getActiveStatesByDotNotation(window, resolve, parentPath);
         * } else {
         * resolve(null);
         * }
         * });
         * const fire = (payloadName, receiverPath) => {
         * if (window.api && window.api.firePayloadEventByName) {
         * window.api.firePayloadEventByName(payloadName, receiverPath);
         * }
         * };
         *
         * function firstStateName(json) {
         * if (!json) return null;
         * try {
         * const arr = typeof json === 'string' ? JSON.parse(json) : json;
         * if (Array.isArray(arr) && arr.length > 0) return arr[0].name;
         * } catch (e) { }
         * return null;
         * }
         *
         * const connDot = document.getElementById('conn-dot');
         * const connLabel = document.getElementById('conn-label');
         * function setConn(state, label) {
         * connDot.className = 'dot ' + state;
         * connLabel.textContent = label;
         * }
         *
         * document.getElementById('btn-run').onclick = () => {
         * if (window.api) { window.api.run(); setConn('live', 'Simulation running'); }
         * else console.log('window.api not available yet');
         * };
         * document.getElementById('btn-start').onclick = () => {
         * if (window.api) { window.api.start(); setConn('live', 'Simulation running'); }
         * else console.log('window.api not available yet');
         * };
         * document.getElementById('btn-pause').onclick = () => {
         * if (window.api) { window.api.pause(); setConn('waiting', 'Paused'); }
         * };
         * document.getElementById('btn-resume').onclick = () => {
         * if (window.api) { window.api.resume(); setConn('live', 'Simulation running'); }
         * };
         * document.getElementById('btn-terminate').onclick = () => {
         * if (window.api) { window.api.terminate(); setConn('', 'Terminated'); }
         * };
         *
         * document.querySelectorAll('.leaf-card').forEach(card => {
         * const path = card.dataset.path;
         * const node = card.dataset.node;
         * card.querySelector('.btn-on').onclick = () => { fire('turnOn', path); };
         * card.querySelector('.btn-off').onclick = () => { fire('turnOff', path); };
         * });
         *
         * const slider = document.getElementById('power-slider');
         * function setSlider(value, max) {
         * const v = Number(value) || 0;
         * const m = Number(max) || 1;
         * slider.max = String(m);
         * slider.value = String(v);
         * slider.style.background = `linear-gradient(to right, var(--amber) 0%, var(--amber) ${(v/m)*100}%, var(--border-soft) ${(v/m)*100}%, var(--border-soft) 100%)`;
         * if (v > m) slider.style.borderColor = 'var(--danger)';
         * }
         *
         * function setNodePower(id, value, isLeaf) {
         * const el = document.getElementById('p-' + id);
         * if (!el) return;
         * const v = value === null || value === undefined ? null : Number(value);
         * el.textContent = (v === null ? '—' : v.toFixed(1)) + ' W';
         * el.classList.toggle('on', !!v);
         * const box = document.getElementById('node-' + id);
         * if (box) box.querySelector('.node-box').classList.toggle('on', !!v);
         * }
         * function setLeafState(id, stateName) {
         * const on = stateName === 'On';
         * const subEl = document.getElementById('s-' + id);
         * if (subEl) { subEl.textContent = stateName || '—'; subEl.classList.toggle('on', on); }
         * const card = document.querySelector(`.leaf-card[data-node="${id}"]`);
         * if (card) {
         * card.querySelector('.lc-state').textContent = stateName || '—';
         * card.querySelector('.lc-state').classList.toggle('on', on);
         * }
         * }
         * function setLeafPower(id, value) {
         * const card = document.querySelector(`.leaf-card[data-node="${id}"]`);
         * if (!card) return;
         * const v = value === null || value === undefined ? null : Number(value);
         * const p = card.querySelector('.lc-power');
         * p.innerHTML = (v === null ? '—' : v.toFixed(1)) + ' <span class="u">W</span>';
         * p.classList.toggle('on', !!v);
         * }
         * function setEdgeFlow(edgeId, flowing) {
         * const el = document.getElementById(edgeId);
         * if (el) el.classList.toggle('flowing', !!flowing);
         * }
         *
         *
         * let updating = false;
         * window.update = async () => {
         * if (updating || !window.api) return;
         * updating = true;
         * try {
         * setConn('live', 'Simulation running');
         *
         * const [
         * scPower, telPower, proPower,
         * ampPower, antPower, tankPower, thrPower,
         * ampStateJson, antStateJson, tankStateJson, thrStateJson,
         * maxRestriction
         * ] = await Promise.all([
         * getFeature('power', PATHS.spaceCraft),
         * getFeature('power', PATHS.telecom),
         * getFeature('power', PATHS.propulsion),
         * getFeature('power', PATHS.amplifier),
         * getFeature('power', PATHS.antenna),
         * getFeature('power', PATHS.tank),
         * getFeature('power', PATHS.thruster),
         * getStates(PATHS.amplifier),
         * getStates(PATHS.antenna),
         * getStates(PATHS.tank),
         * getStates(PATHS.thruster),
         * getFeature('maxPowerRestriction', 'MissionScenario'),
         * ]);
         *
         * // Tree + rollups
         * setNodePower('spaceCraft', scPower);
         * setNodePower('telecom', telPower);
         * setNodePower('propulsion', proPower);
         * setNodePower('amplifier', ampPower);
         * setNodePower('antenna', antPower);
         * setNodePower('tank', tankPower);
         * setNodePower('thruster', thrPower);
         *
         * const ampOn = Number(ampPower) > 0, antOn = Number(antPower) > 0;
         * const tankOn = Number(tankPower) > 0, thrOn = Number(thrPower) > 0;
         * setEdgeFlow('e-tel-amp', ampOn);
         * setEdgeFlow('e-tel-ant', antOn);
         * setEdgeFlow('e-pro-tank', tankOn);
         * setEdgeFlow('e-pro-thr', thrOn);
         * setEdgeFlow('e-sc-tel', ampOn || antOn);
         * setEdgeFlow('e-sc-pro', tankOn || thrOn);
         *
         * // Leaf state badges (from Operating state machine)
         * setLeafState('amplifier', firstStateName(ampStateJson));
         * setLeafState('antenna', firstStateName(antStateJson));
         * setLeafState('tank', firstStateName(tankStateJson));
         * setLeafState('thruster', firstStateName(thrStateJson));
         * setLeafPower('amplifier', ampPower);
         * setLeafPower('antenna', antPower);
         * setLeafPower('tank', tankPower);
         * setLeafPower('thruster', thrPower);
         *
         * // Slider + requirement
         * document.getElementById('power-total').textContent = scPower !== null ? Number(scPower).toFixed(1) : '—';
         * document.getElementById('power-max').textContent = maxRestriction !== null ? Number(maxRestriction).toFixed(0) + ' W' : '— W';
         * setSlider(scPower, maxRestriction);
         *
         * const reqBadge = document.getElementById('req-badge');
         * if (maxRestriction === null || scPower === null) {
         * reqBadge.textContent = 'Requirement: maxPower — awaiting data';
         * reqBadge.className = 'req-badge';
         * } else {
         * const satisfied = Number(scPower) <= Number(maxRestriction);
         * reqBadge.textContent = `Requirement: maxPower — ${satisfied ? 'SATISFIED' : 'VIOLATED'}`;
         * reqBadge.className = 'req-badge ' + (satisfied ? 'pass' : 'fail');
         * }
         *
         *
         *
         * } catch (err) {
         * console.error('update() error:', err);
         * } finally {
         * updating = false;
         * }
         * };
         *
         * // Initial state before the engine connects
         * setConn('', 'Awaiting engine…');
         * })();
         * </script>
         * </body>
         * </html>
         */
    }
    item FlightControlUI_HTML {
        language "HTML"
        /* <!DOCTYPE html>
         * <html lang='en'>
         * <head>
         * <meta charset='UTF-8'>
         * <meta name='viewport' content='width=device-width, initial-scale=1.0'>
         * <title>Flight Control Systems - Mission Cockpit</title>
         * <style>
         * :root {
         * --bg-color: #0b0f19;
         * --panel-bg: #1e293b;
         * --card-bg: #0f172a;
         * --text-main: #f8fafc;
         * --text-dim: #94a3b8;
         * --border: #334155;
         * --primary: #38bdf8;
         * --accent: #f59e0b;
         * --danger: #ef4444;
         * --success: #10b981;
         * }
         *
         * body {
         * font-family: 'Inter', system-ui, -apple-system, sans-serif;
         * background-color: var(--bg-color);
         * color: var(--text-main);
         * margin: 0;
         * padding: 30px;
         * display: flex;
         * flex-direction: column;
         * align-items: center;
         * min-height: 100vh;
         * gap: 20px;
         * }
         *
         * header {
         * width: 100%;
         * max-width: 1000px;
         * border-bottom: 1px solid var(--border);
         * padding-bottom: 10px;
         * margin-bottom: 10px;
         * }
         *
         * header h1 {
         * margin: 0;
         * font-size: 1.5rem;
         * letter-spacing: 0.05em;
         * text-transform: uppercase;
         * color: var(--primary);
         * }
         *
         * header p {
         * margin: 5px 0 0 0;
         * font-size: 0.85rem;
         * color: var(--text-dim);
         * }
         *
         * .dashboard-grid {
         * display: grid;
         * grid-template-columns: 340px 1fr;
         * gap: 24px;
         * width: 100%;
         * max-width: 1000px;
         * }
         *
         * @media (max-width: 850px) {
         * .dashboard-grid { grid-template-columns: 1fr; }
         * }
         *
         * .panel {
         * background-color: var(--panel-bg);
         * border: 1px solid var(--border);
         * border-radius: 12px;
         * padding: 20px;
         * box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.3);
         * display: flex;
         * flex-direction: column;
         * gap: 20px;
         * }
         *
         * .panel-title {
         * margin: 0;
         * font-size: 0.9rem;
         * text-transform: uppercase;
         * letter-spacing: 0.1em;
         * color: var(--text-dim);
         * border-bottom: 1px solid var(--border);
         * padding-bottom: 8px;
         * font-weight: 700;
         * }
         *
         * .control-section {
         * display: flex;
         * flex-direction: column;
         * gap: 12px;
         * }
         *
         * .control-group label {
         * display: block;
         * margin-bottom: 6px;
         * font-size: 0.8rem;
         * font-weight: 600;
         * color: var(--text-main);
         * }
         *
         * .input-wrapper { display: flex; gap: 8px; }
         *
         * input[type='number'] {
         * background: var(--card-bg);
         * border: 1px solid var(--border);
         * color: var(--text-main);
         * padding: 10px;
         * border-radius: 6px;
         * width: 100%;
         * outline: none;
         * font-weight: bold;
         * }
         *
         * input[type='number']:focus { border-color: var(--primary); }
         *
         * button {
         * background-color: var(--primary);
         * color: #000;
         * border: none;
         * padding: 10px 14px;
         * border-radius: 6px;
         * cursor: pointer;
         * font-weight: 700;
         * transition: all 0.2s ease;
         * white-space: nowrap;
         * }
         *
         * button:hover { opacity: 0.9; transform: translateY(-1px); }
         *
         * .btn-signal {
         * background-color: var(--card-bg);
         * color: var(--text-main);
         * border: 1px solid var(--border);
         * width: 100%;
         * text-align: left;
         * padding: 10px 12px;
         * font-size: 0.8rem;
         * display: flex;
         * justify-content: space-between;
         * align-items: center;
         * border-radius: 6px;
         * cursor: pointer;
         * font-weight: 500;
         * transition: all 0.2s ease;
         * }
         *
         * .btn-signal:hover {
         * background-color: var(--border);
         * border-color: var(--primary);
         * color: var(--primary);
         * }
         *
         * .btn-signal::after { content: '⏵'; font-size: 0.7rem; color: var(--primary); }
         *
         * .monitor-layout {
         * display: grid;
         * grid-template-columns: 1fr 140px;
         * gap: 20px;
         * }
         *
         * .telemetry-cards { display: flex; flex-direction: column; gap: 12px; }
         *
         * .telemetry-card {
         * background: var(--card-bg);
         * border: 1px solid var(--border);
         * border-radius: 8px;
         * padding: 14px;
         * display: flex;
         * justify-content: space-between;
         * align-items: center;
         * transition: all 0.3s ease;
         * }
         *
         * .telemetry-meta h4 {
         * margin: 0;
         * font-size: 0.75rem;
         * text-transform: uppercase;
         * color: var(--text-dim);
         * letter-spacing: 0.05em;
         * }
         *
         * .telemetry-meta p { margin: 4px 0 0 0; font-size: 0.7rem; color: #475569; }
         *
         * .telemetry-value {
         * font-size: 1.4rem;
         * font-weight: 800;
         * font-family: monospace;
         * color: var(--text-main);
         * }
         *
         * .state-badge {
         * padding: 6px 14px;
         * border-radius: 4px;
         * font-size: 0.9rem;
         * font-weight: 800;
         * text-transform: uppercase;
         * background: #334155;
         * color: #94a3b8;
         * }
         *
         * .state-off { background-color: #1e293b; color: #64748b; }
         * .state-takeoff { background-color: #1e3a8a; color: #38bdf8; box-shadow: 0 0 12px rgba(56, 189, 248, 0.3); }
         * .state-climb { background-color: #0369a1; color: #f0f9ff; box-shadow: 0 0 15px rgba(3, 105, 161, 0.5); }
         * .state-hold { background-color: #065f46; color: #34d399; box-shadow: 0 0 12px rgba(52, 211, 153, 0.3); }
         * .state-descend { background-color: #7c2d12; color: #fb923c; box-shadow: 0 0 12px rgba(251, 146, 60, 0.3); }
         * .state-land { background-color: #991b1b; color: #fca5a5; box-shadow: 0 0 15px rgba(239, 68, 68, 0.4); }
         *
         * .soc-container {
         * width: 90px;
         * height: 14px;
         * background: #334155;
         * border: 1px solid var(--border);
         * border-radius: 3px;
         * padding: 2px;
         * box-sizing: border-box;
         * }
         * .soc-bar {
         * height: 100%;
         * background: var(--success);
         * border-radius: 1px;
         * width: 100%;
         * transition: width 0.2s ease, background-color 0.4s ease;
         * }
         * .soc-text {
         * font-family: monospace;
         * font-size: 1.1rem;
         * font-weight: bold;
         * margin-left: 8px;
         * min-width: 55px;
         * text-align: right;
         * }
         *
         * .verification-badge {
         * padding: 4px 10px;
         * border-radius: 4px;
         * font-size: 0.75rem;
         * font-weight: 800;
         * text-transform: uppercase;
         * font-family: monospace;
         * }
         * .verif-passed { background-color: rgba(16, 185, 129, 0.2); color: var(--success); border: 1px solid var(--success); }
         * .verif-failed { background-color: rgba(239, 68, 68, 0.2); color: var(--danger); border: 1px solid var(--danger); animation: pulse 1.5s infinite; }
         *
         * @keyframes pulse {
         * 0% { opacity: 1; }
         * 50% { opacity: 0.5; }
         * 100% { opacity: 1; }
         * }
         *
         * .altimeter-wrapper {
         * background: var(--card-bg);
         * border: 1px solid var(--border);
         * border-radius: 8px;
         * height: 100%;
         * min-height: 290px;
         * position: relative;
         * overflow: hidden;
         * display: flex;
         * flex-direction: column;
         * align-items: center;
         * }
         *
         * .altimeter-tape {
         * position: absolute;
         * width: 100%;
         * display: flex;
         * flex-direction: column;
         * align-items: center;
         * }
         *
         * .altimeter-tick {
         * height: 40px;
         * display: flex;
         * align-items: center;
         * justify-content: center;
         * font-family: monospace;
         * font-size: 0.75rem;
         * color: var(--text-dim);
         * width: 100%;
         * border-bottom: 1px dashed rgba(51, 65, 85, 0.3);
         * box-sizing: border-box;
         * }
         *
         * .altimeter-pointer {
         * position: absolute;
         * left: 0;
         * right: 0;
         * top: 125px;
         * margin: auto;
         * width: 92%;
         * height: 36px;
         * border: 2px solid var(--accent);
         * background: rgba(15, 23, 42, 0.9);
         * border-radius: 4px;
         * z-index: 10;
         * pointer-events: none;
         * display: flex;
         * align-items: center;
         * justify-content: flex-end;
         * padding-right: 6px;
         * box-sizing: border-box;
         * box-shadow: 0 4px 6px rgba(0,0,0,0.4);
         * }
         *
         * .altimeter-pointer::before {
         * content: 'ALT';
         * font-size: 0.65rem;
         * font-weight: 800;
         * color: var(--accent);
         * position: absolute;
         * left: 6px;
         * }
         *
         * .profile-status {
         * font-size: 0.75rem;
         * color: var(--accent);
         * margin-top: 4px;
         * font-weight: bold;
         * }
         * </style>
         * </head>
         * <body>
         *
         * <header>
         * <h1>Flight Control System Cockpit</h1>
         * <p>SysML v2 Execution Domain Pipeline</p>
         * </header>
         *
         * <main class='dashboard-grid'>
         * <section class='panel'>
         * <h2 class='panel-title'>Telemetry Environment</h2>
         *
         * <div class='control-section'>
         * <div class='control-group'>
         * <label for='altitude-input'>Manual Altitude Target (ft)</label>
         * <div class='input-wrapper'>
         * <input type='number' id='altitude-input' value='0' step='10'>
         * <button onclick='handleAction("SET_ALTITUDE")'>Set</button>
         * </div>
         * </div>
         *
         * <div class='control-group'>
         * <label for='pressure-input'>Closed-Loop Pressure Injector</label>
         * <div class='input-wrapper'>
         * <input type='number' id='pressure-input' value='101.325' step='0.1'>
         * <button id='btn-inject' onclick='handleAction("TOGGLE_CLOSED_LOOP")'>Auto tracking</button>
         * </div>
         * <div id='profile-phase' class='profile-status'>Status: Ready (Manual Mode)</div>
         * </div>
         * </div>
         *
         * <h2 class='panel-title'>Controller Signal Interface</h2>
         * <div class='control-section'>
         * <button class='btn-signal' onclick='handleAction("CTRL_TAKEOFF")'>TakeOffMsg ➔ Controller</button>
         * <button class='btn-signal' onclick='handleAction("CTRL_CLIMB")'>ClimbMsg ➔ Controller</button>
         * <button class='btn-signal' onclick='handleAction("CTRL_HOLD")'>HoldMsg ➔ Controller</button>
         * <button class='btn-signal' onclick='handleAction("CTRL_DESCEND")'>DescendMsg ➔ Controller</button>
         * </div>
         *
         * <h2 class='panel-title'>Processor Hardware Interface</h2>
         * <div class='control-section'>
         * <button class='btn-signal' onclick='handleAction("SIG_TAKEOFF")'>TakeOffMsg ➔ Processor</button>
         * <button class='btn-signal' onclick='handleAction("SIG_LAND_PROC")'>LandMsg ➔ Processor</button>
         * </div>
         * </section>
         *
         * <section class='panel'>
         * <h2 class='panel-title'>Flight Instrumentation</h2>
         *
         * <div class='monitor-layout'>
         * <div class='telemetry-cards'>
         *
         * <div class='telemetry-card'>
         * <div class='telemetry-meta'>
         * <h4>System State</h4>
         * <p>flightController.currentState</p>
         * </div>
         * <div id='display-state' class='state-badge state-off'>OFF</div>
         * </div>
         *
         * <div class='telemetry-card'>
         * <div class='telemetry-meta'>
         * <h4>Simulation Time</h4>
         * <p>FlightControlSystem.time</p>
         * </div>
         * <div id='display-sim-time' class='telemetry-value' style='color: var(--accent);'>0.00 s</div>
         * </div>
         *
         * <div class='telemetry-card'>
         * <div class='telemetry-meta'>
         * <h4>Battery Reserve</h4>
         * <p>FlightControlSystem.powerControlSystem.SOC</p>
         * </div>
         * <div style="display: flex; align-items: center;">
         * <div class="soc-container">
         * <div id="soc-fill" class="soc-bar"></div>
         * </div>
         * <span id="display-soc" class="soc-text">80.0%</span>
         * </div>
         * </div>
         *
         * <div id='verification-card' class='telemetry-card'>
         * <div class='telemetry-meta'>
         * <h4>Requirement battery_soc</h4>
         * <p>powerControlSystem.SOC &gt;= 10</p>
         * </div>
         * <div id='verification-status' class='verification-badge verif-passed'>PASSED</div>
         * </div>
         *
         * <div class='telemetry-card'>
         * <div class='telemetry-meta'>
         * <h4>Calculated Altitude</h4>
         * <p>flightController.altitude</p>
         * </div>
         * <div id='display-altitude' class='telemetry-value' style='color: var(--primary);'>0.00 ft</div>
         * </div>
         *
         * <div class='telemetry-card'>
         * <div class='telemetry-meta'>
         * <h4>Processor Pressure Register</h4>
         * <p>altitudeProcessor.pressure</p>
         * </div>
         * <div id='display-pressure' class='telemetry-value'>101.325</div>
         * </div>
         *
         * </div>
         *
         * <div class='altimeter-wrapper'>
         * <div class='altimeter-pointer'>
         * <strong id='pointer-val' style='color: var(--accent); font-family: monospace; font-size: 0.95rem;'>0</strong>
         * </div>
         * <div id='alt-tape' class='altimeter-tape'>
         * </div>
         * </div>
         * </div>
         * </section>
         * </main>
         *
         * <script>
         * let state = {
         * altitude: 0.0,
         * pressure: 101.325,
         * currentState: 'Off',
         * simTime: 0.0,
         * soc: 80.0
         * };
         * let isUpdating = false;
         * let isEditing = false;
         * let isClosedLoopActive = false;
         *
         * let altitudeInput, pressureInput;
         * window.addEventListener('DOMContentLoaded', () => {
         * altitudeInput = document.getElementById('altitude-input');
         * pressureInput = document.getElementById('pressure-input');
         *
         * altitudeInput.addEventListener('focus', () => { isEditing = true; });
         * altitudeInput.addEventListener('blur', () => { isEditing = false; });
         * pressureInput.addEventListener('focus', () => { isEditing = true; });
         * pressureInput.addEventListener('blur', () => { isEditing = false; });
         *
         * buildAltimeterTape();
         * updateUI();
         *
         * // Start simulation if available
         * if (window.api && window.api.run) {
         * window.api.run();
         * console.log('✓ Simulation started');
         * }
         * });
         *
         * const PATH_SYSTEM     = 'FlightControlSystem';
         * const PATH_CONTROLLER = 'FlightControlSystem.flightController';
         * const PATH_PROCESSOR  = 'FlightControlSystem.altitudeSensor.altitudeProcessor';
         * const PATH_POWER_SYS  = 'FlightControlSystem.powerControlSystem';
         *
         * const buildAltimeterTape = () => {
         * const tape = document.getElementById('alt-tape');
         * if (!tape) return;
         * tape.innerHTML = '';
         * for(let h = 6000; h >= 0; h -= 500) {
         * const tick = document.createElement('div');
         * tick.className = 'altimeter-tick';
         * tick.innerHTML = `<span>${h}</span>`;
         * tape.appendChild(tick);
         * }
         * };
         *
         * const updateUI = () => {
         * const displayAlt = document.getElementById('display-altitude');
         * const displayPress = document.getElementById('display-pressure');
         * const pointerVal = document.getElementById('pointer-val');
         * const stateBadge = document.getElementById('display-state');
         * const altTape = document.getElementById('alt-tape');
         * const displayClock = document.getElementById('display-sim-time');
         * const displaySocText = document.getElementById('display-soc');
         * const socBarFill = document.getElementById('soc-fill');
         * const verificationStatus = document.getElementById('verification-status');
         * const verificationCard = document.getElementById('verification-card');
         *
         * if (displayAlt) displayAlt.innerText = `${state.altitude.toFixed(2)} ft`;
         * if (displayPress) displayPress.innerText = `${state.pressure.toFixed(3)}`;
         * if (pointerVal) pointerVal.innerText = Math.round(state.altitude);
         * if (displayClock) displayClock.innerText = `${state.simTime.toFixed(2)} s`;
         *
         * if (displaySocText) displaySocText.innerText = `${state.soc.toFixed(1)}%`;
         * if (socBarFill) {
         * socBarFill.style.width = `${Math.min(100, Math.max(0, state.soc))}%`;
         * if (state.soc < 10) socBarFill.style.backgroundColor = 'var(--danger)';
         * else if (state.soc < 25) socBarFill.style.backgroundColor = 'var(--accent)';
         * else socBarFill.style.backgroundColor = 'var(--success)';
         * }
         *
         * if (verificationStatus && verificationCard) {
         * if (state.soc >= 10.0) {
         * verificationStatus.innerText = "PASSED";
         * verificationStatus.className = "verification-badge verif-passed";
         * verificationCard.style.borderColor = "var(--border)";
         * } else {
         * verificationStatus.innerText = "FAILED";
         * verificationStatus.className = "verification-badge verif-failed";
         * verificationCard.style.borderColor = "var(--danger)";
         * }
         * }
         *
         * if (stateBadge) {
         * stateBadge.innerText = state.currentState;
         * stateBadge.className = 'state-badge';
         * const normState = state.currentState.toLowerCase();
         * if (normState.includes('off')) stateBadge.classList.add('state-off');
         * else if (normState.includes('takeof')) stateBadge.classList.add('state-takeoff');
         * else if (normState.includes('climb')) stateBadge.classList.add('state-climb');
         * else if (normState.includes('hold')) stateBadge.classList.add('state-hold');
         * else if (normState.includes('desc')) stateBadge.classList.add('state-descend');
         * else if (normState.includes('land')) stateBadge.classList.add('state-land');
         * else stateBadge.classList.add('state-off');
         * }
         *
         * if (!isEditing) {
         * if (altitudeInput) altitudeInput.value = Math.round(state.altitude);
         * if (pressureInput) pressureInput.value = state.pressure.toFixed(3);
         * }
         *
         * if (altTape) {
         * const tapeTopOffset = 125 - 480 + ((state.altitude / 500) * 40);
         * altTape.style.top = `${tapeTopOffset}px`;
         * }
         * };
         *
         * const predictNextPressure = (currentState, currentPressure) => {
         * const normState = currentState.toLowerCase();
         * const groundPressure = 101.325;
         * const phaseLabel = document.getElementById('profile-phase');
         *
         * switch(true) {
         * case normState.includes('takeof'):
         * if (phaseLabel) phaseLabel.innerText = "Status: TakeOff - Reducing Pressure Profile";
         * return Math.max(98.0, currentPressure - 0.20);
         *
         * case normState.includes('climb'):
         * if (phaseLabel) phaseLabel.innerText = "Status: Climbing - Drop Pressure For Ceiling Target";
         * return Math.max(10.0, currentPressure - 0.45);
         *
         * case normState.includes('hold'):
         * if (phaseLabel) phaseLabel.innerText = "Status: Hold - Stabilizing Cruise Profile";
         * return currentPressure + (Math.sin(Date.now() / 1000) * 0.01);
         *
         * case normState.includes('desc'):
         * if (phaseLabel) phaseLabel.innerText = "Status: Emergency / Normal Descent - Increasing Pressure";
         * return Math.min(groundPressure, currentPressure + 0.35);
         *
         * case normState.includes('land'):
         * if (phaseLabel) phaseLabel.innerText = "Status: Landing - Ground Realignment Track";
         * return Math.min(groundPressure, currentPressure + 0.20);
         *
         * default:
         * return currentPressure;
         * }
         * };
         *
         * const handleAction = (actionType) => {
         * if (!window.api) {
         * console.error('❌ window.api not available - simulation not running');
         * return;
         * }
         *
         * if (actionType === 'SET_ALTITUDE' && altitudeInput) {
         * state.altitude = parseFloat(altitudeInput.value) || 0.0;
         * window.api.updateFeatureValuesByDotNotation('altitude', PATH_CONTROLLER, state.altitude);
         * console.log(`✓ Altitude set to ${state.altitude} ft`);
         * altitudeInput.blur();
         * updateUI();
         * }
         * else if (actionType === 'TOGGLE_CLOSED_LOOP') {
         * const trackBtn = document.getElementById('btn-inject');
         * const phaseLabel = document.getElementById('profile-phase');
         * isClosedLoopActive = !isClosedLoopActive;
         *
         * if (isClosedLoopActive) {
         * if (trackBtn) {
         * trackBtn.innerText = "Tracking Model Loop";
         * trackBtn.style.backgroundColor = "var(--success)";
         * }
         * if (pressureInput) pressureInput.disabled = true;
         * } else {
         * if (trackBtn) {
         * trackBtn.innerText = "Auto tracking";
         * trackBtn.style.backgroundColor = "var(--primary)";
         * }
         * if (pressureInput) pressureInput.disabled = false;
         * if (phaseLabel) phaseLabel.innerText = "Status: Ready (Manual Mode)";
         * }
         * }
         * else if (actionType === 'CTRL_TAKEOFF') {
         * window.api.firePayloadEventByName('TakeOffMsg', PATH_CONTROLLER);
         * console.log('📤 TakeOffMsg → Controller');
         * }
         * else if (actionType === 'CTRL_CLIMB') {
         * window.api.firePayloadEventByName('ClimbMsg', PATH_CONTROLLER);
         * console.log('📤 ClimbMsg → Controller');
         * }
         * else if (actionType === 'CTRL_HOLD') {
         * window.api.firePayloadEventByName('HoldMsg', PATH_CONTROLLER);
         * console.log('📤 HoldMsg → Controller');
         * }
         * else if (actionType === 'CTRL_DESCEND') {
         * window.api.firePayloadEventByName('DescendMsg', PATH_CONTROLLER);
         * console.log('📤 DescendMsg → Controller');
         * }
         * else if (actionType === 'SIG_TAKEOFF') {
         * window.api.firePayloadEventByName('TakeOffMsg', PATH_PROCESSOR);
         * console.log('📤 TakeOffMsg → Processor');
         * }
         * else if (actionType === 'SIG_LAND_PROC') {
         * window.api.firePayloadEventByName('LandMsg', PATH_PROCESSOR);
         * console.log('📤 LandMsg → Processor');
         * }
         * };
         *
         * window.update = async () => {
         * if (isUpdating || !window.api) return;
         * isUpdating = true;
         * try {
         * // Fetch standard attributes
         * state.altitude     = await getFeature('altitude', PATH_CONTROLLER) || 0.0;
         * state.simTime      = await getFeature('time', PATH_SYSTEM) || 0.0;
         * state.soc          = await getFeature('SOC', PATH_POWER_SYS) || 80.0;
         *
         * // Fetch active state using the dedicated API method
         * const stateJson = await fetchState(PATH_CONTROLLER);
         * if (stateJson) {
         * try {
         * const activeStates = JSON.parse(stateJson);
         * if (activeStates && activeStates.length > 0) {
         * state.currentState = activeStates[0].name;
         * } else {
         * state.currentState = 'Off';
         * }
         * } catch (e) {
         * console.error('Failed to parse state JSON', e);
         * state.currentState = 'Off';
         * }
         * } else {
         * state.currentState = 'Off';
         * }
         *
         * // Handle auto-tracking pressure updates based on the verified state
         * if (isClosedLoopActive) {
         * state.pressure = predictNextPressure(state.currentState, state.pressure);
         * window.api.updateFeatureValuesByDotNotation('pressure', PATH_PROCESSOR, state.pressure);
         * } else if (!isEditing) {
         * state.pressure = await getFeature('pressure', PATH_PROCESSOR) || 101.325;
         * }
         *
         * updateUI();
         * }
         * catch (error) {
         * console.error('Update error:', error);
         * }
         * finally { isUpdating = false; }
         * };
         *
         * const getFeature = (name, path) => new Promise(resolve => {
         * if (!window.api || !window.api.getFeatureValuesByDotNotation) {
         * return resolve(null);
         * }
         * window.api.getFeatureValuesByDotNotation(window, resolve, name, path);
         * });
         *
         * // Added helper for fetching active states securely from the engine
         * const fetchState = (path) => new Promise(resolve => {
         * if (!window.api || !window.api.getActiveStatesByDotNotation) {
         * return resolve(null);
         * }
         * window.api.getActiveStatesByDotNotation(window, resolve, path);
         * });
         *
         * window.log = (message) => {
         * console.log('[SIM]', message);
         * };
         * </script>
         * </body>
         * </html>*/
    }
}
