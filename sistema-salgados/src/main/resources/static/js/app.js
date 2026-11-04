import { esc } from './ui.js';

const NAV = [
  { path: '/painel', label: 'Painel' },
  { path: '/ingredientes', label: 'Ingredientes', module: () => import('./pages/ingredients.js') },
  { path: '/produtos', label: 'Produtos e receitas', module: () => import('./pages/products.js') },
  { path: '/compras', label: 'Compras' },
  { path: '/producao', label: 'Produção' },
  { path: '/estoque', label: 'Estoque' },
  { path: '/historico', label: 'Histórico' },
  { path: '/relatorios', label: 'Relatórios' },
  { path: '/configuracoes', label: 'Configurações' },
];

const nav = document.getElementById('nav');
const main = document.getElementById('main');
let navigationId = 0;

nav.innerHTML = NAV.map((item) =>
  `<a href="#${item.path}" data-path="${item.path}">${esc(item.label)}</a>`).join('');


async function navigate() {
  const current = ++navigationId; // evita que uma tela lenta sobrescreva a mais recente
  // "#/produtos/12/receita" -> section "produtos", params { id: "12", sub: "receita" }
  const [, section = 'painel', ...rest] = (location.hash.slice(1) || '/painel').split('/');
  const params = { id: rest[0], sub: rest[1] };
  const item = NAV.find((n) => n.path === `/${section}`) ?? NAV[0];

  nav.querySelectorAll('a').forEach((a) =>
    a.classList.toggle('active', a.dataset.path === item.path));

  main.innerHTML = '';
  if (!item.module) {
    main.innerHTML = `
      <header class="page-header"><div><h1>${esc(item.label)}</h1></div></header>
      <div class="card"><p class="state">Esta área será construída em uma das próximas etapas.</p></div>`;
    return;
  }

  try {
    const page = await item.module();
    if (current !== navigationId) return;
    await page.render(main, params);
  } catch (err) {
    console.error(err);
    main.innerHTML = '<div class="card"><p class="state state-error">Não foi possível carregar esta tela.</p></div>';
  }
}

window.addEventListener('hashchange', navigate);
navigate();