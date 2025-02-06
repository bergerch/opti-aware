package bftsmart.optilog;


import bftsmart.consensus.Decision;
import bftsmart.reconfiguration.ServerViewController;


public interface Monitor {

    // Updates the monitor with consistent measurements output from the consensus engine
    public void onReceiveMonitoringMessage(Decision decision);


}
