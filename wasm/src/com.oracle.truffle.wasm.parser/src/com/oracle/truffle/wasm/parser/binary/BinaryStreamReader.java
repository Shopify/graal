/*
 * Copyright (c) 2019, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.wasm.parser.binary;

public abstract class BinaryStreamReader {
    protected byte[] data;
    protected int offset;

    public BinaryStreamReader(byte[] data) {
        this.data = data;
        this.offset = 0;
    }

    public int readSignedInt32() {
        int result = 0;
        int shift = 0;
        byte b;
        do {
            b = read1();
            result |= ((b & 0x7F) << shift);
            shift += 7;
        } while ((b & 0x80) != 0);

        if ((shift < 32) && (b & 0x40) == 0) {
            result |= (~0 << shift);
        }
        return result;
    }

    public int peakUnsignedInt32(int ahead) {
        int result = 0;
        int shift = 0;
        int i = 0;
        do {
            byte b = peak1(i + ahead);
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
            i++;
        } while (shift < 35);
        if (shift == 35) {
            Assert.fail("Unsigned LEB128 overflow");
        }
        return result;
    }

    // This is used for indices, so we don't expect values larger than 2^31.
    public int readUnsignedInt32() {
        int result = 0;
        int shift = 0;
        do {
            byte b = read1();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
        } while (shift < 35);
        if (shift == 35) {
            Assert.fail("Unsigned LEB128 overflow");
        }
        return result;
    }

    public float readF32() {
        int rawBits = read4();
        return Float.intBitsToFloat(rawBits);
    }

    public double readF64() {
        long rawBits = read8();
        return Double.longBitsToDouble(rawBits);
    }

    public byte read1() {
        return data[offset++];
    }

    public int read4() {
        int result = 0;
        for (int i = 0; i != 4; ++i) {
            int x = Byte.toUnsignedInt(read1());
            result |= x << 8 * i;
        }
        return result;
    }

    public long read8() {
        long result = 0;
        for (int i = 0; i != 8; ++i) {
            long x = Byte.toUnsignedLong(read1());
            result |= x << 8 * i;
        }
        return result;
    }

    public byte peak1() {
        return data[offset];
    }

    public byte peak1(int ahead) {
        return data[offset + ahead];
    }

    public void seek(int offset) {
        this.offset = offset;
    }

    public int offset() {
        return offset;
    }
}
