import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface IntoSelf<T extends IntoSelf<T>> extends Into<T> {
    @Override
    default T into() {
        return (T) this;
    }
}