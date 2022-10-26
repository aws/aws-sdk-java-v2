import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class IsValidHostLabel extends VarargFn {
    public static final String ID = "isValidHostLabel";

    public IsValidHostLabel(FnNode fnNode) {
        super(fnNode);
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitIsValidHostLabel(this);
    }

    public static IsValidHostLabel ofExprs(Expr expr, boolean allowDots) {
        return new IsValidHostLabel(FnNode.ofExprs(ID, expr, Expr.of(allowDots)));
    }

    public Expr hostLabel() {
        return expectTwoArgs().left();
    }

    public Expr allowDots() {
        return expectTwoArgs().right();
    }

    @Override
    public Value eval(Scope<Value> scope) {
        String hostLabel = expectTwoArgs().left().eval(scope).expectString();
        // TODO: use compiled Pattern
        if (allowDots(scope)) {
            return Value.fromBool(hostLabel.matches("[a-zA-Z\\d][a-zA-Z\\d\\-.]{0,62}"));
        } else {
            return Value.fromBool(hostLabel.matches("[a-zA-Z\\d][a-zA-Z\\d\\-]{0,62}"));
        }
    }

    private boolean allowDots(Scope<Value> scope) {
        return allowDots().eval(scope).expectBool();
    }
}
