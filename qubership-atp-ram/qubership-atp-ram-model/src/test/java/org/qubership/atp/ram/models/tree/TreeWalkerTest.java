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

package org.qubership.atp.ram.models.tree;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.Data;
import lombok.NoArgsConstructor;

public class TreeWalkerTest {

    private Row a;
    private Row b;
    private Row c;
    private Row d;
    private Row e;
    private Row f;
    private Row g;
    private Row h;

    private TreeWalker<Row> treeWalker;

    @BeforeEach
    public void setUp() {
        a = new Row("A");
        b = new Row("B");
        c = new Row("C");
        d = new Row("D");
        e = new Row("E");
        f = new Row("F");
        g = new Row("G");
        h = new Row("H");
        b.setRows(new ArrayList<>(asList(c)));
        e.setRows(new ArrayList<>(asList(f)));
        d.setRows(new ArrayList<>(asList(e, g)));
        a.setRows(new ArrayList<>(asList(b, d, h)));

        treeWalker = new TreeWalker<>();
    }

    /*
     * A
     * |--B
     * |  `--C
     * |
     * |--D
     * |  |--E
     * |  |  `--F
     * |  |
     * |  `--G
     * |
     * |--H
     */
    @Test
    public void walkOrderTest() {
        String expectedResult = a.name + b.name + c.name + d.name + e.name + f.name + g.name + h.name;
        StringBuilder actualResult = new StringBuilder();

        treeWalker.walkWithPreProcess(a, Row::getRows, (root, row) -> {
            actualResult.append(row.name);
        });

        Assertions.assertEquals(expectedResult, actualResult.toString());
    }

    @Data
    @NoArgsConstructor
    private static class Row {
        String name;
        List<Row> rows;

        public Row(String name) {
            this.name = name;
        }
    }
}
