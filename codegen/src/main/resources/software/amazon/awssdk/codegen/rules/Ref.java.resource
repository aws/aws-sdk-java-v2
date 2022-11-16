import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A reference to a field.
 */
@SdkInternalApi
public class Ref extends Expr {
    private final Identifier name;

    public Ref(Identifier name) {
        this.name = name;
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitRef(this);
    }

    public Identifier getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Ref ref = (Ref) o;
        return name.equals(ref.name);
    }

    @Override
    public String template() {
        return String.format("{%s}", name);
    }

    @Override
    public String toString() {
        return name.asString();
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public Value eval(Scope<Value> scope) {
        return scope.getValue(this.name).orElse(new Value.None());
    }
}