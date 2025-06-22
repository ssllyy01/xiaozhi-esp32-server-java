package com.xiaozhi.dialogue.llm.tool;

import org.springframework.ai.tool.execution.ToolCallResultConverter;

import java.lang.reflect.Type;

public class ToolCallStringResultConverter implements ToolCallResultConverter {

    public static final ToolCallStringResultConverter INSTANCE = new ToolCallStringResultConverter();

    private ToolCallStringResultConverter() {
        // Private constructor to enforce singleton pattern
    }

    @Override
    public String convert(Object result, Type returnType) {
        return String.valueOf(result);
    }
}
