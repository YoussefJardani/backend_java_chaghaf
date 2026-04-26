package ma.chaghaf.config;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TEMPORARY — diagnostic controller to inspect the existing DB schema.
 * Helps us discover what role values the existing CHECK constraint allows.
 *
 * After we fix the schema, this should be removed.
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final JdbcTemplate jdbc;

    /** Lists all CHECK constraints in the public schema. */
    @GetMapping("/constraints")
    public List<Map<String, Object>> constraints() {
        return jdbc.queryForList(
            "SELECT con.conname AS name, " +
            "       pg_get_constraintdef(con.oid) AS definition, " +
            "       cls.relname AS table_name " +
            "FROM pg_constraint con " +
            "JOIN pg_class cls ON con.conrelid = cls.oid " +
            "JOIN pg_namespace nsp ON cls.relnamespace = nsp.oid " +
            "WHERE nsp.nspname = 'public' AND con.contype = 'c' " +
            "ORDER BY cls.relname, con.conname"
        );
    }

    /** Lists all tables in the public schema. */
    @GetMapping("/tables")
    public List<String> tables() {
        return jdbc.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename",
            String.class
        );
    }

    /** Lists columns of a specific table. */
    @GetMapping("/columns/{table}")
    public List<Map<String, Object>> columns(
            @org.springframework.web.bind.annotation.PathVariable String table) {
        return jdbc.queryForList(
            "SELECT column_name, data_type, is_nullable, column_default " +
            "FROM information_schema.columns " +
            "WHERE table_schema = 'public' AND table_name = ? " +
            "ORDER BY ordinal_position",
            table
        );
    }

    /**
     * NUCLEAR OPTION — drop everything in public schema.
     * Call: POST /api/debug/drop-all-tables?confirm=YES_I_AM_SURE
     */
    @org.springframework.web.bind.annotation.PostMapping("/drop-all-tables")
    public Map<String, Object> dropAll(
            @org.springframework.web.bind.annotation.RequestParam String confirm) {
        Map<String, Object> result = new HashMap<>();
        if (!"YES_I_AM_SURE".equals(confirm)) {
            result.put("error", "Add ?confirm=YES_I_AM_SURE to confirm");
            return result;
        }
        try {
            jdbc.execute("DROP SCHEMA public CASCADE");
            jdbc.execute("CREATE SCHEMA public");
            jdbc.execute("GRANT ALL ON SCHEMA public TO public");
            result.put("status", "success");
            result.put("message", "Schema dropped and recreated. RESTART the service now.");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }
}