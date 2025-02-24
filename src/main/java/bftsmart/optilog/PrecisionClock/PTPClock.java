package bftsmart.optilog.PrecisionClock;

import java.time.Instant;
import java.io.File;

public class PTPClock {

    static {
        System.loadLibrary("ptp"); // Load the compiled native library
    }
    // Native method declaration
    public native static long getPTPTimestamp();

    private static final String PTP_DEVICE_PATH = "/dev/ptp0";

    private static boolean checkPTP() {
        return new java.io.File(PTP_DEVICE_PATH).exists();
    }

    public static void checkTimeProtocol() {
        // Check if PTP is available
        boolean isPTPAvailable = checkPTP();

        // Print whether PTP or NTP is being used
        if (isPTPAvailable) {
            System.out.println("> OptiLog: ✅ Using PTP (Precision Time Protocol)");
        } else {
            System.out.println("> OptiLog: 🕒 Using NTP (Network Time Protocol)");
        }

        // Generate and print a timestamp
        Instant timestamp = PTP_timestamp();
        System.out.println("System started at:" + timestamp);
    }

    public static long precisionTimestamp() {
        Instant now = PTP_timestamp();
        return now.getEpochSecond() * 1_000_000_000L + now.getNano();
    }

    public static Instant PTP_timestamp() {
        long timestampNanos = getPTPTimestamp();

        if (timestampNanos > 0) {
            Instant ptpTime = Instant.ofEpochSecond(
                    timestampNanos / 1_000_000_000L, // Convert nanoseconds to seconds
                    timestampNanos % 1_000_000_000L  // Keep remaining nanoseconds
            );
            System.out.println("✅ PTP Timestamp: " + ptpTime);
            return ptpTime;
        } else {
            Instant fallbackTime = Instant.now(); // Use high-precision Java time
            System.out.println("🕒 Fallback to NTP: " + fallbackTime);
            return fallbackTime;
        }
    }

    public static void main(String[] args) {
        precisionTimestamp();
    }
}