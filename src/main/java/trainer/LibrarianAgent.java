package trainer;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class LibrarianAgent extends Agent {

    private static final Logger LOG = Logger.getLogger(LibrarianAgent.class.getName());
    private static final long TIMEOUT = 10_000;

    public static final String SERVICE = "librarian";

    private AID ontology;

    @Override
    protected void setup() {
        register();
        addBehaviour(new Handle());
        LOG.info("[LibrarianAgent] Готов. Пази записаното и съставя упражнения.");
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (Exception ignored) {}
    }

    private void register() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(SERVICE);
            sd.setName("library-keeper-and-exercise-generator");
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (Exception e) {
            LOG.warning("[LibrarianAgent] Регистрацията в DF пропадна: " + e.getMessage());
        }
    }

    public static String handle(String content) {
        return build(content, null);
    }

    private static String build(String content, List<Map<String, String>> glasove) {
        if (content == null || content.isBlank()) return "ERROR: празно съобщение";
        String cmd = content.trim();

        if (cmd.equalsIgnoreCase("CHANTS"))    return encode(Db.get().loadChants());

        if (cmd.startsWith("SAVE:")) return save(cmd.substring("SAVE:".length()));

        if (cmd.startsWith("DELETE:")) {
            String[] parts = cmd.substring("DELETE:".length()).split(":", 2);
            if (parts.length < 2 || parts[1].isBlank())
                return "ERROR: липсва какво да се изтрие";
            String what = parts[0], id = parts[1].trim();
            if ("chant".equals(what)) { Db.get().deleteChant(id); return "OK"; }
            return "ERROR: непознат вид запис: " + what;
        }

        if (cmd.equalsIgnoreCase("NEXT")) {
            return Exercise.encode(Exercise.next(
                glasove != null ? glasove : Quiz.glasove()));
        }
        if (cmd.startsWith("CHECK:")) {
            String body = cmd.substring("CHECK:".length());
            String[] parts = body.split("\\|", -1);
            if (parts.length < 2) return "0/0;ERROR: липсва отговор";
            return Exercise.check(parts[0], parts[1]);
        }
        return "ERROR: непозната команда";
    }

    private class Handle extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg == null) { block(); return; }

            String content = msg.getContent();

            List<Map<String, String>> glasove =
                (content != null && content.trim().equalsIgnoreCase("NEXT"))
                    ? fetchGlasove() : null;

            String result = build(content, glasove);

            ACLMessage reply = msg.createReply();
            reply.setPerformative(result.startsWith("ERROR:")
                ? ACLMessage.FAILURE : ACLMessage.INFORM);
            reply.setContent(result);
            myAgent.send(reply);
        }
    }

    private static String save(String tyalo) {
        int dvoetochie = tyalo.indexOf(':');
        if (dvoetochie < 0) return "ERROR: липсва вид на записа";
        String vid = tyalo.substring(0, dvoetochie);
        String[] p = tyalo.substring(dvoetochie + 1).split("\\|", -1);

        if ("chant".equals(vid)) {
            if (p.length < 4) return "ERROR: песнопението иска четири полета";
            Db.get().saveChant(p[0], p[1], p[2], p[3]);
            return "OK";
        }
        return "ERROR: непознат вид запис: " + vid;
    }

    static String pole(String v) {
        if (v == null) return "";
        return v.replace("|", "/").replace("\n", " ").replace("\t", " ");
    }

    static String encode(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String[] r : rows) {
            if (sb.length() > 0) sb.append("\n");
            for (int i = 0; i < r.length; i++) {
                if (i > 0) sb.append("\t");
                String v = (r[i] == null) ? "" : r[i];
                sb.append(v.replace("\t", " ").replace("\n", "⏎"));
            }
        }
        return sb.toString();
    }

    static List<String[]> decode(String text) {
        List<String[]> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        for (String line : text.split("\n")) {
            String[] cells = line.split("\t", -1);
            for (int i = 0; i < cells.length; i++)
                cells[i] = cells[i].replace("⏎", "\n");
            out.add(cells);
        }
        return out;
    }

    private List<Map<String, String>> fetchGlasove() {
        try {
            if (ontology == null) ontology = findOntology();
            if (ontology == null) return Quiz.glasove();

            String key = "l-" + System.currentTimeMillis();
            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.addReceiver(ontology);
            req.setContent("GET:glasove");
            req.setReplyWith(key);
            send(req);

            ACLMessage reply = blockingReceive(MessageTemplate.MatchInReplyTo(key), TIMEOUT);
            if (reply == null || reply.getPerformative() == ACLMessage.FAILURE) {
                return Quiz.glasove();
            }
            return Ontology.decodeRows(reply.getContent());
        } catch (Exception e) {
            LOG.warning("[LibrarianAgent] Заявката към онтологията пропадна: " + e.getMessage());
            return Quiz.glasove();
        }
    }

    private AID findOntology() {
        try {
            DFAgentDescription tmpl = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(OntologyAgent.SERVICE);
            tmpl.addServices(sd);
            DFAgentDescription[] found = DFService.search(this, tmpl);
            if (found.length > 0) return found[0].getName();
        } catch (Exception e) {
            LOG.warning("[LibrarianAgent] Търсенето в DF пропадна: " + e.getMessage());
        }
        return null;
    }
}
