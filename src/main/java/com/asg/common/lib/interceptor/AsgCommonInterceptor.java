package com.asg.common.lib.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import com.asg.common.lib.security.model.CustomAuthDetails;
import com.asg.common.lib.security.util.UserContext;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AsgCommonInterceptor extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(AsgCommonInterceptor.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String actionRequested = request.getHeader("X-Action-Requested");
            String documentId = request.getHeader("X-Document-Id");
            String userName = request.getHeader("X-User-Name");
            String userPoidStr = request.getHeader("X-User-Poid");
            String userId = request.getHeader("X-User-Id");
            String groupPoidStr = request.getHeader("X-Group-Poid");
            String companyPoidStr = request.getHeader("X-Company-Poid");
            String userEmail = request.getHeader("X-User-Email");
            String userRole = request.getHeader("X-User-Role");
            String logEnabledStr = request.getHeader("X-Log-Enabled");
            String timeZoneCode = request.getHeader("X-TimeZone-Code");

            if (StringUtils.hasText(userId)) {
                Long userPoid = StringUtils.hasText(userPoidStr) ? Long.parseLong(userPoidStr) : null;
                Long groupPoid = StringUtils.hasText(groupPoidStr) ? Long.parseLong(groupPoidStr) : null;
                Long companyPoid = StringUtils.hasText(companyPoidStr) ? Long.parseLong(companyPoidStr) : null;

                Boolean logEnabled = !StringUtils.hasText(logEnabledStr)
                        || Boolean.parseBoolean(logEnabledStr);

                CustomAuthDetails authDetails = CustomAuthDetails.builder()
                        .actionRequested(actionRequested)
                        .documentId(documentId)
                        .userName(userName)
                        .userPoid(userPoid)
                        .userId(userId)
                        .groupPoid(groupPoid)
                        .companyPoid(companyPoid)
                        .userEmail(userEmail)
                        .userRole(userRole)
                        .logEnabled(logEnabled)
                        .timeZoneCode(timeZoneCode)
                        .build();

                UserContext.setCurrentUser(authDetails);
                logger.debug("User context set for userId: {}", userId);
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Error processing user context", e);
        } finally {
            UserContext.clear();
        }
    }


}