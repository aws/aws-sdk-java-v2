import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public abstract class SingleArgFn extends Fn {

    public SingleArgFn(FnNode fnNode) {
        super(fnNode);
    }

    public Expr target() {
        return expectOneArg();
    }

    @Override
    public Value eval(Scope<Value> scope) {
        return evalArg(expectOneArg().eval(scope));
    }

    protected abstract Value evalArg(Value arg);
}
