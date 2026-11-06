package br.com.salgados.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

@Service
public class CostCalculationService {

    /** Casas decimais do custo por unidade-base (R$ por g, ml ou un). */
    public static final int COST_SCALE = 10;
    public static final RoundingMode COST_ROUNDING = RoundingMode.HALF_UP;

    /** Custo por unidade-base de uma compra: valor total pago / quantidade comprada. */
    public BigDecimal unitCost(BigDecimal totalCost, BigDecimal quantityBase) {
        if (quantityBase.signum() <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        return totalCost.divide(quantityBase, COST_SCALE, COST_ROUNDING);
    }

    /**
     * Custo médio ponderado depois de uma compra:
     * (saldo anterior x custo médio anterior + valor pago) / (saldo anterior + quantidade comprada).
     * Usa o valor pago diretamente, e não quantidade x custo unitário arredondado,
     * para que o erro de arredondamento não se acumule entre compras.
     * Se não havia saldo, vale o custo unitário da nova compra.
     */
    public BigDecimal newAverageCost(BigDecimal previousStock, BigDecimal previousAverageCost,
                                     BigDecimal purchasedQuantity, BigDecimal purchaseTotalCost) {
        if (previousStock.signum() <= 0) {
            return unitCost(purchaseTotalCost, purchasedQuantity);
        }
        BigDecimal totalValue = previousStock.multiply(previousAverageCost).add(purchaseTotalCost);
        BigDecimal totalQuantity = previousStock.add(purchasedQuantity);
        return totalValue.divide(totalQuantity, COST_SCALE, COST_ROUNDING);
    }

    /** Valor do estoque em reais (quantidade x custo médio), arredondado para centavos. */
    public BigDecimal stockValue(BigDecimal quantityBase, BigDecimal unitCost) {
        return quantityBase.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
    }
}