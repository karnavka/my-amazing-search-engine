package com.lyao.searchEng.apis;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocsController {

@GetMapping("/docs")
public String getDocs() {
    return "This is supposed to be";
}

@GetMapping("/search")
public String returnDocsId(@RequestParam(name = "query") String query) {
        return "Search: " + query;
    }

  

}