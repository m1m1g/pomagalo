package trainer;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.logging.Logger;

public class ComposerAgent extends Agent {

    private static final Logger LOG = Logger.getLogger(ComposerAgent.class.getName());

    public static final String SERVICE = "composer";

    public record Composed(String title, String flow, String notes,
                           String mode, String tradition, String style,
                           String ison) {}

    private static final Composer COMPOSER = new Composer();

    @Override
    protected void setup() {
        register();
        addBehaviour(new Handle());
        LOG.info("[ComposerAgent] Готов. Съставя песнопения.");
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
            sd.setName("chant-composer");
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (Exception e) {
            LOG.warning("[ComposerAgent] Регистрацията в DF пропадна: " + e.getMessage());
        }
    }

    public static String handle(String content) {
        if (content == null || content.isBlank()) return "ERROR: празно съобщение";
        String cmd = content.trim();

        if (cmd.equalsIgnoreCase("PING")) return "PONG";
        if (!cmd.startsWith("COMPOSE:")) return "ERROR: непозната команда";

        String[] p = cmd.substring("COMPOSE:".length()).split("\\|", -1);
        if (p.length < 5) return "ERROR: заявката иска пет полета";

        try {
            Composer.Mode mode = modeByNumber(p[0]);
            if (mode == null) return "ERROR: няма глас с номер " + p[0];

            Composer.Chant chant = COMPOSER.compose(
                mode, styleByKey(p[1]), traditionByKey(p[2]), p[3], dylzhina(p[4]));

            return encode(new Composed(
                chant.title(),
                Composer.flow(chant),
                chant.notes(),
                chant.mode() != null ? chant.mode().label() : "",
                chant.tradition() != null ? chant.tradition().label() : "",
                chant.style() != null ? chant.style().label() : "",
                chant.ison()));
        } catch (Exception e) {
            LOG.warning("[ComposerAgent] Съставянето пропадна: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    private static Composer.Mode modeByNumber(String number) {
        int n = dylzhina(number);
        for (Composer.Mode m : COMPOSER.modes()) if (m.number() == n) return m;
        return null;
    }

    private static Composer.Style styleByKey(String key) {
        if (key == null || key.isBlank()) return null;
        for (Composer.Style s : COMPOSER.styles()) if (key.equals(s.key())) return s;
        return null;
    }

    private static Composer.Tradition traditionByKey(String key) {
        if (key == null || key.isBlank()) return null;
        for (Composer.Tradition t : COMPOSER.traditions()) if (key.equals(t.key())) return t;
        return null;
    }

    private static int dylzhina(String v) {
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public static String encode(Composed c) {
        return String.join("|",
            LibrarianAgent.pole(c.title()),
            c.flow().replace("|", "/").replace("\n", "⏎"),
            c.notes().replace("|", "/").replace("\n", "⏎"),
            LibrarianAgent.pole(c.mode()),
            LibrarianAgent.pole(c.tradition()),
            LibrarianAgent.pole(c.style()),
            LibrarianAgent.pole(c.ison()));
    }

    public static Composed decode(String raw) {
        if (raw == null) return null;
        String[] p = raw.split("\\|", -1);
        if (p.length < 7) return null;
        return new Composed(p[0],
                            p[1].replace("⏎", "\n"),
                            p[2].replace("⏎", "\n"),
                            p[3], p[4], p[5], p[6]);
    }

    private class Handle extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg == null) { block(); return; }

            String result = ComposerAgent.handle(msg.getContent());

            ACLMessage reply = msg.createReply();
            reply.setPerformative(result.startsWith("ERROR:")
                ? ACLMessage.FAILURE : ACLMessage.INFORM);
            reply.setContent(result);
            myAgent.send(reply);
        }
    }
}
