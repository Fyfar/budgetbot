package com.home.budgetbot.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;

@Singleton
public class PropertyProvider {

    private final ObjectMapper objectMapper;

    public PropertyProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T getPropertySupplier(String fileName, Class<T> target) {
        File file = new File("./data/" + fileName + ".json");

        if (!file.exists()) {
            return initDefaultValue(file, target);
        }

        try {
            return objectMapper.readValue(file, target);
        } catch (IOException e) {
            throw new IllegalStateException("Error while read value: " + file.getPath(), e);
        }
    }

    private <T> T initDefaultValue(File file, Class<T> target) {
        T instance;

        try {
            instance = target.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Error while create new instance: " + target.getName());
        }

        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            file.createNewFile();
            objectMapper.writeValue(file, instance);
        } catch (IOException e) {
            throw new IllegalStateException("Error while create new file: " + file.getAbsolutePath());
        }

        return instance;
    }
}
