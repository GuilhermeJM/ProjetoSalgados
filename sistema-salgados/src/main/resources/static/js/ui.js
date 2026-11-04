import { ApiError } from './api.js';

/** Escapa texto antes de colocá-lo em innerHTML (evita injeção de HTML/JS). */
export function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

export function debounce(fn, ms) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), ms);
  };
}

export function toast(message, type = 'success') {
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = message;
  document.getElementById('toasts').appendChild(el);
  setTimeout(() => el.remove(), type === 'error' ? 6000 : 3500);
}

/**
 * Lê um número digitado em formato brasileiro.
 * Retorna null se vazio, NaN se inválido, ou o número.
 * Aceita "1,5", "1.5" e "1.234,5".
 */
export function parseDecimal(text) {
  const clean = String(text ?? '').trim().replace(/\s/g, '');
  if (clean === '') return null;
  const normalized = clean.includes(',') ? clean.replace(/\./g, '').replace(',', '.') : clean;
  return /^\d+(\.\d+)?$/.test(normalized) ? Number(normalized) : NaN;
}

// ---------- tabela ordenável ----------

export function sortHeader(label, key, state, cssClass = '') {
  const arrow = state.sortKey === key ? (state.sortDir === 'asc' ? ' ▲' : ' ▼') : '';
  return `<th class="${cssClass}"><button type="button" class="th-sort" data-sort="${key}">${esc(label)}${arrow}</button></th>`;
}

export function toggleSort(state, key) {
  if (state.sortKey === key) {
    state.sortDir = state.sortDir === 'asc' ? 'desc' : 'asc';
  } else {
    state.sortKey = key;
    state.sortDir = 'asc';
  }
}

export function sortItems(items, key, dir) {
  const factor = dir === 'asc' ? 1 : -1;
  return [...items].sort((a, b) => {
    const va = a[key];
    const vb = b[key];
    if (va == null && vb == null) return 0;
    if (va == null) return 1;
    if (vb == null) return -1;
    if (typeof va === 'string') return va.localeCompare(vb, 'pt-BR') * factor;
    return (Number(va) - Number(vb)) * factor;
  });
}

// ---------- diálogos ----------

function clearErrors(form) {
  form.querySelectorAll('.field-error').forEach((el) => { el.textContent = ''; });
  form.querySelectorAll('.invalid').forEach((el) => el.classList.remove('invalid'));
  form.querySelector('.form-error').hidden = true;
}

function showErrors(form, body) {
  for (const [field, message] of Object.entries(body.fieldErrors ?? {})) {
    const target = form.querySelector(`[data-error-for="${field}"]`);
    if (target) target.textContent = message;
    form.elements[field]?.classList?.add('invalid');
  }
  const general = form.querySelector('.form-error');
  general.textContent = body.message ?? 'Não foi possível salvar.';
  general.hidden = false;
}

/**
 * Abre um formulário em diálogo. `onSubmit(form)` deve lançar ApiError em caso de erro;
 * se terminar sem erro, o diálogo fecha.
 */
export function openFormDialog({ title, bodyHtml, submitLabel = 'Salvar', wide = false, onReady, onSubmit }) {
  const dialog = document.createElement('dialog');
  dialog.className = wide ? 'modal modal-wide' : 'modal';
  dialog.innerHTML = `
    <form novalidate>
      <header class="modal-header"><h2>${esc(title)}</h2></header>
      <div class="modal-body">
        ${bodyHtml}
        <p class="form-error" role="alert" hidden></p>
      </div>
      <footer class="modal-footer">
        <button type="button" class="btn" data-cancel>Cancelar</button>
        <button type="submit" class="btn btn-primary">${esc(submitLabel)}</button>
      </footer>
    </form>`;
  document.body.appendChild(dialog);

  const form = dialog.querySelector('form');
  const submitButton = form.querySelector('[type="submit"]');

  form.querySelector('[data-cancel]').addEventListener('click', () => dialog.close());
  dialog.addEventListener('close', () => dialog.remove());

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearErrors(form);
    submitButton.disabled = true;
    try {
      await onSubmit(form);
      dialog.close();
    } catch (err) {
      if (err instanceof ApiError) {
        showErrors(form, err.body);
      } else {
        console.error(err);
        showErrors(form, { message: 'Ocorreu um erro inesperado. Tente novamente.' });
      }
    } finally {
      submitButton.disabled = false;
    }
  });

  dialog.showModal();
  if (onReady) onReady(form);
}

/** Pede confirmação. Retorna uma Promise<boolean>. */
export function confirmDialog({ title, message, confirmLabel = 'Confirmar', danger = false }) {
  return new Promise((resolve) => {
    const dialog = document.createElement('dialog');
    dialog.className = 'modal';
    dialog.innerHTML = `
      <header class="modal-header"><h2>${esc(title)}</h2></header>
      <div class="modal-body"><p>${esc(message)}</p></div>
      <footer class="modal-footer">
        <button type="button" class="btn" data-cancel>Cancelar</button>
        <button type="button" class="btn ${danger ? 'btn-danger' : 'btn-primary'}" data-confirm>${esc(confirmLabel)}</button>
      </footer>`;
    document.body.appendChild(dialog);

    let result = false;
    dialog.querySelector('[data-cancel]').addEventListener('click', () => dialog.close());
    dialog.querySelector('[data-confirm]').addEventListener('click', () => {
      result = true;
      dialog.close();
    });
    dialog.addEventListener('close', () => {
      dialog.remove();
      resolve(result);
    });
    dialog.showModal();
  });
}