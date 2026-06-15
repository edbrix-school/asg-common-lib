package com.asg.common.lib.dto.response;


import com.asg.common.lib.security.util.UserContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiResponse {

    public static ResponseEntity<?> successWithWarnings(String message, Object data,
                                                        List<String> warnings, List<String> infoMessages) {
        boolean hasWarnings = warnings != null && !warnings.isEmpty();
        boolean hasInfo = infoMessages != null && !infoMessages.isEmpty();
        if (!hasWarnings && !hasInfo) {
            return success(message, data);
        }
        Map<String, Object> body = new LinkedHashMap<>(Map.of(
                "statusCode", HttpStatus.OK.value(),
                "success", true,
                "message", message
        ));
        if (hasWarnings) body.put("warnings", warnings);
        if (hasInfo) body.put("info", infoMessages);
        body.put("result", data != null ? Map.of("data", data) : "");
        return ResponseEntity.ok(body);
    }

    //Common method used to respond with success message in all controllers
    public static ResponseEntity<?> success(String message, Object data) {
        String glError = UserContext.getGlPostingError();
        Map<String, Object> body = new LinkedHashMap<>(Map.of(
                "statusCode", HttpStatus.OK.value(),
                "success", true,
                "message", message,
                "result", data != null ? Map.of("data", data) : ""
        ));
        if (glError != null && !glError.isBlank()) body.put("errors", glError);
        return ResponseEntity.ok(body);
    }

    public static ResponseEntity<?> success(String message) {
        String glError = UserContext.getGlPostingError();
        Map<String, Object> body = new LinkedHashMap<>(Map.of(
                "statusCode", HttpStatus.OK.value(),
                "success", true,
                "message", message
        ));
        if (glError != null && !glError.isBlank()) body.put("errors", glError);
        return ResponseEntity.ok(body);
    }


    public static ResponseEntity<?> badRequest(String message) {
        return error(message, HttpStatus.BAD_REQUEST.value());
    }

    public static ResponseEntity<?> internalServerError(String message) {
        return error(message, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    public static ResponseEntity<?> notFound(String message) {
        return error(message, HttpStatus.NOT_FOUND.value());
    }

    public static ResponseEntity<?> unprocessableEntity(String message) {
        return error(message, HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    public static ResponseEntity<?> unauthorized(String message) {
        return error(message, HttpStatus.UNAUTHORIZED.value());
    }

    public static ResponseEntity<?> conflict(String message) {
        return error(message, HttpStatus.CONFLICT.value());
    }

    public static ResponseEntity<?> error(String message, int statusCode) {
        return ResponseEntity.status(statusCode).body(Map.of(
                "success", false,
                "statusCode", String.valueOf(statusCode),
                "message", message
        ));
    }

    public static ResponseEntity<?> error(String message, int statusCode, Object errors) {
        return ResponseEntity.status(statusCode).body(Map.of(
                "success", false,
                "statusCode", String.valueOf(statusCode),
                "message", message,
                "errors", errors
        ));
    }
}
