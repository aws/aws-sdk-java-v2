import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Lazy;

@SdkInternalApi
public final class DefaultPartitionDataProvider implements PartitionDataProvider {
    private static final String DEFAULT_PARTITIONS_DATA = "partitions.json";

    private static final Lazy<Partitions> PARTITIONS = new Lazy<>(DefaultPartitionDataProvider::doLoadPartitions);

    @Override
    public Partitions loadPartitions() {
        return PARTITIONS.getValue();
    }

    private static Partitions doLoadPartitions() {
        InputStream json = DefaultPartitionDataProvider.class.getResourceAsStream(DEFAULT_PARTITIONS_DATA);
        try {
            return Partitions.fromNode(JsonNode.parser().parse(json));
        } finally {
            IoUtils.closeQuietly(json, null);
        }
    }
}
