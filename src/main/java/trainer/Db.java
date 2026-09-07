package trainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class Db {

    private static final Logger LOG = Logger.getLogger(Db.class.getName());

    private static final String IME = "paralagia.db";

    private static Db instance;

    public static synchronized Db get() {
        if (instance == null) instance = new Db();
        return instance;
    }

    private Connection conn;

    private Db() {
        try {
            String url = "jdbc:sqlite:" + Pathove.do_dannite(IME);
            conn = DriverManager.getConnection(url);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS chants (" +
                    "  id        INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  title     TEXT NOT NULL," +
                    "  glas      TEXT NOT NULL," +
                    "  syllables TEXT NOT NULL," +
                    "  notes     TEXT NOT NULL," +
                    "  ts        TEXT NOT NULL)");
            }
            LOG.info("[Db] Готова: " + url);
        } catch (Exception e) {
            LOG.warning("[Db] Грешка при отваряне: " + e.getMessage());
        }
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    public void saveChant(String title, String glas, String syllables, String notes) {
        String sql = "INSERT INTO chants(title, glas, syllables, notes, ts) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, glas);
            ps.setString(3, syllables);
            ps.setString(4, notes);
            ps.setString(5, now());
            ps.executeUpdate();
        } catch (Exception e) {
            LOG.warning("[Db] Грешка при запис на песнопение: " + e.getMessage());
        }
    }

    public List<String[]> loadChants() {
        List<String[]> out = new ArrayList<>();
        String sql = "SELECT id, title, glas, syllables, notes, ts FROM chants ORDER BY id DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new String[]{
                    String.valueOf(rs.getInt(1)), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6)});
            }
        } catch (Exception e) {
            LOG.warning("[Db] Грешка при четене на песнопения: " + e.getMessage());
        }
        return out;
    }

    public void deleteChant(String id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM chants WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            LOG.warning("[Db] Грешка при изтриване на песнопение: " + e.getMessage());
        }
    }

}
