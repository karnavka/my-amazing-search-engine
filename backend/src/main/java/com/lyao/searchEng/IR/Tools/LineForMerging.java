package com.lyao.searchEng.IR.Tools;

import java.util.List;

public interface LineForMerging extends Comparable<LineForMerging> {
   String getTerm();
   List<TempDictItem> getPostings();
   int getBlockIndex();
}
