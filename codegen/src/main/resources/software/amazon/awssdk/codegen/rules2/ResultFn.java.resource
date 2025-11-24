import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
@FunctionalInterface
public interface ResultFn {
    RuleResult apply(Object[] registers);
}
