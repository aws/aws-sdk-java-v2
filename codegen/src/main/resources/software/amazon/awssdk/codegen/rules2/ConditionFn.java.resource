import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
@FunctionalInterface
public interface ConditionFn {
    boolean test(Object[] registers);
}
