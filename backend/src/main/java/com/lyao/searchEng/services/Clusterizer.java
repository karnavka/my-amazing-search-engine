package com.lyao.searchEng.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.lyao.searchEng.IR.TreeIndex;
import com.lyao.searchEng.IR.Tools.ZonePostingItem;
import com.lyao.searchEng.IR.TreeIndex.OffsetToPostList;
@Service
public class Clusterizer {
    private TreeIndex index;
    private TreeMap<String, TreeIndex.OffsetToPostList> dictionary;
    private int numberOfDocs;
    private Map<Integer, Map<String, Double>> docVectors;
    public Clusterizer(TreeIndex index) throws IOException {
        this.index = index;
        this.dictionary = index.dictionary;
        this.numberOfDocs = index.numberOfFiles;
        this.docVectors = buildDocumentVectorsFromZoneIndex();
    }
    public Map<Integer, Map<String, Double>> buildDocumentVectorsFromZoneIndex() throws IOException {
        Map<Integer, Map<String, Double>> docVectors = new HashMap<>();

        for (Map.Entry<String, TreeIndex.OffsetToPostList> entry : dictionary.entrySet()) {
            String term = entry.getKey();
            TreeIndex.OffsetToPostList info = entry.getValue();

            if (info.zoneOffset == -1 || info.zoneDocFrequency == 0) {
                continue;
            }

            List<ZonePostingItem> postings = index.readZonePostingList(info.zoneOffset, info.zoneDocFrequency);

            Map<Integer, Double> zoneSumPerDoc = new HashMap<>();
            for (ZonePostingItem p : postings) {
                zoneSumPerDoc.merge(p.getDocID(), (double) p.getIndexInfo(), Double::sum);
            }

            int df = zoneSumPerDoc.size();
            if (df == 0) {
                continue;
            }

            double idf = Math.log((double) numberOfDocs / df);

            for (Map.Entry<Integer, Double> docEntry : zoneSumPerDoc.entrySet()) {
                int docID = docEntry.getKey();
                double weight = docEntry.getValue() * idf;

                docVectors
                    .computeIfAbsent(docID, k -> new HashMap<>())
                    .put(term, weight);
            }
        }

        normalize(docVectors);
        return docVectors;
    }

    private void normalize(Map<Integer, Map<String, Double>> docVectors) {
        for (Map<String, Double> vector : docVectors.values()) {
            double sumSquares = 0.0;
            for (double weight : vector.values()) {
                sumSquares += weight * weight;
            }
            double norm = Math.sqrt(sumSquares);
            if (norm == 0.0) {
                continue;
            }
            for (Map.Entry<String, Double> entry : vector.entrySet()) {
                entry.setValue(entry.getValue() / norm);
            }
        }
    }

    public List<ZonePostingItem> readZonePostingList(long offset, int postingCount) throws IOException {
        List<ZonePostingItem> postings = new ArrayList<>();
        index.zonePostingFile.seek(offset);

        for (int i = 0; i < postingCount; i++) {
            postings.add(new ZonePostingItem(index.zonePostingFile.readInt(), index.zonePostingFile.readFloat()));
        }

        return postings;
    }

    private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        Map<String, Double> smaller = v1.size() < v2.size() ? v1 : v2;
        Map<String, Double> larger = v1.size() < v2.size() ? v2 : v1;

        double dot = 0.0;
        for (Map.Entry<String, Double> entry : smaller.entrySet()) {
            Double otherWeight = larger.get(entry.getKey());
            if (otherWeight != null) {
                dot += entry.getValue() * otherWeight;
            }
        }

        return dot;
    }

    private Map<String, Double> computeCentroid(List<Integer> clusterDocIDs) {
        Map<String, Double> centroid = new HashMap<>();

        if (clusterDocIDs.isEmpty()) {
            return centroid;
        }

        for (Integer docID : clusterDocIDs) {
            Map<String, Double> vector = docVectors.get(docID);
            if (vector == null) {
                continue;
            }

            for (Map.Entry<String, Double> entry : vector.entrySet()) {
                centroid.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        int clusterSize = clusterDocIDs.size();
        for (Map.Entry<String, Double> entry : centroid.entrySet()) {
            entry.setValue(entry.getValue() / clusterSize);
        }

        double norm = 0.0;
        for (double weight : centroid.values()) {
            norm += weight * weight;
        }
        norm = Math.sqrt(norm);

        if (norm > 0.0) {
            for (Map.Entry<String, Double> entry : centroid.entrySet()) {
                entry.setValue(entry.getValue() / norm);
            }
        }

        return centroid;
    }

    public Map<Integer, Integer> kMeans(int k, int maxIterations) {
        List<Integer> docIDs = new ArrayList<>(docVectors.keySet());

        if (docIDs.isEmpty()) {
            return new HashMap<>();
        }

        if (k <= 0) {
            throw new IllegalArgumentException("k must be > 0");
        }

        if (k > docIDs.size()) {
            k = docIDs.size();
        }

        Collections.shuffle(docIDs, new Random());

        List<Map<String, Double>> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            centroids.add(new HashMap<>(docVectors.get(docIDs.get(i))));
        }

        Map<Integer, Integer> assignments = new HashMap<>();

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            boolean changed = false;

            for (Integer docID : docVectors.keySet()) {
                Map<String, Double> docVector = docVectors.get(docID);

                int bestCluster = -1;
                double bestSimilarity = -Double.MAX_VALUE;

                for (int clusterIndex = 0; clusterIndex < centroids.size(); clusterIndex++) {
                    double similarity = cosineSimilarity(docVector, centroids.get(clusterIndex));
                    if (similarity > bestSimilarity) {
                        bestSimilarity = similarity;
                        bestCluster = clusterIndex;
                    }
                }

                Integer oldCluster = assignments.get(docID);
                if (oldCluster == null || oldCluster != bestCluster) {
                    assignments.put(docID, bestCluster);
                    changed = true;
                }
            }

            List<List<Integer>> clusters = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                clusters.add(new ArrayList<>());
            }

            for (Map.Entry<Integer, Integer> entry : assignments.entrySet()) {
                clusters.get(entry.getValue()).add(entry.getKey());
            }

            List<Map<String, Double>> newCentroids = new ArrayList<>();
            Random random = new Random();

            for (int i = 0; i < k; i++) {
                if (clusters.get(i).isEmpty()) {
                    Integer randomDocID = docIDs.get(random.nextInt(docIDs.size()));
                    newCentroids.add(new HashMap<>(docVectors.get(randomDocID)));
                } else {
                    newCentroids.add(computeCentroid(clusters.get(i)));
                }
            }

            centroids = newCentroids;

            if (!changed) {
                break;
            }
        }

        return assignments;
    }

}