import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public class BooleanEqualsFn extends Fn {
    public static final String ID = "booleanEquals";

    public BooleanEqualsFn(FnNode fnNode) {
        super(fnNode);
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitBoolEquals(this);
    }

    public static BooleanEqualsFn ofExprs(Expr left, Expr right) {
        return new BooleanEqualsFn(FnNode.ofExprs(ID, left, right));
    }

    public Expr getLeft() {
        return expectTwoArgs().left();
    }

    public Expr getRight() {
        return expectTwoArgs().right();
    }

    @Override
    public Value eval(Scope<Value> scope) {
        Pair<Expr, Expr> args = expectTwoArgs();
        return RuleError.ctx("while evaluating booleanEquals",
                   () -> Value.fromBool(args.left().eval(scope).expectBool() == args.right().eval(scope).expectBool()));
    }

    public static BooleanEqualsFn fromParam(Parameter param, Expr value) {
        return ofExprs(param.expr(), value);
    }
}
