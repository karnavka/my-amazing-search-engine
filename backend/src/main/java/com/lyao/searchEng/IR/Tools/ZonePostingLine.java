package com.lyao.searchEng.IR.Tools;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

    // class for comparing lines during merging blocks and keeping track of which
    // block they came from
    // also contains the term and its postings list for the line
    public class ZonePostingLine implements LineForMerging {
        String term;
        ArrayList<TempDictItem> postings;
        int blockIndex;

        public ZonePostingLine(String line, int blockIndex) {
            String[] parts = line.split("\t", 2);
            this.term = parts[0];
            this.postings = new ArrayList<>();

            if (parts.length > 1 && !parts[1].isBlank()) {
                String[] postingTokens = parts[1].trim().split("\\s+");
                for (String postingToken : postingTokens) {
                    String[] docZone = postingToken.split(",");
                    int docID = Integer.parseInt(docZone[0]);
                    float zone = Float.parseFloat(docZone[1]);
                    postings.add(new ZonePostingItem(docID, zone));
                }
            }

            this.blockIndex = blockIndex;
        }

        @Override
        public int compareTo(@NotNull LineForMerging o) {
            return this.term.compareTo(o.getTerm());
        }

        @Override
        public String getTerm() {
            return this.term;
        }

        @Override
        public List<TempDictItem> getPostings() {
            return postings;
        }

        @Override
        public int getBlockIndex() {
            return blockIndex;
        }
    }
