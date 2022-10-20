import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public class RegionOverride {

    private RegionOverride(Builder builder) {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RegionOverride fromNode(JsonNode node) {
        Builder b = new Builder();
        return b.build();
    }

    @Override
    public int hashCode() {
        return 7;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RegionOverride;
    }

    public static class Builder {
        public RegionOverride build() {
            return new RegionOverride(this);
        }
    }
}
