import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class IsSet extends SingleArgFn {
    public static final String ID = "isSet";

    public IsSet(FnNode fnNode) {
        super(fnNode);
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitIsSet(this);
    }

    public static IsSet ofExpr(Expr expr) {
        return new IsSet(FnNode.ofExprs(ID, expr));
    }

    @Override
    protected Value evalArg(Value arg) {
        return Value.fromBool(!arg.isNone());
    }

}
