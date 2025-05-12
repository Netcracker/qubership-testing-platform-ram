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

package org.qubership.atp.ram.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UrlParamsUtilsTest {

    @Test
    public void decodeUrlPath_WhenSetPathWithPlus_ShouldReturnValidValue() {
        String val = "POT_%5BDigital%5D%5BPROJECT-MODIFY%5D%20Add%20non-recurrent%20addon%20through"
                + "%20Assisted%20Channel%20for%20PAYG%20plan.docx";
        String actualVal = UrlParamsUtils.decodeUrlPath(val);
        String expectedVal = "POT_[Digital][PROJECT-MODIFY] Add non-recurrent addon through Assisted "
                + "Channel for PAYG plan.docx";
        Assertions.assertEquals(expectedVal, actualVal, "Value should be valid decoding");
    }
}
