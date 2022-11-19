import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface Into<T> {
    T into();
}
