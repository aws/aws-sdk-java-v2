import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface ExprVisitor<R> {

    R visitLiteral(Literal literal);

    R visitRef(Ref ref);

    R visitFn(Fn fn);

    abstract class Default<R> implements ExprVisitor<R> {

        public abstract R getDefault();

        @Override
        public R visitLiteral(Literal literal) {
            return getDefault();
        }

        @Override
        public R visitRef(Ref ref) {
            return getDefault();
        }

        @Override
        public R visitFn(Fn fn) {
            return getDefault();
        }
    }
}
