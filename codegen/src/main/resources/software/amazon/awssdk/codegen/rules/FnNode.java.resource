import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

/**
 * Parsed but not validated function contents containing the `fn` name and `argv`
 */
@SdkInternalApi
public final class FnNode {
    private static final String ARGV = "argv";
    private static final String FN = "fn";

    private final String fn;
    private final List<Expr> argv;

    private FnNode(Builder builder) {
        this.fn = builder.fn;
        this.argv = builder.argv;
    }

    public static FnNode ofExprs(String fn, Expr... expr) {
        return builder()
            .fn(fn)
            .argv(Arrays.stream(expr).collect(Collectors.toList()))
            .build();
    }

    public Fn validate() {
        switch (fn) {
            case BooleanEqualsFn.ID:
                return new BooleanEqualsFn(this);
            case PartitionFn.ID:
                return new PartitionFn(this);
            case StringEqualsFn.ID:
                return new StringEqualsFn(this);
            case IsSet.ID:
                return new IsSet(this);
            case IsValidHostLabel.ID:
                return new IsValidHostLabel(this);
            case GetAttr.ID:
                return new GetAttr(this);
            case ParseArn.ID:
                return new ParseArn(this);
            case Not.ID:
                return new Not(this);
            case ParseUrl.ID:
                return new ParseUrl(this);
            case Substring.ID:
                return new Substring(this);
            case UriEncodeFn.ID:
                return new UriEncodeFn(this);
            case IsVirtualHostableS3Bucket.ID:
                return new IsVirtualHostableS3Bucket(this);
            default:
                throw RuleError.builder()
                               .cause(SourceException.builder()
                                                     .message(String.format("`%s` is not a valid function", fn))
                                                     .build())
                               .build();
        }
    }

    public String getId() {
        return fn;
    }

    public List<Expr> getArgv() {
        return argv;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FnNode fnNode = (FnNode) o;

        if (fn != null ? !fn.equals(fnNode.fn) : fnNode.fn != null) {
            return false;
        }
        return argv != null ? argv.equals(fnNode.argv) : fnNode.argv == null;
    }

    @Override
    public int hashCode() {
        int result = fn != null ? fn.hashCode() : 0;
        result = 31 * result + (argv != null ? argv.hashCode() : 0);
        return result;
    }

    public static FnNode fromNode(JsonNode node) {
        Map<String, JsonNode> objNode = node.asObject();

        return builder()
            .fn(objNode.get(FN).asString())
            .argv(objNode.get(ARGV).asArray()
                      .stream()
                      .map(Expr::fromNode)
                      .collect(Collectors.toList()))
            .build();
    }

    public static class Builder {
        private String fn;
        private List<Expr> argv;

        public Builder() {
        }

        public Builder argv(List<Expr> argv) {
            this.argv = argv;
            return this;
        }

        public Builder fn(String fn) {
            this.fn = fn;
            return this;
        }

        public FnNode build() {
            return new FnNode(this);
        }
    }

}
