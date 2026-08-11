import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public class RulePartition {
    private static final String DNS_SUFFIX = "dnsSuffix";
    private static final String DUAL_STACK_DNS_SUFFIX = "dualStackDnsSuffix";
    private static final String SUPPORTS_FIPS = "supportsFIPS";
    private static final String SUPPORTS_DUAL_STACK = "supportsDualStack";
    private static final String IMPLICIT_GLOBAL_REGION = "implicitGlobalRegion";

    private final String name;
    private final String dnsSuffix;
    private final String dualStackDnsSuffix;
    private final boolean supportsFips;
    private final boolean supportsDualStack;
    private final String implicitGlobalRegion;

    private RulePartition(Builder builder) {
        this.name = builder.name;
        this.dnsSuffix = builder.dnsSuffix;
        this.dualStackDnsSuffix = builder.dualStackDnsSuffix;
        this.supportsFips = builder.supportsFips;
        this.supportsDualStack = builder.supportsDualStack;
        this.implicitGlobalRegion = builder.implicitGlobalRegion;
    }

    public String name() {
        return name;
    }

    public String dnsSuffix() {
        return dnsSuffix;
    }

    public String dualStackDnsSuffix() {
        return dualStackDnsSuffix;
    }

    public boolean supportsFips() {
        return supportsFips;
    }

    public boolean supportsFIPS() {
        return supportsFips;
    }

    public boolean supportsDualStack() {
        return supportsDualStack;
    }

    public String implicitGlobalRegion() {
        return implicitGlobalRegion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RulePartition that = (RulePartition) o;
        return supportsFips == that.supportsFips && supportsDualStack == that.supportsDualStack
                && Objects.equals(name, that.name) && Objects.equals(dnsSuffix, that.dnsSuffix)
                && Objects.equals(dualStackDnsSuffix, that.dualStackDnsSuffix)
                && Objects.equals(implicitGlobalRegion, that.implicitGlobalRegion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dnsSuffix, dualStackDnsSuffix, supportsFips, supportsDualStack, implicitGlobalRegion);
    }

    public static Builder builder() {
        return new Builder();
    }

    // This translation can go away when we can get rid of the legacy endpoints rule evaluation logic.
    static RulePartition from(String name, Outputs outputs) {
        return RulePartition.builder()
                            .name(name)
                            .dnsSuffix(outputs.dnsSuffix())
                            .dualStackDnsSuffix(outputs.dualStackDnsSuffix())
                            .supportsFips(outputs.supportsFips())
                            .supportsDualStack(outputs.supportsDualStack())
                            .implicitGlobalRegion(outputs.implicitGlobalRegion())
                            .build();
    }

    public static RulePartition fromNode(String name, JsonNode node) {
        Map<String, JsonNode> objNode = node.asObject();
        Builder b = builder();

        b.name(name);
        JsonNode dnsSuffix = objNode.get(DNS_SUFFIX);
        if (dnsSuffix != null) {
            b.dnsSuffix(dnsSuffix.asString());
        }

        JsonNode dualStackDnsSuffix = objNode.get(DUAL_STACK_DNS_SUFFIX);
        if (dualStackDnsSuffix != null) {
            b.dualStackDnsSuffix(dualStackDnsSuffix.asString());
        }

        JsonNode supportsFips = objNode.get(SUPPORTS_FIPS);
        if (supportsFips != null) {
            b.supportsFips(supportsFips.asBoolean());
        }

        JsonNode supportsDualStack = objNode.get(SUPPORTS_DUAL_STACK);
        if (supportsDualStack != null) {
            b.supportsDualStack(supportsDualStack.asBoolean());
        }

        JsonNode implicitGlobalRegion = objNode.get(IMPLICIT_GLOBAL_REGION);
        if (implicitGlobalRegion != null) {
            b.implicitGlobalRegion(implicitGlobalRegion.asString());
        }

        return b.build();
    }

    public static class Builder {
        private String name;
        private String dnsSuffix;
        private String dualStackDnsSuffix;
        private boolean supportsFips;
        private boolean supportsDualStack;
        private String implicitGlobalRegion;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder dnsSuffix(String dnsSuffix) {
            this.dnsSuffix = dnsSuffix;
            return this;
        }

        public Builder dualStackDnsSuffix(String dualStackDnsSuffix) {
            this.dualStackDnsSuffix = dualStackDnsSuffix;
            return this;
        }

        public Builder supportsFips(boolean supportsFips) {
            this.supportsFips = supportsFips;
            return this;
        }

        public Builder supportsDualStack(boolean supportsDualStack) {
            this.supportsDualStack = supportsDualStack;
            return this;
        }

        public Builder implicitGlobalRegion(String implicitGlobalRegion) {
            this.implicitGlobalRegion = implicitGlobalRegion;
            return this;
        }

        public RulePartition build() {
            return new RulePartition(this);
        }
    }
}
