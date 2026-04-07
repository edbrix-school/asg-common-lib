package com.asg.common.lib.dto;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public final class DetRowIdSort {

    private static final Comparator<Long> DET_ROW_ID_ORDER = Comparator.nullsLast(Long::compareTo);

    private static final ConcurrentHashMap<Class<?>, Optional<DetRowIdAccessor>> ACCESSORS = new ConcurrentHashMap<>();

    private DetRowIdSort() {
    }

    public static void sortAscending(List<?> list) {
        sort(list, false);
    }

    public static void sortDescending(List<?> list) {
        sort(list, true);
    }

    private static void sort(List<?> list, boolean descending) {
        if (list == null || list.size() <= 1) {
            return;
        }
        int n = list.size();
        Long[] keys = new Long[n];
        for (int i = 0; i < n; i++) {
            KeyResult kr = readKey(list.get(i));
            if (!kr.ok) {
                return;
            }
            keys[i] = kr.value;
        }
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        if (descending) {
            Arrays.sort(order, (a, b) -> DET_ROW_ID_ORDER.compare(keys[b], keys[a]));
        } else {
            Arrays.sort(order, (a, b) -> DET_ROW_ID_ORDER.compare(keys[a], keys[b]));
        }
        @SuppressWarnings("unchecked")
        List<Object> target = (List<Object>) list;
        Object[] snapshot = target.toArray(new Object[0]);
        for (int i = 0; i < n; i++) {
            target.set(i, snapshot[order[i]]);
        }
    }

    private static KeyResult readKey(Object element) {
        if (element == null) {
            return KeyResult.success(null);
        }
        Optional<DetRowIdAccessor> acc = ACCESSORS.computeIfAbsent(element.getClass(), DetRowIdSort::buildAccessorOptional);
        if (acc.isEmpty()) {
            return KeyResult.fail();
        }
        return acc.get().read(element);
    }

    private static Optional<DetRowIdAccessor> buildAccessorOptional(Class<?> clazz) {
        try {
            Method getter = findNoArgMethod(clazz, "getDetRowId");
            if (getter != null) {
                getter.setAccessible(true);
                return Optional.of(target -> readViaGetter(getter, target));
            }
            Field field = findDeclaredField(clazz, "detRowId");
            if (field != null) {
                field.setAccessible(true);
                return Optional.of(target -> readViaField(field, target));
            }
        } catch (Throwable ignored) {
            // leave list unsorted
        }
        return Optional.empty();
    }

    private static KeyResult readViaGetter(Method getter, Object target) {
        try {
            Object value = getter.invoke(target);
            return toLongResult(value);
        } catch (Throwable ignored) {
            return KeyResult.fail();
        }
    }

    private static KeyResult readViaField(Field field, Object target) {
        try {
            Object value = field.get(target);
            return toLongResult(value);
        } catch (Throwable ignored) {
            return KeyResult.fail();
        }
    }

    private static KeyResult toLongResult(Object value) {
        if (value == null) {
            return KeyResult.success(null);
        }
        if (value instanceof Long l) {
            return KeyResult.success(l);
        }
        if (value instanceof Number n) {
            return KeyResult.success(n.longValue());
        }
        return KeyResult.fail();
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

    @FunctionalInterface
    private interface DetRowIdAccessor {
        KeyResult read(Object target);
    }

    private static final class KeyResult {
        private static final KeyResult FAIL = new KeyResult(null, false);

        final Long value;
        final boolean ok;

        private KeyResult(Long value, boolean ok) {
            this.value = value;
            this.ok = ok;
        }

        static KeyResult success(Long value) {
            return new KeyResult(value, true);
        }

        static KeyResult fail() {
            return FAIL;
        }
    }
}
