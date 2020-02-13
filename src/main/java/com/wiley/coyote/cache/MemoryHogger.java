package com.wiley.coyote.cache;

/**
 * Represents an in-memory resource. Upon creation, allocates a byte array of
 * the given size.
 */
public class MemoryHogger {

    private byte[] hogger;

    private MemoryHogger(int sizeInBytes) {
        this.hogger = new byte[sizeInBytes];
    }

    public static MemoryHogger bytes(int sizeInBytes) {
        return new MemoryHogger(sizeInBytes);
    }

    public static MemoryHogger bytes(long sizeInBytes) {
        sizeInBytes = Math.min(Integer.MAX_VALUE, sizeInBytes);
        return new MemoryHogger((int) sizeInBytes);
    }

    public static MemoryHogger kiloBytes(double sizeInKiloBytes) {
        return new MemoryHogger((int) (sizeInKiloBytes * 1024));
    }

    public static MemoryHogger megaBytes(double sizeInMegaBytes) {
        return new MemoryHogger((int) (sizeInMegaBytes * 1024 * 1024));
    }

    public byte[] getHogger() {
        return hogger;
    }
}
