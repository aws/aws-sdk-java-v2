/*
 * Copyright (PD) 2006 The Bitzi Corporation
 * Please see the end of this file for full license text.
 */

package org.bitpedia.util;

/**
 * Base32 - encodes and decodes RFC3548 Base32
 * (see http://www.faqs.org/rfcs/rfc3548.html )
 *
 * @author Robert Kaye
 * @author Gordon Mohr
 */
public class Base32 {
    private static final String BASE_32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int[] BASE_32_LOOKUP = {
        0xFF, 0xFF, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, // '0', '1', '2', '3', '4', '5', '6', '7'
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, // '8', '9', ':', ';', '<', '=', '>', '?'
        0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, // '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G'
        0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, // 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O'
        0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, // 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W'
        0x17, 0x18, 0x19, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, // 'X', 'Y', 'Z', '[', '\', ']', '^', '_'
        0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, // '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g'
        0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, // 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o'
        0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, // 'p', 'q', 'r', 's', 't', 'u', 'v', 'w'
        0x17, 0x18, 0x19, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF  // 'x', 'y', 'z', '{', '|', '}', '~', 'DEL'
    };

    /**
     * Encodes byte array to Base32 String.
     *
     * @param bytes Bytes to encode.
     * @return Encoded byte array <code>bytes</code> as a String.
     *
     */
    public static String encode(final byte[] bytes) {
        int i = 0;
        int index = 0;
        int digit = 0;
        int currByte;
        int nextByte;
        StringBuilder base32 = new StringBuilder((bytes.length + 7) * 8 / 5);

        while (i < bytes.length) {
            currByte = (bytes[i] >= 0) ? bytes[i] : (bytes[i] + 256); // unsign

            /* Is the current digit going to span a byte boundary? */
            if (index > 3) {
                if ((i + 1) < bytes.length) {
                    nextByte =
                            (bytes[i + 1] >= 0) ? bytes[i + 1] : (bytes[i + 1] + 256);
                } else {
                    nextByte = 0;
                }

                digit = currByte & (0xFF >> index);
                index = (index + 5) % 8;
                digit <<= index;
                digit |= nextByte >> (8 - index);
                i++;
            } else {
                digit = (currByte >> (8 - (index + 5))) & 0x1F;
                index = (index + 5) % 8;
                if (index == 0) {
                    i++;
                }
            }
            base32.append(BASE_32_CHARS.charAt(digit));
        }

        return base32.toString();
    }

    /**
     * Decodes the given Base32 String to a raw byte array.
     *
     * @return Decoded <code>base32</code> String as a raw byte array.
     */
    public static byte[] decode(final String base32) {
        int i;
        int index;
        int lookup;
        int offset;
        int digit;
        byte[] bytes = new byte[base32.length() * 5 / 8];

        for (i = 0, index = 0, offset = 0; i < base32.length(); i++) {
            lookup = base32.charAt(i) - '0';

            /* Skip chars outside the lookup table. */
            if (lookup < 0 || lookup >= BASE_32_LOOKUP.length) {
                continue;
            }

            digit = BASE_32_LOOKUP[lookup];

            /* If this digit is not in the table, ignore it. */
            if (digit == 0xFF) {
                continue;
            }

            if (index <= 3) {
                index = (index + 5) % 8;
                if (index == 0) {
                    bytes[offset] |= digit;
                    offset++;
                    if (offset >= bytes.length) {
                        break;
                    }
                } else {
                    bytes[offset] |= digit << (8 - index);
                }
            } else {
                index = (index + 5) % 8;
                bytes[offset] |= (digit >>> index);
                offset++;

                if (offset >= bytes.length) {
                    break;
                }
                bytes[offset] |= digit << (8 - index);
            }
        }
        return bytes;
    }

    /** For testing, take a command-line argument in Base32, decode, print in hex,
     * encode, print
     *
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Supply a Base32-encoded argument.");
            return;
        }
        System.out.println(" Original: " + args[0]);
        byte[] decoded = Base32.decode(args[0]);
        System.out.print("      Hex: ");
        for (final byte aDecoded : decoded) {
            int b = aDecoded;
            if (b < 0) {
                b += 256;
            }
            System.out.print((Integer.toHexString(b + 256)).substring(1));
        }
        System.out.println();
        System.out.println("Reencoded: " + Base32.encode(decoded));
    }
}

/* (PD) 2003 The Bitzi Corporation
 *
 * 1. Authorship. This work and others bearing the above
 * label were created by, or on behalf of, the Bitzi
 * Corporation. Often other public domain material by
 * other authors is also incorporated; this should be
 * clear from notations in the source code.
 *
 * 2. Release. The Bitzi Corporation places these works
 * into the public domain, disclaiming all rights granted
 * us by copyright law.
 *
 * You are completely free to copy, use, redistribute
 * and modify this work, though you should be aware of
 * points (3) and (4), below.
 *
 * 3. Trademark Advisory. The Bitzi Corporation reserves
 * all rights with regard to any of its trademarks which
 * may appear herein, such as "Bitzi" or "Bitcollider".
 * Please take care that your uses of this work do not
 * infringe on our trademarks or imply our endorsement.
 * For example, you should change labels and identifier
 *  strings in your derivative works where appropriate.
 *
 * 4. Disclaimer. THIS SOFTWARE IS PROVIDED BY THE AUTHOR
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Please see http://bitzi.com/publicdomain or write
 * info@bitzi.com for more info.
 */