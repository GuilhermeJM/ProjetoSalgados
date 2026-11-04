package br.com.salgados.domain.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.salgados.domain.enums.Unit;
import br.com.salgados.domain.model.Ingredient;
import br.com.salgados.dto.request.IngredientRequest;
import br.com.salgados.dto.response.IngredientResponse;
import br.com.salgados.exception.BusinessException;
import br.com.salgados.exception.ConflictException;
import br.com.salgados.exception.NotFoundException;
import br.com.salgados.repository.IngredientRepository;

@Service
public class IngredientService {

    private final IngredientRepository repository;
    private final UnitConversionService conversion;

    public IngredientService(IngredientRepository repository, UnitConversionService conversion) {
        this.repository = repository;
        this.conversion = conversion;
    }

    // Obs.: não usamos @Transactional(readOnly = true) porque o driver do SQLite
    // lança erro ao tentar mudar o modo "somente leitura" de uma conexão já aberta.

    public List<IngredientResponse> list(String search, String category, Boolean active) {
        return repository.search(search, category, active).stream()
                .map(IngredientResponse::from)
                .toList();
    }

    public IngredientResponse findById(long id) {
        return IngredientResponse.from(get(id));
    }

    @Transactional
    public IngredientResponse create(IngredientRequest request) {
        String name = request.name().trim();
        validateBaseUnit(request.baseUnit());
        checkNameAvailable(name, null);
        BigDecimal minimumBase = minimumStockInBase(request);

        Ingredient toSave = new Ingredient(null, name, normalize(request.category()),
                request.baseUnit(), allowFraction(request), minimumBase,
                BigDecimal.ZERO, BigDecimal.ZERO, true, LocalDateTime.now());

        long id = repository.insert(toSave);
        return findById(id);
    }

    @Transactional
    public IngredientResponse update(long id, IngredientRequest request) {
        Ingredient current = get(id);
        String name = request.name().trim();
        validateBaseUnit(request.baseUnit());
        checkNameAvailable(name, id);

        if (request.baseUnit() != current.baseUnit() && repository.hasHistory(id)) {
            throw new BusinessException("UNIT_LOCKED",
                    "A unidade-base não pode ser alterada porque o ingrediente já possui movimentações, "
                    + "compras ou receitas. Crie um novo ingrediente com a unidade desejada.");
        }

        BigDecimal minimumBase = minimumStockInBase(request);
        repository.update(new Ingredient(id, name, normalize(request.category()),
                request.baseUnit(), allowFraction(request), minimumBase,
                current.currentStock(), current.currentAverageCost(), current.active(),
                current.createdAt()));
        return findById(id);
    }

    @Transactional
    public IngredientResponse setActive(long id, boolean active) {
        get(id);
        repository.setActive(id, active);
        return findById(id);
    }

    // ---------- auxiliares ----------

    private Ingredient get(long id) {
        return repository.findById(id).orElseThrow(() ->
                new NotFoundException("INGREDIENT_NOT_FOUND", "Ingrediente não encontrado."));
    }

    private void validateBaseUnit(Unit unit) {
        if (!unit.isBase()) {
            throw new BusinessException("INVALID_BASE_UNIT",
                    "A unidade-base deve ser G, ML ou UN. Para controlar em kg use G; para litros use ML.");
        }
    }

    private void checkNameAvailable(String name, Long excludeId) {
        if (repository.existsByName(name, excludeId)) {
            throw new ConflictException("DUPLICATE_INGREDIENT_NAME",
                    "Já existe um ingrediente chamado \"" + name + "\".");
        }
    }

    private BigDecimal minimumStockInBase(IngredientRequest request) {
        Unit inputUnit = request.minimumStockUnit() != null
                ? request.minimumStockUnit() : request.baseUnit();
        BigDecimal base = conversion.toBase(request.minimumStock(), inputUnit, request.baseUnit());
        if (base.signum() > 0) {
            conversion.validateQuantity(base, request.baseUnit(), allowFraction(request));
        }
        return base;
    }

    private static String normalize(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static boolean allowFraction(IngredientRequest request) {
        return Boolean.TRUE.equals(request.allowFraction());
    }
}