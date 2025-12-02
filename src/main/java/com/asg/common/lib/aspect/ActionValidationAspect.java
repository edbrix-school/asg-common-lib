package com.asg.common.lib.aspect;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.exception.InvalidActionException;
import com.asg.common.lib.security.util.UserContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ActionValidationAspect {

    @Before("@annotation(allowedAction)")
    public void validateAction(AllowedAction allowedAction) {
        String actionRequested = UserContext.getCurrentUser().getActionRequested();
        String validAction = allowedAction.value().name();
        
        if (!validAction.equalsIgnoreCase(actionRequested)) {
            throw new InvalidActionException("Invalid action requested for this API.");
        }
    }
}