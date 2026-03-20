package com.lyao.searchEng.IR.Tools;



public interface TempDictItem extends Comparable<TempDictItem> {
   float zoneWeight = 0;
   public float getIndexInfo();
   public int getDocID();
   public String indicator();
   
}
