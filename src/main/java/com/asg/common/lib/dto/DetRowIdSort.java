package com.asg.common.lib.dto;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sorts lists of arbitrary DTOs that expose {@code detRowId} via {@code getDetRowId()} or a field named {@code detRowId}.
 */
public final class DetRowIdSort {

    private static final Comparator<Long> DET_ROW_ID_ORDER = Comparator.nullsLast(Long::compareTo);

    private static final ConcurrentHashMap<Class<?>, DetRowIdAccessor> ACCESSORS = new ConcurrentHashMap<>();

    private DetRowIdSort() {
    }

    public static void sortAscending(List<?> list) {
        if (list == null || list.size() <= 1) {
            return;
        }
        list.sort(Comparator.comparing(DetRowIdSort::readDetRowId, DET_ROW_ID_ORDER));
    }

    public static void sortDescending(List<?> list) {
        if (list == null || list.size() <= 1) {
            return;
        }
        list.sort(Comparator.comparing(DetRowIdSort::readDetRowId, DET_ROW_ID_ORDER).reversed());
    }

    private static Long readDetRowId(Object element) {
        if (element == null) {
            return null;
        }
        return ACCESSORS.computeIfAbsent(element.getClass(), DetRowIdSort::resolveAccessor).read(element);
    }

    private static DetRowIdAccessor resolveAccessor(Class<?> clazz) {
        Method getter = findNoArgMethod(clazz, "getDetRowId");
        if (getter != null) {
            getter.setAccessible(true);
            return target -> invokeGetter(getter, target);
        }
        Field field = findDeclaredField(clazz, "detRowId");
        if (field != null) {
            field.setAccessible(true);
            return target -> readField(field, target);
        }
        throw new IllegalStateException("Cannot sort by detRowId: no getDetRowId() or detRowId field on " + clazz.getName());
    }

    private static Method findNoArgMethod(Class<?> start, String name) {
        for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name);
                if (m.getParameterCount() == 0) {
                    return m;
                }
            } catch (NoSuchMethodException ignored) {
                // try superclass
            }
        }
        return null;
    }

    private static Field findDeclaredField(Class<?> start, String fieldName) {
        for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                // try superclass
            }
        }
        return null;
    }

    private static Long invokeGetter(Method getter, Object target) {
        try {
            Object value = getter.invoke(target);
            return toLong(value, getter.getReturnType());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read detRowId via " + getter, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("getDetRowId() threw on " + target.getClass().getName(), cause);
        }
    }

    private static Long readField(Field field, Object target) {
        try {
            Object value = field.get(target);
            return toLong(value, field.getType());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read detRowId field on " + target.getClass().getName(), e);
        }
    }

    private static Long toLong(Object value, Class<?> declaredType) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        throw new IllegalStateException("detRowId must be numeric (e.g. Long/long), got runtime type " + value.getClass().getName() + " declared as " + declaredType.getName());
    }

    @FunctionalInterface
    private interface DetRowIdAccessor {
        Long read(Object target);
    }
}
