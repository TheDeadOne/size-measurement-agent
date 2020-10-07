package com.github.thedeadone;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.stream.Stream;
import java.util.stream.Collectors;


public class SizeMeasurementAgent {
    private static volatile Instrumentation instrumentation;
 
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        SizeMeasurementAgent.instrumentation = instrumentation;
    }

    public static long getObjectSize(Object object) {
        if (instrumentation == null) {
            throw new IllegalStateException("Agent not initialized");
        }
        return instrumentation.getObjectSize(object);
    }

    public static synchronized long getCumulativeSize(Object obj) throws IllegalAccessException {
        if (obj == null)
            throw new NullPointerException("Can't measure size of null");

        long size = 0L;

        Set<Object> processed = new HashSet<>();

        Queue<Object> queue = new ArrayDeque<>();
        queue.offer(obj);

        Object item = null;
        while ((item = queue.poll()) != null) {
            size += getObjectSize(item);

            for (Field field : getReferentialFields(item)) {
                field.setAccessible(true);

                Object ref = field.get(item);
                if ((ref != null) && (!processed.contains(ref))) {
                    processed.add(ref);
                    queue.offer(ref);
                }
            }
        }

        return size;
    }

    private static Stream<Field> getAllFields(Object obj) {
        Queue<Class<?>> queue = new ArrayDeque<>();
        Class<?> clazz = obj.getClass();
        queue.offer(clazz);
        while((clazz = clazz.getSuperclass()) != null) {
            queue.offer(clazz);
        }

        return queue.stream()
                    .map(Class::getDeclaredFields)
                    .flatMap(Arrays::stream);
    }

    private static List<Field> getReferentialFields(Object obj) {
        return getAllFields(obj)
            .filter(f -> !f.getType().isPrimitive())
            .collect(Collectors.toList());
    }
}
