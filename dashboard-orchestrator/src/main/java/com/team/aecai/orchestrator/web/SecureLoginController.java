package com.team.aecai.orchestrator.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

@Controller
@Profile("secure")
public class SecureLoginController {

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String login(HttpServletRequest request) {

        Object csrfAttribute = request.getAttribute("_csrf");
        String csrfParameter = "_csrf";
        String csrfToken = "";

        if (csrfAttribute instanceof CsrfToken csrf) {
            csrfParameter = HtmlUtils.htmlEscape(csrf.getParameterName());
            csrfToken = HtmlUtils.htmlEscape(csrf.getToken());
        }

        String notice = "";

        if (request.getParameter("error") != null) {
            notice = """
                <div class="notice error">
                    <span class="notice-dot"></span>
                    Invalid username or password. Try again.
                </div>
                """;
        } else if (request.getParameter("logout") != null) {
            notice = """
                <div class="notice success">
                    <span class="notice-dot"></span>
                    Secure session ended successfully.
                </div>
                """;
        }

        return PAGE
                .replace("__NOTICE__", notice)
                .replace("__CSRF_PARAMETER__", csrfParameter)
                .replace("__CSRF_TOKEN__", csrfToken);
    }

    private static final String PAGE = """
<!doctype html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="theme-color" content="#09090f">
<title>Secure Access · Edge.Cloud</title>

<style>
:root {
    --bg: #09090f;
    --surface: #11111b;
    --surface2: #191925;
    --surface3: #1e1d2c;
    --text: #f5f3ff;
    --muted: #a1a1b5;
    --muted2: #7e7c92;
    --stroke: #302a40;
    --stroke2: #262234;
    --accent: #8b5cf6;
    --accent2: #a78bfa;
    --accent3: #c4b5fd;
    --wash: rgba(139,92,246,.10);
    --danger: #ee8793;
}

* {
    box-sizing: border-box;
}

html, body {
    margin: 0;
    min-height: 100%;
    background: var(--bg);
    color: var(--text);
    font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

body {
    min-height: 100vh;
    overflow-x: hidden;
}

.page {
    min-height: 100vh;
    display: grid;
    grid-template-columns: minmax(0, 1.25fr) minmax(420px, .75fr);
    position: relative;
    isolation: isolate;
}

.background {
    position: fixed;
    inset: 0;
    z-index: -2;
    overflow: hidden;
    background:
        radial-gradient(circle at 22% 32%, rgba(139,92,246,.11), transparent 28%),
        radial-gradient(circle at 80% 75%, rgba(167,139,250,.07), transparent 28%),
        var(--bg);
}

.background::after {
    content: "";
    position: absolute;
    inset: 0;
    opacity: .16;
    background-image:
        linear-gradient(rgba(139,92,246,.15) 1px, transparent 1px),
        linear-gradient(90deg, rgba(139,92,246,.15) 1px, transparent 1px);
    background-size: 54px 54px;
    mask-image: linear-gradient(to right, black, transparent 74%);
}

.glow {
    position: absolute;
    width: 520px;
    height: 520px;
    border-radius: 50%;
    filter: blur(130px);
    background: rgba(139,92,246,.12);
    left: 12%;
    top: 18%;
}

.left {
    padding: 48px clamp(42px, 6vw, 95px);
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    min-height: 100vh;
}

.brand {
    display: flex;
    align-items: center;
    gap: 13px;
}

.brand-mark {
    width: 39px;
    height: 39px;
    border: 1px solid #7358a6;
    border-radius: 9px;
    transform: rotate(-10deg);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 3px;
    background: rgba(139,92,246,.05);
    box-shadow: 0 0 28px rgba(139,92,246,.09);
}

.brand-mark span {
    display: block;
    width: 4px;
    border-radius: 3px;
    background: var(--accent2);
}

.brand-mark span:nth-child(1) { height: 13px; }
.brand-mark span:nth-child(2) { height: 23px; }
.brand-mark span:nth-child(3) { height: 9px; }

.brand-name {
    font-size: 24px;
    font-weight: 750;
    letter-spacing: -.06em;
}

.brand-name i {
    color: var(--accent2);
    font-style: normal;
}

.brand-sub {
    display: block;
    margin-top: 5px;
    color: var(--muted2);
    font-size: 9px;
    font-weight: 700;
    letter-spacing: .20em;
}

.hero {
    max-width: 720px;
    margin: 80px 0;
}

.eyebrow {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--accent3);
    font-size: 10px;
    letter-spacing: .19em;
    font-weight: 700;
    margin-bottom: 23px;
}

.eyebrow::before {
    content: "";
    width: 24px;
    height: 1px;
    background: var(--accent2);
}

h1 {
    font-size: clamp(46px, 5vw, 76px);
    line-height: .98;
    letter-spacing: -.065em;
    margin: 0;
    font-weight: 690;
}

h1 span {
    color: var(--accent2);
}

.hero p {
    color: var(--muted);
    max-width: 600px;
    font-size: 14px;
    line-height: 1.8;
    margin: 28px 0 0;
}

.pipeline {
    display: flex;
    align-items: center;
    gap: 9px;
    margin-top: 36px;
    flex-wrap: wrap;
}

.node {
    display: flex;
    align-items: center;
    gap: 8px;
    border: 1px solid var(--stroke);
    background: rgba(17,17,27,.70);
    padding: 9px 12px;
    border-radius: 7px;
    color: var(--muted);
    font-size: 10px;
}

.node-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--accent2);
    box-shadow: 0 0 0 4px var(--wash);
}

.arrow {
    color: #514767;
    font-size: 12px;
}

.footer {
    color: var(--muted2);
    font-size: 9px;
    letter-spacing: .08em;
}

.right {
    min-height: 100vh;
    border-left: 1px solid var(--stroke2);
    background:
        linear-gradient(180deg, rgba(17,17,27,.91), rgba(12,12,20,.97));
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 42px;
    position: relative;
}

.login-wrap {
    width: min(100%, 430px);
}

.secure-pill {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    border: 1px solid rgba(139,92,246,.26);
    background: var(--wash);
    color: var(--accent3);
    padding: 7px 10px;
    border-radius: 5px;
    font-size: 9px;
    letter-spacing: .12em;
    font-weight: 700;
    margin-bottom: 22px;
}

.lock {
    width: 10px;
    height: 9px;
    border: 1.5px solid currentColor;
    border-radius: 2px;
    position: relative;
}

.lock::before {
    content: "";
    position: absolute;
    width: 6px;
    height: 6px;
    border: 1.5px solid currentColor;
    border-bottom: 0;
    border-radius: 6px 6px 0 0;
    left: 50%;
    top: -7px;
    transform: translateX(-50%);
}

.login-card {
    background: var(--surface);
    border: 1px solid var(--stroke);
    border-radius: 13px;
    box-shadow: 0 24px 80px rgba(0,0,0,.28);
    overflow: hidden;
}

.card-top {
    padding: 28px 30px 22px;
    border-bottom: 1px solid var(--stroke2);
}

.card-top small {
    color: var(--accent3);
    font-size: 9px;
    letter-spacing: .16em;
    font-weight: 700;
}

.card-top h2 {
    margin: 9px 0 8px;
    font-size: 25px;
    letter-spacing: -.045em;
    font-weight: 650;
}

.card-top p {
    color: var(--muted);
    margin: 0;
    font-size: 11px;
    line-height: 1.6;
}

.form {
    padding: 27px 30px 30px;
}

.field {
    margin-bottom: 18px;
}

label {
    display: flex;
    justify-content: space-between;
    color: var(--text);
    font-size: 11px;
    font-weight: 620;
    margin-bottom: 8px;
}

label span {
    color: var(--muted2);
    font-weight: 450;
    font-size: 10px;
}

input[type=text],
input[type=password] {
    width: 100%;
    height: 46px;
    border-radius: 7px;
    border: 1px solid var(--stroke);
    background: var(--surface2);
    color: var(--text);
    padding: 0 14px;
    font: inherit;
    outline: none;
    transition: .18s ease;
}

input::placeholder {
    color: #666277;
}

input:focus {
    border-color: var(--accent);
    box-shadow: 0 0 0 3px var(--wash);
}

button {
    width: 100%;
    height: 47px;
    border: 0;
    border-radius: 7px;
    margin-top: 5px;
    background: var(--accent);
    color: white;
    font: inherit;
    font-size: 12px;
    font-weight: 680;
    cursor: pointer;
    transition: .18s ease;
    box-shadow: 0 8px 24px rgba(139,92,246,.17);
}

button:hover {
    background: var(--accent2);
    transform: translateY(-1px);
}

button:active {
    transform: translateY(0);
}

.notice {
    display: flex;
    align-items: center;
    gap: 9px;
    border-radius: 7px;
    padding: 11px 12px;
    font-size: 11px;
    margin-bottom: 18px;
    border: 1px solid;
}

.notice.error {
    border-color: rgba(238,135,147,.24);
    background: rgba(238,135,147,.06);
    color: var(--danger);
}

.notice.success {
    border-color: rgba(167,139,250,.25);
    background: var(--wash);
    color: var(--accent3);
}

.notice-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: currentColor;
}

.security-note {
    margin-top: 18px;
    display: flex;
    gap: 10px;
    align-items: flex-start;
    color: var(--muted2);
    font-size: 10px;
    line-height: 1.55;
}

.security-note .dot {
    margin-top: 5px;
    width: 5px;
    height: 5px;
    flex: 0 0 5px;
    background: var(--accent2);
    border-radius: 50%;
}

.version {
    text-align: center;
    color: #625e72;
    font-size: 9px;
    margin-top: 21px;
    letter-spacing: .08em;
}

@media (max-width: 900px) {
    .page {
        grid-template-columns: 1fr;
    }

    .left {
        display: none;
    }

    .right {
        border-left: 0;
        min-height: 100vh;
        padding: 24px;
    }
}
</style>
</head>

<body>
<div class="background"><div class="glow"></div></div>

<main class="page">

<section class="left">

<div class="brand">
    <div class="brand-mark">
        <span></span><span></span><span></span>
    </div>
    <div>
        <div class="brand-name">Edge<i>.</i>Cloud</div>
        <span class="brand-sub">ADAPTIVE AI RESEARCH SYSTEM</span>
    </div>
</div>

<div class="hero">
    <div class="eyebrow">SECURE RESEARCH CONSOLE</div>

    <h1>
        Intelligent execution.<br>
        <span>Protected access.</span>
    </h1>

    <p>
        Authenticate to access the adaptive Edge–Cloud research environment,
        live telemetry, explainable routing decisions, experiments and
        execution history.
    </p>

    <div class="pipeline">
        <div class="node"><span class="node-dot"></span>EDGE</div>
        <span class="arrow">→</span>
        <div class="node"><span class="node-dot"></span>DECISION</div>
        <span class="arrow">→</span>
        <div class="node"><span class="node-dot"></span>CLOUD</div>
        <span class="arrow">→</span>
        <div class="node"><span class="node-dot"></span>ESTIMATOR</div>
    </div>
</div>

<div class="footer">
    INTELLIGENT EDGE–CLOUD COMPUTING FRAMEWORK · LOCAL RESEARCH DEPLOYMENT
</div>

</section>

<section class="right">

<div class="login-wrap">

    <div class="secure-pill">
        <span class="lock"></span>
        AUTHENTICATED SESSION REQUIRED
    </div>

    <div class="login-card">

        <div class="card-top">
            <small>EDGE.CLOUD / ACCESS GATEWAY</small>
            <h2>Welcome back.</h2>
            <p>
                Sign in with the credentials configured for this secure
                research session.
            </p>
        </div>

        <form class="form" method="post" action="/login">

            __NOTICE__

            <input
                type="hidden"
                name="__CSRF_PARAMETER__"
                value="__CSRF_TOKEN__">

            <div class="field">
                <label>
                    Username
                    <span>research administrator</span>
                </label>
                <input
                    type="text"
                    name="username"
                    autocomplete="username"
                    placeholder="Enter username"
                    autofocus
                    required>
            </div>

            <div class="field">
                <label>
                    Password
                    <span>secure credential</span>
                </label>
                <input
                    type="password"
                    name="password"
                    autocomplete="current-password"
                    placeholder="Enter password"
                    required>
            </div>

            <button type="submit">
                Enter Research Console →
            </button>

            <div class="security-note">
                <span class="dot"></span>
                <span>
                    Access is session-protected. Authentication credentials
                    are validated by the local Spring Security layer.
                </span>
            </div>

        </form>
    </div>

    <div class="version">
        EDGE.CLOUD · SECURE LOCAL PROTOTYPE
    </div>

</div>
</section>

</main>
</body>
</html>
""";
}
