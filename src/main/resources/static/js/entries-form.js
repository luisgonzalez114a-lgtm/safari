(() => {
    const form = document.getElementById("goods-receipt-form");
    const linesContainer = document.getElementById("receipt-lines");
    const addLineButton = document.getElementById("add-receipt-line");

    if (!form || !linesContainer || !addLineButton) {
        return;
    }

    const viewButtons = Array.from(document.querySelectorAll("[data-entry-view]"));
    const historyViews = Array.from(document.querySelectorAll("[data-entry-history-view]"));
    const viewStorageKey = "safari-entry-history-view";

    const setHistoryView = view => {
        historyViews.forEach(container => {
            container.hidden = container.dataset.entryHistoryView !== view;
        });
        viewButtons.forEach(button => {
            const isSelected = button.dataset.entryView === view;
            button.classList.toggle("is-active", isSelected);
            button.setAttribute("aria-pressed", String(isSelected));
        });
        window.localStorage.setItem(viewStorageKey, view);
    };

    const savedView = window.localStorage.getItem(viewStorageKey);
    setHistoryView(savedView === "rows" ? "rows" : "grid");
    viewButtons.forEach(button => {
        button.addEventListener("click", () => setHistoryView(button.dataset.entryView));
    });

    let nextFieldId = 0;
    const lines = () => Array.from(linesContainer.querySelectorAll("[data-receipt-line]"));

    const associateLabels = line => {
        line.querySelectorAll(".field").forEach(field => {
            const label = field.querySelector("label");
            const control = field.querySelector("input, select");
            if (label && control && !control.id) {
                const id = `receipt-field-${nextFieldId++}`;
                label.htmlFor = id;
                control.id = id;
            }
        });
    };

    const updateLine = line => {
        associateLabels(line);
        const isNew = line.querySelector("[data-product-type]").value === "NUEVO";
        const existingFields = line.querySelector("[data-existing-product]");
        const existingSelect = line.querySelector("[data-existing-select]");
        const newFields = line.querySelector("[data-new-product]");

        existingFields.hidden = isNew;
        newFields.hidden = !isNew;
        existingSelect.required = !isNew;
        line.querySelectorAll("[data-new-required]").forEach(input => {
            input.required = isNew;
        });
        if (isNew) {
            existingSelect.value = "";
        }

        const removeButton = line.querySelector("[data-remove-receipt-line]");
        removeButton.hidden = lines().length === 1;
    };

    const resetLine = line => {
        line.querySelectorAll("input, select").forEach(control => control.removeAttribute("id"));
        line.querySelectorAll("label").forEach(label => label.removeAttribute("for"));
        line.querySelector("[data-product-type]").value = "EXISTENTE";
        line.querySelector("[data-existing-select]").value = "";
        line.querySelectorAll("[data-new-product] input").forEach(input => {
            input.value = input.name === "stockMinimoNuevo" ? "0" : "";
        });
        line.querySelectorAll("[data-new-product] select").forEach(select => {
            select.selectedIndex = 0;
        });
        line.querySelector('[name="cantidad"]').value = "1";
        line.querySelector('[name="precioCompra"]').value = "";
        updateLine(line);
    };

    linesContainer.addEventListener("change", event => {
        if (event.target.matches("[data-product-type]")) {
            updateLine(event.target.closest("[data-receipt-line]"));
        }
    });

    linesContainer.addEventListener("click", event => {
        const removeButton = event.target.closest("[data-remove-receipt-line]");
        if (removeButton && lines().length > 1) {
            removeButton.closest("[data-receipt-line]").remove();
            lines().forEach(updateLine);
        }
    });

    addLineButton.addEventListener("click", () => {
        const line = lines()[0].cloneNode(true);
        resetLine(line);
        linesContainer.append(line);
        lines().forEach(updateLine);
    });

    lines().forEach(updateLine);
})();
