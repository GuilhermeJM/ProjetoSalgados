import { api, ApiError } from '../api.js';
import {
  esc, toast, debounce, parseDecimal, sortHeader, toggleSort, sortItems,
  openFormDialog, confirmDialog,
} from '../ui.js';
import { formatNumber } from '../format.js';

export async function render(container) {
  const state = { items: [], search: '', active: 'true', sortKey: 'name', sortDir: 'asc' };

  container.innerHTML = `
    <header class="page-header">
      <div>
        <h1>Produtos e receitas</h1>
        <p class="subtitle">Cadastro dos salgados produzidos. As receitas entram na próxima etapa.</p>
      </div>
      <button type="button" class="btn btn-primary" id="btn-new">+ Novo produto</button>
    </header>
    <div class="toolbar">
      <input type="search" id="search" placeholder="Buscar por nome…" aria-label="Buscar produto">
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
      state.items = await api('GET', `/api/products?${params}`);
      renderList();
    } catch (err) {
      listEl.innerHTML = `<p class="state state-error">${esc(err.message)}</p>`;
    }
  }

  function renderList() {
    if (state.items.length === 0) {
      const filtered = state.search || state.active !== 'true';
      listEl.innerHTML = filtered
        ? '<p class="state">Nenhum produto encontrado com esses filtros.</p>'
        : '<p class="state">Nenhum produto cadastrado ainda. Clique em “+ Novo produto” para começar.</p>';
      return;
    }

    const rows = sortItems(state.items, state.sortKey, state.sortDir).map((p) => `
      <tr class="${p.active ? '' : 'row-inactive'}">
        <td><strong>${esc(p.name)}</strong></td>
        <td>${esc(p.category ?? '—')}</td>
        <td class="num">${p.unitWeight == null ? '—' : `${formatNumber(p.unitWeight)} g`}</td>
        <td>${p.active
          ? '<span class="badge badge-ok">Ativo</span>'
          : '<span class="badge badge-off">Inativo</span>'}</td>
        <td class="actions">
          <button type="button" class="btn btn-sm" data-action="edit" data-id="${p.id}">Editar</button>
          <button type="button" class="btn btn-sm" data-action="toggle" data-id="${p.id}">${p.active ? 'Inativar' : 'Reativar'}</button>
        </td>
      </tr>`).join('');

    listEl.innerHTML = `
      <table>
        <thead>
          <tr>
            ${sortHeader('Nome', 'name', state)}
            ${sortHeader('Categoria', 'category', state)}
            ${sortHeader('Peso unitário', 'unitWeight', state, 'num')}
            ${sortHeader('Situação', 'active', state)}
            <th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;
  }

  function formBody(item, categories) {
    const weight = item?.unitWeight == null ? '' : String(item.unitWeight).replace('.', ',');
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
        <label for="f-description">Descrição</label>
        <textarea id="f-description" name="description" rows="3" maxlength="500">${esc(item?.description ?? '')}</textarea>
        <small class="field-error" data-error-for="description"></small>
      </div>
      <div class="field">
        <label for="f-weight">Peso unitário final</label>
        <div class="input-group">
          <input id="f-weight" name="unitWeight" type="text" inputmode="decimal" value="${esc(weight)}">
          <span class="input-suffix">g</span>
        </div>
        <small class="hint">Opcional. Peso de cada unidade pronta, usado para estimar o peso da produção.</small>
        <small class="field-error" data-error-for="unitWeight"></small>
      </div>`;
  }

  async function openForm(item = null) {
    let categories = [];
    try {
      const all = await api('GET', '/api/products');
      categories = [...new Set(all.map((p) => p.category).filter(Boolean))].sort();
    } catch {
      // sem sugestões de categoria; o formulário continua funcionando
    }

    openFormDialog({
      title: item ? 'Editar produto' : 'Novo produto',
      bodyHtml: formBody(item, categories),
      onSubmit: async (form) => {
        const weight = parseDecimal(form.elements.unitWeight.value);
        if (Number.isNaN(weight)) {
          throw new ApiError({
            message: 'Há campos inválidos. Corrija-os e tente novamente.',
            fieldErrors: { unitWeight: 'Informe um número válido (ex.: 45,5) ou deixe em branco.' },
          }, 400);
        }
        const body = {
          name: form.elements.name.value,
          category: form.elements.category.value,
          description: form.elements.description.value,
          unitWeight: weight, // null quando em branco
        };
        if (item) {
          await api('PUT', `/api/products/${item.id}`, body);
        } else {
          await api('POST', '/api/products', body);
        }
        toast(item ? 'Produto atualizado.' : 'Produto cadastrado.');
        await load();
      },
    });
  }

  async function toggleActive(item) {
    const next = !item.active;
    if (!next) {
      const ok = await confirmDialog({
        title: 'Inativar produto',
        message: `“${item.name}” deixará de aparecer nas listas de seleção. `
          + 'O histórico de produções e as receitas existentes são preservados.',
        confirmLabel: 'Inativar',
        danger: true,
      });
      if (!ok) return;
    }
    try {
      await api('PATCH', `/api/products/${item.id}/status`, { active: next });
      toast(next ? 'Produto reativado.' : 'Produto inativado.');
      await load();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

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
    const item = state.items.find((p) => p.id === Number(button.dataset.id));
    if (!item) return;
    if (button.dataset.action === 'edit') openForm(item);
    else toggleActive(item);
  });

  await load();
}