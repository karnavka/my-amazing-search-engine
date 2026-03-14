package com.lyao.searchEng.parser;

public class ParserFactory {
    public static Parser createParser(String filename, int docID) {
        if(filename.endsWith(".epub")) {
            return new EpubParser(filename, docID);
        }
        if(filename.endsWith(".txt")) {
            return new TxtParser(filename, docID);
        }
        else throw new IllegalArgumentException();
    }
}
