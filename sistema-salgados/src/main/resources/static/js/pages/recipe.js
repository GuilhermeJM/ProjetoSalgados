import { api, ApiError } from '../api.js';
import { esc, toast, parseDecimal, openFormDialog } from '../ui.js';
import { formatNumber, formatDateTime, unitLabel } from '../format.js';

const UNITS_BY_BASE = { G: ['G', 'KG'], ML: ['ML', 'L'], UN: ['UN'] };

const toInput = (n) => String(n).replace('.', ',');

export async function render(container, params) {
  const productId = Number(params.id);
  // Uma <div> própria: os eventos ficam nela e somem junto quando o usuário troca de tela.
  const view = document.createElement('div');
  container.replaceChildren(view);

  let product = null;
  let recipes = [];

  async function load() {
    view.innerHTML = '<p class="state">Carregando…</p>';
    try {
      const [p, r] = await Promise.all([
        api('GET', `/api/products/${productId}`),
        api('GET', `/api/products/${productId}/recipes`),
      ]);
      product = p;
      recipes = r;
      renderPage();
    } catch (err) {
      view.innerHTML = `<div class="card"><p class="state state-error">${esc(err.message)}</p></div>`;
    }
  }

  // ---------- exibição ----------

  function itemsTable(recipe) {
    const rows = recipe.items.map((i) => `
      <tr>
        <td>${esc(i.ingredientName)}</td>
        <td class="num">${formatNumber(i.displayQuantity)} ${unitLabel(i.displayUnit)}</td>
      </tr>`).join('');
    return `
      <table>
        <thead><tr><th>Ingrediente</th><th class="num">Quantidade por lote</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>`;
  }

  function metaLine(recipe) {
    return `Rende ${formatNumber(recipe.yieldQuantity)} un por lote · criada em ${formatDateTime(recipe.createdAt)}`;
  }

  function activeCard(recipe) {
    return `
      <section class="card recipe-card">
        <div class="recipe-card-head">
          <div>
            <strong>Versão ${recipe.version}</strong>
            <span class="badge badge-ok">Ativa</span>
            <div class="recipe-meta">${esc(metaLine(recipe))}</div>
          </div>
        </div>
        ${itemsTable(recipe)}
      </section>`;
  }

  function archivedCard(recipe) {
    return `
      <details class="card recipe-card">
        <summary class="recipe-summary">
          <strong>Versão ${recipe.version}</strong>
          <span class="badge badge-off">Arquivada</span>
          <span class="recipe-meta">${esc(metaLine(recipe))}</span>
        </summary>
        ${itemsTable(recipe)}
        <div class="recipe-actions">
          <button type="button" class="btn btn-sm" data-edit="${recipe.id}">Usar como base para nova versão</button>
        </div>
      </details>`;
  }

  function renderPage() {
    const active = recipes.find((r) => r.status === 'ACTIVE');
    const archived = recipes.filter((r) => r.status !== 'ACTIVE');

    view.innerHTML = `
      <header class="page-header">
        <div>
          <a href="#/produtos" class="back-link">← Produtos e receitas</a>
          <h1>Receita: ${esc(product.name)}</h1>
          <p class="subtitle">Cada alteração cria uma nova versão. As anteriores ficam guardadas para o histórico de produções.</p>
        </div>
        <button type="button" class="btn btn-primary" data-edit="${active ? active.id : ''}">
          ${active ? 'Editar receita (nova versão)' : 'Criar receita'}
        </button>
      </header>
      ${active
        ? activeCard(active)
        : '<div class="card"><p class="state">Este produto ainda não tem receita. Clique em “Criar receita” para começar.</p></div>'}
      ${archived.length
        ? `<h2 class="section-title">Versões anteriores</h2>${archived.map(archivedCard).join('')}`
        : ''}`;
  }

  // ---------- editor ----------

  function fillUnits(row, wantedUnit) {
    const base = row.querySelector('.row-ingredient').selectedOptions[0]?.dataset.base;
    const unitSelect = row.querySelector('.row-unit');
    const previous = wantedUnit ?? unitSelect.value;
    const units = base ? UNITS_BY_BASE[base] : [];
    unitSelect.innerHTML = units.map((u) => `<option value="${u}">${unitLabel(u)}</option>`).join('');
    if (units.includes(previous)) unitSelect.value = previous;
  }

  function addRow(rowsEl, ingredients, preset) {
    const row = document.createElement('div');
    row.className = 'recipe-row';
    const options = ingredients.map((i) =>
      `<option value="${i.id}" data-base="${i.baseUnit}">${esc(i.name)}</option>`).join('');
    row.innerHTML = `
      <div class="recipe-row-main">
        <select class="row-ingredient" aria-label="Ingrediente">
          <option value="">Selecione…</option>${options}
        </select>
        <input class="row-qty" type="text" inputmode="decimal" placeholder="Qtd." aria-label="Quantidade">
        <select class="row-unit" aria-label="Unidade"></select>
        <button type="button" class="btn btn-sm" data-remove aria-label="Remover ingrediente">✕</button>
      </div>
      <small class="field-error" data-field="ingredientId"></small>
      <small class="field-error" data-field="quantity"></small>`;
    rowsEl.appendChild(row);

    if (preset) {
      row.querySelector('.row-ingredient').value = String(preset.ingredientId);
      row.querySelector('.row-qty').value = toInput(preset.displayQuantity);
      fillUnits(row, preset.displayUnit);
    } else {
      fillUnits(row);
    }
    reindex(rowsEl);
  }

  /** Mantém os data-error-for alinhados com a posição de cada linha (items[0].quantity etc.). */
  function reindex(rowsEl) {
    rowsEl.querySelectorAll('.recipe-row').forEach((row, index) => {
      row.querySelectorAll('[data-field]').forEach((el) =>
        el.setAttribute('data-error-for', `items[${index}].${el.dataset.field}`));
    });
  }

  async function openEditor(base) {
    let ingredients;
    try {
      ingredients = await api('GET', '/api/ingredients?active=true');
    } catch (err) {
      toast(err.message, 'error');
      return;
    }

    // Ingrediente da versão-base que foi inativado depois: aparece marcado, e a API explica o bloqueio.
    for (const item of base?.items ?? []) {
      if (!ingredients.some((i) => i.id === item.ingredientId)) {
        ingredients.push({ id: item.ingredientId, name: `${item.ingredientName} (inativo)`, baseUnit: item.baseUnit });
      }
    }
    if (ingredients.length === 0) {
      toast('Cadastre pelo menos um ingrediente antes de criar uma receita.', 'error');
      return;
    }

    openFormDialog({
      title: base ? `Nova versão de “${product.name}”` : `Receita de “${product.name}”`,
      submitLabel: 'Salvar nova versão',
      wide: true,
      bodyHtml: `
        <div class="field">
          <label for="f-yield">Rendimento do lote *</label>
          <div class="input-group">
            <input id="f-yield" name="yieldQuantity" type="text" inputmode="decimal"
                   value="${base ? esc(toInput(base.yieldQuantity)) : ''}">
            <span class="input-suffix">un</span>
          </div>
          <small class="hint">Quantas unidades prontas as quantidades abaixo rendem.</small>
          <small class="field-error" data-error-for="yieldQuantity"></small>
        </div>
        <div class="field">
          <label>Ingredientes do lote *</label>
          <div id="recipe-rows"></div>
          <div><button type="button" class="btn btn-sm" id="add-row">+ Adicionar ingrediente</button></div>
          <small class="field-error" data-error-for="items"></small>
        </div>`,
      onReady: (form) => {
        const rowsEl = form.querySelector('#recipe-rows');
        for (const item of base?.items ?? [null]) addRow(rowsEl, ingredients, item);

        form.querySelector('#add-row').addEventListener('click', () => addRow(rowsEl, ingredients, null));
        rowsEl.addEventListener('change', (event) => {
          if (event.target.matches('.row-ingredient')) fillUnits(event.target.closest('.recipe-row'));
        });
        rowsEl.addEventListener('click', (event) => {
          if (!event.target.closest('[data-remove]')) return;
          event.target.closest('.recipe-row').remove();
          reindex(rowsEl);
        });
      },
      onSubmit: async (form) => {
        const yieldQuantity = parseDecimal(form.elements.yieldQuantity.value);
        if (yieldQuantity === null || Number.isNaN(yieldQuantity)) {
          throw new ApiError({
            message: 'Há campos inválidos. Corrija-os e tente novamente.',
            fieldErrors: { yieldQuantity: 'Informe um número válido (ex.: 100).' },
          }, 400);
        }

        const items = [];
        const fieldErrors = {};
        form.querySelectorAll('.recipe-row').forEach((row, index) => {
          const ingredientId = Number(row.querySelector('.row-ingredient').value);
          const quantity = parseDecimal(row.querySelector('.row-qty').value);
          if (!ingredientId) fieldErrors[`items[${index}].ingredientId`] = 'Selecione o ingrediente.';
          if (quantity === null || Number.isNaN(quantity)) {
            fieldErrors[`items[${index}].quantity`] = 'Informe um número válido (ex.: 2,5).';
          }
          items.push({ ingredientId, quantity, unit: row.querySelector('.row-unit').value });
        });
        if (Object.keys(fieldErrors).length > 0) {
          throw new ApiError({ message: 'Há campos inválidos. Corrija-os e tente novamente.', fieldErrors }, 400);
        }

        const saved = await api('POST', `/api/products/${productId}/recipes`, { yieldQuantity, items });
        toast(`Receita salva como versão ${saved.version}.`);
        await load();
      },
    });
  }

  view.addEventListener('click', (event) => {
    const button = event.target.closest('[data-edit]');
    if (!button) return;
    const base = recipes.find((r) => r.id === Number(button.dataset.edit)) ?? null;
    openEditor(base);
  });

  await load();
}