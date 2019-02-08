package org.yamcs.tctm.ccsds;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.yamcs.tctm.ccsds.error.BchEncoder;

public class BchEncoderTest {

    @Test
    public void test1() {
        byte[][] msg = {
                { 0x22, (byte) 0xF6, 0x00, (byte) 0xFF, 0x00, 0x42, 0x1A, 0x12 },
                { (byte) 0x8C, (byte) 0xC0, 0x0E, 0x01, 0x0D, 0x19, 0x06, 0x5A },
                { 0x30, 0x1B, 0x00, 0x09, 0x00, (byte) 0x82, 0x00, (byte) 0x54 },
                { 0x10, (byte) 0xE4, (byte) 0xC1, 0x55, 0x55, 0x55, 0x55, 0x3E }
        };

        for(byte[] p: msg) {
            assertEquals(p[7], BchEncoder.encode(p, 7));
        }
    }
}
