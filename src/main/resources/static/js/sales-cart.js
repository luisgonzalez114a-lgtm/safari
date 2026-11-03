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
    const productGrid = document.getElementById("sale-product-grid");
    const emptyProductsMessage = document.getElementById("sale-products-empty");
    const categoryFilters = document.querySelector(".sale-category-filters");
    const customerType = document.getElementById("tipo-cliente");
    const registeredCustomerPanel = document.getElementById("cliente-registrado");
    const registeredCustomerSelect = document.getElementById("cliente-id");
    const temporaryCustomerPanel = document.getElementById("cliente-temporal");
    const temporaryCustomerName = document.getElementById("cliente-nombre");
    const subtotalElement = document.getElementById("sale-subtotal");
    const discountRow = document.getElementById("sale-discount-row");
    const discountElement = document.getElementById("sale-discount");
    const discountPreview = document.getElementById("sale-discount-preview");
    const saleDisclosure = document.querySelector(".sale-form")?.closest(".add-disclosure");

    if (!form || !lineContainer || !productSearch || !quantityInput || !addItemButton
            || !totalElement || !confirmButton || !errorElement || !productGrid
            || !emptyProductsMessage || !categoryFilters || !customerType
            || !registeredCustomerPanel || !registeredCustomerSelect || !temporaryCustomerPanel
            || !temporaryCustomerName || !subtotalElement || !discountRow || !discountElement
            || !discountPreview || !saleDisclosure) {
        return;
    }

    const money = new Intl.NumberFormat("es-VE", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    const categorySearchTerms = {
        dog: "perro perros canino canina caninos caninas dog",
        cat: "gato gatos felino felina felinos felinas cat",
        medicine: "medicina medicamento medicamentos farmacia remedio pastilla tableta jarabe antibiotico antiparasitario antipulgas desparasitante vacuna pipeta",
        health: "consultorio consulta veterinario veterinaria diagnostico curacion salud vitamina suplemento",
        accessories: "accesorio accesorios collar correa arnes pechera placa cama transportadora jaula comedero bebedero bozal ropa abrigo sueter pañal rascador acuario",
        toys: "juguete juguetes pelota mordedor cuerda raton pluma catnip sonajero frisbee",
        food: "comida alimento croqueta pienso concentrado snack premio galleta lata pate leche",
        hygiene: "higiene champu shampoo cepillo peine arena sanitaria toallita limpiador desinfectante dental"
    };
    const products = Array.from(productOptions, option => {
        const product = {
            id: option.dataset.productId,
            value: option.value,
            code: option.dataset.code,
            name: option.dataset.name,
            description: option.dataset.description || "",
            priceCents: Math.round(Number(option.dataset.price) * 100),
            stock: Number.parseInt(option.dataset.stock, 10)
        };
        product.categories = productCategories(product);
        product.searchText = normalize(
            `${product.code} ${product.name} ${product.description} ${product.categories
                .map(category => categorySearchTerms[category])
                .join(" ")}`
        );
        return product;
    });
    const selectedProducts = new Map();
    let activeCategory = "all";

    function normalize(value) {
        return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("es");
    }

    function productCategories(product) {
        const text = normalize(`${product.name} ${product.description}`);
        const categories = [];
        const matches = {
            dog: /\b(?:perr\w*|canin\w*|dog)\b/.test(text),
            cat: /\b(?:gat\w*|felin\w*|cat)\b/.test(text),
            medicine: /\b(?:medic\w*|farmac\w*|farmaco\w*|remedio\w*|pastill\w*|tableta\w*|jarabe\w*|antibiotic\w*|analgesic\w*|antiinflamatori\w*|antiparasitari\w*|antipulga\w*|desparasit\w*|vacun\w*|pipeta\w*|curabicher\w*|unguent\w*)\b/.test(text),
            health: /\b(?:consulta\w*|consultorio|veterinari\w*|clinic\w*|diagnostic\w*|curacion\w*|curas|herida\w*|termometro\w*|vitamina\w*|suplement\w*|salud|gasa\w*|jeringa\w*|venda\w*|sutura\w*)\b/.test(text),
            accessories: /\b(?:accesori\w*|collar\w*|correa\w*|arnes\w*|pechera\w*|placa\w*|identificacion|cama\w*|transportadora\w*|jaula\w*|comedero\w*|bebedero\w*|bozal\w*|ropa|abrigo\w*|sueter\w*|panal\w*|rascador\w*|acuario\w*|cortaunas|corta unas|tijera\w*)\b/.test(text),
            toys: /\b(?:juguete\w*|pelota\w*|mordedor\w*|cuerda\w*|raton\w*|pluma\w*|catnip|sonajero\w*|rasca\w*|frisbee|disco volador)\b/.test(text),
            food: /\b(?:comida\w*|alimento\w*|croqueta\w*|pienso|concentrado\w*|snack\w*|premio\w*|galleta\w*|lata\w*|pate\w*|leche\w*)\b/.test(text),
            hygiene: /\b(?:higiene|champu\w*|shampoo\w*|cepillo\w*|peine\w*|arena sanitaria|toallita\w*|limpiador\w*|desinfectante\w*|dental|pasta dental|cortaunas|corta unas|tijera\w*)\b/.test(text)
        };
        Object.entries(matches).forEach(([category, matched]) => {
            if (matched) categories.push(category);
        });
        return categories;
    }

    function productArt(product) {
        const artByCategory = [
            ["medicine", "🩺", "Medicamento", "medicine"],
            ["health", "🩺", "Consultorio y salud", "health"],
            ["toys", "🧸", "Juguete", "toys"],
            ["accessories", "🦴", "Accesorio", "accessories"],
            ["food", "🥣", "Alimentación", "food"],
            ["hygiene", "🧼", "Higiene y cuidado", "hygiene"],
            ["dog", "🐕", "Línea canina", "dog"],
            ["cat", "🐈", "Línea felina", "cat"]
        ];
        const match = artByCategory.find(([category]) => product.categories.includes(category));
        return match
            ? {icon: match[1], label: match[2], type: match[3]}
            : {icon: "🐾", label: "Cuidado animal", type: "general"};
    }

    function showError(message) {
        errorElement.textContent = message;
        errorElement.hidden = !message;
    }

    function updateCustomerFields() {
        const isRegistered = customerType.value === "registrado";
        registeredCustomerPanel.hidden = !isRegistered;
        registeredCustomerSelect.disabled = !isRegistered;
        registeredCustomerSelect.required = isRegistered;
        temporaryCustomerPanel.hidden = isRegistered;
        temporaryCustomerName.required = !isRegistered;
        renderCart();
    }

    function renderCart() {
        let cartSubtotalCents = 0;
        lineContainer.replaceChildren();

        selectedProducts.forEach((item, productId) => {
            const lineSubtotalCents = item.product.priceCents * item.quantity;
            cartSubtotalCents += lineSubtotalCents;

            const row = document.createElement("tr");
            const productCell = document.createElement("td");
            const productName = document.createElement("strong");
            productName.className = "sale-cart-product-name";
            productName.textContent = item.product.name;
            const productCode = document.createElement("small");
            productCode.textContent = item.product.code;
            productCell.append(productName, productCode);

            const quantityCell = document.createElement("td");
            const quantityControl = document.createElement("div");
            quantityControl.className = "sale-cart-quantity";
            const decreaseButton = document.createElement("button");
            decreaseButton.type = "button";
            decreaseButton.className = "sale-quantity-change";
            decreaseButton.dataset.productId = productId;
            decreaseButton.dataset.change = "-1";
            decreaseButton.textContent = "−";
            decreaseButton.setAttribute("aria-label", `Restar una unidad de ${item.product.name}`);
            const quantityValue = document.createElement("span");
            quantityValue.textContent = String(item.quantity);
            const increaseButton = document.createElement("button");
            increaseButton.type = "button";
            increaseButton.className = "sale-quantity-change";
            increaseButton.dataset.productId = productId;
            increaseButton.dataset.change = "1";
            increaseButton.textContent = "+";
            increaseButton.setAttribute("aria-label", `Añadir una unidad de ${item.product.name}`);
            quantityControl.append(decreaseButton, quantityValue, increaseButton);
            quantityCell.appendChild(quantityControl);

            const actionCell = document.createElement("td");
            const removeButton = document.createElement("button");
            removeButton.type = "button";
            removeButton.className = "remove-sale-item";
            removeButton.dataset.productId = productId;
            removeButton.textContent = "×";
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
            const subtotalCell = document.createElement("td");
            subtotalCell.className = "sale-cart-subtotal";
            subtotalCell.textContent = `$${money.format(lineSubtotalCents / 100)}`;
            row.append(productCell, quantityCell, subtotalCell, actionCell);
            lineContainer.appendChild(row);
        });

        if (selectedProducts.size === 0) {
            const emptyRow = document.createElement("tr");
            emptyRow.className = "sale-cart-empty";
            const emptyCell = document.createElement("td");
            emptyCell.colSpan = 4;
            emptyCell.textContent = "Tu venta está vacía. Añade productos para comenzar.";
            emptyRow.appendChild(emptyCell);
            lineContainer.appendChild(emptyRow);
        }

        const selectedCustomer = registeredCustomerSelect.selectedOptions[0];
        const hasBonus = customerType.value === "registrado"
                && selectedCustomer?.dataset.bonoDisponible === "true";
        const discountCents = hasBonus ? Math.floor((cartSubtotalCents * 5 + 50) / 100) : 0;
        subtotalElement.textContent = `$${money.format(cartSubtotalCents / 100)}`;
        discountRow.hidden = discountCents <= 0;
        discountElement.textContent = `$${money.format(discountCents / 100)}`;
        discountPreview.hidden = !hasBonus;
        discountPreview.textContent = hasBonus
                ? `Bono aplicado: 5% de descuento (${money.format(discountCents / 100)}).`
                : "";
        totalElement.textContent = `$${money.format((cartSubtotalCents - discountCents) / 100)}`;
        confirmButton.disabled = selectedProducts.size === 0;
    }

    function renderProducts() {
        const searchTerm = normalize(productSearch.value.trim());
        const searchTokens = searchTerm.split(/\s+/).filter(token => /[a-z0-9]/.test(token));
        const visibleProducts = products.filter(product => {
            const matchesSearch = searchTokens.every(token => product.searchText.includes(token));
            const matchesCategory = activeCategory === "all"
                    || product.categories.includes(activeCategory);
            return matchesSearch && matchesCategory;
        });
        productGrid.replaceChildren();

        visibleProducts.forEach(product => {
            const art = productArt(product);
            const card = document.createElement("article");
            card.className = `sale-product-card sale-product-${art.type}`;

            const addButton = document.createElement("button");
            addButton.type = "button";
            addButton.className = "sale-product-add";
            addButton.dataset.productId = product.id;
            addButton.setAttribute("aria-label", `Añadir ${product.name} a la venta`);

            const artwork = document.createElement("span");
            artwork.className = "sale-product-art";
            artwork.setAttribute("aria-hidden", "true");
            artwork.textContent = art.icon;
            const identity = document.createElement("span");
            identity.className = "sale-product-identity";
            const name = document.createElement("strong");
            name.textContent = product.name;
            const detail = document.createElement("small");
            detail.textContent = `${product.code} · ${art.label}`;
            identity.append(name, detail);
            const price = document.createElement("strong");
            price.className = "sale-product-price";
            price.textContent = `$${money.format(product.priceCents / 100)}`;
            const stock = document.createElement("small");
            stock.className = "sale-product-stock";
            const selectedItem = selectedProducts.get(product.id);
            if (selectedItem) addButton.classList.add("is-in-cart");
            stock.textContent = selectedItem
                    ? `En venta: ${selectedItem.quantity} · ${product.stock - selectedItem.quantity} por agregar`
                    : `${product.stock} disponibles`;
            addButton.append(artwork, identity, price, stock);
            card.appendChild(addButton);
            productGrid.appendChild(card);
        });

        emptyProductsMessage.hidden = visibleProducts.length > 0;
    }

    function addProduct(selectedProduct) {
        const product = selectedProduct || products.find(item => item.value === productSearch.value);
        const amount = Number(quantityInput.value);

        if (!product) {
            showError("Selecciona un producto de la lista de sugerencias.");
            productSearch.focus();
            return;
        }
        if (!Number.isSafeInteger(amount) || amount < 1) {
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
        renderProducts();
        if (!selectedProduct) productSearch.focus();
    }

    addItemButton.addEventListener("click", addProduct);
    productSearch.addEventListener("input", renderProducts);
    customerType.addEventListener("change", updateCustomerFields);
    registeredCustomerSelect.addEventListener("change", renderCart);
    categoryFilters.addEventListener("click", event => {
        const filter = event.target.closest(".sale-filter-chip");
        if (!filter) return;
        activeCategory = filter.dataset.category;
        categoryFilters.querySelectorAll(".sale-filter-chip").forEach(button => {
            const isActive = button === filter;
            button.classList.toggle("is-active", isActive);
            button.setAttribute("aria-pressed", String(isActive));
        });
        renderProducts();
    });
    productGrid.addEventListener("click", event => {
        const addButton = event.target.closest(".sale-product-add");
        if (!addButton) return;
        const product = products.find(item => item.id === addButton.dataset.productId);
        if (product) addProduct(product);
    });
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
            return;
        }
        const quantityButton = event.target.closest(".sale-quantity-change");
        if (quantityButton) {
            const item = selectedProducts.get(quantityButton.dataset.productId);
            if (!item) return;
            const nextQuantity = item.quantity + Number(quantityButton.dataset.change);
            if (nextQuantity < 1) {
                selectedProducts.delete(quantityButton.dataset.productId);
            } else if (nextQuantity > item.product.stock) {
                showError(`Solo hay ${item.product.stock} unidades disponibles de ${item.product.name}.`);
                return;
            } else {
                selectedProducts.set(quantityButton.dataset.productId, {...item, quantity: nextQuantity});
                showError("");
            }
            renderCart();
        }
    });

    updateCustomerFields();
    renderProducts();
})();
