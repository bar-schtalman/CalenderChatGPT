package com.handson.CalenderGPT.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<?> hello()
    {
        return new ResponseEntity<>("Hello World!", HttpStatus.OK);
    }
}
