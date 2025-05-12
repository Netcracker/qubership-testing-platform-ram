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

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class TreeWalker<T> {

    private void walk(T root,
                      Function<T, List<T>> childrenSupplier,
                      Consumer<T> walkConsumer) {
        List<T> rootChildren = childrenSupplier.apply(root);
        if (!isEmpty(rootChildren)) {
            rootChildren.forEach(walkConsumer);
        }
    }

    /**
     * Walk through abstract tree structure with some operation which be applied to each element of the tree.
     *
     * @param root             root tree element
     * @param childrenSupplier method for retrieving potential element children
     * @param preOperation     element operation function
     */
    public void walkWithPreProcess(T root,
                                   Function<T, List<T>> childrenSupplier,
                                   BiConsumer<T, T> preOperation) {
        preOperation.accept(null, root);
        walk(root, childrenSupplier, child -> walkWithPreProcess(root, child, childrenSupplier, preOperation));
    }

    /**
     * Walk through abstract tree structure with some operation which be applied to each element of the tree.
     *
     * @param root               root tree element
     * @param child              child tree element
     * @param childrenSupplier   method for retrieving potential element children
     * @param callBeforeChildren element operation function
     */
    private void walkWithPreProcess(T root,
                                    T child,
                                    Function<T, List<T>> childrenSupplier,
                                    BiConsumer<T, T> callBeforeChildren) {
        callBeforeChildren.accept(root, child);
        processChildren(child, childrenSupplier,
                subChild -> walkWithPreProcess(child, subChild, childrenSupplier, callBeforeChildren));
    }

    public void walkWithPostProcess(T root,
                                    Function<T, List<T>> childrenSupplier,
                                    BiConsumer<T, T> pastOperation) {
        walk(root, childrenSupplier, child -> walkWithPostProcess(root, child, childrenSupplier, pastOperation));
        pastOperation.accept(null, root);
    }


    private void walkWithPostProcess(T root,
                                     T child,
                                     Function<T, List<T>> childrenSupplier,
                                     BiConsumer<T, T> callAfterChildren) {
        processChildren(child, childrenSupplier,
                subChild -> walkWithPostProcess(child, subChild, childrenSupplier, callAfterChildren));
        callAfterChildren.accept(root, child);
    }

    private void processChildren(T child,
                                 Function<T, List<T>> childrenSupplier,
                                 Consumer<T> callConsumer) {
        List<T> children = childrenSupplier.apply(child);
        if (!isEmpty(children)) {
            children.forEach(callConsumer);
        }
    }
}
