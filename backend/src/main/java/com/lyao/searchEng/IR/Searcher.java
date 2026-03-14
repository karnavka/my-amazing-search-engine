package com.lyao.searchEng.IR;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

public class Searcher {

     RandomAccessFile postingFile;
    TreeSet<String> lastFoundWords;
    Analyzer analyzer = new EnglishAnalyzer();
    TreeMap<String, TreeIndex.OffsetToPostList> dictionary;
    
    public Searcher(TreeIndex invertedIndex) throws IOException {
         this.postingFile = invertedIndex.postingFile;
         dictionary = invertedIndex.dictionary;

    }

private void getPostingsOfWords(Collection<String> listOfWords, Set<Integer> indexes) throws IOException {
        for (String key : listOfWords) {
            long offset = dictionary.get(key).offset;
            for (int i = 0; i < dictionary.get(key).docFrequency; i++) {
                postingFile.seek(offset);
                indexes.add(postingFile.readInt());
            }
        }
    }
}
