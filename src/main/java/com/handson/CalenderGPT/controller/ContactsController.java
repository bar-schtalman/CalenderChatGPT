package com.handson.CalenderGPT.controller;

import com.handson.CalenderGPT.service.GoogleContactsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
public class ContactsController {

    private final GoogleContactsService googleContactsService;

    public ContactsController(GoogleContactsService googleContactsService) {
        this.googleContactsService = googleContactsService;
    }

    @GetMapping("/api/contacts/search")
    public List<Map<String, String>> searchContacts(@RequestParam String query) throws Exception {
        return googleContactsService.searchContacts(query);
    }

}


