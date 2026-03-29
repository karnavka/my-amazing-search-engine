package com.lyao.searchEng.apis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lyao.searchEng.services.Clusterizer;
import com.lyao.searchEng.services.SearchService;
@CrossOrigin(origins = "http://localhost:5173")


@RestController
public class DocsController {

    private final SearchService searchService;
    private final Clusterizer clusterizer;

    public DocsController(SearchService searchService, Clusterizer clusterizer) {
        this.searchService = searchService;
        this.clusterizer=clusterizer;

    }

 /*    @GetMapping("/search")
    public Set<Integer> search(@RequestParam(name = "q") String q) {
        return searchService.search(q);
    }*/

     @GetMapping("/searchByZone")
    public ArrayList<String> searchByZone(@RequestParam(name = "q") String q) {
        return searchService.searchByZone(q);
    }

      @GetMapping("/wildCardSearch")
    public ArrayList<String> searchWildcard(@RequestParam(name = "q") String q) {
        return searchService.searchWildcard(q);
    }

 @GetMapping("/clusterize")
public List<ClusterResponse> clusterize() {
    Map<Integer, Integer> assignments = clusterizer.kMeans(3, 20);

    Map<Integer, List<String>> grouped = new TreeMap<>();

    for (Map.Entry<Integer, Integer> entry : assignments.entrySet()) {
        int docId = entry.getKey();
        int clusterId = entry.getValue();

        String docPath = searchService.getInputFiles()[docId].getPath();

        grouped.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(docPath);
    }

    List<ClusterResponse> result = new ArrayList<>();
    for (Map.Entry<Integer, List<String>> entry : grouped.entrySet()) {
        result.add(new ClusterResponse(entry.getKey(), entry.getValue()));
    }

    return result;
}

private class ClusterResponse {
    private int clusterId;
    private List<String> documents;

    public ClusterResponse(int clusterId, List<String> documents) {
        this.clusterId = clusterId;
        this.documents = documents;
    }

    public int getClusterId() {
        return clusterId;
    }

    public List<String> getDocuments() {
        return documents;
    }
}

  

}