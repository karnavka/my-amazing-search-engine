package com.lyao.searchEng.IR.Tools;

public class PositionalPostingItem implements TempDictItem {
        int docID;
        float position;
        String indicator = "pos";

        public PositionalPostingItem(int docID, float position) {
            this.position = position;
            this.docID = docID;
        }

        @Override
        public int compareTo(TempDictItem arg0) {
            if (this.docID != arg0.getDocID()) {
                return Integer.compare(this.docID, arg0.getDocID());
            }
            return Float.compare(this.position, arg0.getIndexInfo());
        }

        @Override
        public float getIndexInfo() {
            return this.position;
        }

        @Override
        public int getDocID() {
            return this.docID;
        }

        @Override
        public String indicator() {
            return this.indicator;
        }
    }