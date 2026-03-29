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
import java.util.List;

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

    public ArrayList<String> getDocsPathByZone(String query) throws IOException {
        ArrayList<Integer> docIDs = getDocsInZoneOrder(query);
        return getDocPathByIds(docIDs);
    }

    public ArrayList<String> getDocsPathByWildcard(String query) throws IOException {
        TreeSet<Integer> docIDs = wildCardTheeGram(query);
        return getDocPathByIds(new ArrayList<>(docIDs));
    }

    private TreeSet<Integer> getPostingsOfWordsInZoneIndex(Collection<String> words) throws IOException {
        TreeSet<Integer> indexes = new TreeSet<>();
        for (String key : words) {
            TreeIndex.OffsetToPostList entry = dictionary.get(key);
            if (entry == null || entry.zoneOffset < 0)
                continue;
            zonePostingFile.seek(entry.zoneOffset);
            for (int i = 0; i < entry.zoneDocFrequency; i++) {
                int docID = zonePostingFile.readInt();
                zonePostingFile.readFloat();
                indexes.add(docID);
            }
        }
        return indexes;
    }

    private ArrayList<Integer> getDocsInZoneOrder(String query) throws IOException {
        ArrayList<String> listOfWords = normilizeQuery(query);
        ArrayList<docIdWithWeight> docsWithWeights = new ArrayList<>();
        float[] weightPerDoc = new float[inputFiles.length];
        for (String key : listOfWords) {
            TreeIndex.OffsetToPostList entry = dictionary.get(key);
            if (entry == null || entry.zoneOffset < 0)
                continue;

            long offset = entry.zoneOffset;
            zonePostingFile.seek(offset);
            for (int i = 0; i < dictionary.get(key).zoneDocFrequency; i++) {
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


    public TreeSet<Integer> wildCardTheeGram(String word) throws IOException {
    lastFoundWords = new TreeSet<>();
    TreeSet<Integer> resIndexes = new TreeSet<>();

    word = word.toLowerCase();

    ArrayList<String> threeGrams = createThreeGramsFromQuery(word);

    TreeSet<String> matchingWords;

    if (threeGrams.isEmpty()) {
        matchingWords = new TreeSet<>(dictionary.keySet());
    } else {
        matchingWords = threeGramIndex.get(threeGrams.get(0));
        if (matchingWords == null) return resIndexes;

        matchingWords = new TreeSet<>(matchingWords);

        for (int i = 1; i < threeGrams.size(); i++) {
            TreeSet<String> current = threeGramIndex.get(threeGrams.get(i));
            if (current == null) return resIndexes;

            matchingWords = intersectWords(current, matchingWords);
            if (matchingWords.isEmpty()) return resIndexes;
        }
    }

    TreeSet<String> filteredWords = new TreeSet<>();
    for (String candidate : matchingWords) {
        if (matchesWildcard(candidate, word)) {
            filteredWords.add(candidate);
        }
    }

    resIndexes = getPostingsOfWordsInZoneIndex(filteredWords);
    lastFoundWords = filteredWords;
    return resIndexes;
}

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

    /*
     * private void getPostingsOfWords(Collection<String> listOfWords, Set<Integer>
     * indexes) throws IOException {
     * for (String key : listOfWords) {
     * long offset = dictionary.get(key).zoneOffset;
     * System.out.println(key + dictionary.get(key).docFrequency);
     * regularPostingFile.seek(offset);
     * for (int i = 0; i < dictionary.get(key).docFrequency; i++) {
     * int docID = regularPostingFile.readInt();
     * indexes.add(docID);
     * System.out.println("docId " + docID);
     * }
     * }
     * }
     */

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

    private ArrayList<String> createThreeGramsFromQuery(String wildcard) {
    ArrayList<String> grams = new ArrayList<>();

    wildcard = wildcard.toLowerCase();

    String[] parts = wildcard.split("\\*", -1);

    boolean startsWithStar = wildcard.startsWith("*");
    boolean endsWithStar = wildcard.endsWith("*");

    for (int i = 0; i < parts.length; i++) {
        String part = parts[i];
        if (part.isEmpty()) continue;

        String enriched = part;

        if (i == 0 && !startsWithStar) {
            enriched = "$" + enriched;
        }
        if (i == parts.length - 1 && !endsWithStar) {
            enriched = enriched + "$";
        }

        if (enriched.length() < 3) {
            continue;
        }

        for (int j = 0; j <= enriched.length() - 3; j++) {
            grams.add(enriched.substring(j, j + 3));
        }
    }

    return grams;
}

    public File[] getInputFiles() {
    return inputFiles;
}
}
