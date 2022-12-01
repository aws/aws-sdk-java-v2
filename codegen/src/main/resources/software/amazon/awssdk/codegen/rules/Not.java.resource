import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class Not extends SingleArgFn {

    public static final String ID = "not";

    public Not(FnNode fnNode) {
        super(fnNode);
    }

    public static Not ofExpr(Expr expr) {
        return new Not(FnNode.ofExprs(ID, expr));
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitNot(this);
    }

    public static Not ofExprs(Expr expr) {
        return new Not(FnNode.ofExprs(ID, expr));
    }

    @Override
    protected Value evalArg(Value arg) {
        return Value.fromBool(!arg.expectBool());
    }

    public Expr target() {
        return expectOneArg();
    }
}
