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

package org.qubership.atp.ram.controllers.api;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.reflect.ClassPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsistentControllerApiUnitTest {

    final static List<String> controllersPackage = Arrays.asList("org.qubership.atp.ram.service.rest.server.mongo",
            "org.qubership.atp.ram.service.rest.server.executor", "org.qubership.atp.ram.service.rest.server.charts",
            "org.qubership.atp.ram.service.rest.server.mail", "org.qubership.atp.ram.service.rest.server",
            "org.qubership.atp.ram.tsg.controller", "org.qubership.atp.ram.logging.controllers");
    final static String apiPackage = "org.qubership.atp.ram.controllers.api";

    final static Map<Class, List<String>> ctrlMethodsToSkip = new HashMap<>();

    static {
        {
            // put exclusions here if necessary Class-name = List-of-method-name
        }
    }

    @Test
    public void testInterface() {
        Set<Class> apis = findAllClassesInPackage(apiPackage);
        apis.forEach(aClass -> checkController(aClass));
    }

    private void checkController(Class apiClass) {
        String classNameWithApi = apiClass.getSimpleName();
        List<Class<?>> controllersList =
                controllersPackage.stream().map(s -> findControllerByClassName(s, removeApiPostfix(classNameWithApi)))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        if (controllersList.isEmpty()) {
            return;
        }

        if (controllersList.size() > 1) {
            log.error("There are two controllers for api {}", apiClass.getCanonicalName());
            throw new RuntimeException("There are two controllers for api " + apiClass.getCanonicalName());
        }

        List<Method> listOfControllerMethods = Arrays.asList(controllersList.get(0).getDeclaredMethods())
                .stream().filter(method -> isRestMethod(method))
                .collect(Collectors.toList());

        listOfControllerMethods.stream().filter(method -> Modifier.isPublic(method.getModifiers()))
                .forEach(method -> checkMethodInController(method, apiClass));
    }

    private boolean isRestMethod(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PatchMapping.class);
    }

    private void checkMethodInController(Method ctrlMethod, Class<?> apiClass) {
        List<Method> listOfApiMethods = Arrays.asList(apiClass.getDeclaredMethods());
        List<Method> apiMethods =
                listOfApiMethods.stream().filter(apiMethod -> apiMethod.getName().equals(ctrlMethod.getName()))
                        .collect(Collectors.toList());
        if (apiMethods.isEmpty() && (ctrlMethodsToSkip.get(apiClass) == null
                || !ctrlMethodsToSkip.get(apiClass).contains(ctrlMethod.getName()))) {
            // checking for new method in controller
            throw new RuntimeException(
                    "Found new or not excluded method '" + ctrlMethod.getName() + "' for class " + apiClass);
        }
    }

    private String removeApiPostfix(String classNameWithApi) {
        return classNameWithApi.replaceFirst("Api$", "");
    }

    private Class<?> findControllerByClassName(String packagePath, String className) {
        String fullClassName = packagePath + "." + className;
        try {
            return this.getClass().getClassLoader().loadClass(fullClassName);
        } catch (ClassNotFoundException e) {
            log.error("class {} not found in package {}", fullClassName, packagePath);
            return null;
        }
    }

    @SneakyThrows
    public Set<Class> findAllClassesInPackage(String packageName) {
        return ClassPath.from(this.getClass().getClassLoader())
                .getAllClasses()
                .stream()
                .filter(clazz -> clazz.getPackageName().equalsIgnoreCase(packageName))
                .filter(clazz -> clazz.getSimpleName().matches("^.*Api$"))
                .map(clazz -> clazz.load())
                .collect(Collectors.toSet());
    }
}
