package bftsmart.optilog.monitors;

import bftsmart.optilog.SensorApp;
import bftsmart.optilog.sensors.SuspicionMeasurement;
import bftsmart.optilog.sensors.SuspicionSensor;
import bftsmart.optilog.sensors.SuspicionType;
import bftsmart.reconfiguration.ServerViewController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import bftsmart.optilog.Monitor;


public class SuspicionMonitor implements Monitor {


    private static SuspicionMonitor instance;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ServerViewController controller;

    //private List<SuspicionMeasurement> suspicions = new LinkedList<>();

    private SuspicionGraph suspicionGraph;

    // For cleaning up old suspicions perodically:
    private int lastConsensusRemovedOldSuspicions = 0;
    private int removedOldSuspicions = 0;

    private SuspicionMonitor(ServerViewController svc) {
        this.controller = svc;
        this.suspicionGraph = new SuspicionGraph(controller);
    }


    public static SuspicionMonitor getInstance(ServerViewController svc) {
        if (instance == null) {
            instance = new SuspicionMonitor(svc);
        }
        return instance;
    }

    @Override
    public synchronized void notify(int reporter, byte[] measurement, int consensusInstance) {

        SuspicionMeasurement suspicion = SuspicionMeasurement.fromBytes(measurement);
        //suspicions.add(suspicion);
        suspicionGraph.addSuspicion(reporter, suspicion.getSuspect());

        if (suspicion.getType() == SuspicionType.SLOW) {
            SensorApp.getInstance(controller).getSuspicionSensor().returnSuspicionIfFalselyAccused(suspicion, reporter);
        }
        // Proof of Concept for testing:
        if (controller.getStaticConf().getProcessId() == 1) { // For more comprehensive logs, let only first process output to LOG a Warning
            suspicionGraph.printGraphAscii();
        } else {
            logger.trace(">> OptiLog > SuspicionMonitor: New suspicion recorded: {}", suspicion);;
        }

    }

    public synchronized void notify(int consensusInstance) {
        // Todo: Here we "clean up" the suspicion graph, by weakening and removing "old" suspicions
        // Todo:  Replace later by a more refined implementation
        if ((consensusInstance - lastConsensusRemovedOldSuspicions) / 100 > removedOldSuspicions ) {
            suspicionGraph.removeSuspicions(0.1 * (consensusInstance - lastConsensusRemovedOldSuspicions));
            removedOldSuspicions++;
            lastConsensusRemovedOldSuspicions = consensusInstance;
        }

    }

    public SuspicionGraph getSuspicionGraph() {
        return suspicionGraph;
    }

}
