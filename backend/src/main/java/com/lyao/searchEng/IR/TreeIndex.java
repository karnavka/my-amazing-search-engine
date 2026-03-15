package com.lyao.searchEng.IR;

import com.lyao.searchEng.parser.LineOfTerms;
import com.lyao.searchEng.parser.Parser;
import com.lyao.searchEng.parser.ParserFactory;
import java.io.*;
import java.util.*;
import org.jetbrains.annotations.NotNull;

public class TreeIndex {
    private File sourceFolder;
    public File[] inputFiles;
    public TreeMap<String, OffsetToPostList> dictionary;
    public RandomAccessFile postingFile;
    private ArrayList<File> blocksOfPostings;
    private Iterator<LineOfTerms> currentParserIterator;
    private Parser currentParser;
    private int blockNumber;
    private long memorySize;
    private int currentDoc;

    public TreeIndex(String path) throws IOException {
        this.sourceFolder = new File(path);
        inputFiles = sourceFolder.listFiles();
        this.postingFile = new RandomAccessFile("./data/posting_list", "rw");
        this.postingFile.setLength(0);
        dictionary = new TreeMap<>();
        blockNumber = 0;
        memorySize = 40_000_000;
        currentDoc = 0;
        buildIndex();
    }

    private void buildIndex() throws IOException {
        this.currentParser = ParserFactory.createParser(inputFiles[currentDoc].getPath(), currentDoc);
        this.currentParserIterator = this.currentParser.iterator();
        blocksOfPostings = new ArrayList<>();
        createTempBlocksFiles();
        mergeFiles();

    }

    private void createTempBlocksFiles() throws IOException {
        while (currentParserIterator.hasNext()) {
            SPIMIInvert();
            // if (!this.currentParserIterator.hasNext()) {
            // currentDoc++;
            // if (currentDoc >= inputFiles.length) break;
            // Parser currentParser =
            // ParserFactory.createParser(inputFiles[currentDoc].getPath(), currentDoc);
            // currentParserIterator = currentParser.iterator();
            // }
        }
    }

    public void SPIMIInvert() throws IOException {

        int initialMemory = (int) java.lang.Runtime.getRuntime().freeMemory();
        int usedMemory = 0;

        Map<String, List<postingItem>> tempDictionary = new LinkedHashMap<>();

        while (usedMemory < this.memorySize) {
            if (!this.currentParserIterator.hasNext()) {

                this.currentParser.getAuthor().forEach(a -> {
                    tempDictionary.computeIfAbsent(a, k -> new ArrayList<>());
                    tempDictionary.get(a).add(new postingItem(currentParser.getDocId(), Zone.AUTHOR.getWeight()));
                });

                this.currentParser.getTitle().forEach(t -> {
                    tempDictionary.computeIfAbsent(t, k -> new ArrayList<>());
                    tempDictionary.get(t).add(new postingItem(currentParser.getDocId(), Zone.TITLE.getWeight()));
                });

                currentDoc++;
                if (currentDoc >= inputFiles.length)
                    break;
                this.currentParser = ParserFactory.createParser(inputFiles[currentDoc].getPath(), currentDoc);
                this.currentParserIterator = this.currentParser.iterator();
            }

            int currentMemory = (int) java.lang.Runtime.getRuntime().freeMemory();
            usedMemory = initialMemory - currentMemory;

            LineOfTerms chunk = this.currentParserIterator.next();
            ArrayList<String> terms = chunk.getTerms();
            int docID = chunk.getDocID();

            for (int i = 0; i < terms.size(); i++) {
                List<postingItem> postingsList;
                String term = terms.get(i);
                tempDictionary.computeIfAbsent(term, k -> new ArrayList<>());
                postingsList = tempDictionary.get(term);
                postingsList.add(new postingItem(docID, Zone.BODY.getWeight()));
            }

        }
        sortAndWriteBlockToFile(tempDictionary);

    }

    private void sortAndWriteBlockToFile(Map<String, List<postingItem>> dictionary) throws IOException {
        File tempFile = new File("block-" + blockNumber + ".txt");
        tempFile.deleteOnExit();
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
        dictionary.remove("");
        List<String> keys = new ArrayList<>(dictionary.keySet());
        Collections.sort(keys);
        List<String> lines = new ArrayList<>();
        for (String key : keys) {
            Collections.sort(dictionary.get(key));
            StringBuilder index = new StringBuilder();
            index.append(key).append("\t");
            for (postingItem item : dictionary.get(key)) {
                index.append(item.docID).append(",").append(item.zoneWeight).append(" ");
            }
            lines.add(index.toString());
            bw.write(index.toString() + "\n");

        }
        blocksOfPostings.add(tempFile);
        bw.close();
        this.blockNumber++;

    }

    private void mergeFiles() throws IOException {
        BufferedReader[] bufferedReaders = new BufferedReader[blocksOfPostings.size()];
        PriorityQueue<PostingLine> queue = new PriorityQueue<>();
        for (int i = 0; i < blocksOfPostings.size(); i++) {
            bufferedReaders[i] = new BufferedReader(new FileReader(blocksOfPostings.get(i)));
            String firstLine = bufferedReaders[i].readLine();
            if (firstLine != null)
                queue.add(new PostingLine(firstLine, i));
        }
        while (!queue.isEmpty()) {
            PostingLine postingLine = queue.poll();
            String term = postingLine.term;
            Set<postingItem> indexes = new TreeSet<>(postingLine.postings);
            Set<Integer> usedBlocks = new TreeSet<>();
            usedBlocks.add(postingLine.blockIndex);
            while (queue.peek() != null && queue.peek().term.equals(term)) {
                PostingLine sameTermPosting = queue.poll();
                indexes.addAll(sameTermPosting.postings);
                usedBlocks.add(sameTermPosting.blockIndex);
            }
            dictionary.put(term, new OffsetToPostList(postingFile.length(), indexes.size()));
            // putIntoPermutationIndex(term);
            // putIntoThreeGramIndex(term);
            for (postingItem index : indexes) {
                postingFile.writeInt(index.docID);
                postingFile.writeFloat(index.zoneWeight);
            }
            for (Integer index : usedBlocks) {
                String nextLine = bufferedReaders[index].readLine();
                if (nextLine != null)
                    queue.add(new PostingLine(nextLine, index));
            }

        }
        for (int i = 0; i < blocksOfPostings.size(); i++) {
            bufferedReaders[i].close();
        }
        // postingFile.close();
    }

    // class for storing the offset and document frequency of a term in the
    // dictionary
    public class OffsetToPostList {
        public long offset;
        public int docFrequency;

        public OffsetToPostList(long offset, int docFrequency) {
            this.offset = offset;
            this.docFrequency = docFrequency;
        }
    }

    // class for comparing lines during merging blocks and keeping track of which
    // block they came from
    // also contains the term and its postings list for the line
    private class PostingLine implements Comparable<PostingLine> {
        String term;
        ArrayList<postingItem> postings;
        int blockIndex;

        public PostingLine(String line, int blockIndex) {
            String[] parts = line.split("\t", 2);
            this.term = parts[0];
            this.postings = new ArrayList<>();

            if (parts.length > 1 && !parts[1].isBlank()) {
                String[] postingTokens = parts[1].trim().split("\\s+");
                for (String postingToken : postingTokens) {
                    String[] docZone = postingToken.split(",");
                    int docID = Integer.parseInt(docZone[0]);
                    float zone = Float.parseFloat(docZone[1]);
                    postings.add(new postingItem(docID, zone));
                }
            }

            this.blockIndex = blockIndex;
        }

        @Override
        public int compareTo(@NotNull PostingLine o) {
            return this.term.compareTo(o.term);
        }
    }

    private class postingItem implements Comparable<postingItem> {
        int docID;
        float zoneWeight;

        public postingItem(int docID, float zone) {
            this.zoneWeight = zone;
            this.docID = docID;
        }

        @Override
        public int compareTo(postingItem arg0) {
            if (this.docID != arg0.docID) {
                return Integer.compare(this.docID, arg0.docID);
            }
            return Float.compare(this.zoneWeight, arg0.zoneWeight);
        }
    }
}
