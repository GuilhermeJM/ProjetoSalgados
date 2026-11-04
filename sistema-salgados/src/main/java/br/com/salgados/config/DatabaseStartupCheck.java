package br.com.salgados.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseStartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupCheck.class);

    private final JdbcTemplate jdbc;

    public DatabaseStartupCheck(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // integrity_check devolve uma linha "ok" quando está tudo certo,
        // ou uma linha por problema encontrado.
        List<String> result = jdbc.queryForList("PRAGMA integrity_check", String.class);
        if (result.size() != 1 || !"ok".equalsIgnoreCase(result.get(0))) {
            throw new IllegalStateException("Banco de dados com problemas de integridade: " + result);
        }

        Integer foreignKeys = jdbc.queryForObject("PRAGMA foreign_keys", Integer.class);
        if (foreignKeys == null || foreignKeys != 1) {
            throw new IllegalStateException("Chaves estrangeiras não estão habilitadas no SQLite.");
        }

        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
                Integer.class);
        log.info("Banco OK: integridade verificada, foreign_keys ativo, {} tabelas.", tables);
    }
}