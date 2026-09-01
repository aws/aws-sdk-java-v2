package software.amazon.awssdk.services.query.endpoints.internal;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointUrl;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.authscheme.DynamicEndpointAuthSchemeFactory;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryEndpointProvider implements QueryEndpointProvider {
  private volatile CacheEntry cache;

  @Override
  public CompletableFuture<Endpoint> resolveEndpoint(QueryEndpointParams endpointParams) {
    // Single-entry result cache: reuse the last endpoint when the params still match.
    CacheEntry cached = this.cache;
    if (cached != null && cacheParamsMatch(endpointParams, cached.params)) {
      return CompletableFuture.completedFuture(cached.endpoint);
    }
    try {
      Evaluator evaluator = new Evaluator();
      evaluator.params = endpointParams;
      evaluator.region = endpointParams.region() == null ? null : endpointParams.region().id();
      Endpoint result = evaluator.nodeP1();
      if (result == null) {
        return CompletableFutureUtils.failedFuture(SdkClientException.create("Rule engine did not reach an error or endpoint result"));
      }
      this.cache = new CacheEntry(endpointParams, result);
      return CompletableFuture.completedFuture(result);
    } catch (SdkClientException e) {
      String errorMsg = e.getMessage();
      if (errorMsg != null && errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
        return CompletableFutureUtils.failedFuture(SdkClientException.create(errorMsg + ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.", e));
      }
      return CompletableFutureUtils.failedFuture(e);
    } catch (Exception error) {
      return CompletableFutureUtils.failedFuture(error);
    }
  }

  private static boolean cacheParamsMatch(QueryEndpointParams a, QueryEndpointParams b) {
    return Objects.equals(a.useFips(), b.useFips())
            && Objects.equals(a.useDualStack(), b.useDualStack())
            && Objects.equals(a.forcePathStyle(), b.forcePathStyle())
            && Objects.equals(a.accelerate(), b.accelerate())
            && Objects.equals(a.useGlobalEndpoint(), b.useGlobalEndpoint())
            && Objects.equals(a.useObjectLambdaEndpoint(), b.useObjectLambdaEndpoint())
            && Objects.equals(a.disableAccessPoints(), b.disableAccessPoints())
            && Objects.equals(a.disableMultiRegionAccessPoints(), b.disableMultiRegionAccessPoints())
            && Objects.equals(a.useArnRegion(), b.useArnRegion())
            && Objects.equals(a.useS3ExpressControlEndpoint(), b.useS3ExpressControlEndpoint())
            && Objects.equals(a.disableS3ExpressSessionAuth(), b.disableS3ExpressSessionAuth())
            && Objects.equals(a.region(), b.region())
            && Objects.equals(a.bucket(), b.bucket())
            && Objects.equals(a.endpoint(), b.endpoint());
  }

  private static final class Evaluator {
    QueryEndpointParams params;

    String region;

    RulePartition partitionResult;

    String accessPointSuffix;

    String regionPrefix;

    String outpostId_ssa_2;

    String hardwareType;

    String _s3e_ds;

    String _s3e_fips;

    String _s3e_auth;

    RuleUrl url;

    RuleArn bucketArn;

    String s3expressAvailabilityZoneId;

    String uri_encoded_bucket;

    String arnType;

    String accessPointName_ssa_1;

    RulePartition bucketPartition;

    String outpostId_ssa_1;

    String outpostType;

    String accessPointName_ssa_2;

    private Endpoint nodeP0() {
      return null;
    }

    private Endpoint nodeP1() {
      return region != null
              ? nodeP2()
              : result114();
    }

    private Endpoint nodeP2() {
      return Boolean.TRUE.equals(params.accelerate())
              ? nodeP423()
              : nodeP3();
    }

    private Endpoint nodeP3() {
      return Boolean.TRUE.equals(params.useFips())
              ? nodeP271()
              : nodeP4();
    }

    private Endpoint nodeP4() {
      return Boolean.TRUE.equals(params.useDualStack())
              ? nodeP232()
              : nodeP5();
    }

    private Endpoint nodeP5() {
      return params.endpoint() != null
              ? nodeP84()
              : nodeP6();
    }

    private Endpoint nodeP6() {
      return params.bucket() != null
              ? nodeP14()
              : nodeP7();
    }

    private Endpoint nodeP7() {
      return cond8()
              ? nodeP8()
              : result114();
    }

    private Endpoint nodeP8() {
      return cond16()
              ? nodeP9()
              : nodeP12();
    }

    private Endpoint nodeP9() {
      return cond18()
              ? nodeP10()
              : nodeP12();
    }

    private Endpoint nodeP10() {
      return cond19()
              ? nodeP11()
              : nodeP12();
    }

    private Endpoint nodeP11() {
      return cond22()
              ? result13()
              : nodeP12();
    }

    private Endpoint nodeP12() {
      return cond35()
              ? nodeP13()
              : result41();
    }

    private Endpoint nodeP13() {
      return cond36()
              ? result102()
              : nodeP434();
    }

    private Endpoint nodeP14() {
      return cond6()
              ? nodeP270()
              : nodeP15();
    }

    private Endpoint nodeP15() {
      return cond7()
              ? nodeP269()
              : nodeP16();
    }

    private Endpoint nodeP16() {
      return cond8()
              ? nodeP18()
              : nodeP17();
    }

    private Endpoint nodeP17() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP500()
              : nodeP105();
    }

    private Endpoint nodeP18() {
      return cond9()
              ? nodeP19()
              : nodeP23();
    }

    private Endpoint nodeP19() {
      return accessPointSuffix != null && accessPointSuffix.equals("--op-s3")
              ? nodeP20()
              : nodeP23();
    }

    private Endpoint nodeP20() {
      return cond11()
              ? nodeP21()
              : nodeP23();
    }

    private Endpoint nodeP21() {
      return cond12()
              ? nodeP22()
              : nodeP23();
    }

    private Endpoint nodeP22() {
      return cond13()
              ? nodeP546()
              : nodeP23();
    }

    private Endpoint nodeP23() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP76()
              : nodeP24();
    }

    private Endpoint nodeP24() {
      return cond20()
              ? nodeP72()
              : nodeP25();
    }

    private Endpoint nodeP25() {
      return cond26()
              ? nodeP26()
              : nodeP77();
    }

    private Endpoint nodeP26() {
      return cond37()
              ? nodeP27()
              : result85();
    }

    private Endpoint nodeP27() {
      return arnType != null && arnType.equals("")
              ? result85()
              : nodeP28();
    }

    private Endpoint nodeP28() {
      return arnType != null && arnType.equals("accesspoint")
              ? nodeP46()
              : nodeP29();
    }

    private Endpoint nodeP29() {
      return cond48()
              ? result57()
              : nodeP30();
    }

    private Endpoint nodeP30() {
      return cond50()
              ? nodeP31()
              : result84();
    }

    private Endpoint nodeP31() {
      return cond51()
              ? nodeP32()
              : nodeP135();
    }

    private Endpoint nodeP32() {
      return cond55()
              ? result75()
              : nodeP33();
    }

    private Endpoint nodeP33() {
      return cond59()
              ? nodeP34()
              : result83();
    }

    private Endpoint nodeP34() {
      return cond60()
              ? nodeP38()
              : nodeP35();
    }

    private Endpoint nodeP35() {
      return cond61()
              ? nodeP36()
              : result82();
    }

    private Endpoint nodeP36() {
      return cond62()
              ? nodeP37()
              : nodeP145();
    }

    private Endpoint nodeP37() {
      return cond63()
              ? nodeP40()
              : result45();
    }

    private Endpoint nodeP38() {
      return cond61()
              ? nodeP39()
              : result82();
    }

    private Endpoint nodeP39() {
      return cond62()
              ? nodeP40()
              : nodeP149();
    }

    private Endpoint nodeP40() {
      return cond64()
              ? nodeP41()
              : result53();
    }

    private Endpoint nodeP41() {
      return cond66()
              ? nodeP42()
              : result52();
    }

    private Endpoint nodeP42() {
      return cond70()
              ? nodeP43()
              : result51();
    }

    private Endpoint nodeP43() {
      return cond71()
              ? nodeP44()
              : result80();
    }

    private Endpoint nodeP44() {
      return outpostType != null && outpostType.equals("accesspoint")
              ? nodeP45()
              : result79();
    }

    private Endpoint nodeP45() {
      return cond74()
              ? result77()
              : result78();
    }

    private Endpoint nodeP46() {
      return cond40()
              ? nodeP47()
              : result56();
    }

    private Endpoint nodeP47() {
      return accessPointName_ssa_1 != null && accessPointName_ssa_1.equals("")
              ? result56()
              : nodeP48();
    }

    private Endpoint nodeP48() {
      return cond42()
              ? nodeP184()
              : nodeP49();
    }

    private Endpoint nodeP49() {
      return cond48()
              ? nodeP61()
              : nodeP50();
    }

    private Endpoint nodeP50() {
      return cond49()
              ? result44()
              : nodeP51();
    }

    private Endpoint nodeP51() {
      return cond51()
              ? nodeP52()
              : nodeP525();
    }

    private Endpoint nodeP52() {
      return cond60()
              ? nodeP55()
              : nodeP53();
    }

    private Endpoint nodeP53() {
      return cond62()
              ? result54()
              : nodeP54();
    }

    private Endpoint nodeP54() {
      return cond63()
              ? nodeP56()
              : result45();
    }

    private Endpoint nodeP55() {
      return cond62()
              ? result54()
              : nodeP56();
    }

    private Endpoint nodeP56() {
      return cond64()
              ? nodeP57()
              : result53();
    }

    private Endpoint nodeP57() {
      return cond66()
              ? nodeP58()
              : result52();
    }

    private Endpoint nodeP58() {
      return cond69()
              ? nodeP59()
              : result64();
    }

    private Endpoint nodeP59() {
      return cond70()
              ? nodeP60()
              : result51();
    }

    private Endpoint nodeP60() {
      return cond72()
              ? result63()
              : result50();
    }

    private Endpoint nodeP61() {
      return cond49()
              ? result44()
              : nodeP62();
    }

    private Endpoint nodeP62() {
      return cond51()
              ? nodeP63()
              : nodeP525();
    }

    private Endpoint nodeP63() {
      return cond60()
              ? nodeP66()
              : nodeP64();
    }

    private Endpoint nodeP64() {
      return cond62()
              ? result54()
              : nodeP65();
    }

    private Endpoint nodeP65() {
      return cond63()
              ? nodeP67()
              : result45();
    }

    private Endpoint nodeP66() {
      return cond62()
              ? result54()
              : nodeP67();
    }

    private Endpoint nodeP67() {
      return cond64()
              ? nodeP68()
              : result53();
    }

    private Endpoint nodeP68() {
      return cond66()
              ? nodeP69()
              : result52();
    }

    private Endpoint nodeP69() {
      return cond68()
              ? result46()
              : nodeP70();
    }

    private Endpoint nodeP70() {
      return cond70()
              ? nodeP71()
              : result51();
    }

    private Endpoint nodeP71() {
      return cond72()
              ? result49()
              : result50();
    }

    private Endpoint nodeP72() {
      return cond25()
              ? nodeP73()
              : result41();
    }

    private Endpoint nodeP73() {
      return region != null && region.equals("aws-global")
              ? result38()
              : nodeP74();
    }

    private Endpoint nodeP74() {
      return Boolean.TRUE.equals(params.useGlobalEndpoint())
              ? nodeP75()
              : result40();
    }

    private Endpoint nodeP75() {
      return region != null && region.equals("us-east-1")
              ? result39()
              : result40();
    }

    private Endpoint nodeP76() {
      return cond26()
              ? result87()
              : nodeP77();
    }

    private Endpoint nodeP77() {
      return cond28()
              ? result86()
              : nodeP78();
    }

    private Endpoint nodeP78() {
      return cond34()
              ? nodeP81()
              : nodeP79();
    }

    private Endpoint nodeP79() {
      return cond35()
              ? nodeP80()
              : nodeP544();
    }

    private Endpoint nodeP80() {
      return cond36()
              ? result102()
              : result114();
    }

    private Endpoint nodeP81() {
      return region != null && region.equals("aws-global")
              ? result96()
              : nodeP82();
    }

    private Endpoint nodeP82() {
      return Boolean.TRUE.equals(params.useGlobalEndpoint())
              ? nodeP83()
              : result98();
    }

    private Endpoint nodeP83() {
      return region != null && region.equals("us-east-1")
              ? result97()
              : result98();
    }

    private Endpoint nodeP84() {
      return params.bucket() != null
              ? nodeP100()
              : nodeP85();
    }

    private Endpoint nodeP85() {
      return cond8()
              ? nodeP86()
              : result114();
    }

    private Endpoint nodeP86() {
      return cond16()
              ? nodeP87()
              : nodeP88();
    }

    private Endpoint nodeP87() {
      return cond18()
              ? nodeP90()
              : nodeP88();
    }

    private Endpoint nodeP88() {
      return cond19()
              ? nodeP89()
              : nodeP91();
    }

    private Endpoint nodeP89() {
      return cond21()
              ? nodeP96()
              : nodeP94();
    }

    private Endpoint nodeP90() {
      return cond19()
              ? nodeP92()
              : nodeP91();
    }

    private Endpoint nodeP91() {
      return cond21()
              ? nodeP97()
              : nodeP94();
    }

    private Endpoint nodeP92() {
      return cond21()
              ? nodeP96()
              : nodeP93();
    }

    private Endpoint nodeP93() {
      return cond22()
              ? result13()
              : nodeP94();
    }

    private Endpoint nodeP94() {
      return cond35()
              ? nodeP95()
              : result41();
    }

    private Endpoint nodeP95() {
      return cond36()
              ? result102()
              : result41();
    }

    private Endpoint nodeP96() {
      return cond22()
              ? result12()
              : nodeP97();
    }

    private Endpoint nodeP97() {
      return cond35()
              ? nodeP98()
              : result41();
    }

    private Endpoint nodeP98() {
      return cond36()
              ? result100()
              : nodeP99();
    }

    private Endpoint nodeP99() {
      return region != null && region.equals("aws-global")
              ? result109()
              : result110();
    }

    private Endpoint nodeP100() {
      return cond6()
              ? nodeP213()
              : nodeP101();
    }

    private Endpoint nodeP101() {
      return cond7()
              ? nodeP207()
              : nodeP102();
    }

    private Endpoint nodeP102() {
      return cond8()
              ? nodeP118()
              : nodeP103();
    }

    private Endpoint nodeP103() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP117()
              : nodeP104();
    }

    private Endpoint nodeP104() {
      return cond21()
              ? nodeP105()
              : result22();
    }

    private Endpoint nodeP105() {
      return cond26()
              ? nodeP106()
              : nodeP501();
    }

    private Endpoint nodeP106() {
      return cond37()
              ? nodeP107()
              : result85();
    }

    private Endpoint nodeP107() {
      return arnType != null && arnType.equals("")
              ? result85()
              : nodeP108();
    }

    private Endpoint nodeP108() {
      return arnType != null && arnType.equals("accesspoint")
              ? nodeP111()
              : nodeP109();
    }

    private Endpoint nodeP109() {
      return cond48()
              ? result57()
              : nodeP110();
    }

    private Endpoint nodeP110() {
      return cond50()
              ? nodeP135()
              : result84();
    }

    private Endpoint nodeP111() {
      return cond40()
              ? nodeP112()
              : result56();
    }

    private Endpoint nodeP112() {
      return accessPointName_ssa_1 != null && accessPointName_ssa_1.equals("")
              ? result56()
              : nodeP113();
    }

    private Endpoint nodeP113() {
      return cond42()
              ? nodeP114()
              : nodeP499();
    }

    private Endpoint nodeP114() {
      return cond48()
              ? result55()
              : nodeP115();
    }

    private Endpoint nodeP115() {
      return cond52()
              ? nodeP116()
              : result71();
    }

    private Endpoint nodeP116() {
      return Boolean.TRUE.equals(params.disableMultiRegionAccessPoints())
              ? result68()
              : result71();
    }

    private Endpoint nodeP117() {
      return cond21()
              ? nodeP500()
              : result22();
    }

    private Endpoint nodeP118() {
      return cond9()
              ? nodeP119()
              : nodeP123();
    }

    private Endpoint nodeP119() {
      return accessPointSuffix != null && accessPointSuffix.equals("--op-s3")
              ? nodeP120()
              : nodeP123();
    }

    private Endpoint nodeP120() {
      return cond11()
              ? nodeP121()
              : nodeP123();
    }

    private Endpoint nodeP121() {
      return cond12()
              ? nodeP122()
              : nodeP123();
    }

    private Endpoint nodeP122() {
      return cond13()
              ? nodeP201()
              : nodeP123();
    }

    private Endpoint nodeP123() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP194()
              : nodeP124();
    }

    private Endpoint nodeP124() {
      return cond20()
              ? nodeP189()
              : nodeP125();
    }

    private Endpoint nodeP125() {
      return cond21()
              ? nodeP126()
              : result22();
    }

    private Endpoint nodeP126() {
      return cond23()
              ? nodeP127()
              : nodeP128();
    }

    private Endpoint nodeP127() {
      return cond24()
              ? nodeP188()
              : nodeP128();
    }

    private Endpoint nodeP128() {
      return cond26()
              ? nodeP129()
              : nodeP196();
    }

    private Endpoint nodeP129() {
      return cond37()
              ? nodeP130()
              : result85();
    }

    private Endpoint nodeP130() {
      return arnType != null && arnType.equals("")
              ? result85()
              : nodeP131();
    }

    private Endpoint nodeP131() {
      return arnType != null && arnType.equals("accesspoint")
              ? nodeP158()
              : nodeP132();
    }

    private Endpoint nodeP132() {
      return cond48()
              ? result57()
              : nodeP133();
    }

    private Endpoint nodeP133() {
      return cond50()
              ? nodeP134()
              : result84();
    }

    private Endpoint nodeP134() {
      return cond51()
              ? nodeP140()
              : nodeP135();
    }

    private Endpoint nodeP135() {
      return cond55()
              ? result75()
              : nodeP136();
    }

    private Endpoint nodeP136() {
      return cond59()
              ? nodeP137()
              : result83();
    }

    private Endpoint nodeP137() {
      return cond60()
              ? result82()
              : nodeP138();
    }

    private Endpoint nodeP138() {
      return cond61()
              ? nodeP139()
              : result82();
    }

    private Endpoint nodeP139() {
      return cond63()
              ? result82()
              : result45();
    }

    private Endpoint nodeP140() {
      return cond55()
              ? result75()
              : nodeP141();
    }

    private Endpoint nodeP141() {
      return cond59()
              ? nodeP142()
              : result83();
    }

    private Endpoint nodeP142() {
      return cond60()
              ? nodeP147()
              : nodeP143();
    }

    private Endpoint nodeP143() {
      return cond61()
              ? nodeP144()
              : result82();
    }

    private Endpoint nodeP144() {
      return cond62()
              ? nodeP146()
              : nodeP145();
    }

    private Endpoint nodeP145() {
      return cond63()
              ? nodeP149()
              : result45();
    }

    private Endpoint nodeP146() {
      return cond63()
              ? nodeP152()
              : result45();
    }

    private Endpoint nodeP147() {
      return cond61()
              ? nodeP148()
              : result82();
    }

    private Endpoint nodeP148() {
      return cond62()
              ? nodeP152()
              : nodeP149();
    }

    private Endpoint nodeP149() {
      return cond64()
              ? nodeP150()
              : result53();
    }

    private Endpoint nodeP150() {
      return cond66()
              ? nodeP151()
              : result52();
    }

    private Endpoint nodeP151() {
      return cond70()
              ? result81()
              : result51();
    }

    private Endpoint nodeP152() {
      return cond64()
              ? nodeP153()
              : result53();
    }

    private Endpoint nodeP153() {
      return cond66()
              ? nodeP154()
              : result52();
    }

    private Endpoint nodeP154() {
      return cond70()
              ? nodeP155()
              : result51();
    }

    private Endpoint nodeP155() {
      return cond71()
              ? nodeP156()
              : result80();
    }

    private Endpoint nodeP156() {
      return outpostType != null && outpostType.equals("accesspoint")
              ? nodeP157()
              : result79();
    }

    private Endpoint nodeP157() {
      return cond74()
              ? result76()
              : result78();
    }

    private Endpoint nodeP158() {
      return cond40()
              ? nodeP159()
              : result56();
    }

    private Endpoint nodeP159() {
      return accessPointName_ssa_1 != null && accessPointName_ssa_1.equals("")
              ? result56()
              : nodeP160();
    }

    private Endpoint nodeP160() {
      return cond42()
              ? nodeP184()
              : nodeP161();
    }

    private Endpoint nodeP161() {
      return cond48()
              ? nodeP173()
              : nodeP162();
    }

    private Endpoint nodeP162() {
      return cond49()
              ? result44()
              : nodeP163();
    }

    private Endpoint nodeP163() {
      return cond51()
              ? nodeP164()
              : nodeP525();
    }

    private Endpoint nodeP164() {
      return cond60()
              ? nodeP167()
              : nodeP165();
    }

    private Endpoint nodeP165() {
      return cond62()
              ? result54()
              : nodeP166();
    }

    private Endpoint nodeP166() {
      return cond63()
              ? nodeP168()
              : result45();
    }

    private Endpoint nodeP167() {
      return cond62()
              ? result54()
              : nodeP168();
    }

    private Endpoint nodeP168() {
      return cond64()
              ? nodeP169()
              : result53();
    }

    private Endpoint nodeP169() {
      return cond66()
              ? nodeP170()
              : result52();
    }

    private Endpoint nodeP170() {
      return cond69()
              ? nodeP171()
              : result64();
    }

    private Endpoint nodeP171() {
      return cond70()
              ? nodeP172()
              : result51();
    }

    private Endpoint nodeP172() {
      return cond72()
              ? result62()
              : result50();
    }

    private Endpoint nodeP173() {
      return cond49()
              ? result44()
              : nodeP174();
    }

    private Endpoint nodeP174() {
      return cond51()
              ? nodeP175()
              : nodeP525();
    }

    private Endpoint nodeP175() {
      return cond60()
              ? nodeP178()
              : nodeP176();
    }

    private Endpoint nodeP176() {
      return cond62()
              ? result54()
              : nodeP177();
    }

    private Endpoint nodeP177() {
      return cond63()
              ? nodeP179()
              : result45();
    }

    private Endpoint nodeP178() {
      return cond62()
              ? result54()
              : nodeP179();
    }

    private Endpoint nodeP179() {
      return cond64()
              ? nodeP180()
              : result53();
    }

    private Endpoint nodeP180() {
      return cond66()
              ? nodeP181()
              : result52();
    }

    private Endpoint nodeP181() {
      return cond68()
              ? result46()
              : nodeP182();
    }

    private Endpoint nodeP182() {
      return cond70()
              ? nodeP183()
              : result51();
    }

    private Endpoint nodeP183() {
      return cond72()
              ? result47()
              : result50();
    }

    private Endpoint nodeP184() {
      return cond48()
              ? result55()
              : nodeP185();
    }

    private Endpoint nodeP185() {
      return cond52()
              ? nodeP186()
              : result71();
    }

    private Endpoint nodeP186() {
      return Boolean.TRUE.equals(params.disableMultiRegionAccessPoints())
              ? result68()
              : nodeP187();
    }

    private Endpoint nodeP187() {
      return cond67()
              ? result69()
              : result70();
    }

    private Endpoint nodeP188() {
      return cond25()
              ? result35()
              : result41();
    }

    private Endpoint nodeP189() {
      return cond21()
              ? nodeP190()
              : result22();
    }

    private Endpoint nodeP190() {
      return cond25()
              ? nodeP191()
              : result41();
    }

    private Endpoint nodeP191() {
      return cond30()
              ? nodeP193()
              : nodeP192();
    }

    private Endpoint nodeP192() {
      return region != null && region.equals("aws-global")
              ? result33()
              : result35();
    }

    private Endpoint nodeP193() {
      return region != null && region.equals("aws-global")
              ? result32()
              : result34();
    }

    private Endpoint nodeP194() {
      return cond21()
              ? nodeP195()
              : result22();
    }

    private Endpoint nodeP195() {
      return cond26()
              ? result87()
              : nodeP196();
    }

    private Endpoint nodeP196() {
      return cond28()
              ? result86()
              : nodeP197();
    }

    private Endpoint nodeP197() {
      return cond34()
              ? nodeP200()
              : nodeP198();
    }

    private Endpoint nodeP198() {
      return cond35()
              ? nodeP199()
              : nodeP544();
    }

    private Endpoint nodeP199() {
      return cond36()
              ? result100()
              : result114();
    }

    private Endpoint nodeP200() {
      return region != null && region.equals("aws-global")
              ? result94()
              : result95();
    }

    private Endpoint nodeP201() {
      return cond17()
              ? nodeP202()
              : result21();
    }

    private Endpoint nodeP202() {
      return cond20()
              ? nodeP203()
              : result20();
    }

    private Endpoint nodeP203() {
      return cond21()
              ? nodeP204()
              : nodeP549();
    }

    private Endpoint nodeP204() {
      return regionPrefix != null && regionPrefix.equals("beta")
              ? nodeP205()
              : nodeP549();
    }

    private Endpoint nodeP205() {
      return hardwareType != null && hardwareType.equals("e")
              ? result15()
              : nodeP206();
    }

    private Endpoint nodeP206() {
      return hardwareType != null && hardwareType.equals("o")
              ? result17()
              : result19();
    }

    private Endpoint nodeP207() {
      return cond8()
              ? nodeP208()
              : nodeP214();
    }

    private Endpoint nodeP208() {
      return cond16()
              ? nodeP209()
              : nodeP219();
    }

    private Endpoint nodeP209() {
      return cond18()
              ? nodeP210()
              : nodeP219();
    }

    private Endpoint nodeP210() {
      return cond19()
              ? nodeP211()
              : nodeP223();
    }

    private Endpoint nodeP211() {
      return cond20()
              ? nodeP212()
              : nodeP226();
    }

    private Endpoint nodeP212() {
      return cond21()
              ? nodeP230()
              : nodeP400();
    }

    private Endpoint nodeP213() {
      return cond8()
              ? nodeP217()
              : nodeP214();
    }

    private Endpoint nodeP214() {
      return cond19()
              ? nodeP215()
              : result8();
    }

    private Endpoint nodeP215() {
      return cond20()
              ? nodeP216()
              : nodeP226();
    }

    private Endpoint nodeP216() {
      return cond21()
              ? nodeP230()
              : result8();
    }

    private Endpoint nodeP217() {
      return cond16()
              ? nodeP218()
              : nodeP219();
    }

    private Endpoint nodeP218() {
      return cond18()
              ? nodeP222()
              : nodeP219();
    }

    private Endpoint nodeP219() {
      return cond19()
              ? nodeP220()
              : nodeP223();
    }

    private Endpoint nodeP220() {
      return cond20()
              ? nodeP221()
              : nodeP226();
    }

    private Endpoint nodeP221() {
      return cond21()
              ? nodeP230()
              : result11();
    }

    private Endpoint nodeP222() {
      return cond19()
              ? nodeP225()
              : nodeP223();
    }

    private Endpoint nodeP223() {
      return cond20()
              ? nodeP224()
              : result8();
    }

    private Endpoint nodeP224() {
      return cond21()
              ? result8()
              : result11();
    }

    private Endpoint nodeP225() {
      return cond20()
              ? nodeP229()
              : nodeP226();
    }

    private Endpoint nodeP226() {
      return cond21()
              ? nodeP227()
              : result8();
    }

    private Endpoint nodeP227() {
      return cond30()
              ? nodeP228()
              : result8();
    }

    private Endpoint nodeP228() {
      return cond34()
              ? result6()
              : result8();
    }

    private Endpoint nodeP229() {
      return cond21()
              ? nodeP230()
              : nodeP414();
    }

    private Endpoint nodeP230() {
      return cond30()
              ? nodeP231()
              : result7();
    }

    private Endpoint nodeP231() {
      return cond34()
              ? result6()
              : result7();
    }

    private Endpoint nodeP232() {
      return params.endpoint() != null
              ? result1()
              : nodeP233();
    }

    private Endpoint nodeP233() {
      return params.bucket() != null
              ? nodeP234()
              : nodeP479();
    }

    private Endpoint nodeP234() {
      return cond6()
              ? nodeP270()
              : nodeP235();
    }

    private Endpoint nodeP235() {
      return cond7()
              ? nodeP269()
              : nodeP236();
    }

    private Endpoint nodeP236() {
      return cond8()
              ? nodeP237()
              : nodeP490();
    }

    private Endpoint nodeP237() {
      return cond9()
              ? nodeP238()
              : nodeP242();
    }

    private Endpoint nodeP238() {
      return accessPointSuffix != null && accessPointSuffix.equals("--op-s3")
              ? nodeP239()
              : nodeP242();
    }

    private Endpoint nodeP239() {
      return cond11()
              ? nodeP240()
              : nodeP242();
    }

    private Endpoint nodeP240() {
      return cond12()
              ? nodeP241()
              : nodeP242();
    }

    private Endpoint nodeP241() {
      return cond13()
              ? nodeP546()
              : nodeP242();
    }

    private Endpoint nodeP242() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP265()
              : nodeP243();
    }

    private Endpoint nodeP243() {
      return cond20()
              ? nodeP263()
              : nodeP244();
    }

    private Endpoint nodeP244() {
      return cond26()
              ? nodeP245()
              : nodeP266();
    }

    private Endpoint nodeP245() {
      return cond37()
              ? nodeP246()
              : result85();
    }

    private Endpoint nodeP246() {
      return arnType != null && arnType.equals("")
              ? result85()
              : nodeP247();
    }

    private Endpoint nodeP247() {
      return arnType != null && arnType.equals("accesspoint")
              ? nodeP248()
              : nodeP517();
    }

    private Endpoint nodeP248() {
      return cond40()
              ? nodeP249()
              : result56();
    }

    private Endpoint nodeP249() {
      return accessPointName_ssa_1 != null && accessPointName_ssa_1.equals("")
              ? result56()
              : nodeP250();
    }

    private Endpoint nodeP250() {
      return cond42()
              ? nodeP537()
              : nodeP251();
    }

    private Endpoint nodeP251() {
      return cond48()
              ? result42()
              : nodeP252();
    }

    private Endpoint nodeP252() {
      return cond49()
              ? result44()
              : nodeP253();
    }

    private Endpoint nodeP253() {
      return cond51()
              ? nodeP254()
              : nodeP525();
    }

    private Endpoint nodeP254() {
      return cond60()
              ? nodeP257()
              : nodeP255();
    }

    private Endpoint nodeP255() {
      return cond62()
              ? result54()
              : nodeP256();
    }

    private Endpoint nodeP256() {
      return cond63()
              ? nodeP258()
              : result45();
    }

    private Endpoint nodeP257() {
      return cond62()
              ? result54()
              : nodeP258();
    }

    private Endpoint nodeP258() {
      return cond64()
              ? nodeP259()
              : result53();
    }

    private Endpoint nodeP259() {
      return cond66()
              ? nodeP260()
              : result52();
    }

    private Endpoint nodeP260() {
      return cond69()
              ? nodeP261()
              : result64();
    }

    private Endpoint nodeP261() {
      return cond70()
              ? nodeP262()
              : result51();
    }

    private Endpoint nodeP262() {
      return cond72()
              ? result61()
              : result50();
    }

    private Endpoint nodeP263() {
      return cond25()
              ? nodeP264()
              : result41();
    }

    private Endpoint nodeP264() {
      return region != null && region.equals("aws-global")
              ? result30()
              : result31();
    }

    private Endpoint nodeP265() {
      return cond26()
              ? result87()
              : nodeP266();
    }

    private Endpoint nodeP266() {
      return cond28()
              ? result86()
              : nodeP267();
    }

    private Endpoint nodeP267() {
      return cond34()
              ? nodeP268()
              : nodeP543();
    }

    private Endpoint nodeP268() {
      return region != null && region.equals("aws-global")
              ? result92()
              : result93();
    }

    private Endpoint nodeP269() {
      return cond8()
              ? nodeP396()
              : result8();
    }

    private Endpoint nodeP270() {
      return cond8()
              ? nodeP406()
              : result8();
    }

    private Endpoint nodeP271() {
      return Boolean.TRUE.equals(params.useDualStack())
              ? nodeP345()
              : nodeP272();
    }

    private Endpoint nodeP272() {
      return params.endpoint() != null
              ? result2()
              : nodeP273();
    }

    private Endpoint nodeP273() {
      return params.bucket() != null
              ? nodeP283()
              : nodeP274();
    }

    private Endpoint nodeP274() {
      return cond8()
              ? nodeP275()
              : result114();
    }

    private Endpoint nodeP275() {
      return cond15()
              ? result4()
              : nodeP276();
    }

    private Endpoint nodeP276() {
      return cond16()
              ? nodeP277()
              : nodeP280();
    }

    private Endpoint nodeP277() {
      return cond18()
              ? nodeP278()
              : nodeP280();
    }

    private Endpoint nodeP278() {
      return cond19()
              ? nodeP279()
              : nodeP280();
    }

    private Endpoint nodeP279() {
      return cond22()
              ? result13()
              : nodeP280();
    }

    private Endpoint nodeP280() {
      return cond35()
              ? nodeP281()
              : result41();
    }

    private Endpoint nodeP281() {
      return cond36()
              ? result101()
              : nodeP282();
    }

    private Endpoint nodeP282() {
      return region != null && region.equals("aws-global")
              ? result105()
              : result106();
    }

    private Endpoint nodeP283() {
      return cond6()
              ? nodeP404()
              : nodeP284();
    }

    private Endpoint nodeP284() {
      return cond7()
              ? nodeP394()
              : nodeP285();
    }

    private Endpoint nodeP285() {
      return cond8()
              ? nodeP294()
              : nodeP286();
    }

    private Endpoint nodeP286() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP500()
              : nodeP287();
    }

    private Endpoint nodeP287() {
      return cond26()
              ? nodeP288()
              : nodeP501();
    }

    private Endpoint nodeP288() {
      return cond37()
              ? nodeP289()
              : result85();
    }

    private Endpoint nodeP289() {
      return arnType != null && arnType.equals("")
              ? result85()
              : nodeP290();
    }

    private Endpoint nodeP290() {
      return arnType != null && arnType.equals("accesspoint")
              ? nodeP291()
              : nodeP306();
    }

    private Endpoint nodeP291() {
      return cond40()
              ? nodeP292()
              : result56();
    }

    private Endpoint nodeP292() {
      return accessPointName_ssa_1 != null && accessPointName_ssa_1.equals("")
              ? result56()
              : nodeP293();
    }

    private Endpoint nodeP293() {
      return cond42()
              ? nodeP334()
              : nodeP499();
    }

    private Endpoint nodeP294() {
      return cond9()
              ? nodeP295()
              : nodeP299();
    }

    private Endpoint nodeP295() {
      return accessPointSuffix != null && accessPointSuffix.equals("--op-s3")
              ? nodeP296()
              : nodeP299();
    }

    private Endpoint nodeP296() {
      return cond11()
              ? nodeP297()
              : nodeP299();
    }

    private Endpoint nodeP297() {
      return cond12()
              ? nodeP298()
              : nodeP299();
    }

    private Endpoint nodeP298() {
      return cond13()
              ? nodeP393()
              : nodeP299();
    }

    private Endpoint nodeP299() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP338()
              : nodeP300();
    }

    private Endpoint nodeP300() {
      return cond15()
              ? result4()
              : nodeP301();
    }

    private Endpoint nodeP301() {
      return cond20()
              ? nodeP336()
              : nodeP302();
    }

    private Endpoint nodeP302() {
      return cond26()
              ? nodeP303()
              : nodeP340();
    }

    private Endpoint nodeP303() {
      return cond37()
              ? nodeP304()
              : result85();
    }

    private Endpoint nodeP304() {
      return arnType != null && arnType.equals("")
              ? result85()
              : nodeP305();
    }

    private Endpoint nodeP305() {
      return arnType != null && arnType.equals("accesspoint")
              ? nodeP308()
              : nodeP306();
    }

    private Endpoint nodeP306() {
      return cond48()
              ? result57()
              : nodeP307();
    }

    private Endpoint nodeP307() {
      return cond50()
              ? result73()
              : result84();
    }

    private Endpoint nodeP308() {
      return cond40()
              ? nodeP309()
              : result56();
    }

    private Endpoint nodeP309() {
      return accessPointName_ssa_1 != null && accessPointName_ssa_1.equals("")
              ? result56()
              : nodeP310();
    }

    private Endpoint nodeP310() {
      return cond42()
              ? nodeP334()
              : nodeP311();
    }

    private Endpoint nodeP311() {
      return cond48()
              ? nodeP323()
              : nodeP312();
    }

    private Endpoint nodeP312() {
      return cond49()
              ? result44()
              : nodeP313();
    }

    private Endpoint nodeP313() {
      return cond51()
              ? nodeP314()
              : nodeP525();
    }

    private Endpoint nodeP314() {
      return cond60()
              ? nodeP317()
              : nodeP315();
    }

    private Endpoint nodeP315() {
      return cond62()
              ? result54()
              : nodeP316();
    }

    private Endpoint nodeP316() {
      return cond63()
              ? nodeP318()
              : result45();
    }

    private Endpoint nodeP317() {
      return cond62()
              ? result54()
              : nodeP318();
    }

    private Endpoint nodeP318() {
      return cond64()
              ? nodeP319()
              : result53();
    }

    private Endpoint nodeP319() {
      return cond66()
              ? nodeP320()
              : result52();
    }

    private Endpoint nodeP320() {
      return cond69()
              ? nodeP321()
              : result64();
    }

    private Endpoint nodeP321() {
      return cond70()
              ? nodeP322()
              : result51();
    }

    private Endpoint nodeP322() {
      return cond72()
              ? result60()
              : result50();
    }

    private Endpoint nodeP323() {
      return cond49()
              ? result44()
              : nodeP324();
    }

    private Endpoint nodeP324() {
      return cond51()
              ? nodeP325()
              : nodeP525();
    }

    private Endpoint nodeP325() {
      return cond60()
              ? nodeP328()
              : nodeP326();
    }

    private Endpoint nodeP326() {
      return cond62()
              ? result54()
              : nodeP327();
    }

    private Endpoint nodeP327() {
      return cond63()
              ? nodeP329()
              : result45();
    }

    private Endpoint nodeP328() {
      return cond62()
              ? result54()
              : nodeP329();
    }

    private Endpoint nodeP329() {
      return cond64()
              ? nodeP330()
              : result53();
    }

    private Endpoint nodeP330() {
      return cond66()
              ? nodeP331()
              : result52();
    }

    private Endpoint nodeP331() {
      return cond68()
              ? result46()
              : nodeP332();
    }

    private Endpoint nodeP332() {
      return cond70()
              ? nodeP333()
              : result51();
    }

    private Endpoint nodeP333() {
      return cond72()
              ? result48()
              : result50();
    }

    private Endpoint nodeP334() {
      return cond48()
              ? result55()
              : nodeP335();
    }

    private Endpoint nodeP335() {
      return cond52()
              ? result66()
              : result71();
    }

    private Endpoint nodeP336() {
      return cond25()
              ? nodeP337()
              : result41();
    }

    private Endpoint nodeP337() {
      return region != null && region.equals("aws-global")
              ? result26()
              : result27();
    }

    private Endpoint nodeP338() {
      return cond15()
              ? result4()
              : nodeP339();
    }

    private Endpoint nodeP339() {
      return cond26()
              ? result87()
              : nodeP340();
    }

    private Endpoint nodeP340() {
      return cond28()
              ? result86()
              : nodeP341();
    }

    private Endpoint nodeP341() {
      return cond34()
              ? nodeP344()
              : nodeP342();
    }

    private Endpoint nodeP342() {
      return cond35()
              ? nodeP343()
              : nodeP544();
    }

    private Endpoint nodeP343() {
      return cond36()
              ? result101()
              : result114();
    }

    private Endpoint nodeP344() {
      return region != null && region.equals("aws-global")
              ? result90()
              : result91();
    }

    private Endpoint nodeP345() {
      return params.endpoint() != null
              ? result1()
              : nodeP346();
    }

    private Endpoint nodeP346() {
      return params.bucket() != null
              ? nodeP356()
              : nodeP347();
    }

    private Endpoint nodeP347() {
      return cond8()
              ? nodeP348()
              : result114();
    }

    private Endpoint nodeP348() {
      return cond15()
              ? result4()
              : nodeP349();
    }

    private Endpoint nodeP349() {
      return cond16()
              ? nodeP350()
              : nodeP353();
    }

    private Endpoint nodeP350() {
      return cond18()
              ? nodeP351()
              : nodeP353();
    }

    private Endpoint nodeP351() {
      return cond19()
              ? nodeP352()
              : nodeP353();
    }

    private Endpoint nodeP352() {
      return cond22()
              ? result13()
              : nodeP353();
    }

    private Endpoint nodeP353() {
      return cond35()
              ? nodeP354()
              : result41();
    }

    private Endpoint nodeP354() {
      return cond36()
              ? result42()
              : nodeP355();
    }

    private Endpoint nodeP355() {
      return region != null && region.equals("aws-global")
              ? result103()
              : result104();
    }

    private Endpoint nodeP356() {
      return cond6()
              ? nodeP404()
              : nodeP357();
    }

    private Endpoint nodeP357() {
      return cond7()
              ? nodeP394()
              : nodeP358();
    }

    private Endpoint nodeP358() {
      return cond8()
              ? nodeP359()
              : nodeP490();
    }

    private Endpoint nodeP359() {
      return cond9()
              ? nodeP360()
              : nodeP364();
    }

    private Endpoint nodeP360() {
      return accessPointSuffix != null && accessPointSuffix.equals("--op-s3")
              ? nodeP361()
              : nodeP364();
    }

    private Endpoint nodeP361() {
      return cond11()
              ? nodeP362()
              : nodeP364();
    }

    private Endpoint nodeP362() {
      return cond12()
              ? nodeP363()
              : nodeP364();
    }

    private Endpoint nodeP363() {
      return cond13()
              ? nodeP393()
              : nodeP364();
    }

    private Endpoint nodeP364() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP388()
              : nodeP365();
    }

    private Endpoint nodeP365() {
      return cond15()
              ? result4()
              : nodeP366();
    }

    private Endpoint nodeP366() {
      return cond20()
              ? nodeP386()
              : nodeP367();
    }

    private Endpoint nodeP367() {
      return cond26()
              ? nodeP368()
              : nodeP390();
    }

    private Endpoint nodeP368() {
      return cond37()
              ? nodeP369()
              : result85();
    }

    private Endpoint nodeP369() {
      return arnType != null && arnType.equals("")
              ? result85()
              : nodeP370();
    }

    private Endpoint nodeP370() {
      return arnType != null && arnType.equals("accesspoint")
              ? nodeP371()
              : nodeP517();
    }

    private Endpoint nodeP371() {
      return cond40()
              ? nodeP372()
              : result56();
    }

    private Endpoint nodeP372() {
      return accessPointName_ssa_1 != null && accessPointName_ssa_1.equals("")
              ? result56()
              : nodeP373();
    }

    private Endpoint nodeP373() {
      return cond42()
              ? nodeP537()
              : nodeP374();
    }

    private Endpoint nodeP374() {
      return cond48()
              ? result42()
              : nodeP375();
    }

    private Endpoint nodeP375() {
      return cond49()
              ? result44()
              : nodeP376();
    }

    private Endpoint nodeP376() {
      return cond51()
              ? nodeP377()
              : nodeP525();
    }

    private Endpoint nodeP377() {
      return cond60()
              ? nodeP380()
              : nodeP378();
    }

    private Endpoint nodeP378() {
      return cond62()
              ? result54()
              : nodeP379();
    }

    private Endpoint nodeP379() {
      return cond63()
              ? nodeP381()
              : result45();
    }

    private Endpoint nodeP380() {
      return cond62()
              ? result54()
              : nodeP381();
    }

    private Endpoint nodeP381() {
      return cond64()
              ? nodeP382()
              : result53();
    }

    private Endpoint nodeP382() {
      return cond66()
              ? nodeP383()
              : result52();
    }

    private Endpoint nodeP383() {
      return cond69()
              ? nodeP384()
              : result64();
    }

    private Endpoint nodeP384() {
      return cond70()
              ? nodeP385()
              : result51();
    }

    private Endpoint nodeP385() {
      return cond72()
              ? result59()
              : result50();
    }

    private Endpoint nodeP386() {
      return cond25()
              ? nodeP387()
              : result41();
    }

    private Endpoint nodeP387() {
      return region != null && region.equals("aws-global")
              ? result24()
              : result25();
    }

    private Endpoint nodeP388() {
      return cond15()
              ? result4()
              : nodeP389();
    }

    private Endpoint nodeP389() {
      return cond26()
              ? result87()
              : nodeP390();
    }

    private Endpoint nodeP390() {
      return cond28()
              ? result86()
              : nodeP391();
    }

    private Endpoint nodeP391() {
      return cond34()
              ? nodeP392()
              : nodeP543();
    }

    private Endpoint nodeP392() {
      return region != null && region.equals("aws-global")
              ? result88()
              : result89();
    }

    private Endpoint nodeP393() {
      return cond15()
              ? result4()
              : nodeP546();
    }

    private Endpoint nodeP394() {
      return cond8()
              ? nodeP395()
              : result8();
    }

    private Endpoint nodeP395() {
      return cond15()
              ? result4()
              : nodeP396();
    }

    private Endpoint nodeP396() {
      return cond16()
              ? nodeP397()
              : nodeP409();
    }

    private Endpoint nodeP397() {
      return cond18()
              ? nodeP398()
              : nodeP409();
    }

    private Endpoint nodeP398() {
      return cond19()
              ? nodeP399()
              : nodeP409();
    }

    private Endpoint nodeP399() {
      return cond20()
              ? nodeP400()
              : result8();
    }

    private Endpoint nodeP400() {
      return cond27()
              ? nodeP401()
              : result11();
    }

    private Endpoint nodeP401() {
      return cond29()
              ? result10()
              : nodeP402();
    }

    private Endpoint nodeP402() {
      return cond31()
              ? result10()
              : nodeP403();
    }

    private Endpoint nodeP403() {
      return cond32()
              ? result10()
              : nodeP421();
    }

    private Endpoint nodeP404() {
      return cond8()
              ? nodeP405()
              : result8();
    }

    private Endpoint nodeP405() {
      return cond15()
              ? result4()
              : nodeP406();
    }

    private Endpoint nodeP406() {
      return cond16()
              ? nodeP407()
              : nodeP409();
    }

    private Endpoint nodeP407() {
      return cond18()
              ? nodeP408()
              : nodeP409();
    }

    private Endpoint nodeP408() {
      return cond19()
              ? nodeP410()
              : nodeP409();
    }

    private Endpoint nodeP409() {
      return cond20()
              ? result11()
              : result8();
    }

    private Endpoint nodeP410() {
      return cond20()
              ? nodeP413()
              : nodeP411();
    }

    private Endpoint nodeP411() {
      return cond22()
              ? nodeP412()
              : result8();
    }

    private Endpoint nodeP412() {
      return cond34()
              ? result9()
              : result8();
    }

    private Endpoint nodeP413() {
      return cond22()
              ? nodeP415()
              : nodeP414();
    }

    private Endpoint nodeP414() {
      return cond27()
              ? nodeP418()
              : result11();
    }

    private Endpoint nodeP415() {
      return cond27()
              ? nodeP417()
              : nodeP416();
    }

    private Endpoint nodeP416() {
      return cond34()
              ? result9()
              : result11();
    }

    private Endpoint nodeP417() {
      return cond34()
              ? result9()
              : nodeP418();
    }

    private Endpoint nodeP418() {
      return cond43()
              ? result10()
              : nodeP419();
    }

    private Endpoint nodeP419() {
      return cond47()
              ? result10()
              : nodeP420();
    }

    private Endpoint nodeP420() {
      return cond53()
              ? result10()
              : nodeP421();
    }

    private Endpoint nodeP421() {
      return cond54()
              ? result10()
              : nodeP422();
    }

    private Endpoint nodeP422() {
      return cond56()
              ? result10()
              : result11();
    }

    private Endpoint nodeP423() {
      return Boolean.TRUE.equals(params.useFips())
              ? result0()
              : nodeP424();
    }

    private Endpoint nodeP424() {
      return Boolean.TRUE.equals(params.useDualStack())
              ? nodeP477()
              : nodeP425();
    }

    private Endpoint nodeP425() {
      return params.endpoint() != null
              ? result3()
              : nodeP426();
    }

    private Endpoint nodeP426() {
      return params.bucket() != null
              ? nodeP437()
              : nodeP427();
    }

    private Endpoint nodeP427() {
      return cond8()
              ? nodeP428()
              : result114();
    }

    private Endpoint nodeP428() {
      return cond16()
              ? nodeP429()
              : nodeP432();
    }

    private Endpoint nodeP429() {
      return cond18()
              ? nodeP430()
              : nodeP432();
    }

    private Endpoint nodeP430() {
      return cond19()
              ? nodeP431()
              : nodeP432();
    }

    private Endpoint nodeP431() {
      return cond22()
              ? result13()
              : nodeP432();
    }

    private Endpoint nodeP432() {
      return cond35()
              ? nodeP433()
              : result41();
    }

    private Endpoint nodeP433() {
      return cond36()
              ? result43()
              : nodeP434();
    }

    private Endpoint nodeP434() {
      return region != null && region.equals("aws-global")
              ? result111()
              : nodeP435();
    }

    private Endpoint nodeP435() {
      return Boolean.TRUE.equals(params.useGlobalEndpoint())
              ? nodeP436()
              : result113();
    }

    private Endpoint nodeP436() {
      return region != null && region.equals("us-east-1")
              ? result112()
              : result113();
    }

    private Endpoint nodeP437() {
      return cond6()
              ? result5()
              : nodeP438();
    }

    private Endpoint nodeP438() {
      return cond7()
              ? result5()
              : nodeP439();
    }

    private Endpoint nodeP439() {
      return cond8()
              ? nodeP449()
              : nodeP440();
    }

    private Endpoint nodeP440() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP500()
              : nodeP441();
    }

    private Endpoint nodeP441() {
      return cond26()
              ? nodeP442()
              : nodeP501();
    }

    private Endpoint nodeP442() {
      return cond37()
              ? nodeP443()
              : result85();
    }

    private Endpoint nodeP443() {
      return arnType != null && arnType.equals("")
              ? result85()
              : nodeP444();
    }

    private Endpoint nodeP444() {
      return arnType != null && arnType.equals("accesspoint")
              ? nodeP445()
              : nodeP464();
    }

    private Endpoint nodeP445() {
      return cond40()
              ? nodeP446()
              : result56();
    }

    private Endpoint nodeP446() {
      return accessPointName_ssa_1 != null && accessPointName_ssa_1.equals("")
              ? result56()
              : nodeP447();
    }

    private Endpoint nodeP447() {
      return cond42()
              ? nodeP470()
              : nodeP448();
    }

    private Endpoint nodeP448() {
      return cond48()
              ? result43()
              : nodeP499();
    }

    private Endpoint nodeP449() {
      return cond9()
              ? nodeP450()
              : nodeP454();
    }

    private Endpoint nodeP450() {
      return accessPointSuffix != null && accessPointSuffix.equals("--op-s3")
              ? nodeP451()
              : nodeP454();
    }

    private Endpoint nodeP451() {
      return cond11()
              ? nodeP452()
              : nodeP454();
    }

    private Endpoint nodeP452() {
      return cond12()
              ? nodeP453()
              : nodeP454();
    }

    private Endpoint nodeP453() {
      return cond13()
              ? nodeP546()
              : nodeP454();
    }

    private Endpoint nodeP454() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP472()
              : nodeP455();
    }

    private Endpoint nodeP455() {
      return cond15()
              ? nodeP459()
              : nodeP456();
    }

    private Endpoint nodeP456() {
      return cond20()
              ? nodeP457()
              : nodeP460();
    }

    private Endpoint nodeP457() {
      return cond25()
              ? nodeP458()
              : result41();
    }

    private Endpoint nodeP458() {
      return region != null && region.equals("aws-global")
              ? result36()
              : result37();
    }

    private Endpoint nodeP459() {
      return cond20()
              ? nodeP539()
              : nodeP460();
    }

    private Endpoint nodeP460() {
      return cond26()
              ? nodeP461()
              : nodeP473();
    }

    private Endpoint nodeP461() {
      return cond37()
              ? nodeP462()
              : result85();
    }

    private Endpoint nodeP462() {
      return arnType != null && arnType.equals("")
              ? result85()
              : nodeP463();
    }

    private Endpoint nodeP463() {
      return arnType != null && arnType.equals("accesspoint")
              ? nodeP466()
              : nodeP464();
    }

    private Endpoint nodeP464() {
      return cond48()
              ? result57()
              : nodeP465();
    }

    private Endpoint nodeP465() {
      return cond50()
              ? result74()
              : result84();
    }

    private Endpoint nodeP466() {
      return cond40()
              ? nodeP467()
              : result56();
    }

    private Endpoint nodeP467() {
      return accessPointName_ssa_1 != null && accessPointName_ssa_1.equals("")
              ? result56()
              : nodeP468();
    }

    private Endpoint nodeP468() {
      return cond42()
              ? nodeP470()
              : nodeP469();
    }

    private Endpoint nodeP469() {
      return cond48()
              ? result43()
              : nodeP523();
    }

    private Endpoint nodeP470() {
      return cond48()
              ? result43()
              : nodeP471();
    }

    private Endpoint nodeP471() {
      return cond52()
              ? result67()
              : result71();
    }

    private Endpoint nodeP472() {
      return cond26()
              ? result87()
              : nodeP473();
    }

    private Endpoint nodeP473() {
      return cond28()
              ? result86()
              : nodeP474();
    }

    private Endpoint nodeP474() {
      return cond34()
              ? result99()
              : nodeP475();
    }

    private Endpoint nodeP475() {
      return cond35()
              ? nodeP476()
              : nodeP544();
    }

    private Endpoint nodeP476() {
      return cond36()
              ? result43()
              : result114();
    }

    private Endpoint nodeP477() {
      return params.endpoint() != null
              ? result1()
              : nodeP478();
    }

    private Endpoint nodeP478() {
      return params.bucket() != null
              ? nodeP487()
              : nodeP479();
    }

    private Endpoint nodeP479() {
      return cond8()
              ? nodeP480()
              : result114();
    }

    private Endpoint nodeP480() {
      return cond16()
              ? nodeP481()
              : nodeP484();
    }

    private Endpoint nodeP481() {
      return cond18()
              ? nodeP482()
              : nodeP484();
    }

    private Endpoint nodeP482() {
      return cond19()
              ? nodeP483()
              : nodeP484();
    }

    private Endpoint nodeP483() {
      return cond22()
              ? result13()
              : nodeP484();
    }

    private Endpoint nodeP484() {
      return cond35()
              ? nodeP485()
              : result41();
    }

    private Endpoint nodeP485() {
      return cond36()
              ? result42()
              : nodeP486();
    }

    private Endpoint nodeP486() {
      return region != null && region.equals("aws-global")
              ? result107()
              : result108();
    }

    private Endpoint nodeP487() {
      return cond6()
              ? result5()
              : nodeP488();
    }

    private Endpoint nodeP488() {
      return cond7()
              ? result5()
              : nodeP489();
    }

    private Endpoint nodeP489() {
      return cond8()
              ? nodeP502()
              : nodeP490();
    }

    private Endpoint nodeP490() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP500()
              : nodeP491();
    }

    private Endpoint nodeP491() {
      return cond26()
              ? nodeP492()
              : nodeP501();
    }

    private Endpoint nodeP492() {
      return cond37()
              ? nodeP493()
              : result85();
    }

    private Endpoint nodeP493() {
      return arnType != null && arnType.equals("")
              ? result85()
              : nodeP494();
    }

    private Endpoint nodeP494() {
      return arnType != null && arnType.equals("accesspoint")
              ? nodeP495()
              : nodeP517();
    }

    private Endpoint nodeP495() {
      return cond40()
              ? nodeP496()
              : result56();
    }

    private Endpoint nodeP496() {
      return accessPointName_ssa_1 != null && accessPointName_ssa_1.equals("")
              ? result56()
              : nodeP497();
    }

    private Endpoint nodeP497() {
      return cond42()
              ? nodeP537()
              : nodeP498();
    }

    private Endpoint nodeP498() {
      return cond48()
              ? result42()
              : nodeP499();
    }

    private Endpoint nodeP499() {
      return cond49()
              ? result44()
              : nodeP525();
    }

    private Endpoint nodeP500() {
      return cond26()
              ? result87()
              : nodeP501();
    }

    private Endpoint nodeP501() {
      return cond28()
              ? result86()
              : result114();
    }

    private Endpoint nodeP502() {
      return cond9()
              ? nodeP503()
              : nodeP507();
    }

    private Endpoint nodeP503() {
      return accessPointSuffix != null && accessPointSuffix.equals("--op-s3")
              ? nodeP504()
              : nodeP507();
    }

    private Endpoint nodeP504() {
      return cond11()
              ? nodeP505()
              : nodeP507();
    }

    private Endpoint nodeP505() {
      return cond12()
              ? nodeP506()
              : nodeP507();
    }

    private Endpoint nodeP506() {
      return cond13()
              ? nodeP546()
              : nodeP507();
    }

    private Endpoint nodeP507() {
      return Boolean.TRUE.equals(params.forcePathStyle())
              ? nodeP540()
              : nodeP508();
    }

    private Endpoint nodeP508() {
      return cond15()
              ? nodeP512()
              : nodeP509();
    }

    private Endpoint nodeP509() {
      return cond20()
              ? nodeP510()
              : nodeP513();
    }

    private Endpoint nodeP510() {
      return cond25()
              ? nodeP511()
              : result41();
    }

    private Endpoint nodeP511() {
      return region != null && region.equals("aws-global")
              ? result28()
              : result29();
    }

    private Endpoint nodeP512() {
      return cond20()
              ? nodeP539()
              : nodeP513();
    }

    private Endpoint nodeP513() {
      return cond26()
              ? nodeP514()
              : nodeP541();
    }

    private Endpoint nodeP514() {
      return cond37()
              ? nodeP515()
              : result85();
    }

    private Endpoint nodeP515() {
      return arnType != null && arnType.equals("")
              ? result85()
              : nodeP516();
    }

    private Endpoint nodeP516() {
      return arnType != null && arnType.equals("accesspoint")
              ? nodeP519()
              : nodeP517();
    }

    private Endpoint nodeP517() {
      return cond48()
              ? result57()
              : nodeP518();
    }

    private Endpoint nodeP518() {
      return cond50()
              ? result72()
              : result84();
    }

    private Endpoint nodeP519() {
      return cond40()
              ? nodeP520()
              : result56();
    }

    private Endpoint nodeP520() {
      return accessPointName_ssa_1 != null && accessPointName_ssa_1.equals("")
              ? result56()
              : nodeP521();
    }

    private Endpoint nodeP521() {
      return cond42()
              ? nodeP537()
              : nodeP522();
    }

    private Endpoint nodeP522() {
      return cond48()
              ? result42()
              : nodeP523();
    }

    private Endpoint nodeP523() {
      return cond49()
              ? result44()
              : nodeP524();
    }

    private Endpoint nodeP524() {
      return cond51()
              ? nodeP528()
              : nodeP525();
    }

    private Endpoint nodeP525() {
      return cond60()
              ? result54()
              : nodeP526();
    }

    private Endpoint nodeP526() {
      return cond62()
              ? result54()
              : nodeP527();
    }

    private Endpoint nodeP527() {
      return cond63()
              ? result54()
              : result45();
    }

    private Endpoint nodeP528() {
      return cond60()
              ? nodeP531()
              : nodeP529();
    }

    private Endpoint nodeP529() {
      return cond62()
              ? result54()
              : nodeP530();
    }

    private Endpoint nodeP530() {
      return cond63()
              ? nodeP532()
              : result45();
    }

    private Endpoint nodeP531() {
      return cond62()
              ? result54()
              : nodeP532();
    }

    private Endpoint nodeP532() {
      return cond64()
              ? nodeP533()
              : result53();
    }

    private Endpoint nodeP533() {
      return cond66()
              ? nodeP534()
              : result52();
    }

    private Endpoint nodeP534() {
      return cond69()
              ? nodeP535()
              : result64();
    }

    private Endpoint nodeP535() {
      return cond70()
              ? nodeP536()
              : result51();
    }

    private Endpoint nodeP536() {
      return cond72()
              ? result58()
              : result50();
    }

    private Endpoint nodeP537() {
      return cond48()
              ? result42()
              : nodeP538();
    }

    private Endpoint nodeP538() {
      return cond52()
              ? result65()
              : result71();
    }

    private Endpoint nodeP539() {
      return cond25()
              ? result23()
              : result41();
    }

    private Endpoint nodeP540() {
      return cond26()
              ? result87()
              : nodeP541();
    }

    private Endpoint nodeP541() {
      return cond28()
              ? result86()
              : nodeP542();
    }

    private Endpoint nodeP542() {
      return cond34()
              ? result99()
              : nodeP543();
    }

    private Endpoint nodeP543() {
      return cond35()
              ? nodeP545()
              : nodeP544();
    }

    private Endpoint nodeP544() {
      return cond36()
              ? result41()
              : result114();
    }

    private Endpoint nodeP545() {
      return cond36()
              ? result42()
              : result114();
    }

    private Endpoint nodeP546() {
      return cond17()
              ? nodeP547()
              : result21();
    }

    private Endpoint nodeP547() {
      return cond20()
              ? nodeP548()
              : result20();
    }

    private Endpoint nodeP548() {
      return regionPrefix != null && regionPrefix.equals("beta")
              ? nodeP551()
              : nodeP549();
    }

    private Endpoint nodeP549() {
      return hardwareType != null && hardwareType.equals("e")
              ? result16()
              : nodeP550();
    }

    private Endpoint nodeP550() {
      return hardwareType != null && hardwareType.equals("o")
              ? result18()
              : result19();
    }

    private Endpoint nodeP551() {
      return hardwareType != null && hardwareType.equals("e")
              ? result14()
              : nodeP552();
    }

    private Endpoint nodeP552() {
      return hardwareType != null && hardwareType.equals("o")
              ? result14()
              : result19();
    }

    private boolean cond6() {
      return (RulesFunctions.substringEquals(params.bucket(), 0, 6, true, "--x-s3"));
    }

    private boolean cond7() {
      return (RulesFunctions.substringEquals(params.bucket(), 0, 7, true, "--xa-s3"));
    }

    private boolean cond8() {
      partitionResult = RulesFunctions.awsPartition(region);
      return partitionResult != null;
    }

    private boolean cond9() {
      accessPointSuffix = RulesFunctions.substring(params.bucket(), 0, 7, true);
      return accessPointSuffix != null;
    }

    private boolean cond11() {
      regionPrefix = RulesFunctions.substring(params.bucket(), 8, 12, true);
      return regionPrefix != null;
    }

    private boolean cond12() {
      outpostId_ssa_2 = RulesFunctions.substring(params.bucket(), 32, 49, true);
      return outpostId_ssa_2 != null;
    }

    private boolean cond13() {
      hardwareType = RulesFunctions.substring(params.bucket(), 49, 50, true);
      return hardwareType != null;
    }

    private boolean cond15() {
      return ("aws-cn".equals(partitionResult.name()));
    }

    private boolean cond16() {
      _s3e_ds = (params.useDualStack() ? ".dualstack" : "");
      return true;
    }

    private boolean cond17() {
      return (RulesFunctions.isValidHostLabelSingle(outpostId_ssa_2));
    }

    private boolean cond18() {
      _s3e_fips = (params.useFips() ? "-fips" : "");
      return true;
    }

    private boolean cond19() {
      _s3e_auth = (Boolean.TRUE.equals(params.disableS3ExpressSessionAuth()) ? "sigv4" : "sigv4-s3express");
      return true;
    }

    private boolean cond20() {
      return (RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), false));
    }

    private boolean cond21() {
      url = RulesFunctions.parseURL(params.endpoint());
      return url != null;
    }

    private boolean cond22() {
      return (Boolean.TRUE.equals(params.useS3ExpressControlEndpoint()));
    }

    private boolean cond23() {
      return (RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), true));
    }

    private boolean cond24() {
      return ("http".equals(url.scheme()));
    }

    private boolean cond25() {
      return (RulesFunctions.isValidHostLabelSingle(region));
    }

    private boolean cond26() {
      bucketArn = RulesFunctions.awsParseArn(params.bucket());
      return bucketArn != null;
    }

    private boolean cond27() {
      s3expressAvailabilityZoneId = RulesFunctions.listAccess(RulesFunctions.split(params.bucket(), "--", 0), -2);
      return s3expressAvailabilityZoneId != null;
    }

    private boolean cond28() {
      return (RulesFunctions.substringEquals(params.bucket(), 0, 4, false, "arn:"));
    }

    private boolean cond29() {
      return (RulesFunctions.substringEquals(params.bucket(), 16, 18, true, "--"));
    }

    private boolean cond30() {
      return (url.isIp());
    }

    private boolean cond31() {
      return (RulesFunctions.substringEquals(params.bucket(), 21, 23, true, "--"));
    }

    private boolean cond32() {
      return (RulesFunctions.substringEquals(params.bucket(), 27, 29, true, "--"));
    }

    private boolean cond34() {
      uri_encoded_bucket = RulesFunctions.uriEncode(params.bucket());
      return uri_encoded_bucket != null;
    }

    private boolean cond35() {
      return (RulesFunctions.isValidHostLabelMulti(region));
    }

    private boolean cond36() {
      return (Boolean.TRUE.equals(params.useObjectLambdaEndpoint()));
    }

    private boolean cond37() {
      arnType = RulesFunctions.listAccess(bucketArn.resourceId(), 0);
      return arnType != null;
    }

    private boolean cond40() {
      accessPointName_ssa_1 = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
      return accessPointName_ssa_1 != null;
    }

    private boolean cond42() {
      return ("".equals(bucketArn.region()));
    }

    private boolean cond43() {
      return (RulesFunctions.substringEquals(params.bucket(), 14, 16, true, "--"));
    }

    private boolean cond47() {
      return (RulesFunctions.substringEquals(params.bucket(), 19, 21, true, "--"));
    }

    private boolean cond48() {
      return ("s3-object-lambda".equals(bucketArn.service()));
    }

    private boolean cond49() {
      return (Boolean.TRUE.equals(params.disableAccessPoints()));
    }

    private boolean cond50() {
      return ("s3-outposts".equals(bucketArn.service()));
    }

    private boolean cond51() {
      bucketPartition = RulesFunctions.awsPartition(bucketArn.region());
      return bucketPartition != null;
    }

    private boolean cond52() {
      return (RulesFunctions.isValidHostLabelMulti(accessPointName_ssa_1));
    }

    private boolean cond53() {
      return (RulesFunctions.substringEquals(params.bucket(), 26, 28, true, "--"));
    }

    private boolean cond54() {
      return (RulesFunctions.substringEquals(params.bucket(), 15, 17, true, "--"));
    }

    private boolean cond55() {
      return (RulesFunctions.listAccess(bucketArn.resourceId(), 4) != null);
    }

    private boolean cond56() {
      return (RulesFunctions.substringEquals(params.bucket(), 20, 22, true, "--"));
    }

    private boolean cond59() {
      outpostId_ssa_1 = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
      return outpostId_ssa_1 != null;
    }

    private boolean cond60() {
      return (!Boolean.FALSE.equals(params.useArnRegion()));
    }

    private boolean cond61() {
      return (RulesFunctions.isValidHostLabelSingle(outpostId_ssa_1));
    }

    private boolean cond62() {
      outpostType = RulesFunctions.listAccess(bucketArn.resourceId(), 2);
      return outpostType != null;
    }

    private boolean cond63() {
      return (RulesFunctions.stringEquals(region, bucketArn.region()));
    }

    private boolean cond64() {
      return (RulesFunctions.stringEquals(bucketPartition.name(), partitionResult.name()));
    }

    private boolean cond66() {
      return (RulesFunctions.isValidHostLabelMulti(bucketArn.region()));
    }

    private boolean cond67() {
      return (RulesFunctions.stringEquals(bucketArn.partition(), partitionResult.name()));
    }

    private boolean cond68() {
      return ("".equals(bucketArn.accountId()));
    }

    private boolean cond69() {
      return ("s3".equals(bucketArn.service()));
    }

    private boolean cond70() {
      return (RulesFunctions.isValidHostLabelSingle(bucketArn.accountId()));
    }

    private boolean cond71() {
      accessPointName_ssa_2 = RulesFunctions.listAccess(bucketArn.resourceId(), 3);
      return accessPointName_ssa_2 != null;
    }

    private boolean cond72() {
      return (RulesFunctions.isValidHostLabelSingle(accessPointName_ssa_1));
    }

    private boolean cond74() {
      return (RulesFunctions.isValidHostLabelSingle(accessPointName_ssa_2));
    }

    private Endpoint result0() {
      throw SdkClientException.create("Accelerate cannot be used with FIPS");
    }

    private Endpoint result1() {
      throw SdkClientException.create("Cannot set dual-stack in combination with a custom endpoint.");
    }

    private Endpoint result2() {
      throw SdkClientException.create("A custom endpoint cannot be combined with FIPS");
    }

    private Endpoint result3() {
      throw SdkClientException.create("A custom endpoint cannot be combined with S3 Accelerate");
    }

    private Endpoint result4() {
      throw SdkClientException.create("Partition does not support FIPS");
    }

    private Endpoint result5() {
      throw SdkClientException.create("S3Express does not support S3 Accelerate.");
    }

    private Endpoint result6() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + "/" + uri_encoded_bucket + url.path())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(DynamicEndpointAuthSchemeFactory.builder().disableDoubleEncoding(true).signingName("s3express").signingRegion(region).create(_s3e_auth))).build();
    }

    private Endpoint result7() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(DynamicEndpointAuthSchemeFactory.builder().disableDoubleEncoding(true).signingName("s3express").signingRegion(region).create(_s3e_auth))).build();
    }

    private Endpoint result8() {
      throw SdkClientException.create("S3Express bucket name is not a valid virtual hostable name.");
    }

    private Endpoint result9() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3express-control" + _s3e_fips + _s3e_ds + "." + region + "." + partitionResult.dnsSuffix(), -1, "/" + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express").signingRegion(region).build())).build();
    }

    private Endpoint result10() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3express" + _s3e_fips + "-" + s3expressAvailabilityZoneId + _s3e_ds + "." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(DynamicEndpointAuthSchemeFactory.builder().disableDoubleEncoding(true).signingName("s3express").signingRegion(region).create(_s3e_auth))).build();
    }

    private Endpoint result11() {
      throw SdkClientException.create("Unrecognized S3Express bucket name format.");
    }

    private Endpoint result12() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.path())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(DynamicEndpointAuthSchemeFactory.builder().disableDoubleEncoding(true).signingName("s3express").signingRegion(region).create(_s3e_auth))).build();
    }

    private Endpoint result13() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3express-control" + _s3e_fips + _s3e_ds + "." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express").signingRegion(region).build())).build();
    }

    private Endpoint result14() {
      throw SdkClientException.create("Expected a endpoint to be specified but no endpoint was found");
    }

    private Endpoint result15() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".ec2." + url.authority(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build())).build();
    }

    private Endpoint result16() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".ec2.s3-outposts." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build())).build();
    }

    private Endpoint result17() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".op-" + outpostId_ssa_2 + "." + url.authority(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build())).build();
    }

    private Endpoint result18() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".op-" + outpostId_ssa_2 + ".s3-outposts." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build())).build();
    }

    private Endpoint result19() {
      throw SdkClientException.create("Unrecognized hardware type: \"Expected hardware type o or e but got " + hardwareType + "\"");
    }

    private Endpoint result20() {
      throw SdkClientException.create("Invalid Outposts Bucket alias - it must be a valid bucket name.");
    }

    private Endpoint result21() {
      throw SdkClientException.create("Invalid ARN: The outpost Id must only contain a-z, A-Z, 0-9 and `-`.");
    }

    private Endpoint result22() {
      throw SdkClientException.create("Custom endpoint `" + params.endpoint() + "` was not a valid URI");
    }

    private Endpoint result23() {
      throw SdkClientException.create("S3 Accelerate cannot be used in this region");
    }

    private Endpoint result24() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result25() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result26() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3-fips.us-east-1." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result27() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3-fips." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result28() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3-accelerate.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result29() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3-accelerate.dualstack." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result30() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result31() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3.dualstack." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result32() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.normalizedPath() + params.bucket())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result33() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result34() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.normalizedPath() + params.bucket())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result35() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result36() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3-accelerate." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result37() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3-accelerate." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result38() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result39() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result40() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".s3." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result41() {
      throw SdkClientException.create("Invalid region: region was not a valid DNS name.");
    }

    private Endpoint result42() {
      throw SdkClientException.create("S3 Object Lambda does not support Dual-stack");
    }

    private Endpoint result43() {
      throw SdkClientException.create("S3 Object Lambda does not support S3 Accelerate");
    }

    private Endpoint result44() {
      throw SdkClientException.create("Access points are not supported for this operation");
    }

    private Endpoint result45() {
      throw SdkClientException.create("Invalid configuration: region from ARN `" + bucketArn.region() + "` does not match client region `" + region + "` and UseArnRegion is `false`");
    }

    private Endpoint result46() {
      throw SdkClientException.create("Invalid ARN: Missing account id");
    }

    private Endpoint result47() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + "." + url.authority() + url.path())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda").signingRegion(bucketArn.region()).build())).build();
    }

    private Endpoint result48() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-object-lambda-fips." + bucketArn.region() + "." + bucketPartition.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda").signingRegion(bucketArn.region()).build())).build();
    }

    private Endpoint result49() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-object-lambda." + bucketArn.region() + "." + bucketPartition.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda").signingRegion(bucketArn.region()).build())).build();
    }

    private Endpoint result50() {
      throw SdkClientException.create("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `" + accessPointName_ssa_1 + "`");
    }

    private Endpoint result51() {
      throw SdkClientException.create("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `" + bucketArn.accountId() + "`");
    }

    private Endpoint result52() {
      throw SdkClientException.create("Invalid region in ARN: `" + bucketArn.region() + "` (invalid DNS name)");
    }

    private Endpoint result53() {
      throw SdkClientException.create("Client was configured for partition `" + partitionResult.name() + "` but ARN (`" + params.bucket() + "`) has `" + bucketPartition.name() + "`");
    }

    private Endpoint result54() {
      throw SdkClientException.create("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
    }

    private Endpoint result55() {
      throw SdkClientException.create("Invalid ARN: bucket ARN is missing a region");
    }

    private Endpoint result56() {
      throw SdkClientException.create("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
    }

    private Endpoint result57() {
      throw SdkClientException.create("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `" + arnType + "`");
    }

    private Endpoint result58() {
      throw SdkClientException.create("Access Points do not support S3 Accelerate");
    }

    private Endpoint result59() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-accesspoint-fips.dualstack." + bucketArn.region() + "." + bucketPartition.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(bucketArn.region()).build())).build();
    }

    private Endpoint result60() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-accesspoint-fips." + bucketArn.region() + "." + bucketPartition.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(bucketArn.region()).build())).build();
    }

    private Endpoint result61() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-accesspoint.dualstack." + bucketArn.region() + "." + bucketPartition.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(bucketArn.region()).build())).build();
    }

    private Endpoint result62() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + "." + url.authority() + url.path())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(bucketArn.region()).build())).build();
    }

    private Endpoint result63() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-accesspoint." + bucketArn.region() + "." + bucketPartition.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(bucketArn.region()).build())).build();
    }

    private Endpoint result64() {
      throw SdkClientException.create("Invalid ARN: The ARN was not for the S3 service, found: " + bucketArn.service());
    }

    private Endpoint result65() {
      throw SdkClientException.create("S3 MRAP does not support dual-stack");
    }

    private Endpoint result66() {
      throw SdkClientException.create("S3 MRAP does not support FIPS");
    }

    private Endpoint result67() {
      throw SdkClientException.create("S3 MRAP does not support S3 Accelerate");
    }

    private Endpoint result68() {
      throw SdkClientException.create("Invalid configuration: Multi-Region Access Point ARNs are disabled.");
    }

    private Endpoint result69() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", accessPointName_ssa_1 + ".accesspoint.s3-global." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegionSet(Arrays.asList("*")).build())).build();
    }

    private Endpoint result70() {
      throw SdkClientException.create("Client was configured for partition `" + partitionResult.name() + "` but bucket referred to partition `" + bucketArn.partition() + "`");
    }

    private Endpoint result71() {
      throw SdkClientException.create("Invalid Access Point Name");
    }

    private Endpoint result72() {
      throw SdkClientException.create("S3 Outposts does not support Dual-stack");
    }

    private Endpoint result73() {
      throw SdkClientException.create("S3 Outposts does not support FIPS");
    }

    private Endpoint result74() {
      throw SdkClientException.create("S3 Outposts does not support S3 Accelerate");
    }

    private Endpoint result75() {
      throw SdkClientException.create("Invalid Arn: Outpost Access Point ARN contains sub resources");
    }

    private Endpoint result76() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", accessPointName_ssa_2 + "-" + bucketArn.accountId() + "." + outpostId_ssa_1 + "." + url.authority(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(bucketArn.region()).build())).build();
    }

    private Endpoint result77() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", accessPointName_ssa_2 + "-" + bucketArn.accountId() + "." + outpostId_ssa_1 + ".s3-outposts." + bucketArn.region() + "." + bucketPartition.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(bucketArn.region()).build())).build();
    }

    private Endpoint result78() {
      throw SdkClientException.create("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `" + accessPointName_ssa_2 + "`");
    }

    private Endpoint result79() {
      throw SdkClientException.create("Expected an outpost type `accesspoint`, found " + outpostType);
    }

    private Endpoint result80() {
      throw SdkClientException.create("Invalid ARN: expected an access point name");
    }

    private Endpoint result81() {
      throw SdkClientException.create("Invalid ARN: Expected a 4-component resource");
    }

    private Endpoint result82() {
      throw SdkClientException.create("Invalid ARN: The outpost Id may only contain a-z, A-Z, 0-9 and `-`. Found: `" + outpostId_ssa_1 + "`");
    }

    private Endpoint result83() {
      throw SdkClientException.create("Invalid ARN: The Outpost Id was not set");
    }

    private Endpoint result84() {
      throw SdkClientException.create("Invalid ARN: Unrecognized format: " + params.bucket() + " (type: " + arnType + ")");
    }

    private Endpoint result85() {
      throw SdkClientException.create("Invalid ARN: No ARN type specified");
    }

    private Endpoint result86() {
      throw SdkClientException.create("Invalid ARN: `" + params.bucket() + "` was not a valid ARN");
    }

    private Endpoint result87() {
      throw SdkClientException.create("Path-style addressing cannot be used with ARN buckets");
    }

    private Endpoint result88() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1, "/" + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result89() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix(), -1, "/" + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result90() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3-fips.us-east-1." + partitionResult.dnsSuffix(), -1, "/" + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result91() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3-fips." + region + "." + partitionResult.dnsSuffix(), -1, "/" + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result92() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1, "/" + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result93() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3.dualstack." + region + "." + partitionResult.dnsSuffix(), -1, "/" + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result94() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.normalizedPath() + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result95() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.normalizedPath() + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result96() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3." + partitionResult.dnsSuffix(), -1, "/" + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result97() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3." + partitionResult.dnsSuffix(), -1, "/" + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result98() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3." + region + "." + partitionResult.dnsSuffix(), -1, "/" + uri_encoded_bucket)).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result99() {
      throw SdkClientException.create("Path-style addressing cannot be used with S3 Accelerate");
    }

    private Endpoint result100() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.path())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda").signingRegion(region).build())).build();
    }

    private Endpoint result101() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3-object-lambda-fips." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda").signingRegion(region).build())).build();
    }

    private Endpoint result102() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3-object-lambda." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda").signingRegion(region).build())).build();
    }

    private Endpoint result103() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result104() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result105() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3-fips.us-east-1." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result106() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3-fips." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result107() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result108() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3.dualstack." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result109() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.path())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result110() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.path())).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result111() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion("us-east-1").build())).build();
    }

    private Endpoint result112() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result113() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "s3." + region + "." + partitionResult.dnsSuffix(), -1, "")).putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3").signingRegion(region).build())).build();
    }

    private Endpoint result114() {
      throw SdkClientException.create("A region must be set when sending requests to S3.");
    }
  }

  private static final class CacheEntry {
    final QueryEndpointParams params;

    final Endpoint endpoint;

    CacheEntry(QueryEndpointParams params, Endpoint endpoint) {
      this.params = params;
      this.endpoint = endpoint;
    }
  }
}
