package software.amazon.awssdk;

public class TestRequest extends SdkRequest {
    private TestRequest(Builder b) {
        super(b);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder extends SdkRequest.BuilderImpl {

        private Builder() { super(Builder.class); }
        private Builder(TestRequest request) { super(Builder.class, request); }

        @Override
        public TestRequest build() {
            return new TestRequest(this);
        }
    }
}
