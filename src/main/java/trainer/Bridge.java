package trainer;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Bridge extends Agent {

    private static final Logger LOG = Logger.getLogger(Bridge.class.getName());
    private static final long TIMEOUT = 15_000;

    private static volatile Bridge instance;
    public static Bridge get() { return instance; }

    private final Map<String, AID> cache = new HashMap<>();

    @Override
    protected void setup() {
        instance = this;
        LOG.info("[Bridge] Готов.");
    }

    public static void ask(String service, String content,
                           Consumer<String> onOk, Consumer<String> onError) {
        Bridge b = get();
        if (b == null) {
            offline(service, content, onOk, onError);
            return;
        }
        b.addBehaviour(new Send(service, content, onOk, onError));
    }

    private static void offline(String service, String content,
                                Consumer<String> onOk, Consumer<String> onError) {
        try {
            String result;
            if (OntologyAgent.SERVICE.equals(service))      result = OntologyAgent.handle(content);
            else if (QuizAgent.SERVICE.equals(service))     result = QuizAgent.handle(content);
            else if (LibrarianAgent.SERVICE.equals(service)) result = LibrarianAgent.handle(content);
            else if (ComposerAgent.SERVICE.equals(service))  result = ComposerAgent.handle(content);
            else { onError.accept(Lang.t("svcUnknown") + service); return; }

            if (result != null && result.startsWith("ERROR:")) onError.accept(result);
            else onOk.accept(result);
        } catch (Exception e) {
            onError.accept(e.getMessage());
        }
    }

    private static class Send extends OneShotBehaviour {
        private final String service, content;
        private final Consumer<String> onOk, onError;

        Send(String service, String content, Consumer<String> onOk, Consumer<String> onError) {
            this.service = service; this.content = content;
            this.onOk = onOk; this.onError = onError;
        }

        @Override
        public void action() {
            try {
                AID target = ((Bridge) myAgent).resolve(service);
                if (target == null) {
                    offline(service, content, onOk, onError);
                    return;
                }

                String key = "b-" + System.currentTimeMillis();
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(target);
                req.setContent(content);
                req.setReplyWith(key);
                myAgent.send(req);

                ACLMessage reply = myAgent.blockingReceive(
                    MessageTemplate.MatchInReplyTo(key), TIMEOUT);

                if (reply == null) {
                    onError.accept(Lang.t("svcTimeout"));
                } else if (reply.getPerformative() == ACLMessage.INFORM
                        || reply.getPerformative() == ACLMessage.CONFIRM) {
                    onOk.accept(reply.getContent());
                } else {
                    onError.accept(reply.getContent());
                }
            } catch (Exception e) {
                onError.accept(e.getMessage());
            }
        }
    }

    private AID resolve(String service) {
        AID hit = cache.get(service);
        if (hit != null) return hit;
        try {
            DFAgentDescription tmpl = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(service);
            tmpl.addServices(sd);
            DFAgentDescription[] found = DFService.search(this, tmpl);
            if (found.length > 0) {
                cache.put(service, found[0].getName());
                return found[0].getName();
            }
        } catch (Exception e) {
            LOG.warning("[Bridge] Търсенето в DF пропадна: " + e.getMessage());
        }
        return null;
    }
}
