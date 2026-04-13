<template>
  <!-- 动效容器 -->
  <div class="intro-overlay" id="introOverlay">
    <div class="typewriter-text" id="typewriterText"><span class="text-content"></span><span class="cursor intro" id="introCursor"></span></div>
    <div class="intro-core" id="introCore"></div>
  </div>

  <nav class="navbar" id="navbar">
    <div class="brand">ShareHub</div>
    <div class="nav-links">
      <a href="#focus">聚焦</a>
      <a href="#features">能力</a>
      <a href="#flow">流程</a>
    </div>
    <div class="nav-actions">
      <RouterLink to="/login" class="nav-login-link">登录</RouterLink>
      <RouterLink to="/login" class="btn btn-primary btn-pill-sm">开始使用</RouterLink>
    </div>
  </nav>

  <main class="container page-home">
    <section class="hero-section">
      <h1 class="hero-title" id="hTitle"><span id="hTitleText"></span><span class="cursor main" id="mainCursor"></span></h1>
      <p class="hero-subtitle" id="hSubtitle">专为 Agent、MCP 与 RAG 开发者打造。极简记录，生成动态履历。</p>
      
      <div class="hero-search-box" id="heroSearchBox">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
        <input type="text" placeholder="输入你想探索的体系、模型或是前沿面经..." readonly>
      </div>

      <div class="hero-actions" id="hActions">
        <RouterLink to="/login" class="btn btn-primary btn-pill-lg">
          用 GitHub 登录
        </RouterLink>
        <a href="#features" class="btn btn-secondary btn-pill-lg">
          浏览特性
        </a>
      </div>
    </section>

    <section id="focus" class="section">
       <div class="focus-panel reveal" id="focusPanel" data-triggered="false">
          <h2><span id="focusTitleText"></span><span class="cursor focus" id="focusCursor"></span></h2>
          <p id="focusSubtitle" class="focus-subtitle">分享你的 MCP Server 配置、RAG 向量检索心得与大模型微调笔记。<br>在这里，找到同频的技术布道者。</p>
       </div>
    </section>

    <section id="features" class="section section-features">
      <div class="section-header reveal">
        <h2 class="section-title">化繁为简。</h2>
        <p class="section-subtitle">摒弃多余的噪音，提供纯粹的知识托管与归档体验。</p>
      </div>
      <div class="features-grid">
        <div class="clean-card feature-card reveal stagger-1">
          <div class="feature-icon">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path></svg>
          </div>
          <h3>资料托管</h3>
          <p>高效上传架构图、配置文件与外链，原生支持版本追溯与极速分发。</p>
        </div>
        <div class="clean-card feature-card reveal stagger-2">
          <div class="feature-icon">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path></svg>
          </div>
          <h3>路线编排</h3>
          <p>构建结构化的技能图谱。将打碎的知识点，通过章节节点重新组装。</p>
        </div>
        <div class="clean-card feature-card reveal stagger-1">
          <div class="feature-icon">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
          </div>
          <h3>纯粹笔记</h3>
          <p>沉浸式 Markdown 编辑器。专注于笔下的代码块与思路，无广告打扰。</p>
        </div>
        <div class="clean-card feature-card reveal stagger-2">
          <div class="feature-icon">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
          </div>
          <h3>专业履历</h3>
          <p>基于你的代码提交与笔记沉淀，智能分析一键排版，生成高水准简历。</p>
        </div>
      </div>
    </section>

    <section id="flow" class="section">
      <div class="section-header reveal">
        <h2 class="section-title">一切从简。</h2>
        <p class="section-subtitle">没有冗长的填表，保护创作的热情。</p>
      </div>
      <div class="steps-list">
        <div class="step-row reveal">
          <div class="step-number">01</div>
          <div class="step-text">
            <h3>授权登录</h3>
            <p>基于 GitHub 账号体系，一秒钟建立身份信誉，避免信息汪洋。</p>
          </div>
        </div>
        <div class="step-row reveal stagger-1">
          <div class="step-number">02</div>
          <div class="step-text">
            <h3>积累资产</h3>
            <p>发布、点赞并收藏有价值的 MCP 模型接口或是前沿的面经资料。</p>
          </div>
        </div>
        <div class="step-row reveal stagger-2">
          <div class="step-number">03</div>
          <div class="step-text">
            <h3>一键展示</h3>
            <p>平台会自动萃取你的学习路线与高赞笔记，随时生成精美报告。</p>
          </div>
        </div>
      </div>
    </section>

    <section class="bottom-cta reveal">
      <h2>准备好开始了么？</h2>
      <RouterLink to="/login" class="btn btn-primary btn-pill-xl">
        用 GitHub 继续
      </RouterLink>
    </section>
  </main>

  <footer class="container page-home">
     <div class="footer">
        <div>© 2026 ShareHub. 保留所有权利。</div>
        <div class="footer-links"><a href="#">隐私政策</a><a href="#">使用条款</a></div>
     </div>
  </footer>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount } from 'vue'
import { RouterLink } from 'vue-router'
import '@/assets/landing.css'

let observer: IntersectionObserver | null = null;
let currentTitleIdx = 0;
let titleLoopStarted = false;
let heroEntranceStarted = false;
let mainTitles = [
  "沉淀下一代技术资产",
  "随时复用的体系经验",
  "搭建极客代码图谱",
  "联结前沿开发者"
];

const INTRO_KEY = "ShareHub.introPlayed";

function setIntroPlayed() {
  try {
    sessionStorage.setItem(INTRO_KEY, "true");
  } catch (error) {}
}

function hasPlayedIntro() {
  try {
    return sessionStorage.getItem(INTRO_KEY) === "true" || sessionStorage.getItem("introPlayed") === "true";
  } catch (error) {
    return false;
  }
}

function lockScrollToTop() {
  window.scrollTo(0, 0);
}

function resetHistoryScrollState() {
  if ("scrollRestoration" in history) {
    history.scrollRestoration = "manual";
  }
  lockScrollToTop();
}

function initHomePage() {
  const introOverlay = document.getElementById("introOverlay");
  if (!introOverlay) return;

  const refs = {
    body: document.body,
    introOverlay,
    introCore: document.getElementById("introCore"),
    typewriter: document.getElementById("typewriterText"),
    typewriterText: document.querySelector("#typewriterText .text-content"),
    heroSearchBox: document.getElementById("heroSearchBox"),
    navbar: document.getElementById("navbar"),
    heroTitle: document.getElementById("hTitle"),
    heroTitleText: document.getElementById("hTitleText"),
    mainCursor: document.getElementById("mainCursor"),
    heroSubtitle: document.getElementById("hSubtitle"),
    heroActions: document.getElementById("hActions")
  };

  resetHistoryScrollState();
  setupScrollEffect(refs.navbar);
  setupRevealObserver();

  const prefersReducedMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (hasPlayedIntro() || prefersReducedMotion) {
    skipIntro(refs);
    return;
  }

  playIntro(refs);
}

function playIntro(refs: any) {
  const phrases = [
    "正在连接你的 AI 工作流...",
    "加载 Agent / MCP / RAG 知识图谱...",
    "欢迎来到 ShareHub。"
  ];

  let phraseIndex = 0;

  function typeText(text: string, callback: Function) {
    if (!refs.typewriterText) {
      callback();
      return;
    }

    refs.typewriterText.textContent = "";
    let i = 0;

    function tick() {
      refs.typewriterText.textContent += text.charAt(i);
      i += 1;

      if (i >= text.length) {
        setTimeout(callback, 500);
        return;
      }

      setTimeout(tick, 50);
    }

    tick();
  }

  function nextPhrase() {
    if (phraseIndex >= phrases.length) return;

    typeText(phrases[phraseIndex], () => {
      phraseIndex += 1;
      if (phraseIndex === phrases.length) {
        setTimeout(() => retractToDot(refs, phrases), 800);
        return;
      }
      nextPhrase();
    });
  }
  setTimeout(nextPhrase, 380);
}

function retractToDot(refs: any, phrases: string[]) {
  if (!refs.typewriterText || !refs.typewriter || !refs.introCore) {
    finalizeIntro(refs);
    return;
  }

  const text = phrases[phrases.length - 1];
  let i = text.length;

  const interval = setInterval(() => {
    refs.typewriterText.textContent = text.substring(0, i - 1);
    i -= 1;

    if (i > 0) return;

    clearInterval(interval);
    refs.typewriter.classList.add("is-hidden");
    refs.introCore.style.transition = "none";
    refs.introCore.style.opacity = "1";
    refs.introCore.style.width = "16px";
    refs.introCore.style.height = "16px";

    setTimeout(() => {
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          startMorph(refs);
        });
      });
    }, 140);
  }, 15);
}

function startMorph(refs: any) {
  if (!refs.introCore || !refs.heroSearchBox || !refs.introOverlay) {
    finalizeIntro(refs);
    return;
  }

  const rect = refs.heroSearchBox.getBoundingClientRect();
  const viewportWidth = window.visualViewport ? window.visualViewport.width : window.innerWidth;
  const viewportHeight = window.visualViewport ? window.visualViewport.height : window.innerHeight;
  const screenCenterX = viewportWidth / 2;
  const screenCenterY = viewportHeight / 2;
  const targetDx = rect.left + rect.width / 2 - screenCenterX;
  const targetDy = rect.top + rect.height / 2 - screenCenterY;

  refs.introOverlay.classList.add("is-transparent");
  refs.introCore.style.transition = "all 800ms cubic-bezier(0.22, 1, 0.36, 1)";
  refs.introCore.style.width = `${rect.width}px`;
  refs.introCore.style.height = `${rect.height}px`;
  refs.introCore.style.borderRadius = "32px";
  refs.introCore.style.background = "#ffffff";
  refs.introCore.style.border = "1px solid rgba(0, 0, 0, 0.08)";
  refs.introCore.style.boxShadow = "0 2px 8px rgba(0, 0, 0, 0.04)";
  refs.introCore.style.transform = `translate(calc(-50% + ${targetDx}px), calc(-50% + ${targetDy}px))`;

  setTimeout(() => finalizeIntro(refs), 820);
}

function finalizeIntro(refs: any) {
  if (refs.heroSearchBox) refs.heroSearchBox.classList.add("is-visible");
  if (refs.introCore) refs.introCore.classList.add("is-hidden");
  if (refs.introOverlay) refs.introOverlay.classList.add("is-hidden");
  playHeroEntrance(refs);
}

function skipIntro(refs: any) {
  if (refs.introOverlay) refs.introOverlay.classList.add("is-hidden");
  if (refs.heroSearchBox) refs.heroSearchBox.classList.add("is-visible");
  playHeroEntrance(refs);
}

function playHeroEntrance(refs: any) {
  if (heroEntranceStarted) return;
  heroEntranceStarted = true;

  lockScrollToTop();
  if (refs.body) refs.body.classList.remove("animating");

  setIntroPlayed();

  if (refs.navbar) refs.navbar.classList.add("is-visible");

  const searchInput = document.querySelector("#heroSearchBox input");
  if (searchInput) searchInput.removeAttribute("readonly");

  if (refs.heroTitle) refs.heroTitle.classList.add("is-visible");

  setTimeout(() => { if (refs.heroSubtitle) refs.heroSubtitle.classList.add("is-visible"); }, 50);
  setTimeout(() => { if (refs.heroActions) refs.heroActions.classList.add("is-visible"); }, 150);
  setTimeout(() => { if (refs.heroSearchBox) refs.heroSearchBox.classList.add("glow-active"); }, 360);
  setTimeout(() => {
    if (refs.mainCursor) refs.mainCursor.classList.add("is-visible");
    if (!titleLoopStarted) {
      titleLoopStarted = true;
      startLoopingTitles(refs.heroTitleText);
    }
  }, 280);
}

function startLoopingTitles(titleNode: any) {
  if (!titleNode) return;

  function typeTitlePhrase() {
    const text = mainTitles[currentTitleIdx];
    titleNode.textContent = "";
    let i = 0;

    function typeNextChar() {
      if (i < text.length) {
        titleNode.textContent += text.charAt(i);
        i += 1;
        setTimeout(typeNextChar, Math.random() * 30 + 20);
        return;
      }

      setTimeout(deleteTitlePhrase, 3000);
    }
    typeNextChar();
  }

  function deleteTitlePhrase() {
    const text = mainTitles[currentTitleIdx];
    let i = text.length;

    const interval = setInterval(() => {
      titleNode.textContent = text.substring(0, i - 1);
      i -= 1;

      if (i > 0) return;

      clearInterval(interval);
      currentTitleIdx = (currentTitleIdx + 1) % mainTitles.length;
      setTimeout(typeTitlePhrase, 500);
    }, 25);
  }
  typeTitlePhrase();
}

function setupScrollEffect(navbar: any) {
  if (!navbar) return;

  window.addEventListener(
    "scroll",
    () => {
      if (window.scrollY > 20) {
        navbar.classList.add("scrolled");
      } else {
        navbar.classList.remove("scrolled");
      }
    },
    { passive: true }
  );
}

function setupRevealObserver() {
  const revealItems = document.querySelectorAll(".reveal");
  if (!revealItems.length) return;

  if (!("IntersectionObserver" in window)) {
    revealItems.forEach((el) => el.classList.add("active"));
    return;
  }

  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;

        entry.target.classList.add("active");
        if (entry.target.id === "focusPanel" && entry.target.getAttribute("data-triggered") !== "true") {
          entry.target.setAttribute("data-triggered", "true");
          startFocusAnimation();
        }
      });
    },
    { threshold: 0.25, rootMargin: "0px 0px -40px 0px" }
  );

  revealItems.forEach((element) => observer!.observe(element));
}

function startFocusAnimation() {
  const cursor = document.getElementById("focusCursor") as HTMLElement | null;
  const titleText = document.getElementById("focusTitleText") as HTMLElement | null;
  const subtitle = document.getElementById("focusSubtitle") as HTMLElement | null;
  const textToType = "为前沿技术而生。";

  if (!cursor || !titleText) return;
  const cursorEl = cursor;
  const titleTextEl = titleText;
  const subtitleEl = subtitle;

  setTimeout(() => {
    cursorEl.classList.add("dropped");
    setTimeout(() => {
      cursorEl.classList.add("blinking");
      let i = 0;
      function typeNext() {
        if (i < textToType.length) {
          titleTextEl.textContent += textToType.charAt(i);
          i += 1;
          setTimeout(typeNext, 40);
        } else {
          cursorEl.classList.remove("blinking");
          if (subtitleEl) subtitleEl.classList.add("is-visible");
        }
      }
      typeNext();
    }, 600);
  }, 200);
}

onMounted(() => {
  document.body.classList.add('page-home');
  document.body.classList.add('animating');
  initHomePage();
})

onBeforeUnmount(() => {
  document.body.classList.remove('page-home');
  document.body.classList.remove('animating');
  if (observer) {
    observer.disconnect();
  }
})
</script>
