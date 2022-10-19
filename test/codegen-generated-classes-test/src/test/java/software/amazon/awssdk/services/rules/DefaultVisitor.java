package software.amazon.awssdk.services.rules;

import java.util.List;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.BooleanEqualsFn;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.EndpointResult;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.Expr;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.ExprVisitor;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.Fn;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.FnVisitor;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.GetAttr;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.IsSet;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.IsValidHostLabel;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.IsVirtualHostableS3Bucket;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.Literal;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.Not;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.ParseArn;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.ParseUrl;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.PartitionFn;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.Ref;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.Rule;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.RuleValueVisitor;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.StringEqualsFn;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.Substring;
import software.amazon.awssdk.services.protocolrestjson.endpoints.internal.UriEncodeFn;

public abstract class DefaultVisitor<R> implements RuleValueVisitor<R>, ExprVisitor<R>, FnVisitor<R> {
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

    @Override
    public R visitPartition(PartitionFn fn) {
        return getDefault();
    }

    @Override
    public R visitParseArn(ParseArn fn) {
        return getDefault();
    }


    @Override
    public R visitIsValidHostLabel(IsValidHostLabel fn) {
        return getDefault();
    }

    @Override
    public R visitBoolEquals(BooleanEqualsFn fn) {
        return getDefault();
    }

    @Override
    public R visitStringEquals(StringEqualsFn fn) {
        return getDefault();
    }

    @Override
    public R visitIsSet(IsSet fn) {
        return getDefault();
    }

    @Override
    public R visitNot(Not not) {
        return getDefault();
    }

    @Override
    public R visitGetAttr(GetAttr getAttr) {
        return getDefault();
    }

    @Override
    public R visitParseUrl(ParseUrl parseUrl) {
        return getDefault();
    }

    @Override
    public R visitSubstring(Substring substring) { return getDefault(); }

    @Override
    public R visitTreeRule(List<Rule> rules) {
        return getDefault();
    }

    @Override
    public R visitErrorRule(Expr error) {
        return getDefault();
    }

    @Override
    public R visitEndpointRule(EndpointResult endpoint) {
        return getDefault();
    }

    @Override
    public R visitUriEncode(UriEncodeFn fn) {
        return getDefault();
    }

    @Override
    public R visitIsVirtualHostLabelsS3Bucket(IsVirtualHostableS3Bucket fn) {
        return getDefault();
    }
}
