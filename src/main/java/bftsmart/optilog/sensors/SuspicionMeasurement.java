package bftsmart.optilog.sensors;

import java.io.*;
import java.util.Objects;

import java.io.*;
import java.util.Objects;

public class SuspicionMeasurement implements Serializable {
    private int suspect;        // Suspected process ID
    private SuspicionType type; // Either SLOW or FALSE

    // Constructor
    public SuspicionMeasurement(int suspect, SuspicionType type) {
        this.suspect = suspect;
        this.type = type;
    }

    // Getters
    public int getSuspect() {
        return suspect;
    }

    public SuspicionType getType() {
        return type;
    }

    // Setters
    public void setSuspect(int suspect) {
        this.suspect = suspect;
    }

    public void setType(SuspicionType type) {
        this.type = type;
    }

    // Convert SuspicionMeasurement to byte array using DataOutputStream
    public static byte[] toBytes(SuspicionMeasurement measurement) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeInt(measurement.getSuspect()); // Write suspect (int)
            dos.writeInt(measurement.getType().getCode()); // Write type as int

            return baos.toByteArray(); // Return serialized byte array

        } catch (IOException e) {
            throw new RuntimeException("Error serializing SuspicionMeasurement", e);
        }
    }

    // Convert byte array back to SuspicionMeasurement using DataInputStream
    public static SuspicionMeasurement fromBytes(byte[] measurement) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(measurement);
             DataInputStream dis = new DataInputStream(bais)) {

            int suspect = dis.readInt(); // Read suspect
            int typeCode = dis.readInt(); // Read type as int
            SuspicionType type = SuspicionType.fromCode(typeCode); // Convert int to enum

            return new SuspicionMeasurement(suspect, type);

        } catch (IOException e) {
            throw new RuntimeException("Error deserializing SuspicionMeasurement", e);
        }
    }

    // toString method for debugging
    @Override
    public String toString() {
        return "SuspicionMeasurement{" +
                "suspect=" + suspect +
                ", type=" + type +
                '}';
    }

    // equals and hashCode for object comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuspicionMeasurement that = (SuspicionMeasurement) o;
        return suspect == that.suspect && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(suspect, type);
    }
}
