/*
 *  Copyright (c) 2022 Jens Feser
 *
 */

package com.example.consumer;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Semaphore;


@RestController
public class DataController {

    private final Object lock = new Object();

    private Semaphore semaphore = new Semaphore(1);

    @PostMapping("/api/data")
    public ResponseEntity<String> receiveData(HttpServletRequest request) {
        try (InputStream inputStream = request.getInputStream()) {

            Path filePath = Paths.get("/app/resources/data.json");

            // Create the file (if it doesn't exist) before creating the OutputStream
            try {
                Files.createDirectories(filePath.getParent());
                synchronized (lock) {
                    if (!Files.exists(filePath)) {
                        Files.createFile(filePath);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return new ResponseEntity<>("Error when creating the file", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            semaphore.acquire();

            try (OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.APPEND)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return new ResponseEntity<>("Error in data storage", HttpStatus.INTERNAL_SERVER_ERROR);
            } finally {
                semaphore.release();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return new ResponseEntity<>("Data received and stored", HttpStatus.OK);
    }

}