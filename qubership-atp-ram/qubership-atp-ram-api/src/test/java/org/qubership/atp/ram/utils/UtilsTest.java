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

public class UtilsTest {
    private final static String source = "<?xml version=\"\"1.0\"\" encoding=\"\"UTF-8\"\"?><pre>{<br>  \"" +
            "\"subscriberId\"\" : \"\"3248607287\"\",<br>  \"\"numberOfBuckets\"\" : 7,<br>  " +
            "\"\"balanceInquiryBuckets\"\" : [ {<br>    \"\"bucketName\"\" : \"\"BUNDLE:B25 - National and EU calls\"\"," +
            "<br>    \"\"periodNum\"\" : 1659072097,<br>    \"\"bucketId\"\" : 1152,<br>    \"\"remainingBalance\"\" : 21600<br>  }, " +
            "{<br>    \"\"bucketName\"\" : \"\"BUNDLE:B25 - National and EU data\"\",<br>    \"\"periodNum\"\" : 1659072097," +
            "<br>    \"\"bucketId\"\" : 1153,<br>    \"\"remainingBalance\"\" : 3221225472<br>  }, {<br>    " +
            "\"\"bucketName\"\" : \"\"RoamingDataUsageMonitor_TaxInc\"\",<br>    \"\"bucketId\"\" : 0,<br>    \"\"periodNum\"\" : 1659072097," +
            "<br>    \"\"remainingBalance\"\" : 6050000000<br>  }, {<br>    \"\"bucketName\"\" : \"\"PremiumUsageCostMonitor\"\"," +
            "<br>    \"\"bucketId\"\" : 2,<br>    \"\"periodNum\"\" : 1659072098,<br>    \"\"remainingBalance\"\" : 1000000000000000000<br>  }, " +
            "{<br>    \"\"bucketName\"\" : \"\"GameUsageMonitor\"\",<br>    \"\"bucketId\"\" : 4,<br>    \"\"periodNum\"\" : 1659072098," +
            "<br>    \"\"remainingBalance\"\" : 1000000000000000000<br>  }, {<br>    \"\"bucketName\"\" : \"\"OutofBucketMonitor\"\"," +
            "<br>    \"\"bucketId\"\" : 3,<br>    \"\"periodNum\"\" : 1659072098,<br>    \"\"remainingBalance\"\" : 1000000000000000000<br>  }, " +
            "{<br>    \"\"bucketName\"\" : \"\"EVENT COST COUNTER\"\",<br>    \"\"bucketId\"\" : -1,<br>    \"\"periodNum\"\" : 1," +
            "<br>    \"\"remainingBalance\"\" : 500000000000000<br>  } ]<br>}</pre>";

    @Test
    public void cleanHtmlTags_WithHtmlString_RemoveTags() {
        String expectedString = "{ \"\"subscriberId\"\" : \"\"3248607287\"\", \"\"numberOfBuckets\"\" : 7, " +
                "\"\"balanceInquiryBuckets\"\" : [ { \"\"bucketName\"\" : \"\"BUNDLE:B25 - National and EU calls\"\", " +
                "\"\"periodNum\"\" : 1659072097, \"\"bucketId\"\" : 1152, \"\"remainingBalance\"\" : 21600 }, " +
                "{ \"\"bucketName\"\" : \"\"BUNDLE:B25 - National and EU data\"\", \"\"periodNum\"\" : 1659072097, " +
                "\"\"bucketId\"\" : 1153, \"\"remainingBalance\"\" : 3221225472 }, " +
                "{ \"\"bucketName\"\" : \"\"RoamingDataUsageMonitor_TaxInc\"\", \"\"bucketId\"\" : 0, " +
                "\"\"periodNum\"\" : 1659072097, \"\"remainingBalance\"\" : 6050000000 }, " +
                "{ \"\"bucketName\"\" : \"\"PremiumUsageCostMonitor\"\", \"\"bucketId\"\" : 2, \"" +
                "\"periodNum\"\" : 1659072098, \"\"remainingBalance\"\" : 1000000000000000000 }, " +
                "{ \"\"bucketName\"\" : \"\"GameUsageMonitor\"\", \"\"bucketId\"\" : 4, " +
                "\"\"periodNum\"\" : 1659072098, \"\"remainingBalance\"\" : 1000000000000000000 }, " +
                "{ \"\"bucketName\"\" : \"\"OutofBucketMonitor\"\", \"\"bucketId\"\" : 3, " +
                "\"\"periodNum\"\" : 1659072098, \"\"remainingBalance\"\" : 1000000000000000000 }, " +
                "{ \"\"bucketName\"\" : \"\"EVENT COST COUNTER\"\", \"\"bucketId\"\" : -1, \"\"periodNum\"\" : 1, " +
                "\"\"remainingBalance\"\" : 500000000000000 } ] }";

        String result = Utils.cleanXmlTags(source);

        Assertions.assertEquals(expectedString, result);
    }
}
