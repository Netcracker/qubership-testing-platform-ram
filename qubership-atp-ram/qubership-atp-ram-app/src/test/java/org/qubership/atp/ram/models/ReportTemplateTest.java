/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
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

package org.qubership.atp.ram.models;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReportTemplateTest {

    private static final String RECIPIENTS_OBJECT_PATH = "recipients[0].<list element>";
    private static final String SUBJECT_OBJECT_PATH = "subject";
    private static final List<String> CORRECT_EMAILS = asList("user.name_1@some-domain.com", "USER_2@some-domain.com");

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private ReportTemplate reportTemplate;

    @BeforeAll
    public static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @BeforeEach
    public void setUp() {
        reportTemplate = new ReportTemplate();
        reportTemplate.setSubject("Daily Report");
    }

    @AfterAll
    public static void close() {
        validatorFactory.close();
    }

    @Test
    public void reportTemplate_testValidEmails_noViolationsExpected() {
        // given
        reportTemplate.setRecipients(CORRECT_EMAILS);

        // when
        Set<ConstraintViolation<ReportTemplate>> violations = validator.validate(reportTemplate);

        // then
        Assertions.assertTrue(violations.isEmpty());
    }

    @Test
    public void reportTemplate_testInvalidEmailWithoutDogSymbol_violationExpected() {
        // given
        final String emailWithoutDogSymbol = "user.name_1gmail.com";
        reportTemplate.setRecipients(Collections.singletonList(emailWithoutDogSymbol));

        // when
        Set<ConstraintViolation<ReportTemplate>> violations = validator.validate(reportTemplate);

        Iterator<ConstraintViolation<ReportTemplate>> violationIterator = violations.iterator();

        // then
        Assertions.assertTrue(violationIterator.hasNext());
        ConstraintViolation<ReportTemplate> violation = violationIterator.next();

        Assertions.assertEquals(RECIPIENTS_OBJECT_PATH, violation.getPropertyPath().toString());
        Assertions.assertEquals(emailWithoutDogSymbol, violation.getInvalidValue());
        Assertions.assertFalse(violationIterator.hasNext());
    }

    @Test
    public void reportTemplate_testNullSubject_violationExpected() {
        // given
        reportTemplate.setSubject(null); // should be not blank
        reportTemplate.setRecipients(CORRECT_EMAILS);

        // when
        Set<ConstraintViolation<ReportTemplate>> violations = validator.validate(reportTemplate);

        Iterator<ConstraintViolation<ReportTemplate>> violationIterator = violations.iterator();

        // then
        Assertions.assertTrue(violationIterator.hasNext());
        ConstraintViolation<ReportTemplate> violation = violationIterator.next();
        Assertions.assertEquals(SUBJECT_OBJECT_PATH, violation.getPropertyPath().toString());
        Assertions.assertFalse(violationIterator.hasNext());
    }
}
