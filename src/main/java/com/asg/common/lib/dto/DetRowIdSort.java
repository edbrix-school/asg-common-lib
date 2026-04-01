package com.asg.common.lib.dto;

import java.util.Comparator;
import java.util.List;

public final class DetRowIdSort {

    private static final Comparator<Long> DET_ROW_ID_ORDER = Comparator.nullsLast(Long::compareTo);

    private DetRowIdSort() {
    }

    public static <T extends HasDetRowId> void sortAscending(List<T> list) {
        if (list == null || list.size() <= 1) {
            return;
        }
        list.sort(Comparator.comparing(HasDetRowId::getDetRowId, DET_ROW_ID_ORDER));
    }

    public static <T extends HasDetRowId> void sortDescending(List<T> list) {
        if (list == null || list.size() <= 1) {
            return;
        }
        list.sort(Comparator.comparing(HasDetRowId::getDetRowId, DET_ROW_ID_ORDER).reversed());
    }
}

