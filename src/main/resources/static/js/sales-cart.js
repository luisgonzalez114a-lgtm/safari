(() => {
    const form = document.querySelector(".sale-form");
    const lineContainer = document.getElementById("sale-lines");
    const productSearch = document.getElementById("sale-product-search");
    const quantityInput = document.getElementById("sale-quantity");
    const addItemButton = document.getElementById("add-sale-item");
    const totalElement = document.getElementById("sale-total");
    const confirmButton = document.querySelector(".sale-confirm-button");
    const errorElement = document.getElementById("sale-picker-error");
    const productOptions = document.querySelectorAll("#sale-product-options option");

    if (!form || !lineContainer || !productSearch || !quantityInput || !addItemButton
            || !totalElement || !confirmButton || !errorElement) {
        return;
    }

    const money = new Intl.NumberFormat("es-VE", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    const products = Array.from(productOptions, option => ({
        id: option.dataset.productId,
        value: option.value,
        code: option.dataset.code,
        name: option.dataset.name,
        price: Number(option.dataset.price),
        stock: Number.parseInt(option.dataset.stock, 10)
    }));
    const selectedProducts = new Map();

    function showError(message) {
        errorElement.textContent = message;
        errorElement.hidden = !message;
    }

    function renderCart() {
        let total = 0;
        lineContainer.replaceChildren();

        selectedProducts.forEach((item, productId) => {
            const subtotal = item.product.price * item.quantity;
            total += subtotal;

            const row = document.createElement("tr");
            const productCell = document.createElement("td");
            productCell.textContent = `${item.product.code} · ${item.product.name}`;

            const quantityCell = document.createElement("td");
            quantityCell.textContent = String(item.quantity);

            const priceCell = document.createElement("td");
            priceCell.textContent = `$${money.format(item.product.price)}`;

            const subtotalCell = document.createElement("td");
            subtotalCell.className = "sale-cart-subtotal";
            subtotalCell.textContent = `$${money.format(subtotal)}`;

            const actionCell = document.createElement("td");
            const removeButton = document.createElement("button");
            removeButton.type = "button";
            removeButton.className = "btn btn-danger remove-sale-item";
            removeButton.dataset.productId = productId;
            removeButton.textContent = "Quitar";
            removeButton.setAttribute("aria-label", `Quitar ${item.product.name}`);
            actionCell.appendChild(removeButton);

            const productIdInput = document.createElement("input");
            productIdInput.type = "hidden";
            productIdInput.name = "productoIds";
            productIdInput.value = productId;

            const quantityValueInput = document.createElement("input");
            quantityValueInput.type = "hidden";
            quantityValueInput.name = "cantidades";
            quantityValueInput.value = item.quantity;

            productCell.append(productIdInput, quantityValueInput);
            row.append(productCell, quantityCell, priceCell, subtotalCell, actionCell);
            lineContainer.appendChild(row);
        });

        if (selectedProducts.size === 0) {
            const emptyRow = document.createElement("tr");
            emptyRow.className = "sale-cart-empty";
            const emptyCell = document.createElement("td");
            emptyCell.colSpan = 5;
            emptyCell.textContent = "Aún no has añadido productos.";
            emptyRow.appendChild(emptyCell);
            lineContainer.appendChild(emptyRow);
        }

        totalElement.textContent = `$${money.format(total)}`;
        confirmButton.disabled = selectedProducts.size === 0;
    }

    function addProduct() {
        const product = products.find(item => item.value === productSearch.value);
        const amount = Number.parseInt(quantityInput.value, 10);

        if (!product) {
            showError("Selecciona un producto de la lista de sugerencias.");
            productSearch.focus();
            return;
        }
        if (!Number.isInteger(amount) || amount < 1) {
            showError("Indica una cantidad válida, mayor que cero.");
            quantityInput.focus();
            return;
        }

        const existing = selectedProducts.get(product.id);
        const newAmount = amount + (existing ? existing.quantity : 0);
        if (newAmount > product.stock) {
            showError(`Solo hay ${product.stock} unidades disponibles de ${product.name}.`);
            quantityInput.focus();
            return;
        }

        selectedProducts.set(product.id, {product, quantity: newAmount});
        productSearch.value = "";
        quantityInput.value = "1";
        showError("");
        renderCart();
        productSearch.focus();
    }

    addItemButton.addEventListener("click", addProduct);
    form.addEventListener("keydown", event => {
        if (event.key === "Enter" && (event.target === productSearch || event.target === quantityInput)) {
            event.preventDefault();
            addProduct();
        }
    });
    lineContainer.addEventListener("click", event => {
        const removeButton = event.target.closest(".remove-sale-item");
        if (removeButton) {
            selectedProducts.delete(removeButton.dataset.productId);
            renderCart();
        }
    });

    renderCart();
})();
