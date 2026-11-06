package br.com.salgados.dto.response;

import java.util.List;

/** checked = quantos ingredientes foram conferidos; fixed = se as divergências foram corrigidas. */
public record ReconciliationResponse(int checked, boolean fixed, List<ReconciliationEntry> divergences) {
}