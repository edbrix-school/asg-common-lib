package com.asg.common.lib.utility;

import com.asg.common.lib.dto.DiffObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DiffUtil {

    public static <T> List<DiffObject> createDiffList(T oldEntity, T newEntity, Class<T> entityClass) {
        List<DiffObject> diffs = new ArrayList<>();
        
        Field[] fields = entityClass.getDeclaredFields();
        
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object oldValue = oldEntity != null ? field.get(oldEntity) : null;
                Object newValue = field.get(newEntity);
                
                if (!Objects.equals(oldValue, newValue)) {
                    diffs.add(new DiffObject(
                        field.getName(),
                        oldValue != null ? oldValue.toString() : null,
                        newValue != null ? newValue.toString() : null
                    ));
                }
            } catch (IllegalAccessException e) {
                // Skip inaccessible fields
            }
        }
        
        return diffs;
    }
}