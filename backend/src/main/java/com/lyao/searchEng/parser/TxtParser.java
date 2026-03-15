package com.lyao.searchEng.parser;
import org.jetbrains.annotations.NotNull;

import com.lyao.searchEng.IR.Zone;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class TxtParser implements Parser {
    private final String path;
    private final int docID;

    public TxtParser(String path, int docID) {
        this.path = path;
        this.docID = docID;
    }

    @NotNull
    @Override
    public Iterator<LineOfTerms> iterator() {
        try {
            return new TxtIterator(new BufferedReader(new FileReader(path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class TxtIterator implements Iterator<LineOfTerms> {
        BufferedReader reader;
        String nextLine;

        TxtIterator(BufferedReader reader) throws IOException {
            this.reader = reader;
            nextLine = reader.readLine();
        }

        @Override
        public boolean hasNext() {
            return nextLine != null;
        }

        @Override
        public LineOfTerms next() {
            String line = nextLine;
            try {
                nextLine = reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                return new LineOfTerms(line, docID, Zone.BODY);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    public int getDocId() {
        return docID;
    }

    @Override
    public List<String> getAuthor() {
       return List.of("");
    }

    @Override
    public List<String> getTitle() {
        return List.of(path);
    }
}

