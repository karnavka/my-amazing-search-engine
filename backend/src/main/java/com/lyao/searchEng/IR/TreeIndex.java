package com.lyao.searchEng.IR;

import com.lyao.searchEng.IR.Tools.LineForMerging;
import com.lyao.searchEng.IR.Tools.LineForMergingFactory;
import com.lyao.searchEng.IR.Tools.TempDictItem;
import com.lyao.searchEng.IR.Tools.ZonePostingItem;
import com.lyao.searchEng.IR.Tools.ZonePostingLine;
import com.lyao.searchEng.parser.LineOfTerms;
import com.lyao.searchEng.parser.Parser;
import com.lyao.searchEng.parser.ParserFactory;

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.geo.Line;

public class TreeIndex {
    private File sourceFolder;
    public File[] inputFiles;
    public RandomAccessFile zonePostingFile, regularPostingFile, positionalPostingFile;
    private ArrayList<File> blocksOfZonePostings, blocksOfPositionalPostings;
    private Iterator<LineOfTerms> currentParserIterator;
    private Parser currentParser;
    private long memorySize;
    private int currentDoc, wordPosition, blockNumber;
    public TreeMap<String, OffsetToPostList> dictionary;
    public TreeMap<String, TreeSet<String>> threeGramIndex;
    public int numberOfFiles;

    public TreeIndex(String path) throws IOException {
        this.sourceFolder = new File(path);

        System.out.println("PATH ARG: " + path);
        System.out.println("ABSOLUTE PATH: " + sourceFolder.getAbsolutePath());
        System.out.println("EXISTS: " + sourceFolder.exists());
        System.out.println("IS DIRECTORY: " + sourceFolder.isDirectory());
        inputFiles = sourceFolder.listFiles();

         if (inputFiles == null || inputFiles.length == 0) {
        throw new IllegalArgumentException(
            "No input files found in: " + sourceFolder.getAbsolutePath()
        );
    }
        numberOfFiles = inputFiles.length;
  
        File dataDir = new File("./data");
        if (!dataDir.exists())
            dataDir.mkdirs();
        this.zonePostingFile = new RandomAccessFile("./data/zone_posting_list", "rw");
        this.zonePostingFile.setLength(0);
        this.regularPostingFile = new RandomAccessFile("./data/regular_posting_list", "rw");
        this.regularPostingFile.setLength(0);
        this.positionalPostingFile = new RandomAccessFile("./data/positional_posting_list", "rw");
        this.positionalPostingFile.setLength(0);
        dictionary = new TreeMap<>();
        threeGramIndex = new TreeMap<>();
        memorySize = 40_000_000;
        currentDoc = 0;
        blockNumber = 0;
        wordPosition = 0;
        buildIndex();
    }

    private void buildIndex() throws IOException {
        this.currentParser = ParserFactory.createParser(inputFiles[currentDoc].getPath(), currentDoc);
        this.currentParserIterator = this.currentParser.iterator();
        this.blocksOfZonePostings = new ArrayList<>();
        this.blocksOfPositionalPostings = new ArrayList<>();

        createTempBlocksFiles();

        mergeFiles(blocksOfPositionalPostings, "positional", positionalPostingFile);
        mergeFiles(blocksOfZonePostings, "zone", zonePostingFile);
        buildThreeGramIndex();
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

        Map<String, List<Integer>> tempDictionary = new LinkedHashMap<>();
        Map<String, List<TempDictItem>> tempZoneDictionary = new LinkedHashMap<>();
        Map<String, List<TempDictItem>> tempPositionalDictionary = new LinkedHashMap<>();

        while (usedMemory < this.memorySize) {
            if (!this.currentParserIterator.hasNext()) {
                putAutorAndTitleIntoZoneIndex(tempZoneDictionary);
                currentDoc++;
                if (currentDoc >= inputFiles.length)
                    break;
                this.currentParser = ParserFactory.createParser(inputFiles[currentDoc].getPath(), currentDoc);
                this.currentParserIterator = this.currentParser.iterator();
                wordPosition = 0;
            }

            int currentMemory = (int) java.lang.Runtime.getRuntime().freeMemory();
            usedMemory = initialMemory - currentMemory;

            LineOfTerms chunk = this.currentParserIterator.next();
            ArrayList<String> terms = chunk.getTerms();
            int docID = chunk.getDocID();

            for (int i = 0; i < terms.size(); i++) {
                String term = terms.get(i);

                List<TempDictItem> zonePostingsList;
                tempZoneDictionary.computeIfAbsent(term, k -> new ArrayList<>());
                zonePostingsList = tempZoneDictionary.get(term);
                zonePostingsList.add(new ZonePostingItem(docID, Zone.BODY.getWeight()));

                List<TempDictItem> posPostingsList;
                tempPositionalDictionary.computeIfAbsent(term, k -> new ArrayList<>());
                posPostingsList = tempPositionalDictionary.get(term);
                posPostingsList.add(new ZonePostingItem(docID, wordPosition++));
            }

        }
        sortAndWriteBlockToFile(tempZoneDictionary, "zone");
        sortAndWriteBlockToFile(tempPositionalDictionary, "pos");
    }

    private void putAutorAndTitleIntoZoneIndex(Map<String, List<TempDictItem>> tempDictionary) {
        this.currentParser.getAuthor().forEach(a -> {
            try {
                for(String term : normilizeQuery(a)) {
                    tempDictionary.computeIfAbsent(term, k -> new ArrayList<>());
                    tempDictionary.get(term).add(new ZonePostingItem(currentParser.getDocId(), Zone.AUTHOR.getWeight()));
                }
            } catch (IOException e) {
              System.err.println("Error normalizing author term: " + a);
            }
        });

        this.currentParser.getTitle().forEach(t -> {
            try {
                for(String term : normilizeQuery(t)) {
                    tempDictionary.computeIfAbsent(term, k -> new ArrayList<>());
                    tempDictionary.get(term).add(new ZonePostingItem(currentParser.getDocId(), Zone.TITLE.getWeight()));
                }
            } catch (IOException e) {
              System.err.println("Error normalizing title term: " + t);
            }
        });
    }

    private void sortAndWriteBlockToFile(Map<String, List<TempDictItem>> tempDictionary, String indicator)
            throws IOException {
        File tempFile = new File("block-" + blockNumber + indicator + ".txt");
        tempFile.deleteOnExit();
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
        tempDictionary.remove("");
        List<String> keys = new ArrayList<>(tempDictionary.keySet());
        Collections.sort(keys);
        List<String> lines = new ArrayList<>();
        for (String key : keys) {
            Collections.sort(tempDictionary.get(key));
            StringBuilder index = new StringBuilder();
            index.append(key).append("\t");
            for (TempDictItem item : tempDictionary.get(key)) {
                index.append(item.getDocID()).append(",").append(item.getIndexInfo()).append(" ");
            }
            lines.add(index.toString());
            bw.write(index.toString() + "\n");

        }
        if (indicator.equals("zone"))
            blocksOfZonePostings.add(tempFile);
        else if (indicator.equals("pos"))
            blocksOfPositionalPostings.add(tempFile);
        bw.close();
        this.blockNumber++;
    }

    private void mergeFiles(ArrayList<File> blocks, String indicator, RandomAccessFile outputFile) throws IOException {
        BufferedReader[] bufferedReaders = new BufferedReader[blocks.size()];

        PriorityQueue<LineForMerging> queue = new PriorityQueue<>();
        for (int i = 0; i < blocks.size(); i++) {
            bufferedReaders[i] = new BufferedReader(new FileReader(blocks.get(i)));
            String firstLine = bufferedReaders[i].readLine();
            if (firstLine != null) {
                queue.add(LineForMergingFactory.createLineForMerging(firstLine, i, indicator));
            }
        }
        while (!queue.isEmpty()) {

            LineForMerging postingLine = queue.poll();
            String term = postingLine.getTerm();
            Set<TempDictItem> indexes = new TreeSet<>(postingLine.getPostings());
            Set<Integer> usedBlocks = new TreeSet<>();
            usedBlocks.add(postingLine.getBlockIndex());
            while (!queue.isEmpty() && queue.peek().getTerm().equals(term)) {
                LineForMerging sameTermPosting = queue.poll();
                indexes.addAll(sameTermPosting.getPostings());
                usedBlocks.add(sameTermPosting.getBlockIndex());
            }

            if (dictionary.containsKey(term)) {
                if (indicator.equals("zone")) {
                    dictionary.get(term).zoneOffset = outputFile.length();
                    dictionary.get(term).zoneDocFrequency = indexes.size();
                } else if (indicator.equals("pos")) {
                    dictionary.get(term).positionalOffset = outputFile.length();
                    dictionary.get(term).docFrequency = indexes.size();
                }
            } else if (indicator.equals("zone")) {
                dictionary.put(term, new OffsetToPostList(outputFile.length(), -1, -1, 0, indexes.size()));
            } else if (indicator.equals("pos")) {
                dictionary.put(term, new OffsetToPostList(-1, -1, outputFile.length(), indexes.size(), 0));
            }

            for (TempDictItem index : indexes) {
                outputFile.writeInt(index.getDocID());
                outputFile.writeFloat(index.getIndexInfo());
            }
            for (Integer index : usedBlocks) {
                String nextLine = bufferedReaders[index].readLine();
                if (nextLine != null) {
                    queue.add(LineForMergingFactory.createLineForMerging(nextLine, index, indicator));
                }
            }
        }
        for (BufferedReader reader : bufferedReaders) {
            if (reader != null) {
                reader.close();
            }
        }
    }

    // class for storing the offset and document frequency of a term in the
    // dictionary
    public class OffsetToPostList {
        public long zoneOffset = -1;
        public long regularOffset = -1;
        public long positionalOffset = -1;
        public int docFrequency = 0;
        public int zoneDocFrequency = 0;

        public OffsetToPostList() {
        }

        public OffsetToPostList(long zoneOffset, long regularOffset, long positionalOffset, int docFrequency,
                int zoneDocFrequency) {
            this.zoneOffset = zoneOffset;
            this.regularOffset = regularOffset;
            this.positionalOffset = positionalOffset;
            this.docFrequency = docFrequency;
            this.zoneDocFrequency = zoneDocFrequency;
        }
    }

    private void buildThreeGramIndex() {
        for (String term : dictionary.keySet()) {
            putIntoThreeGramIndex(term);
        }
    }

    private void putIntoThreeGramIndex(String term) {
        StringBuilder threeGramTerm = new StringBuilder(term);
        threeGramTerm.append('$');
        threeGramTerm.insert(0, '$');
        for (int ch = 0; ch <= threeGramTerm.length() - 3; ch++) {
            String threeGram = threeGramTerm.substring(ch, ch + 3);
            threeGramIndex
                    .computeIfAbsent(threeGram, k -> new TreeSet<>())
                    .add(term);
        }
    }

    public void close() throws IOException {
        if (zonePostingFile != null)
            zonePostingFile.close();
        if (regularPostingFile != null)
            regularPostingFile.close();
        if (positionalPostingFile != null)
            positionalPostingFile.close();
    }

    public List<ZonePostingItem> readZonePostingList(long offset, int zoneDocFr) throws IOException {
        List<ZonePostingItem> t = new ArrayList<>();
        zonePostingFile.seek(offset);
        for (int i = 0; i < zoneDocFr; i++) {
            t.add(new ZonePostingItem(zonePostingFile.readInt(), zonePostingFile.readFloat()));
        }
        return t;
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
}
