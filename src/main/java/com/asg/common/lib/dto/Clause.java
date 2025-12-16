package com.asg.common.lib.dto;

import java.util.List;

public record Clause(String sql, List<Object> params) {}