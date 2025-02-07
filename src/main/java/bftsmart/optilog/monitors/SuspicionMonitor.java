package bftsmart.optilog.monitors;

import bftsmart.reconfiguration.ServerViewController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import bftsmart.optilog.Monitor;

public class SuspicionMonitor implements Monitor {


    private static SuspicionMonitor instance;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ServerViewController controller;

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
    public void notify(int senderReplicaId, byte[] measurement, int consensusInstance) {

        logger.warn("SuspicionMonitor.java: Method not implemented yet");
    }
}
