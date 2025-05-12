<table>
    <tr>
        <td style="padding: 24px 0; width: 800px;">
            <table>
                <tr>
                    <td style="font-family: sans-serif; font-size: 15px; line-height: 20px;"><b>Execution Summary</b></td>
                </tr>
            </table>
            <table>
                <tr style="height: 12px;"></tr>
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 16px;">
                    <td style="width: 181px;"><b>START DATE</b></td>
                    <td style="width: 200px;"><b>END DATE</b></td>
                    <td style="width: 100px;"><b>DURATION</b></td>
                    <td style="width: 100px;"><b>THREADS</b></td>
                </tr>
                <tr style="height: 4px;"></tr>
                <tr style="font-family: sans-serif; font-size: 13px; line-height: 18px;">
                    <td style="width: 181px;">${startDate!}</td>
                    <td style="width: 200px;">${finishDate!}</td>
                    <td style="width: 100px;">${duration!}</td>
                    <td style="width: 100px;">${threads!}</td>
                </tr>
            </table>
            <table>
                <tr style="height: 16px;"></tr>
                <tr style="font-family: sans-serif; font-size: 11px; line-height: 16px;">
                    <td style="width: 80px; border-right: 1px solid #D5DCE3"><b style="color: #8F9EB4">TEST CASES</b><p style="margin-top: 6px; font-size: 13px">${testCasesCount!}</p></td>
                    <td style="width: 20px;"></td>
                    <td style="width: 80px;"><b style="color: #00BB5B">PASSED</b><p style="margin-top: 6px; font-size: 13px">${passedRate!}% (${passedCount!})</p></td>
                    <td style="width: 100px;"><b style="color: #FF5260">FAILED</b><p style="margin-top: 6px; font-size: 13px">${failedRate!}% (${failedCount!})</p></td>
                    <td style="width: 100px;"><b style="color: #FFB02E">WARNINGS</b><p style="margin-top: 6px; font-size: 13px">${warningRate!}% (${warningCount!})</p></td>
                    <td style="width: 100px;"><b style="color: #7F00FF">STOPPED</b><p style="margin-top: 6px; font-size: 13px">${stoppedRate!}% (${stoppedCount!})</p></td>
                    <td style="width: 100px;"><b style="color: #A9A9A9">NOT STARTED</b><p style="margin-top: 6px; font-size: 13px">${notStartedRate!}% (${notStartedCount!})</p></td>
                    <#if (skippedCount?? && skippedCount!=0) || (blockedCount?? && blockedRate!=0) || (inProgressCount?? && inProgressCount!=0)>
                        <td style="width: 20px; border-left: 1px solid #D5DCE3"></td>
                        <#if blockedCount?? && blockedRate!=0>
                            <td style="width: 80px;"><b style="color: #710606">BLOCKED</b><p style="margin-top: 6px; font-size: 13px">${blockedRate!}% (${blockedCount!})</p></td>
                        </#if>
                        <#if skippedCount?? && skippedCount!=0>
                            <td style="width: 80px;"><b style="color: #8F9EB4">SKIPPED</b><p style="margin-top: 6px; font-size: 13px">${skippedCount}</p></td>
                        </#if>
                        <#if inProgressCount?? && inProgressCount!=0>
                            <td style="width: 100px;"><b style="color: #8F9EB4">IN PROGRESS</b><p style="margin-top: 6px; font-size: 13px">${inProgressCount}</p></td>
                        </#if>
                    </#if>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td style="width: 100%;">
            <table>
                <tr style="height: 16px;"></tr>
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 16px;">
                    <td style="width: 381px;"><b>ENVIRONMENT</b></td>
                    <td style="width: 381px;"><b>ER LINK</b></td>
                    <td style="width: calc(100% - 762px);"><b>SCOPE</b></td>
                </tr>
                <tr style="height: 4px;"></tr>
                <tr style="font-family: sans-serif; font-size: 13px; line-height: 18px;">
                    <td style="width: 381px;">
                        <#if environmentLink??>
                            <a href="${environmentLink}"> Environment
                                <#if environmentName??>
                                    <#if (environmentName?length > 100)>
                                        ${environmentName [0..100]}<span>...</span>
                                    <#else>
                                        ${environmentName}
                                    </#if>
                                <#else>
                                    Link
                                </#if>
                            </a>
                        </#if>
                    </td>
                    <td style="width: 381px;">
                        <#if executionRequestLink?? && name??>
                            <a href="${executionRequestLink}">
                                <#if (name?length > 200)>
                                    ${name [2 .. 198]}<span>...<span>
                                <#else>
                                    ${name}
                                </#if>
                            </a>
                        <#else>
                            ${name!}
                        </#if>
                    </td>
                    <td style="width: calc(100% - 762px);">
                        <#if scopeLink??>
                            <a href="${scopeLink}">
                                <#if (scopeName?length > 200)>
                                    ${scopeName [2 .. 198]}<span>...<span>
                                <#else>
                                    ${scopeName}
                                </#if>
                            </a>
                        <#else>
                            <span>—</span>
                        </#if>
                    </td>
                </tr>
            </table>
            <#if browserSessionLink??>
                <table>
                    <tr>
                        <td style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 16px; padding: 16px 0 4px 0;"><b>BROWSER</b></td>
                    </tr>
                    <tr>
                        <td style="font-family: sans-serif; font-size: 13px; line-height: 18px;">
                            <#list browserSessionLink as link>
                                <a href="${link}">${link}</a><#if ! link?is_last>,&nbsp;</#if>
                            </#list>
                        </td>
                    </tr>
                </table>
            </#if>
            <#if labels??>
                <table>
                    <tr>
                        <td style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 16px; padding:
                         16px 0 4px 0;"><b>FILTERED BY LABELS</b></td>
                    </tr>
                    <tr>
                        <td style="font-family: sans-serif; font-size: 13px; line-height: 18px;">
                            <#list labels as label>
                                <span style="
                                    padding-top: 1px;
                                    padding-right: 7px;
                                    padding-bottom: 1px;
                                    padding-left: 7px;
                                    border-radius: 3px;
                                    background-color: #d6edff;
                                    font-size: 13px;
                                    margin: 1px;
                                ">
                                    ${label.name}
                                </span>
                            </#list>
                        </td>
                    </tr>
                </table>
            </#if>
            <#if description?has_content>
                <table>
                    <tr>
                        <td style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 16px; padding: 20px 0 4px 0;"><b>DESCRIPTION</b></td>
                    </tr>
                    <tr>
                        <td style="font-family: sans-serif; font-size: 13px; line-height: 18px;">${description}</td>
                    </tr>
                </table>
            </#if>
        </td>
    </tr>
</table>


<#--execution summary
startDate = "11.12.2020, 08:21:52"
finishDate = "11.12.2020, 08:28:42"
duration = "00:06:50"
threads = 8
testCasesCount = 12
passedRate = 33.3
passedCount = 2
failedRate = 50
failedCount = 3
warningRate = 0
warningCount = 0
stoppedRate = 16.7
stoppedCount = 1
notStartedRate = 0
notStartedCount = 0
skippedCount = 1
environmentLink = "http://some-url.com"
executionRequestLink = "http://some-url.com"
name = "Scope Grouping Scope[08:21:51]"
browserSessionLink = [
    "http://some-url.com"
]-->
