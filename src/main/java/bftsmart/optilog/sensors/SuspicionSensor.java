package bftsmart.optilog.sensors;

import bftsmart.reconfiguration.ServerViewController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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


    /**
     * Creates a new instance of a latency monitor
     *
     * @param controller server view controller
     */
    public SuspicionSensor(ServerViewController controller) {
        this.window = controller.getStaticConf().getMonitoringWindow();
        this.controller = controller;
        // init();
    }
}