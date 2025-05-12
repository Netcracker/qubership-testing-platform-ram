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

package org.qubership.atp.ram.comparators;

import java.util.Comparator;
import java.util.List;

public class ComplexListComparator<T extends Comparable<? super T>> implements Comparator<List<T>> {

    private final Comparator<? super T> itemsComparator;

    public ComplexListComparator(Comparator<? super T> itemsComparator) {
        this.itemsComparator = itemsComparator;
    }

    @Override
    public int compare(List<T> first, List<T> second) {
        if (first.size() != second.size()) {
            return first.size() - second.size();
        }
        for (int i = 0; i < first.size(); i++) {
            int iteratorComparisonResult = itemsComparator.compare(first.get(i), second.get(i));
            if (iteratorComparisonResult != 0) {
                return iteratorComparisonResult;
            }
        }
        return 0;
    }
}