<table style="width: 100%;">
    <tr>
        <td style="padding: 24px 0; width: 800px;">
            <table>
                <tr>
                    <td style="font-family: sans-serif; font-size: 15px; line-height: 20px;"><b>Environments Info</b></td>
                    <td style="padding-left: 16px">
                        <#if status??>
                            <table>
                                <tr>
                                    <td style="font-family: sans-serif; background-color: <#if statusBgColor??>${statusBgColor}<#else>#8F9EB4</#if>; color: white; border-radius: 4px; padding: 3px 16px; font-size: 10px;"><b>${status}</b></td>
                                </tr>
                            </table>
                        </#if>
                    </td>
                </tr>
            </table>
            <table>
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 16px;">
                    <td style="width: 200px; padding: 16px 0 4px 0;"><b>ENVIRONMENT</b></td>
                    <td style="width: 200px; padding: 16px 0 4px 0;"><b>TA TOOL GROUP</b></td>
                    <td style="width: 200px; padding: 16px 0 4px 0;"><b>START DATE</b></td>
                    <td style="width: 200px; padding: 16px 0 4px 0;"><b>END DATE</b></td>
                    <td style="width: 200px; padding: 16px 0 4px 0;"><b>DURATION</b></td>
                    <td style="width: 200px; padding: 16px 0 4px 0;"><b>LINKS TO SSM METRICS</b></td>
                    <td style="width: 200px; padding: 16px 0 4px 0;"><b>HEALTHCHECK RESULTS</b></td>
                </tr>
                <tr style="font-family: sans-serif; font-size: 13px; line-height: 18px;">
                    <td style="width: 200px;">
                        <#if environmentLink??>
                            <a href="${environmentLink}">${environmentName}</a>
                        </#if>
                    </td>
                    <td style="width: 200px;">
                        <#if toolGroupLink??>
                            <a href="${toolGroupLink}">${toolGroupName}</a>
                        </#if>
                    </td>
                    <td style="width: 200px;">${startDate!}</td>
                    <td style="width: 200px;">${endDate!}</td>
                    <td style="width: 200px;">${duration!}</td>
                    <td style="width: 200px;">
                        <#if ssmMetricsMicroservicesReportLink??>
                            <a href="${ssmMetricsMicroservicesReportLink}">${ssmMetricsMicroservicesReportName}</a>
                        </#if>
                        <br>
                        <#if ssmMetricsProblemContextReportLink??>
                            <a href="${ssmMetricsProblemContextReportLink}">${ssmMetricsProblemContextReportName}</a>
                        </#if>
                    </td>
                    <td style="width: 200px;">
                        <#if mandatoryChecksReportLink??>
                            <a href="${mandatoryChecksReportLink}">${mandatoryChecksReportName}</a>
                        </#if>
                    </td>
                </tr>
            </table>
            <table>
                <tr>
                    <td style="font-family: sans-serif; font-size: 13px; line-height: 18px; padding-top: 20px;"><b>QA Environments</b></td>
                </tr>
            </table>
            <table style="width: 100%;">
                <tr style="height: 16px;">
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                </tr>
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                    <td style="width: 20%; padding: 8px 0 8px 16px;"><b>NAME</b></td>
                    <td style="width: 20%; padding: 8px 0;"><b>STATUS</b></td>
                    <td style="width: 20%; padding: 8px 0;"><b>MONITORING SYSTEM</b></td>
                    <td style="width: 20%; padding: 8px 0;"><b>VERSION</b></td>
                    <td style="width: 20%; padding: 8px 0;"><b>URL</b></td>
                </tr>
                <tr style="height: 8px;">
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                </tr>
                <#list qaSystemInfoList! as list>
                    <tr style="
                            font-family: sans-serif;
                            font-size: 9pt;
                            line-height: 18px;
                    <#if list?item_parity == 'even'>background:#F5F7FA</#if>
                            ">
                        <td style="padding: 6px 0 6px 16px">
                            <#if list.nameUrl?? && list.name??>
                                <a href="${list.nameUrl}">${list.name}</a>
                            <#else>
                                ${list.name!}
                            </#if>
                        </td>
                        <td style="font-size: 9pt; line-height: 15px; padding: 6px 0; color: <#if list.statusColor??>${list.statusColor}<#else>#8F9EB4</#if>;"><b>${list.status!}</b></td>
                        <td style="padding: 6px 0;">${list.monitoringSystem!}</td>
                        <td style="padding: 6px 0;">${list.version!}</td>
                        <td style="padding: 6px 0;">
                            <#if list.urls??>
                                <table>
                                    <#list list.urls as url>
                                        <tr>
                                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px;"><a href="${url}">${url}</a></td>
                                        </tr>
                                    </#list>
                                </table>
                            </#if>
                        </td>
                    </tr>
                </#list>
            </table>
            <table>
                <tr>
                    <td style="font-family: sans-serif; font-size: 13px; line-height: 18px; padding-top: 20px;"><b>TA Tools</b></td>
                </tr>
            </table>
            <table style="width: 100%;">
                <tr style="height: 16px;">
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                </tr>
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                    <td style="width: 25%; padding: 8px 0 8px 16px;"><b>NAME</b></td>
                    <td style="width: 25%; padding: 8px 0;"><b>STATUS</b></td>
                    <td style="width: 25%; padding: 8px 0;"><b>VERSION</b></td>
                    <td style="width: 25%; padding: 8px 0;"><b>URL</b></td>
                </tr>
                <tr style="height: 8px;">
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                </tr>
                <#list taSystemInfoList! as list>
                    <tr style="
                            font-family: sans-serif;
                            font-size: 9pt;
                            line-height: 18px;
                    <#if list?item_parity == 'even'>background:#F5F7FA</#if>
                            ">
                        <td style="padding: 6px 0 6px 16px">
                            <#if list.nameUrl?? && list.name??>
                                <a href="${list.nameUrl}">${list.name}</a>
                            <#else>
                                ${list.name!}
                            </#if>
                        </td>
                        <td style="font-size: 9pt; line-height: 15px; padding: 6px 0; color: <#if list.statusColor??>${list.statusColor}<#else>#8F9EB4</#if>;"><b>${list.status!}</b></td>
                        <td style="padding: 6px 0;">${list.version!}</td>
                        <td style="padding: 6px 0;">
                            <#if list.urls??>
                                <table>
                                    <#list list.urls as url>
                                        <tr>
                                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px;"><a href="${url}">${url}</a></td>
                                        </tr>
                                    </#list>
                                </table>
                            </#if>
                        </td>
                    </tr>
                </#list>
            </table>
            <#if toolsInfo??>
                <table>
                    <tr>
                        <td style="font-family: sans-serif; font-size: 13px; line-height: 18px; padding-top: 20px;"><b>Tools Info</b></td>
                    </tr>
                </table>
                <table>
                    <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 16px;">
                        <td style="width: 200px; padding: 16px 0 4px 0;"><b>DEALER</b></td>
                        <td style="width: 200px; padding: 16px 0 4px 0;"><b>TOOL</b></td>
                    </tr>
                    <tr style="font-family: sans-serif; font-size: 13px; line-height: 18px;">
                        <td style="width: 200px;">
                            <#if toolsInfo.dealerLogsUrl?? && toolsInfo.dealer??>
                                <a href="${toolsInfo.dealerLogsUrl}">${toolsInfo.dealer}</a>
                            <#else>
                                ${toolsInfo.dealer!}
                            </#if>
                        </td>
                        <td style="width: 200px;">
                            <#if toolsInfo.toolLogsUrl?? && toolsInfo.tool??>
                                <a href="${toolsInfo.toolLogsUrl}">${toolsInfo.tool}</a>
                            <#else>
                                ${toolsInfo.tool!}
                            </#if>
                        </td>
                    </tr>
                </table>
                <table>
                    <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 16px;">
                        <td style="width: 200px; padding: 20px 0 4px 0;"><b>SELENOID</b></td>
                        <td style="width: 200px; padding: 20px 0 4px 0;"><b>SESSION ID</b></td>
                    </tr>
                    <tr style="font-family: sans-serif; font-size: 13px; line-height: 18px;">
                        <td style="width: 200px;">
                            <#if toolsInfo.selenoidLogsUrl?? && toolsInfo.selenoid??>
                                <a href="${toolsInfo.selenoidLogsUrl}">${toolsInfo.selenoid}</a>
                            <#else>
                                ${toolsInfo.selenoid!}
                            </#if>
                        </td>
                        <td style="width: 200px;">
                            <#if toolsInfo.sessionLogsUrl?? && toolsInfo.sessionId??>
                                <a href="${toolsInfo.sessionLogsUrl}">${toolsInfo.sessionId}</a>
                            <#else>
                                ${toolsInfo.sessionId!}
                            </#if>
                        </td>
                    </tr>
                </table>
                <#list wdShellTables! as table>
                    <table>
                        <tr>
                            <#list table.partition! as wdShell>
                                <td style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 16px; width: 200px; padding: 20px 0 4px 0;"><b>${wdShell.name!}</b></td>
                            </#list>
                        </tr>
                        <tr>
                            <#list table.partition! as wdShell>
                                <td style="font-family: sans-serif; font-size: 13px; line-height: 18px; width: 200px;">${wdShell.version!}</td>
                            </#list>
                        </tr>
                    </table>
                </#list>
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
