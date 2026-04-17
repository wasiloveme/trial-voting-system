package com.votingsystem.services;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class InputSanitizationService {

    public String sanitizeText(String input) {
        if (input == null) return null;
        return Jsoup.clean(input, Safelist.basic());
    }

    public String sanitizeUrl(String url) {
        if (url == null) return null;
        return Jsoup.clean(url, Safelist.simpleText());
    }
}
