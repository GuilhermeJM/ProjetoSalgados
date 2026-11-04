package br.com.salgados.domain.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import br.com.salgados.domain.enums.Unit;
import br.com.salgados.exception.BusinessException;

@Service
public class UnitConversionService {

    /**
     * Converte uma quantidade informada pelo usuário (ex.: 2,5 kg)
     * para a unidade-base do ingrediente (ex.: 2500 g).
     */
    public BigDecimal toBase(BigDecimal quantity, Unit inputUnit, Unit ingredientBaseUnit) {
        if (!ingredientBaseUnit.isBase()) {
            throw new IllegalArgumentException(ingredientBaseUnit + " não é uma unidade-base.");
        }
        if (inputUnit.getDimension() != ingredientBaseUnit.getDimension()) {
            throw new BusinessException("INVALID_UNIT_CONVERSION",
                    "Não é possível converter " + inputUnit + " para " + ingredientBaseUnit
                    + ": são grandezas diferentes e a conversão exigiria a densidade do ingrediente.");
        }
        return quantity.multiply(inputUnit.getFactorToBase());
    }

    /** Converte da unidade-base para a unidade de exibição (ex.: 4000 g -> 4 kg). */
    public BigDecimal fromBase(BigDecimal quantityBase, Unit displayUnit) {
        // Os fatores são 1 ou 1000, então a divisão é sempre exata (não precisa de arredondamento).
        return quantityBase.divide(displayUnit.getFactorToBase());
    }

    /** Valida que a quantidade é positiva e, para "un", inteira (salvo se o ingrediente permitir fração). */
    public void validateQuantity(BigDecimal quantityBase, Unit baseUnit, boolean allowFraction) {
        if (quantityBase.signum() <= 0) {
            throw new BusinessException("INVALID_QUANTITY", "A quantidade deve ser maior que zero.");
        }
        if (baseUnit == Unit.UN && !allowFraction && quantityBase.stripTrailingZeros().scale() > 0) {
            throw new BusinessException("INVALID_QUANTITY",
                    "Este ingrediente é contado em unidades inteiras e não aceita fração.");
        }
    }
}