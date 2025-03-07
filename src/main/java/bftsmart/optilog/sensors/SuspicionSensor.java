package bftsmart.optilog.sensors;

import bftsmart.consensus.messages.ConsensusMessage;
import bftsmart.optilog.PrecisionClock.PTPClock;
import bftsmart.optilog.SensorApp;
import bftsmart.reconfiguration.ServerViewController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;


/**
 * This class allows to store and receive a replica's own  monitoring information. Note that all measurement data in here
 * is viewed by the perspective of what a single replica has measured recently by itself without a guarantee to be synchronized yet
 *
 * @author cb
 */
public class SuspicionSensor {

    private int window;
    private ServerViewController controller;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    private long deltaRound = 0;        // delay until leader should start disseminating a proposal
                                        // in ms
    private long consensusLatencyExpectation = 0;

    private HashMap<Integer, Long> proposalSentTimes;
    private HashMap<Integer, Long> proposalReceivedTimes;


    private SensorApp sensorapp;

    private boolean initialized = false;

    /**
     * Creates a new instance of a latency monitor
     *
     * @param controller server view controller
     */
    public SuspicionSensor(ServerViewController controller, SensorApp sensorAppInstance) {
        this.window = controller.getStaticConf().getMonitoringWindow();
        this.controller = controller;

        proposalSentTimes = new HashMap<>();
        proposalReceivedTimes = new HashMap<>();

        sensorapp = sensorAppInstance;
        // init();
    }

    public synchronized boolean checkProposal(ConsensusMessage proposal) {


        if (!initialized || proposal == null || 0 >= proposal.getNumber()) {
            return true;
        }
        int consensusNumber = proposal.getNumber();

        long sentTime = proposal.getSentTimestamp();
        proposalSentTimes.put(consensusNumber, sentTime);

        //long receivedTime = PTPClock.precisionTimestamp();
        //proposalReceivedTimes.put(consensusNumber, receivedTime);

        long lastSentTime = -1;
        if (proposalSentTimes.get(consensusNumber - 1) != null) {
            lastSentTime = proposalSentTimes.get(consensusNumber - 1);
        } else {
            return true;
        }
        long roundTime = sentTime - lastSentTime;
        boolean delayed = roundTime > deltaRound;

        /* Uses that code to test functionality
        if (consensusNumber % 10 == 0) {
            StringBuilder s = new StringBuilder();
            for (Integer i : proposalSentTimes.keySet()) {
                s.append("(").append(i).append(",").append(proposalSentTimes.get(i)).append(")");
            }
            logger.info("Hashmap {}", s.toString());
            long now = PTPClock.precisionTimestamp();
            logger.info("PrecisionClock now() {}", now);
            logger.info("Delayed by {} ms, is it delayed? {}, RoundTime: {} ms, Expectation: {} ms, SentTime: {}, LastSentTime: {}",
                    (roundTime-deltaRound)/1000000, delayed, roundTime/1000000, this.consensusLatencyExpectation/1000000, sentTime, lastSentTime);
        }*/

        if (delayed) {
            SuspicionMeasurement suspicion = new SuspicionMeasurement(proposal.getSender(), SuspicionType.SLOW);
            if (controller.getStaticConf().getProcessId() == 0) { // For more comprehensive logs, let only first process output to LOG a Warning
                logger.warn("OptiLog >> SuspicionSensor >> Delayed proposal, expected consensus to be {} ns but I observed {} ns for consensus {}", deltaRound, roundTime, consensusNumber);
            } else {
                logger.trace("OptiLog >> SuspicionSensor >> Delayed proposal, expected consensus to be {} ns but I observed {} ns for consensus {}", deltaRound, roundTime, consensusNumber);
            }
            sensorapp.publishSuspicion(suspicion);
        }
        return !delayed;
    }

    public synchronized boolean checkVote(ConsensusMessage vote) {

        // Todo implement
        return true;
    }

    public synchronized void setDeltaRound(long consensusLatencyExpectation) {
        this.consensusLatencyExpectation = consensusLatencyExpectation;
        this.deltaRound = (long) (consensusLatencyExpectation * controller.getStaticConf().getSuspicionDelta());
        initialized = true;
    }

    public synchronized void clear() {
        initialized = false;
    }

}