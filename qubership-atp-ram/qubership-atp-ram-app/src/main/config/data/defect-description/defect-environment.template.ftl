<#if environmentLink??>
[${environmentLink.name}|${environmentLink.url}]

<#if qaSystems?? && qaSystems?size gt 0>
||Name||Status||Version||URL||
<#list qaSystems! as system><#--
-->|${(system.name?has_content)?then(system.name,'-')}<#--
-->|${(system.status?has_content)?then(system.status,'-')}<#--
-->|${(system.version?has_content)?then(system.version,'-')}<#--
-->|<#if system.urls??><#list system.urls! as url>${url}${'\n'}</#list></#if>|
</#list>
</#if>
<#else>
    -
</#if>