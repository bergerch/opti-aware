package bftsmart.optilog;

import bftsmart.optilog.monitors.LatencyMonitor;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.ServiceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class disseminates this replicas measurements with total order
 *
 * @author cb
 */
public class GlobalSynchronizer {

    private static GlobalSynchronizer instance;

    private ServiceProxy consensusEngine;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<Monitor> monitors = new ArrayList<>();

    /**
     * Creates a new Synchronizer to disseminate data with total order
     *
     * @param svc server view controller
     */
    public GlobalSynchronizer(ServerViewController svc) {

        int myID = svc.getStaticConf().getProcessId();
        consensusEngine = new ServiceProxy(myID);

        // Create a time to periodically broadcast this replica's measurements to all replicas
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                // Get freshest write latenciesfrom Monitor
                Long[] writeLatencies = LatencyMonitor.getInstance(svc).getFreshestWriteLatencies();
                Long[] proposeLatencies = LatencyMonitor.getInstance(svc).getFreshestProposeLatencies();

                LatencyMeasurement li = new LatencyMeasurement(svc.getCurrentViewN(), writeLatencies, proposeLatencies);
                byte[] data = li.toBytes();

                consensusEngine.invokeOrderedMonitoring(data);

                logger.debug("|---> Disseminating monitoring information with total order! ");
            }
        }, svc.getStaticConf().getSynchronisationDelay(), svc.getStaticConf().getSynchronisationPeriod());
    }

    public GlobalSynchronizer getInstance(ServerViewController svc) {
        if (GlobalSynchronizer.instance == null) {
            GlobalSynchronizer.instance = new GlobalSynchronizer(svc);
        }
        return GlobalSynchronizer.instance;
    }

    public void attachMonitor(Monitor monitor) {
        monitors.add(monitor);
    }

    /**
     * Converts Long array to byte array
     *
     * @param array Long array
     * @return byte array
     * @throws IOException
     */
    public static byte[] longToBytes(Long[] array) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (Long l : array)
            dos.writeLong(l);

        dos.close();
        return baos.toByteArray();
    }

    /**
     * Converts byte array to Long array
     *
     * @param array byte array
     * @return Long array
     * @throws IOException
     */
    public static Long[] bytesToLong(byte[] array) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(array);
        DataInputStream dis = new DataInputStream(bais);
        int n = array.length / Long.BYTES;
        Long[] result = new Long[n];
        for (int i = 0; i < n; i++)
            result[i] = dis.readLong();

        dis.close();
        return result;
    }



}
