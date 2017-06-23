<#macro content operationModel>

<#if operationModel.operationName == "GetJobOutput">

GetJobOutputResult result = response.getAwsResponse();

// wrapping the response with the LengthCheckInputStream.
result.setBody(new LengthCheckInputStream(result.getBody(), Long.parseLong(response.getHttpResponse().getHeaders().get("Content-Length")), software.amazon.awssdk.util.LengthCheckInputStream.INCLUDE_SKIPPED_BYTES));

// wrapping the response with the service client holder input stream to avoid client being GC'ed.
result.setBody(new ServiceClientHolderInputStream(result.getBody(), this));

</#if>

</#macro>
