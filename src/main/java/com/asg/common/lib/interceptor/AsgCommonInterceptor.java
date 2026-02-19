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
            String actionRequested = getOrDefault(request, "X-Action-Requested", "PRINT");
            String documentId      = getOrDefault(request, "X-Document-Id", "800-320");
            String userName        = getOrDefault(request, "X-User-Name", "DEVUSER2");
            String userPoidStr     = getOrDefault(request, "X-User-Poid", "3371");
            String userId          = getOrDefault(request, "X-User-Id", "DEVUSER2");
            String groupPoidStr    = getOrDefault(request, "X-Group-Poid", "1");
            String companyPoidStr  = getOrDefault(request, "X-Company-Poid", "1");
            String userEmail       = getOrDefault(request, "X-User-Email", "ssundaram@hexalytics.com");
            String userRole        = getOrDefault(request, "X-User-Role", "ADMIN");
            String logEnabledStr   = getOrDefault(request, "X-Log-Enabled", "true");
            String timeZoneCode    = getOrDefault(request, "X-TimeZone-Code", "GMT+3");

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

    private String getOrDefault(HttpServletRequest request, String headerName, String defaultValue) {
        String value = request.getHeader(headerName);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }


}