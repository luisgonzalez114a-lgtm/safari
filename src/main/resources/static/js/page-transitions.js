(() => {
    const managedDisclosureSelector = [
        ".add-disclosure",
        ".user-form-disclosure",
        ".inventory-row-details",
        ".supplier-row-details",
        ".catalog-row-details",
        ".user-row-details",
        ".user-inline-edit",
        ".supplier-inline-edit",
        ".catalog-inline-edit"
    ].join(",");
    const contextElementsSelector = [
        ".safari-sidebar",
        ".topbar",
        ".hero-grid",
        ".stat-card",
        ".panel",
        ".layout",
        ".users-section",
        ".users-section-heading",
        ".users-section-actions",
        ".section-heading-row",
        ".panel > h2",
        ".section-title",
        ".permission-reference",
        ".pagination",
        ".sale-products-panel",
        ".sale-checkout",
        ".notice",
        ".alert-box",
        ".table-wrap"
    ].join(",");
    const listSelector = ".data-table, .simple-list, .catalog-list, .table-wrap";
    const dimmedSelector = ".safari-dimmed, .safari-row-dimmed";
    const openDisclosureSelector = managedDisclosureSelector
        .split(",")
        .map((selector) => `${selector}[open]`)
        .join(",");

    const syncDisclosureState = () => {
        const openDisclosures = [...document.querySelectorAll(managedDisclosureSelector)]
            .filter((disclosure) => disclosure.open);
        const appShell = document.querySelector(".app-shell");
        if (!appShell) return;

        appShell.querySelectorAll(".safari-dimmed, .safari-row-dimmed").forEach((element) => {
            element.classList.remove("safari-dimmed", "safari-row-dimmed");
        });

        const activeDisclosure = openDisclosures[openDisclosures.length - 1];
        if (!activeDisclosure) return;

        const activeRow = activeDisclosure.closest("tr");
        const contextTarget = activeRow
            || activeDisclosure.closest(".panel, .users-section")
            || activeDisclosure;
        const contextElements = appShell.querySelectorAll(contextElementsSelector);
        contextElements.forEach((element) => {
            if (element.contains(contextTarget) || contextTarget.contains(element)) return;
            if (!activeRow && element.querySelector(listSelector)) return;
            if (element.parentElement?.closest(".safari-dimmed")) return;
            element.classList.add("safari-dimmed");
        });

        if (activeRow) {
            const rowBody = activeRow.parentElement;
            if (rowBody) {
                [...rowBody.rows].forEach((row) => {
                    if (row !== activeRow) row.classList.add("safari-row-dimmed");
                });
            }
        }
    };

    document.querySelectorAll(managedDisclosureSelector).forEach((disclosure) => {
        disclosure.addEventListener("toggle", () => {
            syncDisclosureState();
            if (disclosure.open) {
                window.requestAnimationFrame(() => {
                    const target = disclosure.closest("tr")
                        || disclosure.closest(".panel, .users-section")
                        || disclosure;
                    target.scrollIntoView({behavior: "smooth", block: "start", inline: "nearest"});
                });
            }
        });
    });
    syncDisclosureState();
    const initiallyOpenDisclosure = [...document.querySelectorAll(managedDisclosureSelector)]
        .filter((disclosure) => disclosure.open)
        .pop();
    if (initiallyOpenDisclosure) {
        window.requestAnimationFrame(() => {
            const target = initiallyOpenDisclosure.closest("tr")
                || initiallyOpenDisclosure.closest(".panel, .users-section")
                || initiallyOpenDisclosure;
            target.scrollIntoView({behavior: "smooth", block: "start", inline: "nearest"});
        });
    }

    document.addEventListener("click", (event) => {
        if (!(event.target instanceof Element) || !event.target.closest(dimmedSelector)) return;

        event.preventDefault();
        event.stopPropagation();
        document.querySelectorAll(openDisclosureSelector).forEach((disclosure) => {
            disclosure.open = false;
        });
        syncDisclosureState();
    }, true);

    const inventoryTarget = document.getElementById("inventory-title")
        || document.getElementById("inventory-workspace");
    const inventoryLink = document.querySelector('.sidebar-link[href$="#inventory-workspace"]');
    const scrollInventoryToCenter = () => {
        inventoryTarget?.scrollIntoView({behavior: "smooth", block: "start", inline: "nearest"});
    };
    if (inventoryLink) {
        inventoryLink.addEventListener("click", (event) => {
            const linkUrl = new URL(inventoryLink.href);
            if (linkUrl.pathname !== window.location.pathname) return;
            event.preventDefault();
            scrollInventoryToCenter();
        });
    }
    if (inventoryTarget && window.location.hash === "#inventory-workspace") {
        window.requestAnimationFrame(scrollInventoryToCenter);
        window.history.replaceState(null, "", `${window.location.pathname}${window.location.search}`);
    }

    const themeToggle = document.querySelector(".theme-toggle");
    if (themeToggle) {
        const savedTheme = localStorage.getItem("safari-theme");
        const applyTheme = (theme) => {
            document.documentElement.dataset.theme = theme;
            const isDark = theme === "dark";
            themeToggle.textContent = isDark ? "☀" : "☾";
            themeToggle.setAttribute("aria-label", `Cambiar a tema ${isDark ? "claro" : "oscuro"}`);
            themeToggle.setAttribute("aria-pressed", String(!isDark));
            themeToggle.title = `Cambiar a tema ${isDark ? "claro" : "oscuro"}`;
        };

        applyTheme(savedTheme === "light" ? "light" : "dark");
        themeToggle.addEventListener("click", () => {
            const nextTheme = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
            localStorage.setItem("safari-theme", nextTheme);
            applyTheme(nextTheme);
        });
    }

    document.querySelectorAll(".user-form-disclosure").forEach((disclosure) => {
        disclosure.addEventListener("toggle", () => {
            if (!disclosure.open) return;
            document.querySelectorAll(".user-form-disclosure").forEach((otherDisclosure) => {
                if (otherDisclosure !== disclosure) otherDisclosure.open = false;
            });
        });
    });

    document.querySelectorAll(".data-table:not(.users-table)").forEach((table) => {
        const body = table.tBodies[0];
        if (!body) return;

        const rows = [...body.rows].filter((row) => !row.querySelector(".empty-state"));
        const pageSize = 5;
        const pageCount = Math.ceil(rows.length / pageSize);
        if (pageCount < 2) return;

        let currentPage = 0;
        const pagination = document.createElement("nav");
        pagination.className = "pagination table-pagination";
        pagination.setAttribute("aria-label", "Paginación de filas");

        const previous = document.createElement("button");
        previous.className = "btn btn-secondary";
        previous.type = "button";
        previous.textContent = "Anterior";

        const status = document.createElement("span");
        status.className = "pagination-current";
        status.setAttribute("aria-live", "polite");

        const next = document.createElement("button");
        next.className = "btn btn-secondary";
        next.type = "button";
        next.textContent = "Siguiente";

        const renderPage = () => {
            const firstRow = currentPage * pageSize;
            rows.forEach((row, index) => {
                row.hidden = index < firstRow || index >= firstRow + pageSize;
                if (row.hidden) {
                    row.querySelectorAll(managedDisclosureSelector).forEach((disclosure) => {
                        disclosure.open = false;
                    });
                }
            });
            syncDisclosureState();
            status.textContent = `Página ${currentPage + 1} de ${pageCount} · filas ${firstRow + 1}–${Math.min(firstRow + pageSize, rows.length)} de ${rows.length}`;
            previous.disabled = currentPage === 0;
            next.disabled = currentPage === pageCount - 1;
        };

        previous.addEventListener("click", () => {
            if (currentPage === 0) return;
            currentPage -= 1;
            renderPage();
        });
        next.addEventListener("click", () => {
            if (currentPage >= pageCount - 1) return;
            currentPage += 1;
            renderPage();
        });

        pagination.append(previous, status, next);
        table.closest(".table-wrap")?.after(pagination);
        renderPage();
    });

    const loader = document.querySelector(".page-loader");
    const logoutLink = document.querySelector(".topbar-logout");
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    let loaderShownAt = 0;
    let isLoggingOut = false;

    const showLoader = (text) => {
        if (!loader) return;
        const message = loader.querySelector(".page-loader-message");
        if (message) message.textContent = text;
        loader.setAttribute("aria-hidden", "false");
        loader.classList.add("is-visible");
        document.body.classList.add("is-loading");
        loaderShownAt = Date.now();
    };

    const hideLoader = () => {
        if (!loader) return;
        const minimumVisibleTime = reducedMotion ? 0 : 600;
        const remainingTime = Math.max(0, minimumVisibleTime - (Date.now() - loaderShownAt));
        window.setTimeout(() => {
            loader.classList.remove("is-visible");
            loader.setAttribute("aria-hidden", "true");
            document.body.classList.remove("is-loading");
        }, remainingTime);
    };

    if (loader) {
        showLoader("Cargando SAFARI...");
        if (document.readyState === "complete") {
            hideLoader();
        } else {
            window.addEventListener("load", hideLoader, { once: true });
        }
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
