package org.takesome.frozenlands.engine.icons;

final class IcoBinary {
    private IcoBinary() {
    }

    static int readU16(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    static int readI32(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    static int readPackedBit(byte[] data, int rowOffset, int x) {
        int index = rowOffset + x / 8;
        int bit = 7 - (x % 8);
        return (data[index] >> bit) & 0x01;
    }

    static int readPackedNibble(byte[] data, int rowOffset, int x) {
        int b = data[rowOffset + x / 2] & 0xFF;
        return (x & 1) == 0 ? (b >> 4) & 0x0F : b & 0x0F;
    }
}
