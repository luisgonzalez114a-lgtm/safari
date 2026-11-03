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
        const loadTheme = () => {
            try {
                return window.localStorage.getItem("safari-theme");
            } catch (error) {
                console.warn("No se pudo leer la preferencia de tema de SAFARI.", error);
                return null;
            }
        };
        const saveTheme = (theme) => {
            try {
                window.localStorage.setItem("safari-theme", theme);
            } catch (error) {
                console.warn("No se pudo guardar la preferencia de tema de SAFARI.", error);
            }
        };
        const savedTheme = loadTheme();
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
            saveTheme(nextTheme);
            applyTheme(nextTheme);
        });
    }

    if (themeToggle?.dataset.authenticated === "true") {
        const pawTrail = document.createElement("div");
        pawTrail.className = "paw-trail";
        pawTrail.setAttribute("aria-hidden", "true");
        document.body.append(pawTrail);

        const leavePawPrint = () => {
            const print = document.createElementNS("http://www.w3.org/2000/svg", "svg");
            print.classList.add("paw-print");
            print.setAttribute("viewBox", "0 0 64 72");
            print.setAttribute("aria-hidden", "true");

            const pad = document.createElementNS("http://www.w3.org/2000/svg", "path");
            pad.setAttribute("d", "M32 36c-11 0-19 8-19 18 0 8 7 12 19 12s19-4 19-12c0-10-8-18-19-18Z");
            print.append(pad);

            [[14, 29, 6, 8], [25, 18, 6, 8], [39, 18, 6, 8], [50, 29, 6, 8]].forEach(([cx, cy, rx, ry]) => {
                const toe = document.createElementNS("http://www.w3.org/2000/svg", "ellipse");
                toe.setAttribute("cx", cx);
                toe.setAttribute("cy", cy);
                toe.setAttribute("rx", rx);
                toe.setAttribute("ry", ry);
                print.append(toe);
            });

            const maxX = Math.max(0, window.innerWidth - 68);
            const maxY = Math.max(0, window.innerHeight - 76);
            print.style.left = `${Math.random() * maxX}px`;
            print.style.top = `${Math.random() * maxY}px`;
            print.style.setProperty("--paw-angle", `${Math.random() * 100 - 50}deg`);
            pawTrail.append(print);
            window.setTimeout(() => print.remove(), 4600);
        };

        leavePawPrint();
        window.setInterval(leavePawPrint, 1000);
    }

    document.querySelectorAll(".user-form-disclosure").forEach((disclosure) => {
        disclosure.addEventListener("toggle", () => {
            if (!disclosure.open) return;
            document.querySelectorAll(".user-form-disclosure").forEach((otherDisclosure) => {
                if (otherDisclosure !== disclosure) otherDisclosure.open = false;
            });
        });
    });

    document.querySelectorAll(".data-table:not(.users-table):not(.inventory-table)").forEach((table) => {
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
