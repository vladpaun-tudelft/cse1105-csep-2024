package client.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import commons.Collection;

public class Config {
    public File config;

    public Config() throws IOException {
        config = new File("client/src/main/resources/config.json");
        if (!config.exists()) {
            config.createNewFile();
        }
    }

    public List<Collection> readFromFile() throws IOException {
        if (config.length() == 0) {
            return new ArrayList<>();
        }
        String collectionsJson = FileUtils.readFileToString(config, "UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(collectionsJson, new TypeReference<List<Collection>>() {});
    }

    public void writeToFile(Collection collection) throws IOException {
        List<Collection> collections = readFromFile();
        collections.add(collection);
        System.out.println(collections);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(config, collections);
    }
}
