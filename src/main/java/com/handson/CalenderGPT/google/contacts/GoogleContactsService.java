package com.handson.CalenderGPT.google.contacts;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.SearchResponse;
import com.google.api.services.people.v1.model.SearchResult;
import com.handson.CalenderGPT.context.CalendarContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoogleContactsService {

    private final GooglePeopleProvider peopleProvider;
    private final CalendarContext calendarContext;

    public GoogleContactsService(GooglePeopleProvider peopleProvider, CalendarContext calendarContext) {
        this.peopleProvider = peopleProvider;
        this.calendarContext = calendarContext;
    }

    public List<Map<String, String>> searchContacts(String query) throws Exception {
        List<Map<String, String>> contacts = new ArrayList<>();

        OAuth2AuthorizedClient client = calendarContext.getAuthorizedClient();
        if (client == null) {
            System.err.println("❌ No OAuth2AuthorizedClient found in session!");
            return contacts;
        }

        PeopleService peopleService = peopleProvider.getPeopleService(client);

        SearchResponse response = peopleService.people().searchContacts().setQuery(query).setReadMask("names,emailAddresses").setPageSize(10).execute();

        if (response.getResults() != null) {
            for (SearchResult result : response.getResults()) {
                Person person = result.getPerson();
                if (person.getNames() != null && person.getEmailAddresses() != null) {
                    Map<String, String> contact = new HashMap<>();
                    contact.put("name", person.getNames().get(0).getDisplayName());
                    contact.put("email", person.getEmailAddresses().get(0).getValue());
                    contacts.add(contact);
                }
            }
        }

        return contacts;
    }

}

