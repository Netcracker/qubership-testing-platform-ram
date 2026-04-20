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

package org.qubership.atp.ram.service.mail;

public interface MailConstants {

    String HEADER_ER_REPORT = "<html dir=\"ltr\"><head>\n"
            + "<style>\n"
            + "<!--\n"
            + ".Investigation\n"
            + "\t{background-color:#836FFF}\n"
            + ".Error\n"
            + "\t{background-color:#836FFF}\n"
            + ".Environment\n"
            + "\t{background-color:#F4A460}\n"
            + ".EnvironmentIssue\n"
            + "\t{background-color:#F4A460}\n"
            + ".AT\n"
            + "\t{background-color:#87CEFA}\n"
            + ".Performance\n"
            + "\t{background-color:#FFFF00}\n"
            + ".Design\n"
            + "\t{background-color:#32CD32}\n"
            + ".Solution\n"
            + "\t{background-color:#FF0000}\n"
            + ".summaryParameters\n"
            + "\t{padding:0px 4px 0px 4px; font-size:10pt;}\n"
            + ".statusPadding\n"
            + "\t{padding-top:4px;padding-bottom:4px;}\n"
            + "-->\n"
            + "</style><style>\n"
            + "<!--\n"
            + ".product_row\n"
            + "\t{display:block}\n"
            + ".root_causes td\n"
            + "\t{padding: 2px}\n"
            + "-->\n"
            + "</style>\n"
            + "<style type=\"text/css\" id=\"owaParaStyle\"></style><style type=\"text/css\" id=\"owaTempEditStyle\">"
            + "</style></head>\n"
            + "<body fpstyle=\"1\" ocsi=\"1\" style=\"font-family:Calibri, Helvetica, sans-serif\">\n"
            + "\n"
            + "<h4><b>Execution Date: </b>${START_DATE}</h4>\n"
            + "<h4><b>ER Link: </b>${RAM_ER_LINK}</h4>\n"
            //+ "<h4><b>Compare with: </b>${COMPARE_LINK}<br></h4>\n"
            + "${CI_JOB_URL}";

    String ROOT_CAUSES_STATISTICS = """
            <h3>Root Causes Statistics</h3>
            <table class='root_causes' border='1' cellpadding='2' cellspacing='0' style='background:#F6FFF2'>
            <body>
            <tr style='background:#E3FFE6'>\
            <td rowspan='2' style='width:15px'><b>#</b></td>
            <td rowspan='2' style='width:auto'><b>Start Date</b></td>
            <td rowspan='2' style='width:auto'><b>Execution Request</b></td>
            ${HEADER_ROOT_CAUSE}
            </tr>
            <tr>
            <td style='width:15px'>1</td>
            <td style='width:auto'>${START_DATE}</td>
            <td style='width:auto'>${ER_NAME}</td>
            ${TD_WITH_COUNT_AND_RATE}
            </tr>
            <tr
            >\
            ${TD_WITH_PREV_COUNT_AND_RATE}
            </tr>
            </body>
            </table><br>
            """;

    String SUMMARY_TABLE_TEMPLATE = """
            <h3>Summary</h3>
            <div style="border:solid; border-width:1px; border-color:#474747">
            <table class="MsoNormalTable" border="0" cellspacing="0" cellpadding="0" width="100%" \
            style="width:100.0%; background:#F6FFF2">
            <tbody>
            <tr style="background:#E3FFE6">
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">\
            Execution Status:</span>
            </b></p>
            </td>
            <td class="summaryParameters statusPadding">
            <p class="MsoNormal"><span style="font-size:12pt;"><span id="fullExecStatus" \
            style="font-size:16px; border:1px solid black;\
             background:${EXECUTION_RESULT_COLOR}; color:black; display:inline-block; font-size:16px">&nbsp;&zwnj;\
            &nbsp;&zwnj;&nbsp;\
            &zwnj;&nbsp;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&nbsp;&zwnj;&nbsp;</span>&nbsp;\
            ${EXECUTION_RESULT}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Start Time:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${START_DATE}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenarios:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${TR_COUNT}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${ACTIONS_COUNT}
            </span></p>
            </td>
            </tr>
            <tr style="">
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Server:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${QA_HOST}</a>
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">End time:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${FINISH_DATE}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenarios passed:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${TR_PASSED}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions passed:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${ACTION_PASSED}
            </span></p>
            </td>
            </tr>
            <tr style="background:#E3FFE6">
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Build:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${SOLUTION_BUILD}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Duration:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${DURATION}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenarios failed:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${TR_FAILED}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions failed:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${ACTION_FAILED}
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
            <p class="MsoNormal"><span>${TR_WARNING}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions with warnings:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${ACTION_WARNING}
            </span></p>
            </td>
            </tr>
            <tr style="background:#E3FFE6">
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenario's failed rate:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${FAIL_RATE_FORMULA}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenario's passed rate:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${PASS_RATE_FORMULA}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenario's with errors:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${TR_ERROR}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions with errors:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${ACTION_ERROR}
            </span></p>
            </td>
            </tr>
            <tr style="">
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenario error's rate:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${ERROR_RATE_FORMULA}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Scenario warning's rate:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${WARN_RATE_FORMULA}
            </span></p>
            </td>
            <td style="padding:0cm 0cm 0cm 0cm; min-width:50px"></td>
            <td style="padding:0cm 0cm 0cm 0cm; min-width:50px"></td>
            <td style="padding:0cm 0cm 0cm 0cm">
            <p class="MsoNormal"><b><span style="font-size:12pt;">Actions failed rate:</span>
            </b></p>
            </td>
            <td class="summaryParameters">
            <p class="MsoNormal"><span>${ACTION_FAIL_RATE}
            </span></p>
            </td>
            </tr>
            </tbody>
            </table>
            </div>
            
            """;

    String TR_TABLE_TEMPLATE = """
            <h3>Scenarios </h3>
            <table class="MsoNormalTable" border="1" cellspacing="0" cellpadding="0" width="100%" \
            style="width:100.0%; background:#F6FFF2; border:outset black 1.0pt">
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
            ${TEST_RUNS_ROWS}
            </tbody>
            </table>
            
            
            </body></html>
            """;
}
