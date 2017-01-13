/*
Copyright (C) 2017 Boris Timofeev

This file is part of UniPatcher.

UniPatcher is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

UniPatcher is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with UniPatcher.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.emunix.unipatcher.patch;

import android.content.Context;

import org.apache.commons.io.IOUtils;
import org.emunix.unipatcher.R;
import org.emunix.unipatcher.Utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class APS_GBA extends Patch {
    private static final byte[] MAGIC_NUMBER = {0x41, 0x50, 0x53, 0x31}; // APS1
    private static final int CHUNK_SIZE = 65536;

    private static final int[] table = { // 16-bit CRC-CCITT table
            0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50A5, 0x60C6, 0x70E7,
            0x8108, 0x9129, 0xA14A, 0xB16B, 0xC18C, 0xD1AD, 0xE1CE, 0xF1EF,
            0x1231, 0x0210, 0x3273, 0x2252, 0x52B5, 0x4294, 0x72F7, 0x62D6,
            0x9339, 0x8318, 0xB37B, 0xA35A, 0xD3BD, 0xC39C, 0xF3FF, 0xE3DE,
            0x2462, 0x3443, 0x0420, 0x1401, 0x64E6, 0x74C7, 0x44A4, 0x5485,
            0xA56A, 0xB54B, 0x8528, 0x9509, 0xE5EE, 0xF5CF, 0xC5AC, 0xD58D,
            0x3653, 0x2672, 0x1611, 0x0630, 0x76D7, 0x66F6, 0x5695, 0x46B4,
            0xB75B, 0xA77A, 0x9719, 0x8738, 0xF7DF, 0xE7FE, 0xD79D, 0xC7BC,
            0x48C4, 0x58E5, 0x6886, 0x78A7, 0x0840, 0x1861, 0x2802, 0x3823,
            0xC9CC, 0xD9ED, 0xE98E, 0xF9AF, 0x8948, 0x9969, 0xA90A, 0xB92B,
            0x5AF5, 0x4AD4, 0x7AB7, 0x6A96, 0x1A71, 0x0A50, 0x3A33, 0x2A12,
            0xDBFD, 0xCBDC, 0xFBBF, 0xEB9E, 0x9B79, 0x8B58, 0xBB3B, 0xAB1A,
            0x6CA6, 0x7C87, 0x4CE4, 0x5CC5, 0x2C22, 0x3C03, 0x0C60, 0x1C41,
            0xEDAE, 0xFD8F, 0xCDEC, 0xDDCD, 0xAD2A, 0xBD0B, 0x8D68, 0x9D49,
            0x7E97, 0x6EB6, 0x5ED5, 0x4EF4, 0x3E13, 0x2E32, 0x1E51, 0x0E70,
            0xFF9F, 0xEFBE, 0xDFDD, 0xCFFC, 0xBF1B, 0xAF3A, 0x9F59, 0x8F78,
            0x9188, 0x81A9, 0xB1CA, 0xA1EB, 0xD10C, 0xC12D, 0xF14E, 0xE16F,
            0x1080, 0x00A1, 0x30C2, 0x20E3, 0x5004, 0x4025, 0x7046, 0x6067,
            0x83B9, 0x9398, 0xA3FB, 0xB3DA, 0xC33D, 0xD31C, 0xE37F, 0xF35E,
            0x02B1, 0x1290, 0x22F3, 0x32D2, 0x4235, 0x5214, 0x6277, 0x7256,
            0xB5EA, 0xA5CB, 0x95A8, 0x8589, 0xF56E, 0xE54F, 0xD52C, 0xC50D,
            0x34E2, 0x24C3, 0x14A0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
            0xA7DB, 0xB7FA, 0x8799, 0x97B8, 0xE75F, 0xF77E, 0xC71D, 0xD73C,
            0x26D3, 0x36F2, 0x0691, 0x16B0, 0x6657, 0x7676, 0x4615, 0x5634,
            0xD94C, 0xC96D, 0xF90E, 0xE92F, 0x99C8, 0x89E9, 0xB98A, 0xA9AB,
            0x5844, 0x4865, 0x7806, 0x6827, 0x18C0, 0x08E1, 0x3882, 0x28A3,
            0xCB7D, 0xDB5C, 0xEB3F, 0xFB1E, 0x8BF9, 0x9BD8, 0xABBB, 0xBB9A,
            0x4A75, 0x5A54, 0x6A37, 0x7A16, 0x0AF1, 0x1AD0, 0x2AB3, 0x3A92,
            0xFD2E, 0xED0F, 0xDD6C, 0xCD4D, 0xBDAA, 0xAD8B, 0x9DE8, 0x8DC9,
            0x7C26, 0x6C07, 0x5C64, 0x4C45, 0x3CA2, 0x2C83, 0x1CE0, 0x0CC1,
            0xEF1F, 0xFF3E, 0xCF5D, 0xDF7C, 0xAF9B, 0xBFBA, 0x8FD9, 0x9FF8,
            0x6E17, 0x7E36, 0x4E55, 0x5E74, 0x2E93, 0x3EB2, 0x0ED1, 0x1EF0
    };

    public APS_GBA(Context context, File patch, File rom, File output) {
        super(context, patch, rom, output);
    }

    @Override
    public void apply() throws PatchException, IOException {
        long fileSize1, fileSize2, bytesLeft, offset;
        int crc, patchCrc1, patchCrc2, pCount, oCount;
        boolean isOriginal = false;
        boolean isModified = false;

        byte[] romBuf = new byte[CHUNK_SIZE];
        byte[] patchBuf = new byte[CHUNK_SIZE];

        BufferedInputStream patchStream = null;
        RandomAccessFile output = null;

        Utils.copyFile(context, romFile, outputFile);

        try {
            patchStream = new BufferedInputStream(new FileInputStream(patchFile));
            output = new RandomAccessFile(outputFile, "rw");

            byte[] magic = new byte[4];
            pCount = patchStream.read(magic);
            if (pCount < 4 || !Arrays.equals(magic, MAGIC_NUMBER))
                throw new PatchException(context.getString(R.string.notify_error_not_aps_patch));

            fileSize1 = readLEInt(patchStream);
            fileSize2 = readLEInt(patchStream);
            if (fileSize1 < 0 || fileSize2 < 0)
                throw new PatchException(context.getString(R.string.notify_error_not_aps_patch));

            bytesLeft = patchFile.length() - 12;

            while (bytesLeft > 0) {
                offset = readLEInt(patchStream);
                patchCrc1 = readLEChar(patchStream);
                patchCrc2 = readLEChar(patchStream);
                bytesLeft -= 8;
                if (offset < 0 || patchCrc1 < 0 || patchCrc2 < 0)
                    throw new PatchException(context.getString(R.string.notify_error_not_aps_patch));

                output.seek(offset);
                oCount = output.read(romBuf);
                pCount = patchStream.read(patchBuf);
                bytesLeft -= CHUNK_SIZE;
                if (pCount < CHUNK_SIZE)
                    throw new PatchException(context.getString(R.string.notify_error_not_aps_patch));

                if (oCount < CHUNK_SIZE) {
                    if (oCount < 0) oCount = 0;
                    for (int i = oCount; i < CHUNK_SIZE; i++)
                        romBuf[i] = 0x0;
                }

                crc = crc16(romBuf);

                for(int i = 0; i < CHUNK_SIZE; i++)
                    romBuf[i] ^= patchBuf[i];

                if (crc == patchCrc1) {
                    isOriginal = true;
                } else if (crc == patchCrc2) {
                    isModified = true;
                } else {
                    throw new PatchException(context.getString(R.string.notify_error_rom_not_compatible_with_patch));
                }
                if (isOriginal && isModified)
                    throw new PatchException(context.getString(R.string.notify_error_not_aps_patch));

                output.seek(offset);
                output.write(romBuf);
            }
        } finally {
            IOUtils.closeQuietly(patchStream);
            IOUtils.closeQuietly(output);
        }

        if(isOriginal) {
            Utils.truncateFile(outputFile, fileSize2);
        } else {
            Utils.truncateFile(outputFile, fileSize1);
        }
    }

    private long readLEInt(InputStream stream) throws IOException {
        long result = 0;
        int x;
        for (int i = 0; i < 4; i++) {
            x = stream.read();
            if (x == -1)
                return -1;
            result += ((long) x) << (i * 8);
        }
        return result;
    }

    private int readLEChar(InputStream stream) throws IOException {
        int result = 0;
        int x;
        for (int i = 0; i < 2; i++) {
            x = stream.read();
            if (x == -1)
                return -1;
            result += x << (i * 8);
        }
        return result;
    }

    private int crc16(byte[] data) {
        int crc = 0xffff;
        for (byte b : data) {
            crc = (crc << 8) ^ table[((crc >> 8) ^ b) & 0xff];
            crc = (short) crc & 0xffff;
        }
        return crc;
    }
}
