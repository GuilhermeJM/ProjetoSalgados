package br.com.salgados.domain.enums;

import java.math.BigDecimal;

public enum Unit {
    G(Dimension.MASS, "1"),
    KG(Dimension.MASS, "1000"),
    ML(Dimension.VOLUME, "1"),
    L(Dimension.VOLUME, "1000"),
    UN(Dimension.COUNT, "1");

    public enum Dimension { MASS, VOLUME, COUNT }

    private final Dimension dimension;
    private final BigDecimal factorToBase;

    Unit(Dimension dimension, String factorToBase) {
        this.dimension = dimension;
        this.factorToBase = new BigDecimal(factorToBase);
    }

    public Dimension getDimension() {
        return dimension;
    }

    public BigDecimal getFactorToBase() {
        return factorToBase;
    }

    /** Unidades-base são as que o sistema usa para guardar e comparar: G, ML e UN. */
    public boolean isBase() {
        return factorToBase.compareTo(BigDecimal.ONE) == 0;
    }
}