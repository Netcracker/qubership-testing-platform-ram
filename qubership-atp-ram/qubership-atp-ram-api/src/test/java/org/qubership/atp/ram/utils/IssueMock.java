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

package org.qubership.atp.ram.utils;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.DefectPriority;
import org.qubership.atp.ram.models.FailPattern;
import org.qubership.atp.ram.models.Issue;
import org.qubership.atp.ram.models.JiraTicket;
import org.qubership.atp.ram.models.LogRecord;

import lombok.experimental.UtilityClass;

@UtilityClass
public class IssueMock {

    public static final String FIRST_STACKTRACE = """
            <pre>org.qubership.automation.wd.shell.errors.exception.WDIllegalArgumentException: Page of CHROME#0 > GWT:Login{} not found in the chain Error: not found
            Element: Page of CHROME#0 > GWT:Login{}
            wdShell versions: Core [1.3.22], GWTElements [1.3.22], ROEElements [1.3.22], TUIElements [1.3.22], CBCElements [1.3.22], PortalElements-2.0-SNAPSHOT.jar [please use the latest version]
            at org.qubership.automation.wd.shell.errors.exception.WDPreconditions.checkArgument(WDPreconditions.java:28)
            at org.qubership.automation.wd.shell.elements.common.Login.isEditable(Login.java:122)
            at org.qubership.automation.wd.shell.elements.common.Login.clear(Login.java:97)
            at org.qubership.automation.wd.shell.elements.common.Login.setValue(Login.java:73)
            at org.qubership.automation.keyworddriven.wdactions.NavigationActions.login(NavigationActions.java:44)
            at org.qubership.automation.tool.actions.qdl.PageActions.login(PageActions.java:75)
            at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
            at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
            at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
            at java.lang.reflect.Method.invoke(Method.java:498)
            at org.qubership.automation.cerm.invocation.java.JavaInvoker.invoke(JavaInvoker.java:87)
            at org.qubership.automation.cerm.invocation.java.ReflectMethod.invoke(ReflectMethod.java:140)
            at org.qubership.automation.cerm.runner.model.TestActionRunner2.execute(TestActionRunner2.java:114)
            at org.qubership.automation.cerm.runner.model.AbstractRunner.run(AbstractRunner.java:492)
            at org.qubership.automation.cerm.runner.model.RunnerController.runChildRunner(RunnerController.java:193)
            at org.qubership.automation.cerm.runner.model.TestContainerRunner.execute(TestContainerRunner.java:107)
            at org.qubership.automation.cerm.runner.model.AbstractRunner.run(AbstractRunner.java:492)
            at org.qubership.automation.cerm.runner.model.FunctionalRunner.execute(FunctionalRunner.java:65)
            at org.qubership.automation.cerm.runner.model.AbstractRunner.run(AbstractRunner.java:492)
            at org.qubership.automation.cerm.runner.model.RunnerPool$RunnerTask.call(RunnerPool.java:445)
            at org.qubership.automation.cerm.runner.model.RunnerPool$RunnerTask.call(RunnerPool.java:311)
            at java.util.concurrent.FutureTask.run(FutureTask.java:266)
            at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.access$201(ScheduledThreadPoolExecutor.java:180)
            at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:293)
            at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
            at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
            at java.lang.Thread.run(Thread.java:748)
            </pre>""";

    public static final String SECOND_STACKTRACE = """
            javax.servlet.ServletException: Something bad happened
            
                at com.example.myproject.OpenSessionInViewFilter.doFilter(OpenSessionInViewFilter.java:60)
            
                at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1157)
            
                at com.example.myproject.ExceptionHandlerFilter.doFilter(ExceptionHandlerFilter.java:28)
            
                at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1157)
            
                at com.example.myproject.OutputBufferFilter.doFilter(OutputBufferFilter.java:33)
            
                at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1157)
            
                at org.mortbay.jetty.servlet.ServletHandler.handle(ServletHandler.java:388)
            
                at org.mortbay.jetty.security.SecurityHandler.handle(SecurityHandler.java:216)
            
                at org.mortbay.jetty.servlet.SessionHandler.handle(SessionHandler.java:182)
            
                at org.mortbay.jetty.handler.ContextHandler.handle(ContextHandler.java:765)
            
                at org.mortbay.jetty.webapp.WebAppContext.handle(WebAppContext.java:418)
            
                at org.""";

    public static final String FIRST_RULE =
            "(org.qubership.automation.wd.shell.errors.exception.WDIllegalArgumentException)";

    public static final String SECOND_RULE =
            "(org.qubership.automation.wd.shell.errors.exception)";

    public static final String PATTERN_MESSAGE =
            "(org.qubership.automation.wd.shell.errors.exception.WDIllegalArgumentException)";

    public static final String JIRA_TICKET = "https://service-address/browse/SOMEPROJECT-98765";

    public static LogRecord logRecord(String message, UUID testRunId) {
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        logRecord.setTestingStatus(TestingStatuses.FAILED);
        logRecord.setTestRunId(testRunId);
        logRecord.setMessage(message);
        logRecord.setStartDate(new Timestamp(System.currentTimeMillis() - Short.MAX_VALUE));
        logRecord.setEndDate(new Timestamp(System.currentTimeMillis()));
        return logRecord;
    }

    public static FailPattern failPattern(String rule) {
        FailPattern failPattern = new FailPattern();
        failPattern.setUuid(UUID.randomUUID());
        failPattern.setMessage(PATTERN_MESSAGE);
        failPattern.setRule(rule);
        failPattern.setFailReasonId(UUID.randomUUID());
        failPattern.setJiraTickets(Collections.singletonList(JIRA_TICKET));
        failPattern.setPriority(DefectPriority.CRITICAL);
        return failPattern;
    }

    public static Issue generateIssue(String name, UUID testRunId, JiraTicket... jiraDefects) {
        Issue issue = new Issue();
        issue.setUuid(UUID.randomUUID());
        issue.setName(name);
        issue.setFailedTestRunIds(Collections.singletonList(testRunId));
        issue.setJiraDefects(Arrays.asList(jiraDefects));

        return issue;
    }
}
