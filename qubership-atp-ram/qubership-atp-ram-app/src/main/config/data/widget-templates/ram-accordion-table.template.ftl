<table style="width: 100%">
    <tr>
        <td style="padding: 24px 0;">
            <#--  header  -->
            <table>
                <tr>
                    <td style="font-family: sans-serif; font-size: 15px; line-height: 20px;">
                        <#if widgetUrl??>
                            <b>
                                <a href="${widgetUrl}">${title!}</a>
                                <#if filtered?? && filtered == true>
                                    <@filterIcon></@filterIcon>
                                </#if>
                            </b>
                        <#else>
                            <b>
                                ${title!}
                                <#if filtered?? && filtered == true>
                                    <@filterIcon></@filterIcon>
                                </#if>
                            </b>
                        </#if>
                    </td>
                </tr>
                <tr>
                    <td style="height: 10px;"></td>
                </tr>
            </table>

            <#--  body  -->
            <#if tableModel??>
                <table style="width: 100%">
                    <tr style="
                        font-family: sans-serif;
                        font-size: 11px;
                        color: #8F9EB4;
                        line-height: 15px;
                        text-align: left;
                        background-color: #EDF1F5;
                    ">
                        <#if tableModel.header??>
                            <#list tableModel.header.columns! as column>
                                <#assign paddingRight = column?is_last?then('8px', '0')>
                                <td style="
                                    font-weight: bold;
                                    white-space: nowrap; <#-- not work in windows outlook app -->
                                    padding: 8px ${paddingRight} 8px 16px;
                                ">
                                    ${column.text!}
                                </td>
                            </#list>
                        </#if>
                    </tr>

                    <#if tableModel.body?? && tableModel.body.rows??>
                        <@tableRows rows=tableModel.body.rows lvl=1></@tableRows>
                    </#if>
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
<#macro tableRows rows lvl>
    <#list rows! as row>
        <tr style="
            font-family: sans-serif;
            font-size: 9pt;
            line-height: 15px;
            text-align: left;
        <#if row.even?? && row.even == true>background:#F5F7FA</#if>">
            <#list row.columns! as column>
                <#assign paddingLeft = column?is_first?then('${16 * lvl}px', '16px')>
                <#assign paddingRight = column?is_last?then('8px', '0')>
                <#assign fontWeight = column.fontWeight?has_content?then('${column.fontWeight}', '400')>
                <#assign color = column.color?has_content?then('${column.color}', 'black')>
                <#assign backgroundColor = column.backgroundColor?has_content?then('${column.backgroundColor}', '')>
                <#assign alternativeBackgroundColor = column.alternativeBackgroundColor?has_content?then('${column.alternativeBackgroundColor}', '')>

                <#if column.type?? && column.type == 'link'>
                    <td style="
                        padding: 8px ${paddingRight} 8px ${paddingLeft};
                        vertical-align: top;
                    ">
                        <#if column.url?? && column.text??>
                            <a href="${column.url}">${column.text}</a>
                        <#else>
                            ${column.text!}
                        </#if>
                    </td>
                <#elseif column.type?? && column.type == 'linkList'>
                    <td style="
                        padding: 8px ${paddingRight} 8px ${paddingLeft};
                        vertical-align: top;
                    ">
                        <table style="
                            font-family: sans-serif;
                            font-size: 9pt;
                            line-height: 15px;
                            text-align: left;
                        ">
                            <#list column.links! as link>
                                <tr>
                                    <td style="padding: 0 0 6px 0;">
                                        <#if link.url?? && link.text??>
                                            <a href="${link.url}">${link.text}</a>
                                        <#else>
                                            ${link.text!}
                                        </#if>
                                    </td>
                                </tr>
                            </#list>
                        </table>
                    </td>
                <#elseif column.type?? && column.type == 'labelList'>
                    <td style="
                        padding: 8px ${paddingRight} 8px ${paddingLeft};
                        vertical-align: top;
                    ">
                        <table style="
                            font-family: sans-serif;
                            font-size: 9pt;
                            line-height: 15px;
                            text-align: left;
                        ">
                            <#list column.labels! as label>
                                <tr>
                                    <td style="padding: 0 0 6px 0;">
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
                                    </td>
                                </tr>
                            </#list>
                        </table>
                    </td>
                <#else>
                    <td style="
                        font-weight: ${fontWeight};
                        color: ${color};
                        padding: 8px ${paddingRight} 8px ${paddingLeft};
                        vertical-align: top;
                    <#if row.even?? && row.even == true>
                        background-color: ${backgroundColor};
                    <#else>
                        background-color: ${alternativeBackgroundColor};
                    </#if>
                    ">
                        ${column.text!} <span style="font-weight: normal; color: #8F9EB4">${column.textSuffix!}</span>
                    </td>
                </#if>

            </#list>
        </tr>

        <#if row.children??>
            <@tableRows rows=row.children lvl=lvl+1></@tableRows>
        </#if>
    </#list>
</#macro>

<#macro filterIcon>
    <img alt="" style="vertical-align: text-top" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA8AAAAPCAYAAAA71pVKAAAAAXNSR0IArs4c6QAAAJBJREFUOE/VkssRgCAMRDedWIp2oje7ULvITUqhE+lEJzLyGQFxPJkZbnm7SxLCh6KTHfcGQP9CR4HJhPAKoK0Q0GDqpM/C3n2rgDsw6Ri2AjOAqSCwgEl6zvLO3j0X38VNw+X4Lm4eTseP4j7Bsror/i1uGbbuAsvuZadDaojxwMKO38JyqvIMmNS7P1fc6QEA6jQQAetmjwAAAABJRU5ErkJggg==" />
</#macro>

<#--summary statistic
title = "Summary Statistic"
description = "Simple Description"
tableModel = {
    "header": {
        "columns": [
            {"text": "ID"},
            {"text": "TC COUNT"},
            {"text": "GENERAL PASS RATE"},
            {"text": "BPP"},
            {"text": "SSP"},
            {"text": "BPP + SSP"},
            {"text": "Resolution"},
            {"text": "MLG"}
        ]
    },
    "body": {
        "rows": [
            {
                "columns": [
                    {"fontWeight": "bold", "text": "Data National"},
                    {"text": "5"},
                    {"fontWeight": "bold", "color": "#00BB5B", "text": "75%"},
                    {"fontWeight": "bold", "color": "#FFB02E", "text": "50%"},
                    {"fontWeight": "bold", "color": "#FF5260", "text": "25%"},
                    {"fontWeight": "bold", "color": "#00BB5B", "text": "75%"},
                    {"fontWeight": "bold", "color": "#FFB02E", "text": "50%"},
                    {"fontWeight": "bold", "color": "#FF5260", "text": "25%"}
                ],
                "children": [
                    {
                        "columns": [
                            {"fontWeight": "bold", "text": "Data National"},
                            {"text": "5"},
                            {"fontWeight": "bold", "color": "#00BB5B", "text": "75%"},
                            {"fontWeight": "bold", "color": "#FFB02E", "text": "50%"},
                            {"fontWeight": "bold", "color": "#FF5260", "text": "25%"},
                            {"fontWeight": "bold", "color": "#00BB5B", "text": "75%"},
                            {"fontWeight": "bold", "color": "#FFB02E", "text": "50%"},
                            {"fontWeight": "bold", "color": "#FF5260", "text": "25%"}
                        ]
                    }
                ]
            }
        ]
    }
} -->

<#--test cases
title = "Test Cases"
description = "Simple Description"
tableModel = {
    "header": {
        "columns": [
            {"text": "NAME"},
            {"text": "STATUS"},
            {"text": "PASSED RATE"},
            {"text": "ISSUE"},
            {"text": "DURATION"},
            {"text": "FAILURE REASON"},
            {"text": "FAILED STEP"},
            {"text": "DATA SET URL"}
        ]
    },
    "body": {
        "rows": [
            {
                "columns": [
                    {"fontWeight": "bold", "text": "Data National"},
                    {"fontWeight": "bold", "color": "#FF5260", "text": "FAILED"},
                    {"fontWeight": "bold", "color": "#FFB02E", "text": "50%"},
                    {"text": "—"},
                    {"text": "00:01:20"},
                    {"text": "Error"},
                    {"text": "—"},
                    {"text": "—"}
                ],
                "children": [
                    {
                        "columns": [
                            {"type": "link", "url": "https://google.com", "text": "_ [Scope_Grouping] Test Case E Test"},
                            {"fontWeight": "bold", "color": "#00BB5B", "text": "PASSED"},
                            {"fontWeight": "bold", "color": "#FFB02E", "text": "50%"},
                            {"text": "issue1"},
                            {"text": "00:01:20"},
                            {"text": "Error"},
                            {"type": "linkList", "links": [
                                {"url": "https://google.com", "text": "Login as \"User\" with password \"Password\""},
                                {"url": "https://google.com", "text": "Navigate to Main Page"}
                            ]},
                            {"type": "link", "url": "https://google.com", "text": "Test Data Set"}
                        ]
                    }
                ]
            }
        ]
    }
} -->
