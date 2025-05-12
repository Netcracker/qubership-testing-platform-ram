<table style="width: 100%">
    <tr>
        <td style="padding: 24px 0;">
            <table>
                <tr>
                    <td style="font-family: sans-serif; font-size: 15px; line-height: 20px;"><b>Top Issues</b></td>
                </tr>
                <tr style="height: 12px;"></tr>
            </table>
            <#if topIssuesLink?has_content>
                <table>
                    <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 16px;">
                        <td><b>ER LINK</b></td>
                    </tr>
                    <tr style="height: 4px;"></tr>
                    <tr style="font-family: sans-serif; font-size: 13px; line-height: 18px;">
                        <td>
                            <a href="${topIssuesLink}">Top Issues</a>
                        </td>
                    </tr>
                    <tr style="height: 12px;"></tr>
                </table>
            </#if>
            <table style="width: 100%;">
                <tr style="font-family: sans-serif; font-size: 11px; color: #8F9EB4; line-height: 15px; text-align: left; background-color: #EDF1F5;">
                    <td style="width: 3%; padding: 8px 0 8px 16px; white-space: nowrap;"><b>PR</b></td>
                    <td style="width: 15%; padding: 8px 0 8px 6px; white-space: nowrap;"><b>ISSUE</b></td>
                    <td style="width: 22%; padding: 8px 0 8px 6px; white-space: nowrap;"><b>FAIL PATTERN</b></td>
                    <td style="width: 19%; padding: 8px 0 8px 6px; white-space: nowrap;"><b>FAIL REASON</b></td>
                    <td style="width: 22%; padding: 8px 0 8px 6px; white-space: nowrap;"><b>MESSAGE</b></td>
                    <td style="width: 19%; padding: 8px 0 8px 6px; white-space: nowrap;"><b>COUNT OF FAILED CASES</b></td>
                </tr>
                <tr style="height: 8px;"></tr>
                <#if topIssues?has_content>
                <#list topIssues! as issue>
                    <tr style="<#if issue?item_parity == 'even'>background:#F5F7FA</#if>">
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px; vertical-align: top; padding: 3px 0 3px 16px;">
                            <#switch issue.priority!>
                                <#case "LOW">
                                    <@lowPriorityImage></@lowPriorityImage>
                                    <#break>
                                <#case "NORMAL">
                                    <@normalPriorityImage></@normalPriorityImage>
                                    <#break>
                                <#case "MAJOR">
                                    <@majorPriorityImage></@majorPriorityImage>
                                    <#break>
                                <#case "CRITICAL">
                                    <@criticalPriorityImage></@criticalPriorityImage>
                                    <#break>
                                <#case "BLOCKER">
                                    <@blockerPriorityImage></@blockerPriorityImage>
                                    <#break>
                                <#default>
                                    —
                            </#switch>
                        </td>
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px; vertical-align: top; padding: 3px 0 3px 6px;">
                            <#list issue.tickets! as ticket>
                                <a<#if ticket.url??> href="${ticket.url}"</#if>>${ticket.name}</a>
                            <#else>
                                —
                            </#list>
                        </td>
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px; vertical-align: top; padding: 3px 0 3px 6px;">
                            <#if issue.failPatternUrl?? && issue.failPatternUrl?has_content && issue.failPattern?? && issue.failPattern?has_content>
                                <a href="${issue.failPatternUrl}">${issue.failPattern}</a>
                            <#elseif issue.failPattern?has_content>
                                ${issue.failPattern}
                            <#else>
                                —
                            </#if>
                        </td>
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px; vertical-align: top; padding: 3px 0 3px 6px;">
                            <#if issue.failReason?? && issue.failReason?has_content>
                                ${issue.failReason}
                            <#else>
                                —
                            </#if>
                        </td>
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px; vertical-align: top; padding: 3px 0 3px 6px;">${issue.message!"—"}</td>
                        <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px; vertical-align: top; padding: 3px 0 3px 6px;">${issue.testRunsCount!}</td>
                    </tr>
                </#list>
            </table>
            <#else>
</table>
<table style="width: 100%;">
    <tr>
        <td style="font-family: sans-serif; font-size: 9pt; line-height: 18px; text-align: center;">NO TOP ISSUES</td>
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

<#macro lowPriorityImage>
    <img alt="low" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAwAAAAMCAMAAABhq6zVAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAABIUExURf///7rW/8Pc/1ic//T5/3ev//j7/wBo/wpu/+Pv/zSH/53F/wNq/8/j/zmK/7zX/x15/3+z/xl3/26p/1CX/zeJ/4i5/+z0/74iqEsAAABISURBVAgdBcCHEYQgAATAVcEDw0dD/506sL8B+G+fAqAmXwDll9YBrMkEwJKMAMZkoQOmpL4GQG9Jm4H5askB2Ou5ZQWgDDcPjVYB9EpRhLgAAAAASUVORK5CYII=">
</#macro>

<#macro normalPriorityImage>
    <img alt="normal" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAwAAAAMCAMAAABhq6zVAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAA8UExURQC7W1/Vmez68szx3t/36ozgtdHz4f///43htvv+/c/y4NDy4ajox0TNh+T47ijGdTHIeyHEcFHRj3HZpB3fpa8AAABASURBVAjXjctJEkAwAADBycIIiVj+/1cH5UaVYx8a1dxaVUV1hOEHFpjeMX8gpgpnyoqWA2C/YQnQ1+eUsEXVCxRBA466oBlOAAAAAElFTkSuQmCC">
</#macro>

<#macro majorPriorityImage>
    <img alt="major" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAwAAAAMCAMAAABhq6zVAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAABIUExURf+wLv/47f/Rg/+zNf/Tiv+5Rv/BW//////ryP/89v/v1f+3QP/z4P/szv/25//04v///v/IbP/Ymf+/Vv/luf/grv+7TP/mvLsxEGQAAABHSURBVAgdBcGFAcIAAMCwzsgE1/8/JQkAAsNvfq0E+1LNBLajvgQ+tXQWbEfT9bIK5npCuE0tG4RHvUGsdbqDONUOxDCOgD80nAPZsg/yKQAAAABJRU5ErkJggg==">
</#macro>

<#macro criticalPriorityImage>
    <img alt="critical" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAwAAAAMBAMAAACkW0HUAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAwUExURf9SYP9dav+Ikf////9YZv/R1P/w8f9vev+utf/Y2//6+v99h/+zuf/q7P/Bxv9qdplUfPQAAAAnSURBVAjXYzA2zp5sbMxgbFbgCqLuM7AEA6kXBR6ngJSxgrAx6RQA8qYPs4k7AUwAAAAASUVORK5CYII=">
</#macro>

<#macro blockerPriorityImage>
    <img alt="blocker" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAwAAAAMCAMAAABhq6zVAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAABFUExURf+utf/HzP/5+f+Vnf+/xP9aaP9VY/9SYP////+9w//s7v/z9P9uev+Jkv99iP/Z3P+5v/9jcP9mcv+hqf+gqP/l5//i5Knnoq8AAABRSURBVAgdBcABCsIgAAXQ59cxBxR1/1sOAhSmUToMJwTm9zOhwfpZENSDoyLoyZMLITfcIYxAGcSrAvWttr14SlH2yjnRNuaZDbXDbhkY4PoDTRAXVlV22tYAAAAASUVORK5CYII=">
</#macro>
