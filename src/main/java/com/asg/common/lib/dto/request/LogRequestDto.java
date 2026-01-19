package com.asg.common.lib.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LogRequestDto<T> {
    private T oldObj;
    private T newObj;
    private Class<T> clazz;
    private String documentId;
    private String docKeyPoid;
    private String logDetail;
}
