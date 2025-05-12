<table style="width: 100%">
    <tr>
        <td style="padding: 24px 0;">
            <table>
                <tr>
                    <td style="font-family: sans-serif; font-size: 15px; line-height: 20px;"><b>Root Causes Statistics</b></td>
                </tr>
                <tr style="height: 12px;">
                    <td></td>
                </tr>
            </table>
            <table style="width: 100%;">
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                    <td style="padding: 8px 0 8px 16px;"><b>#</b></td>
                    <td style="padding: 8px 0 8px 8px;"><b>START DATE</b></td>
                    <td style="padding: 8px 0 8px 8px;"><b>EXECUTION REQUEST</b></td>
                    <#if statistics?? && statistics[0]??>
                        <#list statistics[0].rootCausesGroups! as column>
                            <td style="padding: 8px;"><b>${column.rootCauseName!?upper_case}</b></td>
                        </#list>
                    </#if>
                </tr>
                <#list statistics! as statistic>
                    <tr style="<#if statistic?item_parity == 'even'>background:#F5F7FA</#if>">
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px; padding: 3px 0 8px 16px;">
                            ${statistic?index + 1}
                        </td>
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px; padding: 8px 0 8px 8px;">
                            ${statistic.startDate!}
                        </td>
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px; padding: 8px 0 8px 8px;">
                            ${statistic.executionRequestName!}
                        </td>
                        <#list statistic.rootCausesGroups! as column>
                            <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px; padding: 8px">
                                ${column.count!} (${column.percent!}%)
                            </td>
                        </#list>
                    </tr>
                </#list>
            </table>
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
