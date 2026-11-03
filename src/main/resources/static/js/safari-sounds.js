(() => {
    const soundToggle = document.querySelector("[data-sound-toggle]");
    if (!soundToggle) {
        return;
    }

    const storageKey = "safari-sounds-enabled";
    const loadEnabled = () => {
        try {
            return window.localStorage.getItem(storageKey) === "true";
        } catch (error) {
            console.warn("No se pudo leer la preferencia de sonido de SAFARI.", error);
            return false;
        }
    };
    const saveEnabled = value => {
        try {
            window.localStorage.setItem(storageKey, String(value));
        } catch (error) {
            console.warn("No se pudo guardar la preferencia de sonido de SAFARI.", error);
        }
    };
    let enabled = loadEnabled();
    let audioContext;
    let loginSubmissionPending = false;

    const updateToggle = () => {
        soundToggle.textContent = enabled ? "🔊" : "🔇";
        soundToggle.setAttribute("aria-pressed", String(enabled));
        soundToggle.setAttribute("aria-label", enabled ? "Silenciar sonidos" : "Activar sonidos");
        soundToggle.title = enabled ? "Silenciar sonidos" : "Activar sonidos";
    };

    const getAudioContext = () => {
        const AudioContextConstructor = window.AudioContext || window.webkitAudioContext;
        if (!AudioContextConstructor) {
            soundToggle.disabled = true;
            soundToggle.title = "Este navegador no admite efectos de sonido";
            return null;
        }
        audioContext ??= new AudioContextConstructor();
        if (audioContext.state === "suspended") {
            audioContext.resume().catch(error => {
                console.warn("No se pudieron activar los sonidos de SAFARI.", error);
            });
        }
        return audioContext;
    };

    const playSound = kind => {
        if (!enabled) {
            return;
        }
        const context = getAudioContext();
        if (!context) {
            return;
        }

        const sounds = {
            click: [740],
            toggle: [660, 880],
            login: [587, 740, 988],
            logout: [740, 587]
        };
        const frequencies = sounds[kind];
        if (!frequencies) {
            return;
        }
        const start = context.currentTime;
        frequencies.forEach((frequency, index) => {
            const noteStart = start + index * 0.075;
            const duration = kind === "click" ? 0.045 : 0.09;
            const oscillator = context.createOscillator();
            const volume = context.createGain();
            oscillator.type = "sine";
            oscillator.frequency.setValueAtTime(frequency, noteStart);
            volume.gain.setValueAtTime(0.0001, noteStart);
            volume.gain.exponentialRampToValueAtTime(0.055, noteStart + 0.008);
            volume.gain.exponentialRampToValueAtTime(0.0001, noteStart + duration);
            oscillator.connect(volume);
            volume.connect(context.destination);
            oscillator.start(noteStart);
            oscillator.stop(noteStart + duration + 0.01);
        });
    };

    updateToggle();
    soundToggle.addEventListener("click", () => {
        enabled = !enabled;
        saveEnabled(enabled);
        updateToggle();
        if (enabled) {
            playSound("toggle");
        }
    });

    let lastClickAt = 0;
    document.addEventListener("click", event => {
        if (!(event.target instanceof Element)) {
            return;
        }
        const target = event.target.closest("button, a[href], summary, input[type='submit'], [role='button']");
        if (!target || target.matches("[data-sound-toggle]")) {
            return;
        }

        if (target.matches(".topbar-logout")) {
            event.preventDefault();
            playSound("logout");
            window.setTimeout(() => window.location.assign(target.href), enabled ? 180 : 0);
            return;
        }

        if (target.closest('form[action$="/login"]')) {
            return;
        }

        const now = performance.now();
        if (now - lastClickAt < 45) {
            return;
        }
        lastClickAt = now;
        playSound("click");
    });

    document.addEventListener("submit", event => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement) || !form.matches('form[action$="/login"]') || loginSubmissionPending) {
            return;
        }
        loginSubmissionPending = true;
        playSound("click");
    });

    if (document.body.dataset.soundEvent === "login") {
        playSound("login");
        delete document.body.dataset.soundEvent;
    }
})();
