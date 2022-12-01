import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public class Outputs {
    private static final String DNS_SUFFIX = "dnsSuffix";
    private static final String DUAL_STACK_DNS_SUFFIX = "dualStackDnsSuffix";
    private static final String SUPPORTS_FIPS = "supportsFIPS";
    private static final String SUPPORTS_DUAL_STACK = "supportsDualStack";

    private final String dnsSuffix;
    private final String dualStackDnsSuffix;
    private final boolean supportsFips;
    private final boolean supportsDualStack;

    private Outputs(Builder builder) {
        this.dnsSuffix = builder.dnsSuffix;
        this.dualStackDnsSuffix = builder.dualStackDnsSuffix;
        this.supportsFips = builder.supportsFips;
        this.supportsDualStack = builder.supportsDualStack;
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

    public boolean supportsDualStack() {
        return supportsDualStack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Outputs outputs = (Outputs) o;

        if (supportsFips != outputs.supportsFips) {
            return false;
        }
        if (supportsDualStack != outputs.supportsDualStack) {
            return false;
        }
        if (dnsSuffix != null ? !dnsSuffix.equals(outputs.dnsSuffix) : outputs.dnsSuffix != null) {
            return false;
        }
        return dualStackDnsSuffix != null ? dualStackDnsSuffix.equals(outputs.dualStackDnsSuffix)
                                          : outputs.dualStackDnsSuffix == null;
    }

    @Override
    public int hashCode() {
        int result = dnsSuffix != null ? dnsSuffix.hashCode() : 0;
        result = 31 * result + (dualStackDnsSuffix != null ? dualStackDnsSuffix.hashCode() : 0);
        result = 31 * result + (supportsFips ? 1 : 0);
        result = 31 * result + (supportsDualStack ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Outputs{" +
               "dnsSuffix='" + dnsSuffix + '\'' +
               ", dualStackDnsSuffix='" + dualStackDnsSuffix + '\'' +
               ", supportsFips=" + supportsFips +
               ", supportsDualStack=" + supportsDualStack +
               '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Outputs fromNode(JsonNode node) {
        Map<String, JsonNode> objNode = node.asObject();

        Builder b = builder();

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

        return b.build();
    }

    public static class Builder {
        private String dnsSuffix;
        private String dualStackDnsSuffix;
        private boolean supportsFips;
        private boolean supportsDualStack;

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

        public Outputs build() {
            return new Outputs(this);
        }
    }
}
