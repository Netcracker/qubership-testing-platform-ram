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

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.function.Function;

public class TreeConsoleDrawer<T> {

    private T root;
    private Function<T, List<T>> childrenSupplier;
    private Function<T, String> nameFunc;

    public TreeConsoleDrawer(T root,
                             Function<T, List<T>> childrenSupplier,
                             Function<T, String> nameFunc) {
        this.root = root;
        this.childrenSupplier = childrenSupplier;
        this.nameFunc = nameFunc;
    }

    public void draw() {
        int depth = 1;
        StringBuilder builder = new StringBuilder();
        builder.append(nameFunc.apply(root)).append("\n");
        List<T> rootChildren = childrenSupplier.apply(root);
        if (!isEmpty(rootChildren)) {
            rootChildren.forEach(child -> draw(child, builder, depth));
        }
    }

    public void draw(T child,
                     StringBuilder builder,
                     int depth) {
        int curDepth = depth + 1;
        for (int i = 0; i < depth; i++) {
            builder.append("  ");
        }
        builder.append(nameFunc.apply(child)).append("\n");
        List<T> children = childrenSupplier.apply(child);
        if (!isEmpty(children)) {
            children.forEach(subChild -> draw(subChild, builder, curDepth));
        }
    }
}
