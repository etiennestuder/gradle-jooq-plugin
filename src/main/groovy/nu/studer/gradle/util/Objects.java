/**
 Copyright 2014 Etienne Studer

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package nu.studer.gradle.util;

import groovy.lang.Closure;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Utility class.
 */
public final class Objects {

    /**
     * Applies the given closure to the given delegate.
     *
     * @param closure  the closure to apply
     * @param delegate the delegate that the closure is applied to
     */
    public static void applyClosureToDelegate(Closure closure, Object delegate) {
        Closure copy = (Closure) closure.clone();
        copy.setResolveStrategy(Closure.DELEGATE_FIRST);
        copy.setDelegate(delegate);
        if (copy.getMaximumNumberOfParameters() == 0) {
            copy.call();
        } else {
            copy.call(delegate);
        }
    }

    /**
     * Calculates a hash code for the given object by traversing recursively into each field that does not contain
     * an {@link Object#hashCode()} implementation. {@link Collection}s and {@link Map }s are always traversed recursively.
     *
     * @param element the element for which to create the deep hash code
     * @return the deep hash code
     */
    public static int deepHashCode(final Object element) {
        if (element == null) {
            return 0;
        }

        LinkedList<Object> stack = new LinkedList<Object>();
        stack.addFirst(element);

        int result = 1;

        while (!stack.isEmpty()) {
            Object current = stack.removeFirst();
            if (current == null) {
                result = calculateHashCode(null, result);
                continue;
            }

            Class<?> currentClass = current.getClass();

            if (currentClass.isArray()) {
                stack.addFirst(currentClass.getName());
                for (int i = 0, len = Array.getLength(current); i < len; i++) {
                    stack.addFirst(Array.get(current, i));
                }
                continue;
            }

            if (current instanceof List) {
                stack.addFirst(currentClass.getName());
                stack.addAll(0, (List<?>) current);
                continue;
            }

            if (current instanceof SortedSet) {
                stack.addFirst(currentClass.getName());
                stack.addAll(0, (SortedSet<?>) current);
                continue;
            }

            if (current instanceof Collection) {
                stack.addFirst(currentClass.getName());
                @SuppressWarnings("unchecked") List<Object> sortedByDeepHashCode = sort((Collection<Object>) current, DeepHashCodeComparator.INSTANCE);
                stack.addAll(0, sortedByDeepHashCode);
                continue;
            }

            if (current instanceof SortedMap) {
                stack.addFirst(currentClass.getName());
                stack.addAll(0, ((Map<?, ?>) current).keySet());
                stack.addAll(0, ((Map<?, ?>) current).values());
                continue;
            }

            if (current instanceof Map) {
                stack.addFirst(currentClass.getName());
                SortedMap<Object, Object> sortedByDeepHashCode = new TreeMap<Object, Object>(DeepHashCodeComparator.INSTANCE);
                sortedByDeepHashCode.putAll((Map<?, ?>) current);
                stack.addAll(0, sortedByDeepHashCode.keySet());
                stack.addAll(0, sortedByDeepHashCode.values());
                continue;
            }

            if (hasCustomHashCode(currentClass)) {
                result = calculateHashCode(currentClass.getName(), result);
                result = calculateHashCode(current, result);
                continue;
            }

            Collection<Field> fields = getInstanceFields(currentClass);
            for (Field field : fields) {
                stack.addFirst(getFieldValue(field, current));
            }
        }

        return result;
    }

    private static Collection<Field> getInstanceFields(Class clazz) {
        Collection<Field> fieldsInHierarchy = new ArrayList<Field>();
        Class current = clazz;
        while (current != null) {
            List<Field> instanceFields = new ArrayList<Field>();
            for (Field field : current.getDeclaredFields()) {
                if (isInstanceField(field)) {
                    instanceFields.add(field);
                }
            }
            List<Field> sortedInstanceFields = sort(instanceFields, FieldNameComparator.INSTANCE);
            fieldsInHierarchy.addAll(sortedInstanceFields);
            current = current.getSuperclass();
        }
        return fieldsInHierarchy;
    }

    private static boolean isInstanceField(Field field) {
        int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers) && !field.getName().startsWith("this$");
    }

    private static Object getFieldValue(Field field, Object object) {
        makeFieldAccessibleIfNeeded(field);
        try {
            return field.get(object);
        } catch (IllegalAccessException shouldNeverHappen) {
            throw new RuntimeException(shouldNeverHappen);
        }
    }

    private static void makeFieldAccessibleIfNeeded(Field field) {
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    private static boolean hasCustomHashCode(Class<?> c) {
        try {
            Class<?> declaringClass = c.getMethod("hashCode").getDeclaringClass();
            return !declaringClass.equals(Object.class);
        } catch (NoSuchMethodException shouldNeverHappen) {
            throw new RuntimeException(shouldNeverHappen);
        }
    }

    private static int calculateHashCode(Object element, int currentResult) {
        return 31 * currentResult + (element == null ? 0 : element.hashCode());
    }

    private static <T> List<T> sort(Collection<? extends T> current, Comparator<T> comparator) {
        List<T> result = new ArrayList<T>(current);
        Collections.sort(result, comparator);
        return result;
    }

    private static final class FieldNameComparator implements Comparator<Field> {

        private static final Comparator<Field> INSTANCE = new FieldNameComparator();

        @Override
        public int compare(Field o1, Field o2) {
            String n1 = o1.getName();
            String n2 = o2.getName();
            return n1.compareTo(n2);
        }

    }

    private static final class DeepHashCodeComparator implements Comparator<Object> {

        private static final Comparator<Object> INSTANCE = new DeepHashCodeComparator();

        @Override
        public int compare(Object o1, Object o2) {
            int h1 = deepHashCode(o1);
            int h2 = deepHashCode(o2);
            return h1 < h2 ? -1 : h1 == h2 ? 0 : 1;
        }

    }

}
