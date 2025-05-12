<table style="width: 100%">
    <tr>
        <td style="padding: 24px 0;">
            <table>
                <tr>
                    <td style="font-family: sans-serif; font-size: 15px; line-height: 20px;"><b>Server Summary</b></td>
                </tr>
                <tr style="height: 12px;">
                    <td></td>
                </tr>
            </table>
            <table style="width: 100%;">
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                    <td style="width: 50%; padding: 8px 0 8px 16px;"><b>SERVER</b></td>
                    <td style="width: 50%; padding: 8px;"><b>BUILD</b></td>
                </tr>
                <tr style="height: 6px;">
                    <td></td>
                    <td></td>
                </tr>
                <#list serverSummary! as row>
                    <tr style="<#if row?item_parity == 'even'>background:#F5F7FA</#if>">
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; vertical-align: top; padding: 8px 0 8px 16px;">
                            <#if row.server?has_content>
                                <a href="${row.server}">${row.server}</a>
                            <#else>
                                —
                            </#if>
                        </td>
                        <td style="padding: 8px 0">
                            <table>
                                <#list row.build! as build>
                                    <#assign paddingBottom = build?is_first?then('0', '6px')>
                                    <tr>
                                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 15px; padding: 0 8px ${paddingBottom} 8px;">${build}</td>
                                    </tr>
                                </#list>
                            </table>
                        </td>
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
