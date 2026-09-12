package trainer;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class QuizAgent extends Agent {

    private static final Logger LOG = Logger.getLogger(QuizAgent.class.getName());
    private static final long TIMEOUT = 10_000;

    public static final String SERVICE = "quiz";

    private AID ontology;

    @Override
    protected void setup() {
        register();
        addBehaviour(new Handle());
        LOG.info("[QuizAgent] Готов.");
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
            sd.setName("quiz-generator");
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (Exception e) {
            LOG.warning("[QuizAgent] Регистрацията в DF пропадна: " + e.getMessage());
        }
    }

    private static String build(String content,
                                List<Map<String, String>> glasove,
                                List<Map<String, String>> stapki) {
        if (content == null || !content.trim().equalsIgnoreCase("NEXT")) {
            return "ERROR: непозната команда";
        }
        Quiz.Question q = Quiz.question(glasove, stapki);
        if (q == null) return "ERROR: няма достатъчно данни за въпрос";
        return Quiz.encode(q);
    }

    private class Handle extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg == null) { block(); return; }

            List<Map<String, String>> glasove = fetch("GET:glasove");
            List<Map<String, String>> stapki  = fetch("GET:stapki");

            String result = build(msg.getContent(), glasove, stapki);

            ACLMessage reply = msg.createReply();
            reply.setPerformative(result.startsWith("ERROR:")
                ? ACLMessage.FAILURE : ACLMessage.INFORM);
            reply.setContent(result);
            myAgent.send(reply);
        }
    }

    private List<Map<String, String>> fetch(String command) {
        try {
            if (ontology == null) ontology = findOntology();
            if (ontology == null) return List.of();

            String key = "q-" + System.currentTimeMillis();
            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.addReceiver(ontology);
            req.setContent(command);
            req.setReplyWith(key);
            send(req);

            ACLMessage reply = blockingReceive(MessageTemplate.MatchInReplyTo(key), TIMEOUT);
            if (reply == null || reply.getPerformative() == ACLMessage.FAILURE) {
                return List.of();
            }
            return Ontology.decodeRows(reply.getContent());
        } catch (Exception e) {
            LOG.warning("[QuizAgent] Заявката към онтологията пропадна: " + e.getMessage());
            return List.of();
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
            LOG.warning("[QuizAgent] Търсенето в DF пропадна: " + e.getMessage());
        }
        return null;
    }
}
