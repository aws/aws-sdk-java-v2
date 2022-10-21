import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public class Partition {
    private static final String ID = "id";
    private static final String REGION_REGEX = "regionRegex";
    private static final String REGIONS = "regions";
    private static final String OUTPUTS = "outputs";

    private final String id;
    private final String regionRegex;
    private final Map<String, RegionOverride> regions;
    private final Outputs outputs;

    private Partition(Builder builder) {
        this.id = builder.id;
        this.regionRegex = builder.regionRegex;
        this.regions = new HashMap<>(builder.regions);
        this.outputs = builder.outputs;
    }

    public String id() {
        return id;
    }

    public String regionRegex() {
        return regionRegex;
    }

    public Map<String, RegionOverride> regions() {
        return regions;
    }

    public Outputs outputs() {
        return outputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Partition partition = (Partition) o;

        if (id != null ? !id.equals(partition.id) : partition.id != null) {
            return false;
        }
        if (regionRegex != null ? !regionRegex.equals(partition.regionRegex) : partition.regionRegex != null) {
            return false;
        }
        if (regions != null ? !regions.equals(partition.regions) : partition.regions != null) {
            return false;
        }
        return outputs != null ? outputs.equals(partition.outputs) : partition.outputs == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (regionRegex != null ? regionRegex.hashCode() : 0);
        result = 31 * result + (regions != null ? regions.hashCode() : 0);
        result = 31 * result + (outputs != null ? outputs.hashCode() : 0);
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Partition fromNode(JsonNode node) {
        Builder b = builder();

        Map<String, JsonNode> objNode = node.asObject();

        JsonNode id = objNode.get(ID);
        if (id != null) {
            b.id(id.asString());
        }

        JsonNode regionRegex = objNode.get(REGION_REGEX);
        if (regionRegex != null) {
            b.regionRegex(regionRegex.asString());
        }

        JsonNode regions = objNode.get(REGIONS);
        if (regions != null) {
            Map<String, JsonNode> regionsObj = regions.asObject();
            regionsObj.forEach((k, v) -> b.putRegion(k, RegionOverride.fromNode(v)));
        }

        JsonNode outputs = objNode.get(OUTPUTS);
        if (outputs != null) {
            b.outputs(Outputs.fromNode(outputs));
        }

        return b.build();
    }

    public static class Builder {
        private String id;
        private String regionRegex;
        private Map<String, RegionOverride> regions = new HashMap<>();
        private Outputs outputs;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder regionRegex(String regionRegex) {
            this.regionRegex = regionRegex;
            return this;
        }

        public Builder regions(Map<String, RegionOverride> regions) {
            this.regions.clear();
            if (regions != null) {
                this.regions.putAll(regions);
            }
            return this;
        }

        public Builder putRegion(String name, RegionOverride regionOverride) {
            regions.put(name, regionOverride);
            return this;
        }

        public Builder outputs(Outputs outputs) {
            this.outputs = outputs;
            return this;
        }

        public Partition build() {
            return new Partition(this);
        }
    }
}
