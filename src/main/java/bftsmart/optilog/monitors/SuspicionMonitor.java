package bftsmart.optilog.monitors;

import bftsmart.optilog.sensors.SuspicionMeasurement;
import bftsmart.reconfiguration.ServerViewController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import bftsmart.optilog.Monitor;

import java.util.LinkedList;
import java.util.List;


public class SuspicionMonitor implements Monitor {


    private static SuspicionMonitor instance;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ServerViewController controller;

    private List<SuspicionMeasurement> suspicions = new LinkedList<>();

    private SuspicionMonitor(ServerViewController svc) {
        this.controller = svc;
    }


    public static SuspicionMonitor getInstance(ServerViewController svc) {
        if (instance == null) {
            instance = new SuspicionMonitor(svc);
        }
        return instance;
    }

    @Override
    public synchronized void notify(int senderReplicaId, byte[] measurement, int consensusInstance) {

        SuspicionMeasurement suspicion = SuspicionMeasurement.fromBytes(measurement);
        suspicions.add(suspicion);
        // Proof of Concept for testing:
        if (controller.getStaticConf().getProcessId() == 0) { // For more comprehensive logs, let only first process output to LOG a Warning
            logger.warn(">> OptiLog > SuspicionMonitor: New suspicion recorded: {}", suspicion);
        } else {
            logger.trace(">> OptiLog > SuspicionMonitor: New suspicion recorded: {}", suspicion);;
        }

    }


}
