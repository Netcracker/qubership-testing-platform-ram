<table>
    <tr>
        <td style="padding: 24px 0">
            <!-- title -->
            <table>
                <tr>
                    <td style="font-family: sans-serif; font-size: 15px; line-height: 20px;"><b>Test Cases</b></td>
                </tr>
                <tr style="height: 10px"></tr>
            </table>

            <!-- table -->
            <#if model??>
                <table style="width: 100%">
                    <tr style="
                        font-family: sans-serif;
                        font-size: 11px;
                        color: #8F9EB4;
                        line-height: 15px;
                        text-align: left;
                        background-color: #EDF1F5;
                    ">
                        <td style="
                            padding: 6px;
                            font-weight: 700;
                        ">NAME</td>

                        <td style="
                            padding: 6px;
                            font-weight: 700;
                        ">STATUS</td>

                        <#if model.children?? && model.children[0]?? && model.children[0].passedRate??>
                            <td style="
                                padding: 6px;
                                font-weight: 700;
                            ">PASSED RATE</td>
                        </#if>

                        <td style="
                            padding: 6px;
                            font-weight: 700;
                        ">ISSUE</td>

                        <td style="
                            padding: 6px;
                            font-weight: 700;
                        ">DURATION</td>

                        <td style="
                            padding: 6px;
                            font-weight: 700;
                        ">FAILURE REASON</td>

                        <td style="
                            padding: 6px;
                            font-weight: 700;
                        ">FAILED STEP</td>

                        <td style="
                            padding: 6px;
                            font-weight: 700;
                        ">LABELS</td>

                        <td style="
                            padding: 6px;
                            font-weight: 700;
                        ">DATA SET URL</td>
                    </tr>

                    <#list model.children! as lables>
                        <@row data=lables lvl=1></@row>
                    </#list>

                    <#list model.testRuns! as testRun>
                        <@row data=testRun lvl=1></@row>
                    </#list>

                </table>
            <#else>
                <table style="
                    padding: 10px 0 0 0;
                    font-family: sans-serif;
                ">
                    <tr>
                        <td>Sorry, something went wrong.</td>
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


<#macro row data lvl>
    <tr style="
        font-family: sans-serif;
        font-size: 13px;
    <#if data.isEven?? && data.isEven == true>background:#F5F7FA</#if>
    ">
        <#if data.name??>
            <@nameTestCase name=data.name url=data.url lvl=lvl></@nameTestCase>
        </#if>
        <#if data.labelName??>
            <@nameLabel name=data.labelName lvl=lvl></@nameLabel>
        </#if>


        <#if data.testingStatus??>
            <@status data=data.testingStatus></@status>
        </#if>
        <#if data.status??>
            <@status data=data.status></@status>
        </#if>

        <#if data.passedRate?? && model.children?? && model.children[0]?? && model.children[0].passedRate?? && data.labelName??>
            <@passedRate data=data.passedRate></@passedRate>
        <#elseif model.children?? && model.children[0]?? && model.children[0].passedRate??><td></td>
        <#else><td></td>
        </#if>

        <#if data.issue??>
            <@issue data=data.issue></@issue>
        <#else><td></td>
        </#if>

        <#if data.duration??>
            <@duration data=data.duration></@duration>
        <#else><td></td>
        </#if>

        <#if data.failureReason??>
            <@failureReason data=data.failureReason></@failureReason>
        <#else><td></td>
        </#if>

        <#if data.failedStep??>
            <@failedStep data=data.failedStep></@failedStep>
        <#else><td></td>
        </#if>

        <#if data.labels??>
            <@labels data=data.labels></@labels>
        <#else><td></td>
        </#if>

        <#if data.dataSetUrl?? && data.dataSetName??>
            <@dataSetUrl name=data.dataSetName url=data.dataSetUrl></@dataSetUrl>
        <#else><td></td>
        </#if>
    </tr>

    <#list data.children! as child>
        <@row data=child lvl=lvl+1></@row>
    </#list>

    <#list data.testRuns! as testRun>
        <@row data=testRun lvl=lvl+1></@row>
    </#list>
</#macro>

<#--  name cell  -->
<#macro nameLabel name lvl>
    <td style="
        padding: 6px 6px 6px ${lvl*16}px;
        vertical-align: top;
        font-weight: bold;
    ">${name}</td>
</#macro>
<#macro nameTestCase name url lvl>
    <td style="
        padding: 6px 6px 6px ${lvl*16}px;
        vertical-align: top;
    ">
        <a href="${url}">
            ${name}
        </a>
    </td>
</#macro>

<#--  status cell  -->
<#macro status data>
    <td style="
        color: <#if data.color??>${data.color}<#else>#8F9EB4</#if>;
        font-size: 11px;
        font-weight: 700;
        padding: 6px;
        vertical-align: top;
    ">${data.status}</td>
</#macro>

<#--  passedRate cell  -->
<#macro passedRate data>
    <#if data??>
        <td style="
            color: <#if data gt 70>#00BB5B <#elseif data gt 30>#FFB02E <#else>#FF5260</#if>;
            font-size: 11px;
            font-weight: 700;
            padding: 6px;
            vertical-align: top;
        ">${data}%</td>
    </#if>
</#macro>

<#--  issue cell  -->
<#macro issue data>
    <td style="
        padding: 6px;
        vertical-align: top;
    ">${data}</td>
</#macro>

<#--  duration cell  -->
<#macro duration data>
    <td style="
        padding: 6px;
        vertical-align: top;
    ">${data}</td>
</#macro>

<#--  failureReason cell  -->
<#macro failureReason data>
    <td style="
        padding: 6px;
        vertical-align: top;
    ">${data}</td>
</#macro>

<#--  failedStep cell  -->
<#macro failedStep data>
    <td style="
        padding: 6px;
        vertical-align: top;
    ">
        <table>
            <#list data! as link>
                <tr>
                    <td style="padding: 0 0 6px 0;">
                        <a href="${link.link}"
                        >${link.name}</a>
                    </td>
                </tr>
            </#list>
        </table>
    </td>
</#macro>

<#--  labels cell  -->
<#macro labels data>
    <td style="
        padding: 6px;
        vertical-align: top;
    ">
        <table>
            <#list data! as label>
                <tr>
                    <td style="padding: 0 0 6px 0;">
                        <span style="
                            padding: 1px 7px;
                            border-radius: 3px;
                            background-color: #d6edff;
                            font-size: 13px;
                        ">
                            ${label.name}
                        </span>
                    </td>
                </tr>
            </#list>
        </table>
    </td>
</#macro>

<#--  dataSetUrl cell  -->
<#macro dataSetUrl name url>
    <td style="
        padding: 6px;
        vertical-align: top;
    ">
        <a href="${url}">${name}</a>
    </td>
</#macro>
