import { api, ApiError } from '../api.js';
import {
  esc, toast, debounce, parseDecimal, sortHeader, toggleSort, sortItems,
  openFormDialog, confirmDialog,
} from '../ui.js';
import { formatQuantity, formatCostPerUnit, formUnit, unitLabel } from '../format.js';

const BASE_OPTIONS = [
  { value: 'G', label: 'Massa (g)' },
  { value: 'ML', label: 'Volume (ml)' },
  { value: 'UN', label: 'Contagem (un)' },
];
const UNITS_BY_BASE = { G: ['G', 'KG'], ML: ['ML', 'L'], UN: ['UN'] };

export async function render(container) {
  const state = { items: [], search: '', active: 'true', sortKey: 'name', sortDir: 'asc' };

  container.innerHTML = `
    <header class="page-header">
      <div>
        <h1>Ingredientes</h1>
        <p class="subtitle">Cadastro de insumos, estoque mínimo e custo médio.</p>
      </div>
      <button type="button" class="btn btn-primary" id="btn-new">+ Novo ingrediente</button>
    </header>
    <div class="toolbar">
      <input type="search" id="search" placeholder="Buscar por nome…" aria-label="Buscar ingrediente">
      <select id="filter-active" aria-label="Filtrar por situação">
        <option value="true">Ativos</option>
        <option value="false">Inativos</option>
        <option value="">Todos</option>
      </select>
    </div>
    <div id="list" class="card"></div>`;

  const listEl = container.querySelector('#list');

  async function load() {
    listEl.innerHTML = '<p class="state">Carregando…</p>';
    try {
      const params = new URLSearchParams();
      if (state.search) params.set('search', state.search);
      if (state.active !== '') params.set('active', state.active);
      state.items = await api('GET', `/api/ingredients?${params}`);
      renderList();
    } catch (err) {
      listEl.innerHTML = `<p class="state state-error">${esc(err.message)}</p>`;
    }
  }

  function renderList() {
    if (state.items.length === 0) {
      const filtered = state.search || state.active !== 'true';
      listEl.innerHTML = filtered
        ? '<p class="state">Nenhum ingrediente encontrado com esses filtros.</p>'
        : '<p class="state">Nenhum ingrediente cadastrado ainda. Clique em “+ Novo ingrediente” para começar.</p>';
      return;
    }

    const rows = sortItems(state.items, state.sortKey, state.sortDir).map((i) => `
      <tr class="${i.active ? '' : 'row-inactive'}">
        <td><strong>${esc(i.name)}</strong></td>
        <td>${esc(i.category ?? '—')}</td>
        <td class="num">
          ${formatQuantity(i.currentStock, i.baseUnit)}
          ${i.belowMinimum ? '<span class="badge badge-warn">Abaixo do mínimo</span>' : ''}
        </td>
        <td class="num">${formatQuantity(i.minimumStock, i.baseUnit)}</td>
        <td class="num">${formatCostPerUnit(i.currentAverageCost, i.baseUnit)}</td>
        <td>${i.active
          ? '<span class="badge badge-ok">Ativo</span>'
          : '<span class="badge badge-off">Inativo</span>'}</td>
        <td class="actions">
          <button type="button" class="btn btn-sm" data-action="edit" data-id="${i.id}">Editar</button>
          <button type="button" class="btn btn-sm" data-action="toggle" data-id="${i.id}">${i.active ? 'Inativar' : 'Reativar'}</button>
        </td>
      </tr>`).join('');

    listEl.innerHTML = `
      <table>
        <thead>
          <tr>
            ${sortHeader('Nome', 'name', state)}
            ${sortHeader('Categoria', 'category', state)}
            ${sortHeader('Estoque atual', 'currentStock', state, 'num')}
            ${sortHeader('Mínimo', 'minimumStock', state, 'num')}
            ${sortHeader('Custo médio', 'currentAverageCost', state, 'num')}
            ${sortHeader('Situação', 'active', state)}
            <th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;
  }

  // ---------- formulário ----------

  function formBody(item, categories) {
    const base = item?.baseUnit ?? 'G';
    const minimum = item ? formUnit(item.minimumStock, base) : { value: '', unit: base };
    const unitOptions = UNITS_BY_BASE[base].map((u) =>
      `<option value="${u}" ${u === minimum.unit ? 'selected' : ''}>${unitLabel(u)}</option>`).join('');
    const baseOptions = BASE_OPTIONS.map((o) =>
      `<option value="${o.value}" ${o.value === base ? 'selected' : ''}>${o.label}</option>`).join('');
    const categoryOptions = categories.map((c) => `<option value="${esc(c)}"></option>`).join('');

    return `
      <div class="field">
        <label for="f-name">Nome *</label>
        <input id="f-name" name="name" type="text" maxlength="100" value="${esc(item?.name ?? '')}">
        <small class="field-error" data-error-for="name"></small>
      </div>
      <div class="field">
        <label for="f-category">Categoria</label>
        <input id="f-category" name="category" type="text" maxlength="50" list="category-options"
               value="${esc(item?.category ?? '')}">
        <datalist id="category-options">${categoryOptions}</datalist>
        <small class="field-error" data-error-for="category"></small>
      </div>
      <div class="field">
        <label for="f-base">Unidade-base *</label>
        <select id="f-base" name="baseUnit">${baseOptions}</select>
        <small class="hint">Unidade em que o estoque é controlado. Não pode ser trocada depois que houver compras, receitas ou movimentações.</small>
        <small class="field-error" data-error-for="baseUnit"></small>
      </div>
      <div class="field field-check" data-fraction ${base === 'UN' ? '' : 'hidden'}>
        <input id="f-fraction" name="allowFraction" type="checkbox" ${item?.allowFraction ? 'checked' : ''}>
        <label for="f-fraction">Permitir quantidades fracionadas (ex.: 0,5 un)</label>
      </div>
      <div class="field">
        <label for="f-min">Estoque mínimo *</label>
        <div class="input-group">
          <input id="f-min" name="minimumStock" type="text" inputmode="decimal"
                 value="${minimum.value === '' ? '' : esc(String(minimum.value).replace('.', ','))}">
          <select name="minimumStockUnit" aria-label="Unidade do estoque mínimo">${unitOptions}</select>
        </div>
        <small class="hint">Abaixo desse valor o ingrediente aparece em alerta. Use 0 se não houver mínimo.</small>
        <small class="field-error" data-error-for="minimumStock"></small>
      </div>`;
  }

  function bindFormBehavior(form) {
    const baseSelect = form.elements.baseUnit;
    const unitSelect = form.elements.minimumStockUnit;
    const fractionWrap = form.querySelector('[data-fraction]');

    baseSelect.addEventListener('change', () => {
      const base = baseSelect.value;
      const previous = unitSelect.value;
      unitSelect.innerHTML = UNITS_BY_BASE[base]
        .map((u) => `<option value="${u}">${unitLabel(u)}</option>`).join('');
      unitSelect.value = UNITS_BY_BASE[base].includes(previous) ? previous : base;
      fractionWrap.hidden = base !== 'UN';
      if (base !== 'UN') form.elements.allowFraction.checked = false;
    });
  }

  async function openForm(item = null) {
    let categories = [];
    try {
      const all = await api('GET', '/api/ingredients');
      categories = [...new Set(all.map((i) => i.category).filter(Boolean))].sort();
    } catch {
      // sem sugestões de categoria; o formulário continua funcionando
    }

    openFormDialog({
      title: item ? 'Editar ingrediente' : 'Novo ingrediente',
      bodyHtml: formBody(item, categories),
      onReady: bindFormBehavior,
      onSubmit: async (form) => {
        const minimum = parseDecimal(form.elements.minimumStock.value);
        if (minimum === null || Number.isNaN(minimum)) {
          throw new ApiError({
            message: 'Há campos inválidos. Corrija-os e tente novamente.',
            fieldErrors: { minimumStock: 'Informe um número válido (ex.: 2,5). Use 0 se não houver mínimo.' },
          }, 400);
        }
        const body = {
          name: form.elements.name.value,
          category: form.elements.category.value,
          baseUnit: form.elements.baseUnit.value,
          allowFraction: form.elements.allowFraction.checked,
          minimumStock: minimum,
          minimumStockUnit: form.elements.minimumStockUnit.value,
        };
        if (item) {
          await api('PUT', `/api/ingredients/${item.id}`, body);
        } else {
          await api('POST', '/api/ingredients', body);
        }
        toast(item ? 'Ingrediente atualizado.' : 'Ingrediente cadastrado.');
        await load();
      },
    });
  }

  async function toggleActive(item) {
    const next = !item.active;
    if (!next) {
      const ok = await confirmDialog({
        title: 'Inativar ingrediente',
        message: `“${item.name}” deixará de aparecer nas listas de seleção. `
          + 'O histórico, as compras e as receitas existentes são preservados.',
        confirmLabel: 'Inativar',
        danger: true,
      });
      if (!ok) return;
    }
    try {
      await api('PATCH', `/api/ingredients/${item.id}/status`, { active: next });
      toast(next ? 'Ingrediente reativado.' : 'Ingrediente inativado.');
      await load();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // ---------- eventos ----------

  container.querySelector('#btn-new').addEventListener('click', () => openForm());

  container.querySelector('#search').addEventListener('input', debounce((event) => {
    state.search = event.target.value.trim();
    load();
  }, 250));

  container.querySelector('#filter-active').addEventListener('change', (event) => {
    state.active = event.target.value;
    load();
  });

  listEl.addEventListener('click', (event) => {
    const sortButton = event.target.closest('[data-sort]');
    if (sortButton) {
      toggleSort(state, sortButton.dataset.sort);
      renderList();
      return;
    }
    const button = event.target.closest('[data-action]');
    if (!button) return;
    const item = state.items.find((i) => i.id === Number(button.dataset.id));
    if (!item) return;
    if (button.dataset.action === 'edit') openForm(item);
    else toggleActive(item);
  });

  await load();
}