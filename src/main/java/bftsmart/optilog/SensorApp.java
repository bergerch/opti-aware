package bftsmart.optilog;

import bftsmart.optilog.sensors.LatencyMeasurement;
import bftsmart.optilog.sensors.LatencySensor;
import bftsmart.optilog.sensors.SuspicionSensor;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.ServiceProxy;
import bftsmart.tom.core.messages.TOMMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class disseminates this replicas measurements with total order
 *
 * @author cb
 */
public class SensorApp {

    private static SensorApp instance;

    private ServiceProxy consensusEngine;
    private ServerViewController svc;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /***
     * Sensors
     **/
    // Collects local latency monitoring information
    private LatencySensor proposeLatencySensor;
    private LatencySensor writeLatencySensor;

    // Collects local suspicion information
    private SuspicionSensor suspicionSensor;

    private SensorApp(ServerViewController svc) {

        // Init the gateway to the consensus engine:
        int myID = svc.getStaticConf().getProcessId();
        consensusEngine = new ServiceProxy(myID);
        this.svc = svc;

        // Init all of the sensors to collect data from:
        proposeLatencySensor = new LatencySensor(svc);
        writeLatencySensor = new LatencySensor(svc);
    }

    public static SensorApp getInstance(ServerViewController svc) {
        if (SensorApp.instance == null) {
            SensorApp.instance = new SensorApp(svc);
        }
        return SensorApp.instance;
    }

    public void start() {
        // Create a time tor periodically disseminate this replica's latency measurements to all replicas
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                // Get freshest write latencies from Monitor
                Long[] writeLatencies = writeLatencySensor.create_L("WRITE");
                Long[] proposeLatencies = proposeLatencySensor.create_L("PROPOSE");

                LatencyMeasurement li = new LatencyMeasurement(svc.getCurrentViewN(), writeLatencies, proposeLatencies);
                byte[] data = li.toBytes();

                consensusEngine.propose(data, TOMMessageType.MEASUREMENT_LATENCY);
                logger.debug("|---> Disseminating monitoring information with total order! ");
            }
        }, svc.getStaticConf().getSynchronisationDelay(), svc.getStaticConf().getSynchronisationPeriod());
    }

    public synchronized LatencySensor getWriteLatencySensor() {
        return writeLatencySensor;
    }

    public synchronized LatencySensor getProposeLatencySensor() {
        return proposeLatencySensor;
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
