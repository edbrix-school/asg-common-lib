package com.asg.common.lib.annotation;

import com.asg.common.lib.enums.UserRolesRightsEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedAction {
    UserRolesRightsEnum value();
}
