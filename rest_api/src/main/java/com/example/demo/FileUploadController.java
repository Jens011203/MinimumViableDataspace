
/*
 *  Copyright (c) 2022 Jens Feser
 *
 */
package com.example.demo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;






@RestController
public class FileUploadController {

    private File tempFile;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {


        try {
            String filename = file.getOriginalFilename();

            if (filename == null || !filename.endsWith(".json")) {
                return ResponseEntity.badRequest().body("Invalid file. Please only load valid json files");
            }

            tempFile = File.createTempFile("upload", ".tmp");


            try (InputStream in = file.getInputStream()) {
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024];
                    int read;

                    // Read InputStream and write data to ByteArrayOutputStream
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }

            try (JsonParser jsonParser = new ObjectMapper().getFactory().createParser(new FileInputStream(tempFile))) {
                while (jsonParser.nextToken() != null) {
                    // process each token, without loading the complete file into memory
                }
                return ResponseEntity.ok("File uploaded");
            } catch (JsonProcessingException e) {
                return ResponseEntity.badRequest().body("Invalid file. Please only load valid json files");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during uploading file: " + e.getMessage());
        }
    }

    @GetMapping("/uploaded-data")
    public ResponseEntity<Resource> uploadData() {
        if (tempFile != null && tempFile.exists()) {
            try {
                HttpHeaders headers = new HttpHeaders();

                headers.setContentType(MediaType.APPLICATION_JSON);

                InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));

                return new ResponseEntity<>(resource, headers, HttpStatus.OK);

            } catch (IOException e) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @PreDestroy
    public void cleanUp() {
        if (tempFile != null && tempFile.exists()) {
            boolean success = tempFile.delete();
            if (!success) {
                System.err.println("Could not delete temporary file" + tempFile.getAbsolutePath());
            }
        }
    }
}
