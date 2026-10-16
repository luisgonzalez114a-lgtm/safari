(() => {
    const loader = document.querySelector(".page-loader");
    if (!loader) return;

    const message = loader.querySelector(".page-loader-message");
    const logoutLink = document.querySelector(".topbar-logout");
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    let loaderShownAt = 0;
    let isLoggingOut = false;

    const showLoader = (text) => {
        if (message) message.textContent = text;
        loader.setAttribute("aria-hidden", "false");
        loader.classList.add("is-visible");
        document.body.classList.add("is-loading");
        loaderShownAt = Date.now();
    };

    const hideLoader = () => {
        const minimumVisibleTime = reducedMotion ? 0 : 600;
        const remainingTime = Math.max(0, minimumVisibleTime - (Date.now() - loaderShownAt));
        window.setTimeout(() => {
            loader.classList.remove("is-visible");
            loader.setAttribute("aria-hidden", "true");
            document.body.classList.remove("is-loading");
        }, remainingTime);
    };

    showLoader("Cargando SAFARI...");
    if (document.readyState === "complete") {
        hideLoader();
    } else {
        window.addEventListener("load", hideLoader, { once: true });
    }

    if (logoutLink) {
        logoutLink.addEventListener("click", (event) => {
            if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
            if (isLoggingOut) {
                event.preventDefault();
                return;
            }

            event.preventDefault();
            isLoggingOut = true;
            const logoutUrl = logoutLink.href;
            logoutLink.setAttribute("aria-disabled", "true");
            showLoader("Cerrando sesión...");
            window.setTimeout(() => window.location.assign(logoutUrl), reducedMotion ? 100 : 1200);
        });
    }
})();
