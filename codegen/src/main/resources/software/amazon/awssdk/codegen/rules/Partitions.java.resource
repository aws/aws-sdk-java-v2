import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public final class Partitions {
    private static final String VERSION = "version";
    private static final String PARTITIONS = "partitions";

    private final String version;
    private final List<Partition> partitions;

    private Partitions(Builder builder) {
        this.version = builder.version;
        this.partitions = new ArrayList<>(builder.partitions);
    }

    public String version() {
        return version;
    }

    public List<Partition> partitions() {
        return partitions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Partitions that = (Partitions) o;

        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }
        return partitions != null ? partitions.equals(that.partitions) : that.partitions == null;
    }

    @Override
    public int hashCode() {
        int result = version != null ? version.hashCode() : 0;
        result = 31 * result + (partitions != null ? partitions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Partitions{" +
               "version='" + version + '\'' +
               ", partitions=" + partitions +
               '}';
    }

    public static Partitions fromNode(JsonNode node) {
        Map<String, JsonNode> objNode = node.asObject();

        Builder b = builder();

        JsonNode version = objNode.get(VERSION);
        if (version != null) {
            b.version(version.asString());
        }

        JsonNode partitions = objNode.get(PARTITIONS);
        if (partitions != null) {
            partitions.asArray().forEach(partNode -> b.addPartition(Partition.fromNode(partNode)));
        }

        return b.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String version;
        private List<Partition> partitions = new ArrayList<>();

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder partitions(List<Partition> partitions) {
            this.partitions.clear();
            if (partitions != null) {
                this.partitions.addAll(partitions);
            }
            return this;
        }

        public Builder addPartition(Partition p) {
            this.partitions.add(p);
            return this;
        }

        public Partitions build() {
            return new Partitions(this);
        }
    }
}
