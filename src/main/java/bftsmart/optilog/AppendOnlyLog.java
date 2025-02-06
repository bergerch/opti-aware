package bftsmart.optilog;

import bftsmart.consensus.Decision;
import bftsmart.optilog.monitors.LatencyMonitor;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AppendOnlyLog {

    private static AppendOnlyLog instance;

    private final Logger APPEND_TO_LOG = LoggerFactory.getLogger(this.getClass());

    private ServerViewController svc;

    public static AppendOnlyLog getInstance(ServerViewController svc) {
        if (instance == null) {
            instance = new AppendOnlyLog(svc);
        }
        return AppendOnlyLog.instance;
    }

    private AppendOnlyLog(ServerViewController controller) {
        svc = controller;
    }

    public void record(Decision decision) {
        for (TOMMessage tm : decision.getDeserializedValue()) {
            if (TOMMessageType.isMonitoringType(tm.getReqType())) {
                APPEND_TO_LOG.trace("Consensus output monitoring message, " + tm.toString());
                switch (tm.getReqType()) {
                    case MEASUREMENT_LATENCY:
                        LatencyMonitor.getInstance(svc)
                                .notify(tm.getSender(), tm.getContent(), decision.getConsensusId());
                        break;
                    case MEASUREMENT_SUSPICION:
                        // TODO Implement later
                        break;
                    case MEASUREMENT_MISBEHAVIOR:
                        // Todo Implement later
                        break;
                }
                //onReceiveMonitoringInformation(tm.getSender(), tm.getContent(), decision.getConsensusId());

            } else {
                APPEND_TO_LOG.trace("Consensus output client command ");
            }
        }
    }

}
