package software.amazon.awssdk.core.io;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.io.ChecksumValidatingInputStream;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ChecksumValidatingInputStreamTest {

    @Test
    public void validCheckSumMatch() throws IOException {
        String initialString = "Hello world";
        InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
        SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(Algorithm.SHA256);
        ChecksumValidatingInputStream checksumValidatingInputStream =
                new ChecksumValidatingInputStream(targetStream, sdkChecksum,
                        "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=");

        Assertions.assertThat(readAsStrings(checksumValidatingInputStream)).isEqualTo("Hello world");

    }
    @Test
    public void validCheckSumMismatch()  {
        String initialString = "Hello world";
        InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
        SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(Algorithm.SHA256);
        ChecksumValidatingInputStream checksumValidatingInputStream =
                new ChecksumValidatingInputStream(targetStream, sdkChecksum,
                        "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfz1=");

        Assertions.assertThatExceptionOfType(SdkClientException.class)
                .isThrownBy(() ->readAsStrings(checksumValidatingInputStream));
    }


    private String readAsStrings(ChecksumValidatingInputStream checksumValidatingInputStream) throws IOException {
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (checksumValidatingInputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        return textBuilder.toString();
    }

}
