package com.lyao.searchEng.parser;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import com.lyao.searchEng.IR.Zone;

public class EpubParser implements Parser {
    int docID;
    Book book;
    public EpubParser(String filename, int docID)  {
        this.docID = docID;
        EpubReader epubReader = new EpubReader();
        try {
            book = epubReader.readEpub(new FileInputStream(filename));
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @NotNull
    @Override
    public Iterator<LineOfTerms> iterator() {
        return new EpubIterator(book, docID);
    }

    @Override
    public int getDocId() {
        return docID;
    }

    public class EpubIterator implements Iterator<LineOfTerms> {
        Book book;
        int docID;
        Queue<Resource> res;
        EpubIterator(Book book, int docID) {
            this.book = book;
            this.docID = docID;
            res = new LinkedList<>();
            res.addAll(book.getContents());
        }

        @Override
        public boolean hasNext() {
            return res.peek() != null;
        }

        @Override
        public LineOfTerms next() throws RuntimeException {
            String xhtml = "";
            try {
                xhtml = new String(res.poll().getData(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String text = Jsoup.parse(xhtml).text();
            try {
                return new LineOfTerms(text, docID, Zone.BODY);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<String> getAuthor() {
        return book.getMetadata().getAuthors().stream().map(a -> a.getFirstname() + " " + a.getLastname()).toList();
    }

    @Override
    public List<String> getTitle() {
        return book.getMetadata().getTitles();
    }
}
