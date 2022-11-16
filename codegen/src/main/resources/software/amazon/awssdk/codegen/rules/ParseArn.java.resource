import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.MapUtils;

@SdkInternalApi
public class ParseArn extends SingleArgFn {
    public static final String ID = "aws.parseArn";
    public static final Identifier PARTITION = Identifier.of("partition");
    public static final Identifier SERVICE = Identifier.of("service");
    public static final Identifier REGION = Identifier.of("region");
    public static final Identifier ACCOUNT_ID = Identifier.of("accountId");
    private static final Identifier RESOURCE_ID = Identifier.of("resourceId");

    public ParseArn(FnNode fnNode) {
        super(fnNode);
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitParseArn(this);
    }

    public static ParseArn ofExprs(Expr expr) {
        return new ParseArn(FnNode.ofExprs(ID, expr));
    }

    @Override
    protected Value evalArg(Value arg) {
        String value = arg.expectString();
        Optional<Arn> arnOpt = Arn.parse(value);
        return arnOpt.map(arn ->
                (Value) Value.fromRecord(MapUtils.of(
                        PARTITION, Value.fromStr(arn.partition()),
                        SERVICE, Value.fromStr(arn.service()),
                        REGION, Value.fromStr(arn.region()),
                        ACCOUNT_ID, Value.fromStr(arn.accountId()),
                        RESOURCE_ID, Value.fromArray(arn.resource().stream()
                                .map(v -> (Value) Value.fromStr(v))
                                .collect(Collectors.toList()))
                ))
        ).orElse(new Value.None());
    }
}
