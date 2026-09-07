package trainer;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class Ontology {

    private static final Logger LOG = Logger.getLogger(Ontology.class.getName());

    private static final String IME = "byzantine_music3.owl";
    private static final String NS   = "http://www.paralagia.bg/ontology/vizantiiska-muzika#";
    private static final String BASE = "http://www.paralagia.bg/ontology/vizantiiska-muzika";

    private static final String FORMAT = "TURTLE";

    public static final String PREFIX =
        "PREFIX : <" + NS + ">\n" +
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
        "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

    private static Ontology instance;

    public static synchronized Ontology get() {
        if (instance == null) instance = new Ontology();
        return instance;
    }

    private OntModel model;
    private boolean  loaded;
    private String   lastError;

    private Ontology() {
        java.nio.file.Path fajl = Pathove.danni(IME);
        try (InputStream in = new FileInputStream(fajl.toFile())) {
            model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
            model.read(in, BASE, FORMAT);
            loaded = true;
            LOG.info("[Ontology] Заредени " + model.size() + " тройки от " + fajl);
        } catch (Exception e) {
            lastError = e.getMessage();
            LOG.severe("[Ontology] Не мога да заредя " + fajl + ": "
                     + e.getMessage() + ". Приложението ще се отвори без "
                     + "данни — това е сгрешена папка, а не липсваща "
                     + "онтология.");
        }
    }

    public synchronized List<Map<String, String>> select(String sparql) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (!loaded) return rows;
        try {
            Query q = QueryFactory.create(PREFIX + sparql);
            try (QueryExecution qe = QueryExecutionFactory.create(q, model)) {
                ResultSet rs = qe.execSelect();
                List<String> vars = rs.getResultVars();
                while (rs.hasNext()) {
                    QuerySolution sol = rs.nextSolution();
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String v : vars) {
                        if (sol.contains(v)) {
                            row.put(v, sol.get(v).isLiteral()
                                ? sol.getLiteral(v).getString()
                                : sol.getResource(v).getLocalName());
                        } else {
                            row.put(v, "");
                        }
                    }
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            lastError = e.getMessage();
            LOG.warning("[Ontology] Грешка в заявката: " + e.getMessage());
        }
        return rows;
    }

    public synchronized boolean update(String sparqlUpdate) {
        if (!loaded) { lastError = "Онтологията не е заредена"; return false; }
        try {
            UpdateRequest req = UpdateFactory.create(PREFIX + sparqlUpdate);
            UpdateAction.execute(req, model);
            try (FileOutputStream out =
                     new FileOutputStream(Pathove.danni(IME).toFile())) {
                model.getBaseModel().write(out, FORMAT, BASE);
            }
            lastError = null;
            return true;
        } catch (Exception e) {
            lastError = e.getMessage();
            LOG.warning("[Ontology] Грешка при запис: " + e.getMessage());
            return false;
        }
    }

    public String  lastError() { return lastError; }
    public long    size()      { return loaded ? model.size() : 0; }
    public String  ns()        { return NS; }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    public static String encodeRows(List<Map<String, String>> rows) {
        if (rows == null || rows.isEmpty()) return "";
        List<String> keys = new ArrayList<>(rows.get(0).keySet());
        StringBuilder sb = new StringBuilder(String.join("\t", keys));
        for (Map<String, String> r : rows) {
            sb.append("\n");
            List<String> values = new ArrayList<>();
            for (String k : keys) {
                String v = r.getOrDefault(k, "");
                values.add(v.replace("\t", " ").replace("\n", " "));
            }
            sb.append(String.join("\t", values));
        }
        return sb.toString();
    }

    public static List<Map<String, String>> decodeRows(String text) {
        List<Map<String, String>> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        String[] lines = text.split("\n");
        String[] keys = lines[0].split("\t", -1);
        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split("\t", -1);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < keys.length; j++) {
                row.put(keys[j], j < values.length ? values[j] : "");
            }
            out.add(row);
        }
        return out;
    }
}
