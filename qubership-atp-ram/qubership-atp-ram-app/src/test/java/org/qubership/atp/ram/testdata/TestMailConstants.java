/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.ram.testdata;

public interface TestMailConstants {
    String HEADER_ER_REPORT = """
            <html dir="ltr"><head>
            <style>
            <!--
            .Investigation
            	{background-color:#836FFF}
            .Error
            	{background-color:#836FFF}
            .Environment
            	{background-color:#F4A460}
            .EnvironmentIssue
            	{background-color:#F4A460}
            .AT
            	{background-color:#87CEFA}
            .Performance
            	{background-color:#FFFF00}
            .Design
            	{background-color:#32CD32}
            .Solution
            	{background-color:#FF0000}
            .summaryParameters
            	{padding:0px 4px 0px 4px; font-size:10pt;}
            .statusPadding
            	{padding-top:4px;padding-bottom:4px;}
            -->
            </style><style>
            <!--
            .product_row
            	{display:block}
            .root_causes td
            	{padding: 2px}
            -->
            </style>
            <style type="text/css" id="owaParaStyle"></style><style type="text/css" id="owaTempEditStyle"></style></head>
            <body fpstyle="1" ocsi="1" style="font-family:Calibri, Helvetica, sans-serif">
            
            <h4><b>Execution Date: </b>${DATE}</h4>
            <h4><b>ER Link: </b>\
            <a href='http://home/project/null/ram/execution-request/1f984659-779d-458a-aed4-77ed8d941283'>\
            Er 1f984659-779d-458a-aed4-77ed8d941283</a></h4>
            """;
    //+ "<h4><b>Compare with: </b><a href='http://home/execution-requests/tp1/compare?uuids=er1,er2'>Er "
    //+ "er1</a><br></h4>\n";

    String ROOT_CAUSES_STATISTICS = """
            <h3>Root Causes Statistics</h3>
            <table class='root_causes' border='1' cellpadding='2' cellspacing='0' style='background:#F6FFF2'>
            <body>
            <tr style='background:#E3FFE6'><td rowspan='2' style='width:15px'><b>#</b></td>
            <td rowspan='2' style='width:auto'><b>Start Date</b></td>
            <td rowspan='2' style='width:auto'><b>Execution Request</b></td>
            </tr><tr>
            </tr>
            <tr>
            <td style='width:15px'>1</td>
            <td style='width:auto'>${DATE}</td>
            <td style='width:auto'>Er 1f984659-779d-458a-aed4-77ed8d941283</td>
            
            </tr>
            <tr
            >
            </tr>
            </body>
            </table><br>
            """;

    String SUMMARY_TABLE_TEMPLATE = """
            <h3>Summary</h3>
            <div style="border:solid; border-width:1px; border-color:#474747">
            <table class="MsoNormalTable" border="0" cellspacing="0" cellpadding="0" width="100%" style="width:100.0%; background:#F6FFF2">
            <tbody>
            <tr style="background:#E3FFE6">
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Execution Status:</span>
            </b></p>
            </td>
            <td class="summaryParameters statusPadding">
            <p class="MsoNormal"><span style="font-size:12pt;"><span id="fullExecStatus" \
            style="font-size:16px; border:1px solid black; background:#71FF35; color:black; display:inline-block;\
             font-size:16px">&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&nbsp;&zwnj;&nbsp;</span>&nbsp;Finished
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Start Time:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${DATE}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenarios:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>5
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>10
            </span></p>
            </td>
            </tr>
            <tr style="">
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Server:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span><table></table></a>
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">End time:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${DATE}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenarios passed:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>4
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions passed:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>2
            </span></p>
            </td>
            </tr>
            <tr style="background:#E3FFE6">
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Build:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span><table></table>
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Duration:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>00:00
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenarios failed:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>1
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions failed:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>8
            </span></p>
            </td>
            </tr>
            <tr style="">
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Browser:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span></span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Threads:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>1
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenarios with warnings:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>0
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions with warnings:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>0
            </span></p>
            </td>
            </tr>
            <tr style="background:#E3FFE6">
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenario's failed rate:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>(1/5) * 100% =  20.0%
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenario's passed rate:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>(4/5) * 100% =  80.0%
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenario's with errors:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>0
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions with errors:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>0
            </span></p>
            </td>
            </tr>
            <tr style="">
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenario error's rate:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>(0/5) * 100% =  0.0%
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenario warning's rate:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>(0/5) * 100% =  0.0%
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm; min-width:50px"></td>
            <td style="padding:0cm 0cm 0cm 0cm; min-width:50px"></td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions failed rate:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>(8/10) * 100% =  80.0%
            </span></p>
            </td>
            </tr>
            </tbody>
            </table>
            </div>
            
            """;

    String TR_TABLE_TEMPLATE = """
            <h3>Scenarios </h3>
            <table class="MsoNormalTable" border="1" cellspacing="0" cellpadding="0" width="100%" style="width:100.0%; background:#F6FFF2; border:outset black 1.0pt">
            <tbody>
            <tr style="">
            <td style="border:inset black 1.0pt; padding:1.5pt 1.5pt 1.5pt 1.5pt">
            <p class="MsoNormal"><b><span style="font-size:9.0pt;">Status</span></b>
            </p>
            </td>
            <td style="border:inset black 1.0pt; padding:1.5pt 1.5pt 1.5pt 1.5pt">
            <p class="MsoNormal"><b><span style="font-size:9.0pt;">Scenario</span></b>
            </p>
            </td>
            <td style="border:inset black 1.0pt; padding:1.5pt 1.5pt 1.5pt 1.5pt">
            <p class="MsoNormal"><b><span style="font-size:9.0pt;">Time</span></b>
            </p>
            </td>
            <td style="border:inset black 1.0pt; padding:1.5pt 1.5pt 1.5pt 1.5pt">
            <p class="MsoNormal"><b><span style="font-size:9.0pt;">Causes</span></b>
            </p>
            </td>
            <td style="border:inset black 1.0pt; padding:1.5pt 1.5pt 1.5pt 1.5pt">
            <p class="MsoNormal"><b><span style="font-size:9.0pt;">Failed Steps</span></b>
            </p>
            </td>
            <td style="border:inset black 1.0pt; padding:1.5pt 1.5pt 1.5pt 1.5pt">
            <p class="MsoNormal"><b><span style="font-size:9.0pt;">Additional Information</span></b>
            </p>
            </td>
            </tr>
            <tr><td colspan='6' style="padding: 2px"><h5>Suite Name: null</h5></td></tr><tr><td style="padding: 2px; background:limegreen; color:black">Passed</td>\
            <td style="padding: 2px"><a href='http://home/project/null/ram/execution-request/er1/0'>TR0</a></td\
            ><td style="padding: 2px">00:00</td>\
            <td style="padding: 2px">Not Analyzed</td><td style="padding: 2px"></td><td style="padding: 2px"></td></tr>\
            <tr><td style="padding: 2px; background:limegreen; color:black">Passed</td><td style="padding: \
            2px"><a href='http://home/project/null/ram/execution-request/er1/1'>TR1</a></td>\
            <td style="padding: 2px">00:00</td><td style="padding: 2px">Not Analyzed</td><td style="padding: 2px"></td><td style="padding: 2px"></td></tr>\
            <tr><td style="padding: 2px; background:red; color:black">Failed</td><td style="padding: 2px"><a \
            href='http://home/project/null/ram/execution-request/er1/3'>TR3</a></td>\
            <td style="padding: 2px">00:00</td><td style="padding: 2px">Not Analyzed</td><td style="padding: \
            2px"><b>Step: </b><a href='http://home/project/null/ram/execution-request/er1/3/1'>LR0</a><br><b>Step:\
             </b><a href='http://home/project/null/ram/execution-request/er1/3/1'>LR1</a><br></td>\
            <td style="padding: 2px"></td></tr><tr><td colspan='6' style="padding: 2px"><h5>Suite Name: Execution Request's Logs</h5></td></tr>\
            <tr><td style="padding: 2px; background:limegreen; color:black">Passed</td><td style="padding: \
            2px"><a href='http://home/project/null/ram/execution-request/er1/0'>TR0</a></td>\
            <td style="padding: 2px">00:00</td><td style="padding: 2px">Not Analyzed</td><td style="padding: 2px"></td><td style="padding: 2px"></td></tr>\
            <tr><td style="padding: 2px; background:limegreen; color:black">Passed</td><td style="padding: \
            2px"><a href='http://home/project/null/ram/execution-request/er1/1'>TR1</a></td>\
            <td style="padding: 2px">00:00</td><td style="padding: 2px">Not Analyzed</td><td style="padding: 2px"></td><td style="padding: 2px"></td></tr>
            </tbody>
            </table>
            
            
            </body></html>
            """;

}
