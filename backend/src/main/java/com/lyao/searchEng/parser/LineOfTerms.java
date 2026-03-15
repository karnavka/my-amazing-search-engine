package com.lyao.searchEng.parser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import com.lyao.searchEng.IR.Zone;

import java.io.IOException;
import java.util.ArrayList;

public class LineOfTerms {
    String text;
    int docID;
    ArrayList<String> terms;
    Zone zone;
    public LineOfTerms(String text, int docID, Zone zone) throws IOException {
        this.text = text;
        this.docID = docID;
        this.zone = zone;
        normaliseWords();
    }

    private void normaliseWords () throws IOException {
        terms = new ArrayList<>();
        Analyzer  analyzer =  new EnglishAnalyzer();
        TokenStream ts = analyzer.tokenStream("field", text);
        ts.reset();
    

        while (ts.incrementToken()) {
           terms.add(ts.getAttribute(CharTermAttribute.class).toString());
        }
        analyzer.close();
    }

    public  ArrayList<String> getTerms() {
        return terms;
    }
    public int getDocID() {return docID;}
    public Zone getZone() {return zone;}
}
