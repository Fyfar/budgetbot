package com.home.budgetbot.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class PropertyProvider {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ResourceLoader resourceLoader;

    public <T> T getPropertySupplier(String fileName, Class<T> target) {
        String resourcePath = "file:./data/"+fileName+".json";

        File file;
        try {
            file = resourceLoader.getResource(resourcePath).getFile();
        } catch (Exception e) {
            throw new IllegalStateException("Error while get resource: " + resourcePath, e);
        }

        if(!file.exists()) {
            return initDefaultValue(file, target);
        }

        try {
            return objectMapper.readValue(file, target);
        } catch (IOException e) {
            throw new IllegalStateException("Error while read value: " + resourcePath, e);
        }
    }

    private <T> T initDefaultValue(File file, Class<T> target) {
        T instance;

        try {
            instance = target.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Error while create new instance: "+target.getName());
        }

        try {
            file.createNewFile();
            objectMapper.writeValue(file, instance);
        } catch (IOException e) {
            throw new IllegalStateException("Error while create new file: "+file.getAbsolutePath());
        }

        return instance;
    }
}
