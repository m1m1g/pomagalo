package trainer;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class LibrarianAgent extends Agent {

    private static final Logger LOG = Logger.getLogger(LibrarianAgent.class.getName());

    public static final String SERVICE = "librarian";

    @Override
    protected void setup() {
        register();
        addBehaviour(new Handle());
        LOG.info("[LibrarianAgent] Готов. Пази записаното в базата.");
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
            sd.setName("library-keeper");
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (Exception e) {
            LOG.warning("[LibrarianAgent] Регистрацията в DF пропадна: " + e.getMessage());
        }
    }

    private static String build(String content) {
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

        return "ERROR: непозната команда";
    }

    private class Handle extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg == null) { block(); return; }

            String result = build(msg.getContent());

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
            if (p.length < 4) return "ERROR: упражнението иска четири полета";
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
}
