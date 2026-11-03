(() => {
    const searchInput = document.getElementById("inventory-search-input");
    const table = document.querySelector(".inventory-table");
    const resultCount = document.getElementById("inventory-search-count");
    const noResults = document.getElementById("inventory-no-results");
    const emptyCatalog = document.getElementById("inventory-empty");

    if (!searchInput || !table || !resultCount || !noResults || !table.tBodies[0]) {
        return;
    }

    const rows = Array.from(table.tBodies[0].querySelectorAll("[data-product-search]"));
    const normalize = value => value
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLocaleLowerCase();
    const total = rows.length;

    const filterProducts = () => {
        const query = normalize(searchInput.value.trim());
        let visibleCount = 0;

        rows.forEach(row => {
            const matches = normalize(row.dataset.productSearch || "").includes(query);
            row.hidden = !matches;
            if (!matches) {
                row.querySelectorAll("details[open]").forEach(details => {
                    details.open = false;
                });
            } else {
                visibleCount++;
            }
        });

        noResults.hidden = !query || visibleCount > 0;
        if (emptyCatalog) {
            emptyCatalog.hidden = Boolean(query);
        }
        resultCount.textContent = query
            ? `${visibleCount} de ${total} productos`
            : `${total} productos`;
    };

    searchInput.addEventListener("input", filterProducts);
    filterProducts();
})();
