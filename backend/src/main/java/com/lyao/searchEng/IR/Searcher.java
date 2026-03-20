package com.lyao.searchEng.IR;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Collections;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class Searcher {
    RandomAccessFile zonePostingFile, regularPostingFile, biWordPostingFile, positionalPostingFile;
    TreeSet<String> lastFoundWords;
    Analyzer analyzer = new EnglishAnalyzer();
    TreeMap<String, TreeIndex.OffsetToPostList> dictionary;
    File[] inputFiles;
    TreeMap<String, TreeSet<String>> threeGramIndex;

    public Searcher(TreeIndex invertedIndex) throws IOException {
        this.zonePostingFile = invertedIndex.zonePostingFile;
        this.regularPostingFile = invertedIndex.regularPostingFile;
        this.positionalPostingFile = invertedIndex.positionalPostingFile;
        dictionary = invertedIndex.dictionary;
        inputFiles = invertedIndex.inputFiles;
        threeGramIndex = invertedIndex.threeGramIndex;
    }

   /* private Set<Integer> getPostingsOfWordsInZoneIndex(String query) throws IOException {
        Set<Integer> indexes = new TreeSet<>();
        ArrayList<String> listOfWords = normilizeQuery(query);
        for (String key : listOfWords) {
            long offset = dictionary.get(key).offset;
            zonePostingFile.seek(offset);
            for (int i = 0; i < dictionary.get(key).docFrequency; i++) {
                int docID = zonePostingFile.readInt();
                zonePostingFile.readFloat();
                indexes.add(docID);
            }
        }
        return indexes;
    }*/

    public ArrayList<String> getDocsPathByZone(String query) throws IOException {
        ArrayList<Integer> docIDs = getDocsInZoneOrder(query);
        return getDocPathByIds(docIDs);
    }

    private ArrayList<Integer> getDocsInZoneOrder(String query) throws IOException {
        ArrayList<String> listOfWords = normilizeQuery(query);
        ArrayList<docIdWithWeight> docsWithWeights = new ArrayList<>();
        float[] weightPerDoc = new float[inputFiles.length];
        for (String key : listOfWords) {
            long offset = dictionary.get(key).zoneOffset;
            zonePostingFile.seek(offset);
            for (int i = 0; i < dictionary.get(key).docFrequency; i++) {
                int docID = zonePostingFile.readInt();
                float zoneWeight = zonePostingFile.readFloat();
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

   /*  public TreeSet<Integer> wildCardTheeGram(String word) throws IOException {
        lastFoundWords = new TreeSet<>();
        TreeSet<Integer> resIndexes = new TreeSet<>();
        ArrayList<String> threeGrams = createThreeGramsFromQuery(word);
        TreeSet<String> matchingWords = threeGramIndex.get(threeGrams.getFirst());
        for (int i = 1; i < threeGrams.size(); i++) {
            TreeSet<String> matchingWordsToCurrentThreeGram = threeGramIndex.get(threeGrams.get(i));
            matchingWords = intersectWords(matchingWordsToCurrentThreeGram, matchingWords);
        }
        if (matchingWords.isEmpty()) return resIndexes;
        TreeSet<String> filteredWords = new TreeSet<>();
        for (String candidate : matchingWords) {
            if (matchesWildcard(candidate, word)) {
                filteredWords.add(candidate);
            }
        }
        getPostingsOfWords(filteredWords, resIndexes);
        lastFoundWords = filteredWords;
        return resIndexes;
    }*/ 
    private boolean matchesWildcard(String candidate, String wildcard) {
        String regex = wildcardToRegex(wildcard);
        return candidate.matches(regex);
    }
    private String wildcardToRegex(String wildcard) {
        StringBuilder sb = new StringBuilder();
        sb.append("^");
        for (char c : wildcard.toCharArray()) {
            if (c == '*') {
                sb.append(".*");
            } else {
                if ("\\.[]{}()+-^$|?".indexOf(c) >= 0) {
                    sb.append("\\");
                }
                sb.append(c);
            }
        }
        sb.append("$");
        return sb.toString();
    }

    private TreeSet<String> intersectWords(Set<String> matchingWordsToCurrentThreeGram, Set<String> matchingWords) {
        TreeSet<String> intersection = new TreeSet<>();
        for (String matchingWord : matchingWordsToCurrentThreeGram) {
            if (matchingWords.contains(matchingWord)) {
                intersection.add(matchingWord);
            }
        }
        return intersection;
    }
   
    private void getPostingsOfWords(Collection<String> listOfWords, Set<Integer> indexes) throws IOException {
        for (String key : listOfWords) {
            long offset = dictionary.get(key).zoneOffset;
            System.out.println(key + dictionary.get(key).docFrequency );
            regularPostingFile.seek(offset);
            for (int i = 0; i < dictionary.get(key).docFrequency; i++) {
                int docID =  regularPostingFile.readInt();
                indexes.add(docID);
                System.out.println("docId " +docID);
            }
        }
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

    private ArrayList<String> createThreeGramsFromQuery(String word) {
    ArrayList<String> threeGrams = new ArrayList<>();

    String query = "$" + word + "$";

    for (int i = 0; i <= query.length() - 3; i++) {
        String threeGram = query.substring(i, i + 3);

        if (!threeGram.contains("*")) {
            threeGrams.add(threeGram);
        }
    }

    return threeGrams;
}
}
