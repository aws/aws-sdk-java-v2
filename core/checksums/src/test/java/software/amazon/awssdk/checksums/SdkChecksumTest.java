package software.amazon.awssdk.checksums;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SdkChecksumTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "CRC32C",
        "CRC32",
        "SHA1",
        "SHA256",
        "MD5",
        "CRC64NVME"
    })
    void checksumCachesAreParallelSafe(String algorithm) {
        int numChecksums = 100;
        List<SdkChecksum> checksums = new ArrayList<>(numChecksums);
        List<byte[]> checksumBytes = new ArrayList<>(numChecksums);

        for (int i = 0; i < numChecksums; i++) {
            SdkChecksum checksum = SdkChecksum.forAlgorithm(() -> algorithm);
            byte[] bytes = getRandomBytes();
            checksum.update(bytes);
            checksumBytes.add(checksum.getChecksumBytes());

            checksum = SdkChecksum.forAlgorithm(() -> algorithm);
            checksum.reset();
            checksum.update(bytes);
            checksums.add(checksum);
        }

        SdkChecksum checksumToUpdate = checksums.get(0);
        checksumToUpdate.update(getRandomBytes());
        byte[] newChecksumBytes = checksumToUpdate.getChecksumBytes();
        checksumBytes.set(0, newChecksumBytes);

        for (int j = 1; j < numChecksums; j++) {
            SdkChecksum checksumToTest = checksums.get(j);
            byte[] expected = checksumBytes.get(j);
            byte[] actual = checksumToTest.getChecksumBytes();
            assertArrayEquals(expected, actual);
        }
    }

    private static byte[] getRandomBytes() {
        byte[] randomBytes = new byte[1024];
        Random random = new Random();
        random.nextBytes(randomBytes);
        return randomBytes;
    }
}