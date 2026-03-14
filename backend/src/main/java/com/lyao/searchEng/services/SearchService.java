package com.lyao.searchEng.services;
import org.springframework.stereotype.Service;
import com.lyao.searchEng.IR.TreeIndex;

@Service
public class SearchService {

    private TreeIndex index;

    public SearchService() {
        try {
            this.index = new TreeIndex("data/books");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

 //   public Object search(String query) {
 //       return index.search(query);
  //  }
}