package com.cla.demo.tasks;

import java.util.HashMap;
import java.util.Map;

public class TaskContext {

    private Map<String, Object> internalDataMap = new HashMap<>();

    public TaskContext add(String key, Object value) {
        internalDataMap.put(key, value);
        return this;
    }

    public <T> T get(Object key, Class<T> type) {
        Object value = this.internalDataMap.get(key);
        if (value == null) {
            return null;
        }
        if (!type.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("Incorrect type specified for task context '" +
                    key + "'. Expected [" + type + "] but actual type is [" + value.getClass() + "]");
        }
        return (T) value;
    }

    @Override
    public String toString() {
        return internalDataMap.toString();

    }
}
