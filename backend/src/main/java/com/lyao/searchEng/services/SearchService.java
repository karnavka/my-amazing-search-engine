package com.lyao.searchEng.services;

import java.util.ArrayList;
import java.util.Set;

import org.springframework.stereotype.Service;
import com.lyao.searchEng.IR.TreeIndex;
import com.lyao.searchEng.IR.Searcher;

@Service
public class SearchService {

    private Searcher searcher;

    public SearchService() {
        try {
            this.searcher = new Searcher(new TreeIndex("./data/books"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Integer> search(String query) {
        try {
            return searcher.getPostingsOfWords(query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<String> searchByZone(String query) {
        try {
            return searcher.getDocsPathByZone(query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}