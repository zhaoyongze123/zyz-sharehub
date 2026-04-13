(() => {
  const INTRO_KEY = "ShareHub.introPlayed";
  const mainTitles = [
    "沉淀下一代技术资产",
    "随时复用的体系经验",
    "搭建极客代码图谱",
    "联结前沿开发者"
  ];

  let currentTitleIdx = 0;
  let titleLoopStarted = false;
  let heroEntranceStarted = false;

  function setIntroPlayed() {
    try {
      sessionStorage.setItem(INTRO_KEY, "true");
    } catch (error) {
      console.debug("sessionStorage 写入失败", error);
    }
  }

  function hasPlayedIntro() {
    try {
      return sessionStorage.getItem(INTRO_KEY) === "true" || sessionStorage.getItem("introPlayed") === "true";
    } catch (error) {
      console.debug("sessionStorage 读取失败", error);
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

    if (window.location.hash) {
      history.replaceState(null, "", window.location.pathname + window.location.search);
    }

    lockScrollToTop();
  }

  function initHomePage() {
    const introOverlay = document.getElementById("introOverlay");
    if (!introOverlay) {
      return;
    }

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

  function playIntro(refs) {
    const phrases = [
      "正在连接你的 AI 工作流...",
      "加载 Agent / MCP / RAG 知识图谱...",
      "欢迎来到 ShareHub。"
    ];

    let phraseIndex = 0;

    function typeText(text, callback) {
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
      if (phraseIndex >= phrases.length) {
        return;
      }

      typeText(phrases[phraseIndex], () => {
        phraseIndex += 1;

        if (phraseIndex === phrases.length) {
          setTimeout(retractToDot, 800);
          return;
        }

        nextPhrase();
      });
    }

    function retractToDot() {
      if (!refs.typewriterText || !refs.typewriter || !refs.introCore) {
        finalizeIntro(refs);
        return;
      }

      const text = phrases[phrases.length - 1];
      let i = text.length;

      const interval = setInterval(() => {
        refs.typewriterText.textContent = text.substring(0, i - 1);
        i -= 1;

        if (i > 0) {
          return;
        }

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

    setTimeout(nextPhrase, 380);
  }

  function startMorph(refs) {
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

  function finalizeIntro(refs) {
    if (refs.heroSearchBox) {
      refs.heroSearchBox.classList.add("is-visible");
    }

    if (refs.introCore) {
      refs.introCore.classList.add("is-hidden");
    }

    if (refs.introOverlay) {
      refs.introOverlay.classList.add("is-hidden");
    }

    playHeroEntrance(refs);
  }

  function skipIntro(refs) {
    if (refs.introOverlay) {
      refs.introOverlay.classList.add("is-hidden");
    }

    if (refs.heroSearchBox) {
      refs.heroSearchBox.classList.add("is-visible");
    }

    playHeroEntrance(refs);
  }

  function playHeroEntrance(refs) {
    if (heroEntranceStarted) {
      return;
    }
    heroEntranceStarted = true;

    lockScrollToTop();

    if (refs.body) {
      refs.body.classList.remove("animating");
    }

    setIntroPlayed();
    try {
      sessionStorage.setItem("introPlayed", "true");
    } catch (error) {
      console.debug("sessionStorage 写入失败", error);
    }

    if (refs.navbar) {
      refs.navbar.classList.add("is-visible");
    }

    const searchInput = document.querySelector("#heroSearchBox input");
    if (searchInput) {
      searchInput.removeAttribute("readonly");
    }

    if (refs.heroTitle) {
      refs.heroTitle.classList.add("is-visible");
    }

    setTimeout(() => {
      if (refs.heroSubtitle) {
        refs.heroSubtitle.classList.add("is-visible");
      }
    }, 50);

    setTimeout(() => {
      if (refs.heroActions) {
        refs.heroActions.classList.add("is-visible");
      }
    }, 150);

    setTimeout(() => {
      if (refs.heroSearchBox) {
        refs.heroSearchBox.classList.add("glow-active");
      }
    }, 360);

    setTimeout(() => {
      if (refs.mainCursor) {
        refs.mainCursor.classList.add("is-visible");
      }

      if (!titleLoopStarted) {
        titleLoopStarted = true;
        startLoopingTitles(refs.heroTitleText);
      }
    }, 280);
  }

  function startLoopingTitles(titleNode) {
    if (!titleNode) {
      return;
    }

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

        if (i > 0) {
          return;
        }

        clearInterval(interval);
        currentTitleIdx = (currentTitleIdx + 1) % mainTitles.length;
        setTimeout(typeTitlePhrase, 500);
      }, 25);
    }

    typeTitlePhrase();
  }

  function setupScrollEffect(navbar) {
    if (!navbar) {
      return;
    }

    window.addEventListener(
      "scroll",
      () => {
        if (window.scrollY > 20) {
          navbar.classList.add("scrolled");
          return;
        }

        navbar.classList.remove("scrolled");
      },
      { passive: true }
    );
  }

  function setupRevealObserver() {
    const revealItems = document.querySelectorAll(".reveal");
    if (!revealItems.length) {
      return;
    }

    if (!("IntersectionObserver" in window)) {
      revealItems.forEach((el) => el.classList.add("active"));
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) {
            return;
          }

          entry.target.classList.add("active");

          if (entry.target.id === "focusPanel" && entry.target.getAttribute("data-triggered") !== "true") {
            entry.target.setAttribute("data-triggered", "true");
            startFocusAnimation();
          }
        });
      },
      { threshold: 0.25, rootMargin: "0px 0px -40px 0px" }
    );

    revealItems.forEach((element) => observer.observe(element));
  }

  function startFocusAnimation() {
    const cursor = document.getElementById("focusCursor");
    const titleText = document.getElementById("focusTitleText");
    const subtitle = document.getElementById("focusSubtitle");
    const textToType = "为前沿技术而生。";

    if (!cursor || !titleText) {
      return;
    }

    setTimeout(() => {
      cursor.classList.add("dropped");

      setTimeout(() => {
        cursor.classList.add("blinking");

        let i = 0;
        function typeNext() {
          if (i < textToType.length) {
            titleText.textContent += textToType.charAt(i);
            i += 1;
            setTimeout(typeNext, Math.random() * 40 + 30);
            return;
          }

          setTimeout(() => {
            if (subtitle) {
              subtitle.classList.add("is-visible");
            }
          }, 150);
        }

        typeNext();
      }, 600);
    }, 250);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initHomePage, { once: true });
  } else {
    initHomePage();
  }
})();
