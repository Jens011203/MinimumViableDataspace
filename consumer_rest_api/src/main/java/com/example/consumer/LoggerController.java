/*
 *  Copyright (c) 2022 Jens Feser
 *
 */

package com.example.consumer;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoggerController {
    @RequestMapping(value = "/api/logger", method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> receiveData(@RequestBody(required = false) String data, HttpServletRequest request) {
        System.out.println("Incoming request");
        System.out.println("Method: " + request.getMethod());
        System.out.println("Path: /api/logger");
        System.out.println("Body:");
        System.out.println(data);
        System.out.println("=============");

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            return ResponseEntity.ok(data);
        }

        return ResponseEntity.ok().build();
    }
}
