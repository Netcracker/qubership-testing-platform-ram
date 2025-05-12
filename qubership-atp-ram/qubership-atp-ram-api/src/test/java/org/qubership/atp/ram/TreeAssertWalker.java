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

package org.qubership.atp.ram;

import static java.util.Arrays.asList;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TreeAssertWalker<T> {

    private T root;
    private Function<T, List<T>> childrenSupplier;
    private Map<Predicate<T>, List<BiConsumer<T, T>>> asserts = new HashMap<>();

    public TreeAssertWalker(T root,
                            Function<T, List<T>> childrenSupplier) {
        this.root = root;
        this.childrenSupplier = childrenSupplier;
    }

    /**
     * Walk through abstract tree structure with some operation which be applied to each element of the tree.
     *
     * @param root             root tree element
     * @param childrenSupplier method for retrieving potential element children
     */
    public void walk(T root,
                     T child,
                     Function<T, List<T>> childrenSupplier) {
        asserts.forEach((predicate, asserts) -> {
            if (predicate.test(child)) {
                asserts.forEach(anAssert -> anAssert.accept(root, child));
            }
        });
        List<T> children = childrenSupplier.apply(root);
        if (!isEmpty(children)) {
            children.forEach(subChild -> walk(child, subChild, childrenSupplier));
        }
    }

    public void assertAll() {
        asserts.forEach((predicate, asserts) -> {
            if (predicate.test(root)) {
                asserts.forEach(anAssert -> anAssert.accept(null, root));
            }
        });
        List<T> children = childrenSupplier.apply(root);
        if (!isEmpty(children)) {
            children.forEach(child -> walk(root, child, childrenSupplier));
        }
    }


    public TreeAssertWalker<T> setAssert(Predicate<T> nodePredicate, BiConsumer<T, T>... asserts) {
        Arrays.stream(asserts).forEach(anAssert -> this.asserts.put(nodePredicate, asList(asserts)));

        return this;
    }
}
