CREATE TABLE IF NOT EXISTS ingredient (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    name                  TEXT NOT NULL COLLATE NOCASE UNIQUE,
    category              TEXT,
    base_unit             TEXT NOT NULL CHECK (base_unit IN ('G', 'ML', 'UN')),
    allow_fraction        INTEGER NOT NULL DEFAULT 0 CHECK (allow_fraction IN (0, 1)),
    minimum_stock         TEXT NOT NULL DEFAULT '0',
    current_stock         TEXT NOT NULL DEFAULT '0',
    current_average_cost  TEXT NOT NULL DEFAULT '0',
    active                INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at            TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS product (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT NOT NULL COLLATE NOCASE UNIQUE,
    category     TEXT,
    description  TEXT,
    unit_weight  TEXT,
    active       INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at   TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS recipe (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id      INTEGER NOT NULL REFERENCES product (id),
    version         INTEGER NOT NULL,
    yield_quantity  TEXT NOT NULL,
    yield_unit      TEXT NOT NULL DEFAULT 'UN',
    status          TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at      TEXT NOT NULL,
    UNIQUE (product_id, version)
);

CREATE TABLE IF NOT EXISTS recipe_item (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    recipe_id      INTEGER NOT NULL REFERENCES recipe (id),
    ingredient_id  INTEGER NOT NULL REFERENCES ingredient (id),
    quantity_base  TEXT NOT NULL,
    display_unit   TEXT NOT NULL,
    UNIQUE (recipe_id, ingredient_id)
);

CREATE TABLE IF NOT EXISTS purchase (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_date  TEXT NOT NULL,
    notes          TEXT,
    total_amount   TEXT NOT NULL,
    created_at     TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS purchase_item (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_id    INTEGER NOT NULL REFERENCES purchase (id),
    ingredient_id  INTEGER NOT NULL REFERENCES ingredient (id),
    quantity_base  TEXT NOT NULL,
    total_cost     TEXT NOT NULL,
    unit_cost_base TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS production (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id              INTEGER NOT NULL REFERENCES product (id),
    recipe_id               INTEGER NOT NULL REFERENCES recipe (id),
    recipe_version          INTEGER NOT NULL,
    product_name_snapshot   TEXT NOT NULL,
    recipe_yield_snapshot   TEXT NOT NULL,
    scale_factor            TEXT NOT NULL,
    requested_quantity      TEXT NOT NULL,
    produced_quantity       TEXT NOT NULL,
    measured_weight         TEXT,
    status                  TEXT NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('DRAFT', 'CONFIRMED')),
    production_date         TEXT NOT NULL,
    direct_cost             TEXT NOT NULL,
    additional_costs        TEXT NOT NULL DEFAULT '0',
    total_cost              TEXT NOT NULL,
    unit_cost               TEXT NOT NULL,
    created_at              TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS production_item (
    id                        INTEGER PRIMARY KEY AUTOINCREMENT,
    production_id             INTEGER NOT NULL REFERENCES production (id),
    ingredient_id             INTEGER NOT NULL REFERENCES ingredient (id),
    ingredient_name_snapshot  TEXT NOT NULL,
    base_unit_snapshot        TEXT NOT NULL,
    planned_quantity          TEXT NOT NULL,
    actual_quantity           TEXT NOT NULL,
    unit_cost_snapshot        TEXT NOT NULL,
    total_cost_snapshot       TEXT NOT NULL
);

-- quantity é assinada: positiva = entrada, negativa = saída
CREATE TABLE IF NOT EXISTS stock_movement (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    ingredient_id  INTEGER NOT NULL REFERENCES ingredient (id),
    movement_type  TEXT NOT NULL CHECK (movement_type IN
                     ('PURCHASE', 'PRODUCTION', 'ADJUSTMENT', 'LOSS', 'REVERSAL')),
    quantity       TEXT NOT NULL,
    source_type    TEXT NOT NULL CHECK (source_type IN ('PURCHASE', 'PRODUCTION', 'MANUAL')),
    source_id      INTEGER,
    occurred_at    TEXT NOT NULL,
    reason         TEXT,
    created_at     TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS app_setting (
    key    TEXT PRIMARY KEY,
    value  TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_recipe_product        ON recipe (product_id);
CREATE INDEX IF NOT EXISTS idx_recipe_item_recipe    ON recipe_item (recipe_id);
CREATE INDEX IF NOT EXISTS idx_purchase_date         ON purchase (purchase_date);
CREATE INDEX IF NOT EXISTS idx_purchase_item_purch   ON purchase_item (purchase_id);
CREATE INDEX IF NOT EXISTS idx_production_date       ON production (production_date);
CREATE INDEX IF NOT EXISTS idx_production_product    ON production (product_id);
CREATE INDEX IF NOT EXISTS idx_production_item_prod  ON production_item (production_id);
CREATE INDEX IF NOT EXISTS idx_stock_mov_ingredient  ON stock_movement (ingredient_id);
CREATE INDEX IF NOT EXISTS idx_stock_mov_source      ON stock_movement (source_type, source_id);

INSERT OR IGNORE INTO app_setting (key, value) VALUES ('target_margin_percent', '30');

-- No máximo uma versão ACTIVE por produto
CREATE UNIQUE INDEX IF NOT EXISTS idx_recipe_one_active
    ON recipe (product_id) WHERE status = 'ACTIVE';