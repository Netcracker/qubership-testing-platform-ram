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

package org.qubership.atp.ram.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.ram.model.FailPatternCheckRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class FailPatternServiceTest {
    private FailPatternService failPatternService;

    @Test
    public void checkCorrectPatternPassedResult() {
        failPatternService = mock(FailPatternService.class);
        String pattern = ".*Hazelcast cache check(\\n|.)+?not found(\\n|.)+?";
        String message = """
                Situation 'Hazelcast cache check': incoming message validation is failed (Compare Result is 'MODIFIED')
                 Details:\s
                HazelcastAR MODIFIED
                ER	AR
                8 !=null
                       \s
                8 not found
                       \s
                org.qubership.automation.itf.core.integration.EngineIntegrationException: Situation 'Hazelcast cache check': incoming message validation is failed (Compare Result is 'MODIFIED')
                 Details:\s
                HazelcastAR MODIFIED
                ER	AR
                8 !=null
                       \s
                8 not found
                       \s
                
                	at org.qubership.automation.itf.core.instance.situation.SituationExecutor.validationInIntegration(SituationExecutor.java:591)
                	at org.qubership.automation.itf.core.instance.situation.SituationExecutor.messageValidation(SituationExecutor.java:584)
                	at org.qubership.automation.itf.core.instance.situation.SituationExecutor.executeInstanceStep(SituationExecutor.java:304)
                	at org.qubership.automation.itf.core.instance.situation.SituationExecutor.executeInstance(SituationExecutor.java:84)
                	at org.qubership.automation.itf.core.instance.situation.SituationExecutor.execute(SituationExecutor.java:632)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.postSituationStep(NextCallChainSubscriber.java:188)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.executeStep(NextCallChainSubscriber.java:282)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.executeNext(NextCallChainSubscriber.java:226)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.executeNext(NextCallChainSubscriber.java:228)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.onEvent(NextCallChainSubscriber.java:136)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.onEvent(NextCallChainSubscriber.java:44)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.AbstractChainSubscriber.handleEvent(AbstractChainSubscriber.java:30)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.handle(NextCallChainSubscriber.java:69)
                	at sun.reflect.GeneratedMethodAccessor412.invoke(Unknown Source)
                	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                	at java.lang.reflect.Method.invoke(Method.java:498)
                	at com.google.common.eventbus.Subscriber.invokeSubscriberMethod(Subscriber.java:91)""";
        FailPatternCheckRequest failPatternCheckRequest = new FailPatternCheckRequest();
        failPatternCheckRequest.setRule(pattern);
        failPatternCheckRequest.setMessage(message);
        failPatternService.check(failPatternCheckRequest);
        when(failPatternService.check(any())).thenCallRealMethod();
        HttpStatus httpStatus = failPatternService.check(failPatternCheckRequest);
        Assertions.assertEquals(HttpStatus.OK, httpStatus);
    }

    @Test
    public void checkIncorrectPatternPassedResult() {
        failPatternService = mock(FailPatternService.class);
        String pattern = ".*Hazelcast cache check(\\n|.)*not found(\\n|.)*";
        String message = """
                Situation 'Hazelcast cache check': incoming message validation is failed (Compare Result is 'MODIFIED')
                 Details:\s
                HazelcastAR MODIFIED
                ER	AR
                8 !=null
                       \s
                8 not found
                       \s
                org.qubership.automation.itf.core.integration.EngineIntegrationException: Situation 'Hazelcast cache check': incoming message validation is failed (Compare Result is 'MODIFIED')
                 Details:\s
                HazelcastAR MODIFIED
                ER	AR
                8 !=null
                       \s
                8 not found
                       \s
                
                	at org.qubership.automation.itf.core.instance.situation.SituationExecutor.validationInIntegration(SituationExecutor.java:591)
                	at org.qubership.automation.itf.core.instance.situation.SituationExecutor.messageValidation(SituationExecutor.java:584)
                	at org.qubership.automation.itf.core.instance.situation.SituationExecutor.executeInstanceStep(SituationExecutor.java:304)
                	at org.qubership.automation.itf.core.instance.situation.SituationExecutor.executeInstance(SituationExecutor.java:84)
                	at org.qubership.automation.itf.core.instance.situation.SituationExecutor.execute(SituationExecutor.java:632)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.postSituationStep(NextCallChainSubscriber.java:188)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.executeStep(NextCallChainSubscriber.java:282)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.executeNext(NextCallChainSubscriber.java:226)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.executeNext(NextCallChainSubscriber.java:228)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.onEvent(NextCallChainSubscriber.java:136)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.onEvent(NextCallChainSubscriber.java:44)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.AbstractChainSubscriber.handleEvent(AbstractChainSubscriber.java:30)
                	at org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber.handle(NextCallChainSubscriber.java:69)
                	at sun.reflect.GeneratedMethodAccessor412.invoke(Unknown Source)
                	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                	at java.lang.reflect.Method.invoke(Method.java:498)
                	at com.google.common.eventbus.Subscriber.invokeSubscriberMethod(Subscriber.java:91)""";
        FailPatternCheckRequest failPatternCheckRequest = new FailPatternCheckRequest();
        failPatternCheckRequest.setRule(pattern);
        failPatternCheckRequest.setMessage(message);
        failPatternService.check(failPatternCheckRequest);
        when(failPatternService.check(any())).thenCallRealMethod();
        HttpStatus httpStatus = failPatternService.check(failPatternCheckRequest);
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpStatus);
    }

}
