package trainer;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.logging.Logger;

public class OntologyAgent extends Agent {

    private static final Logger LOG = Logger.getLogger(OntologyAgent.class.getName());

    public static final String SERVICE = "ontology";

    @Override
    protected void setup() {
        Ontology.get();
        register();
        addBehaviour(new Handle());
        LOG.info("[OntologyAgent] Готов. Онтологията има " + Ontology.get().size() + " тройки.");
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
            sd.setName("byzantine-ontology");
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (Exception e) {
            LOG.warning("[OntologyAgent] Регистрацията в DF пропадна: " + e.getMessage());
        }
    }

    public static String handle(String content) {
        if (content == null || content.isBlank()) return "ERROR: празно съобщение";

        if (content.startsWith("GET:")) {
            String what = content.substring("GET:".length()).trim();
            if ("glasove".equals(what)) return Ontology.encodeRows(Quiz.glasove());
            if ("stapki".equals(what))  return Ontology.encodeRows(Quiz.stapki());
            return "ERROR: непознати данни: " + what;
        }

        if (content.startsWith("UPDATE:")) {
            String sparql = content.substring("UPDATE:".length()).trim();
            if (Ontology.get().update(sparql)) return "OK";
            String why = Ontology.get().lastError();
            return "ERROR: " + (why != null ? why : "неуспешна промяна");
        }

        if (content.trim().equalsIgnoreCase("PING")) return "PONG";
        return "ERROR: непозната команда";
    }

    private class Handle extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg == null) { block(); return; }

            String result = OntologyAgent.handle(msg.getContent());
            ACLMessage reply = msg.createReply();

            if (result.startsWith("ERROR:")) {
                reply.setPerformative(ACLMessage.FAILURE);
            } else if (result.equals("OK")) {
                reply.setPerformative(ACLMessage.CONFIRM);
            } else {
                reply.setPerformative(ACLMessage.INFORM);
            }
            reply.setContent(result);
            myAgent.send(reply);
        }
    }
}
