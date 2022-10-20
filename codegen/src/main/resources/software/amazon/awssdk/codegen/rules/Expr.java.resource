import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public abstract class Expr implements Eval {

    public abstract <R> R accept(ExprVisitor<R> visitor);

    public GetAttr getAttr(String path) {
        return GetAttr.builder().target(this).path(path).build();
    }

    public GetAttr getAttr(Identifier path) {
        return GetAttr.builder().target(this).path(path.asString()).build();
    }

    public static Expr fromNode(JsonNode node) {
        if (node.isObject()) {
            Map<String, JsonNode> objNode = node.asObject();
            JsonNode ref = objNode.get("ref");
            JsonNode fn = objNode.get("fn");
            if ((ref != null ? 1 : 0) + (fn != null ? 1 : 0) != 1) {
                throw SourceException.builder().message("expected exactly one of `ref` or `fn` to be set").build();
            }
            if (ref != null) {
                return ref(Identifier.of(ref.asString()));
            }
            return RuleError.ctx("while parsing fn", () -> FnNode.fromNode(node).validate());
        } else if (node.isString()) {
            return Literal.fromStr(node.asString());
        } else {
            return Literal.fromNode(node);
        }
    }

    /**
     * Parse a value from a "short form" used within a template
     *
     * @param shortForm
     * @return
     */
    public static Expr parseShortform(String shortForm) {
        return RuleError.ctx("while parsing `" + shortForm + "` within a template", () -> {
            if (shortForm.contains("#")) {
                String[] parts = shortForm.split("#", 2);
                String base = parts[0];
                String pattern = parts[1];
                return GetAttr.builder()
                              .target(ref(Identifier.of(base)))
                              .path(pattern).build();
            } else {
                return ref(Identifier.of(shortForm));
            }
        });
    }

    public String template() {
        throw new RuntimeException(String.format("cannot convert %s to a string template", this));
    }

    public static Ref ref(Identifier name) {
        return new Ref(name);
    }

    public static Expr of(boolean value) {
        return Literal.fromBool(value);
    }

    public static Expr of(int value) {
        return Literal.fromInteger(value);
    }

    public static Expr of(String value) {
        return Literal.fromStr(value);
    }

}
