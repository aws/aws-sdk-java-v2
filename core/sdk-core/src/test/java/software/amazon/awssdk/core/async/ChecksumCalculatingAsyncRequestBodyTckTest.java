package software.amazon.awssdk.core.async;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.internal.async.ChecksumCalculatingAsyncRequestBody;

public class ChecksumCalculatingAsyncRequestBodyTckTest extends PublisherVerification<ByteBuffer> {

    private static final int MAX_ELEMENTS = 1000;
    private final FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
    private final Path rootDir = fs.getRootDirectories().iterator().next();
    private static final int CHUNK_SIZE = 16 * 1024;
    private final byte[] chunkData = new byte[CHUNK_SIZE];

    public ChecksumCalculatingAsyncRequestBodyTckTest() throws IOException {
        super(new TestEnvironment());
    }

    @Override
    public long maxElementsFromPublisher() {
        return MAX_ELEMENTS;
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long n) {
        return  ChecksumCalculatingAsyncRequestBody.builder()
                .asyncRequestBody(AsyncRequestBody.fromFile(fileOfNChunks(n)))
                .algorithm(Algorithm.CRC32)
                .trailerHeader("x-amz-checksum-crc32")
                .build();
    }

    private Path fileOfNChunks(long nChunks) {
        String name = String.format("%d-chunks-file.dat", nChunks);
        Path p = rootDir.resolve(name);
        if (!Files.exists(p)) {
            try (OutputStream os = Files.newOutputStream(p)) {
                for (int i = 0; i < nChunks; ++i) {
                    os.write(chunkData);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return p;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }
}