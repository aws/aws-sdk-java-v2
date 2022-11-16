import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface Eval {
    Value eval(Scope<Value> scope);
}