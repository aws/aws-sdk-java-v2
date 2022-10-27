import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface FnVisitor<R> {
    R visitPartition(PartitionFn fn);

    R visitParseArn(ParseArn fn);

    R visitIsValidHostLabel(IsValidHostLabel fn);

    R visitBoolEquals(BooleanEqualsFn fn);

    R visitStringEquals(StringEqualsFn fn);

    R visitIsSet(IsSet fn);

    R visitNot(Not not);

    R visitGetAttr(GetAttr getAttr);

    R visitParseUrl(ParseUrl parseUrl);

    R visitSubstring(Substring substring);

    R visitUriEncode(UriEncodeFn fn);

    R visitIsVirtualHostLabelsS3Bucket(IsVirtualHostableS3Bucket fn);
}