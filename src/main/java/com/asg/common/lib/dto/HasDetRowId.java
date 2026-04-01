package com.asg.common.lib.dto;

/**
 * Marker for DTO rows keyed by {@code DET_ROW_ID} so lists can be sorted consistently.
 */
public interface HasDetRowId {

    Long getDetRowId();
}

