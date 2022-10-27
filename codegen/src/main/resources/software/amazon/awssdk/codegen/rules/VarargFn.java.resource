import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
abstract class VarargFn extends Fn {

    VarargFn(FnNode fnNode) {
        super(fnNode);
    }

    public abstract Value eval(Scope<Value> scope);

    protected List<Expr> args() {
        return this.fnNode.getArgv();
    }
}
