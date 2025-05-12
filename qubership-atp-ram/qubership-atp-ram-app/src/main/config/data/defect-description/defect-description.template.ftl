*Log record:*
<#if logRecordLink??>
[${logRecordLink.name}|${logRecordLink.url}]
<#else>
-
</#if>

*Message:*
<#if message?? && message?has_content>
{code:java}
${message}
{code}
<#else>
-
</#if>

*Fail Pattern:*
${(failPattern?has_content)?then(failPattern,'-')}

*Failure Reason:*
${(failReason?has_content)?then(failReason,'-')}

*Link to ER:*
<#if linkToEr?? && linkToEr?has_content>
[Link to ER|${linkToEr}]
<#else>
-
</#if>

*Links to POT:*
<#if potLinks?? && potLinks?size gt 0>
    <#list potLinks! as potLink>
        [${potLink.name}|${potLink.url}]
    </#list>
<#else>
-
</#if>

*Links to SVP:*
<#if svpLinks?? && svpLinks?size gt 0>
    <#list svpLinks! as svpLink>
        [${svpLink.name}|${svpLink.url}]
    </#list>
<#else>
    -
</#if>

*Link to Log Collector logs:*
<#if linkToLc?? && linkToLc?has_content>
    [Link to LC|${linkToLc}]
<#else>
    -
</#if>

*Blocks:*
<#if blockLinks?? && blockLinks?size gt 0>
    <#list blockLinks! as blockLink>
        [${blockLink.name}|${blockLink.url}]
    </#list>
<#else>
-
</#if>
*Links to SSM metrics:*
<#if ssmMetricsLinks?? && ssmMetricsLinks?size gt 0>
    <#list ssmMetricsLinks! as ssmMetricsLink>
    [${ssmMetricsLink.name}|${ssmMetricsLink.url}]
</#list>
<#else>
-
</#if>
