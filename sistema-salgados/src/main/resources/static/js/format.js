const LABELS = { G: 'g', KG: 'kg', ML: 'ml', L: 'L', UN: 'un' };

export const unitLabel = (unit) => LABELS[unit] ?? unit;

export function formatNumber(value, maxDigits = 3) {
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: maxDigits }).format(value);
}

export function formatCurrency(value, minDigits = 2, maxDigits = 2) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: minDigits,
    maximumFractionDigits: maxDigits,
  }).format(value);
}

/** Para exibição: 5000 g vira 5 kg; 500 g continua 500 g. */
export function displayUnit(value, baseUnit) {
  if ((baseUnit === 'G' || baseUnit === 'ML') && Math.abs(value) >= 1000) {
    return { value: value / 1000, unit: baseUnit === 'G' ? 'KG' : 'L' };
  }
  return { value, unit: baseUnit };
}

export function formatQuantity(value, baseUnit) {
  const shown = displayUnit(value, baseUnit);
  return `${formatNumber(shown.value)} ${unitLabel(shown.unit)}`;
}

/**
 * Para preencher formulários: só converte quando o resultado é exato
 * (5000 g -> 5 kg, mas 1500 g continua 1500 g), para nunca arredondar sem querer ao salvar.
 */
export function formUnit(value, baseUnit) {
  if ((baseUnit === 'G' || baseUnit === 'ML') && value >= 1000 && value % 1000 === 0) {
    return { value: value / 1000, unit: baseUnit === 'G' ? 'KG' : 'L' };
  }
  return { value, unit: baseUnit };
}

/** O custo é guardado por g/ml/un, mas o usuário pensa em R$/kg e R$/L. */
export function formatCostPerUnit(costPerBase, baseUnit) {
  if (!costPerBase) return '—';
  const factor = baseUnit === 'UN' ? 1 : 1000;
  const label = baseUnit === 'G' ? 'kg' : baseUnit === 'ML' ? 'L' : 'un';
  return `${formatCurrency(costPerBase * factor, 2, 4)}/${label}`;
}