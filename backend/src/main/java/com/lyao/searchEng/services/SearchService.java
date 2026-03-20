package com.lyao.searchEng.services;

import java.io.File;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import com.lyao.searchEng.IR.TreeIndex;
import com.lyao.searchEng.IR.Searcher;

@Service
public class SearchService {

    private Searcher searcher;

    public SearchService() {
        try {
            this.searcher = new Searcher(new TreeIndex(".\\data\\books"));
            searcher.getDocsPathByWildcard("m*on");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * public Set<Integer> search(String query) {
     * try {
     * return searcher.getPostingsOfWords(query);
     * } catch (Exception e) {
     * throw new RuntimeException(e);
     * }
     * }
     */
    public ArrayList<String> searchByZone(String query) {
        try {
            return searcher.getDocsPathByZone(query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<String> searchWildcar(String query) {
        try {
            return searcher.getDocsPathByWildcard(query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}