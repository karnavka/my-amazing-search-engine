package com.lyao.searchEng.IR.Tools;

public class ZonePostingItem implements TempDictItem {
        int docID;
        float zoneWeight;
        String indicator = "zone";

        public ZonePostingItem(int docID, float zone) {
            this.zoneWeight = zone;
            this.docID = docID;
        }

        @Override
        public int compareTo(TempDictItem arg0) {
            if (this.docID != arg0.getDocID()) {
                return Integer.compare(this.docID, arg0.getDocID());
            }
            return Float.compare(this.zoneWeight, arg0.getIndexInfo());
        }

        @Override
        public float getIndexInfo() {
            return this.zoneWeight;
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

