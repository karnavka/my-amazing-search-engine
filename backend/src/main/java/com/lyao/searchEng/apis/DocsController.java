package com.lyao.searchEng.apis;

import java.util.ArrayList;


import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lyao.searchEng.services.SearchService;
@CrossOrigin(origins = "http://localhost:5173")


@RestController
public class DocsController {

    private final SearchService searchService;

    public DocsController(SearchService searchService) {
        this.searchService = searchService;
    }

 /*    @GetMapping("/search")
    public Set<Integer> search(@RequestParam(name = "q") String q) {
        return searchService.search(q);
    }*/

     @GetMapping("/searchByZone")
    public ArrayList<String> searchByZone(@RequestParam(name = "q") String q) {
        return searchService.searchByZone(q);
    }

  

}