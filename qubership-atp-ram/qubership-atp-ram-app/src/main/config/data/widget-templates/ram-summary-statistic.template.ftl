<table style="width: 100%">
    <tr>
        <td style="padding: 24px 0;">
            <#--  header  -->
            <table>
                <tr>
                    <td style="font-family: sans-serif; font-size: 15px; line-height: 20px;"><b>Summary Statistic</b></td>
                </tr>
                <tr>
                    <td style="height: 10px;"></td>
                </tr>
            </table>

            <#--  body  -->
            <#if model??>
                <table style="width: 100%">
                    <#if model[0]??>
                        <tr style="
                            font-family: sans-serif;
                            font-size: 11px;
                            color: #8F9EB4;
                            line-height: 15px;
                            text-align: left;
                            background-color: #EDF1F5;
                        ">
                            <#if model[0].name??>
                                <td style="font-weight: bold; padding: 8px 0 8px 16px;">ID</td>
                            </#if>

                            <#if model[0].testRunCount??>
                                <td style="font-weight: bold; padding: 8px 0 8px 16px;">TC COUNT</td>
                            </#if>

                            <#if model[0].passedRate??>
                                <td style="font-weight: bold; padding: 8px 8px 8px 16px;">GENERAL PASS RATE STATUS</td>
                            </#if>
                        </tr>
                    </#if>

                    <@tableRow data=model lvl=1></@tableRow>
                </table>
            <#else>
                <table style="
                    padding: 10px 0 0 0;
                    font-family: sans-serif;
                ">
                    <tr>
                        <td>
                            Sorry, something went wrong.
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

<#--  row component   -->
<#macro tableRow data lvl>
    <#list data! as row>
        <#if row.nodeType=='LABEL_TEMPLATE_NODE'>
            <tr style="
                font-family: sans-serif;
                font-size: 13px;
                line-height: 15px;
                text-align: left;
            <#if row.isEven?? && row.isEven == true>background:#F5F7FA</#if>
                    ">
                <#if row.name??>
                    <td style="
                        padding: 8px 0 8px ${16 * lvl}px;
                        font-weight: bold;
                    ">${row.name}</td>
                </#if>

                <#if row.testRunCount??>
                    <td style="padding: 8px 0 8px 16px;">${row.testRunCount}</td>
                </#if>


                <#if row.passedRate??>
                    <td style="
                        color: <#if row.passedRate gt 70>#00BB5B <#elseif row.passedRate gt 30>#FFB02E <#else>#FF5260</#if>;
                        font-size: 11px;
                        font-weight: 700;
                        padding: 8px 8px 8px 16px;
                        vertical-align: top;
                    ">${row.passedRate}%</td>
                </#if>
            </tr>

            <#if row.children??>
                <@tableRow data=row.children lvl=lvl+1></@tableRow>
            </#if>
        </#if>
    </#list>
</#macro>
