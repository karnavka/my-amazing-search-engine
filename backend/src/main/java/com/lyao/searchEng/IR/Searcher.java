package com.lyao.searchEng.IR;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Collections;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class Searcher {
    RandomAccessFile postingFile;
    TreeSet<String> lastFoundWords;
    Analyzer analyzer = new EnglishAnalyzer();
    TreeMap<String, TreeIndex.OffsetToPostList> dictionary;
    File[] inputFiles;

    public Searcher(TreeIndex invertedIndex) throws IOException {
        this.postingFile = invertedIndex.postingFile;
        dictionary = invertedIndex.dictionary;
        inputFiles = invertedIndex.inputFiles;

    }

    public Set<Integer> getPostingsOfWords(String query) throws IOException {
        Set<Integer> indexes = new TreeSet<>();
        ArrayList<String> listOfWords = normilizeQuery(query);
        for (String key : listOfWords) {
            long offset = dictionary.get(key).offset;
            postingFile.seek(offset);
            for (int i = 0; i < dictionary.get(key).docFrequency; i++) {
                int docID = postingFile.readInt();
                 postingFile.readFloat();
                indexes.add(docID);
            }
        }
        return indexes;
    }

    public ArrayList<String> getDocsPathByZone(String query) throws IOException {
        ArrayList<Integer> docIDs = getDocsInZoneOrder(query);
        return getDocPathByIds(docIDs);
    }

    private ArrayList<Integer> getDocsInZoneOrder(String query) throws IOException {
        ArrayList<String> listOfWords = normilizeQuery(query);
        ArrayList<docIdWithWeight> docsWithWeights = new ArrayList<>();
        float[] weightPerDoc = new float[inputFiles.length];
        for (String key : listOfWords) {
            long offset = dictionary.get(key).offset;
            postingFile.seek(offset);
            for (int i = 0; i < dictionary.get(key).docFrequency; i++) {
                int docID = postingFile.readInt();
                float zoneWeight = postingFile.readFloat();
                weightPerDoc[docID] += zoneWeight;
            }
        }
        for (int i = 0; i < weightPerDoc.length; i++) {
            if (weightPerDoc[i] > 0) {
                docsWithWeights.add(new docIdWithWeight(i, weightPerDoc[i]));
            }
        }
        Collections.sort(docsWithWeights);
        ArrayList<Integer> docIDsInZoneOrder = new ArrayList<>();
        for (docIdWithWeight doc : docsWithWeights) {
            docIDsInZoneOrder.add(doc.docID);
        }
        return docIDsInZoneOrder;
    }

    private class docIdWithWeight implements Comparable<docIdWithWeight> {
        int docID;
        float weight;

        public docIdWithWeight(int docID, float weight) {
            this.docID = docID;
            this.weight = weight;
        }

        @Override
        public int compareTo(docIdWithWeight o) {
            return Float.compare(o.weight, this.weight);
        }
    }

    private ArrayList<String> normilizeQuery(String termsQuery) throws IOException {

        ArrayList<String> terms = new ArrayList<>();
        Analyzer analyzer = new EnglishAnalyzer();
        TokenStream ts = analyzer.tokenStream("field", termsQuery);
        ts.reset();
        while (ts.incrementToken()) {
            terms.add(ts.getAttribute(CharTermAttribute.class).toString());
        }
        ts.close();
        analyzer.close();
        return terms;
    }

    private ArrayList<String> getDocPathByIds(ArrayList<Integer> docIDs) {
        ArrayList<String> paths = new ArrayList<>();
        for (Integer id : docIDs) {
            paths.add(inputFiles[id].getPath());
        }
        return paths;
    }

}
