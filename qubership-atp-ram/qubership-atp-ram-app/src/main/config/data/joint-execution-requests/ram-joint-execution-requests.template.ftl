<table style="width: 100%">
    <tr>
        <td style="padding: 24px 0;">
            <table>
                <tr>
                    <td style="font-family: sans-serif; font-size: 15px; line-height: 20px;"><b>Join Execution Request
                            Results</b></td>
                </tr>
                <tr style="height: 12px;">
                    <td></td>
                </tr>
            </table>
            <table cellspacing="0" style="width: 100%;">
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                    <td style="width: 33%; padding: 8px 0 8px 16px;"><b>SERVER</b></td>
                    <td style="width: 33%; padding: 8px;"><b>VERSION</b></td>
                    <td style="width: 33%; padding: 8px;"><b>DATE</b></td>
                </tr>
                <tr style="height: 2px;">
                    <td></td>
                </tr>
                <#list environmentsData.environments! as environment >
                    <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                        <td colspan="3" style="padding: 8px 0 8px 16px;">
                            <b>ENVIRONMENT: </b><a style="text-decoration:none; color: #0068FF" href="${environment.link}">${environment.name}</a>
                        </td>
                    </tr>
                    <#list environment.systems! as system >
                        <tr style="<#if system?item_parity == 'even'>background:#F5F7FA</#if>">
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 16px;">
                                <#if system.name?has_content>
                                    ${system.name}
                                <#else>
                                    —
                                </#if>
                            </td>
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                                <#if system.version?has_content>
                                    ${system.version}
                                <#else>
                                    —
                                </#if>
                            </td>
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                                <#if system.date?has_content>
                                    ${system.date}
                                <#else>
                                    —
                                </#if>
                            </td>
                        </tr>
                    </#list>
                </#list>
            </table>
        </td>
    </tr>
</table>

<table style="width: 100%">
    <tr>
        <td style="padding: 24px 0;">
            <table cellspacing="0" style="width: 100%;">
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                    <td rowspan="2" style="padding: 8px 0 8px 16px;"><b>EXECUTION REQUEST NAME</b></td>
                    <td rowspan="2" style="padding: 8px;"><b>TEST CASE COUNT</b></td>
                    <#list executionRequestsData.statuses! as status >
                        <td colspan="2" style="padding: 8px; text-align: center"><b>${status}</b></td>
                    </#list>
                </tr>
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                    <#list executionRequestsData.statuses! as status >
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                            <b>COUNT</b>
                        </td>
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                            <b>%</b>
                        </td>
                    </#list>
                </tr>
                <#list executionRequestsData.executionRequestCounts! as erRun >
                    <tr style="<#if erRun?item_parity == 'even'>background:#F5F7FA</#if>">
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 16px;">
                            <a style="text-decoration:none; color: #0068FF" href="${erRun.link}">${erRun.name}
                        </td>
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                            ${erRun.testCaseCount}
                        </td>
                        <#list erRun.statusCounts! as statusCount >
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                                <#if statusCount.count?has_content>
                                    ${statusCount.count}
                                <#else>
                                    —
                                </#if>
                            </td>
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                                <#if statusCount.percent?has_content>
                                    ${statusCount.percent}
                                <#else>
                                    —
                                </#if>
                            </td>
                        </#list>
                    </tr>
                </#list>
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                    <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                        <b>RESULT</b>
                    </td>
                    <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                        <b>${executionRequestsData.testCaseTotalCount}</b>
                    </td>
                    <#list executionRequestsData.totalStatusCounts! as statusCount >
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                            <b>
                                <#if statusCount.count?has_content>
                                    ${statusCount.count}
                                <#else>
                                    —
                                </#if>
                            </b>
                        </td>
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                            <b>
                                <#if statusCount.percent?has_content>
                                    ${statusCount.percent}
                                <#else>
                                    —
                                </#if>
                            </b>
                        </td>
                    </#list>
                </tr>
            </table>
        </td>
    </tr>
</table>

<table style="width: 100%">
    <tr>
        <td style="padding: 24px 0;">
            <table cellspacing="0" style="width: 100%;">
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                    <td style="width: 16.6%; padding: 8px 0 8px 16px;"><b>STATUS</b></td>
                    <td style="width: 16.6%; padding: 8px;"><b>TEST CASE</b></td>
                    <td style="width: 16.6%; padding: 8px;"><b>TIME</b></td>
                    <td style="width: 16.6%; padding: 8px;"><b>FIRST FAILED STEP</b></td>
                    <td style="width: 16.6%; padding: 8px;"><b>FAILURE REASON</b></td>
                    <td style="width: 16.6%; padding: 8px;"><b>COMMENT</b></td>
                </tr>
                <tr style="height: 2px;">
                    <td></td>
                </tr>
                <#list executionRequestsData.executionRequestCounts! as executionRequest >
                    <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                        <td colspan="6" style="padding: 8px 0 8px 16px;">
                            <a style="text-decoration:none; color: #0068FF" href="${executionRequest.link}">${executionRequest.name}</a>
                        </td>
                    </tr>
                    <#list executionRequest.testCases! as testCase >
                        <tr style="<#if testCase?item_parity == 'even'>background:#F5F7FA</#if>">
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 16px;">
                                <#if testCase.status?has_content>
                                    <span style="font-weight: bold; color: ${testCase.status?switch(
                                        'PASSED', '#00bb5b',
                                        'FAILED', '#ff5260',
                                        'WARNING', '#ffb02e',
                                        'STOPPED', '#9000b5',
                                        'SKIPPED', '#8f9eb4',
                                        'NOT STARTED', '#353c4e',
                                        'BLOCKED', '#710606',
                                        '#626d82')}
                                    ">
                                        ${testCase.status}
                                    </span>
                                <#else>
                                    —
                                </#if>
                            </td>
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                                <#if testCase.name?has_content>
                                    ${testCase.name}
                                <#else>
                                    —
                                </#if>
                            </td>
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                                <#if testCase.duration?has_content>
                                    ${testCase.duration}
                                <#else>
                                    —
                                </#if>
                            </td>
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                                <#if testCase.firstFailedStepName?has_content>
                                    ${testCase.firstFailedStepName}
                                <#else>
                                    —
                                </#if>
                            </td>
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                                <#if testCase.failureReason?has_content>
                                    ${testCase.failureReason}
                                <#else>
                                    —
                                </#if>
                            </td>
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 8px;">
                                <#if testCase.comment?has_content>
                                    ${testCase.comment}
                                <#else>
                                    —
                                </#if>
                            </td>
                        </tr>
                    </#list>
                </#list>
            </table>
        </td>
    </tr>
</table>