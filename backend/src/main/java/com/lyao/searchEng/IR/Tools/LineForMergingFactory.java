package com.lyao.searchEng.IR.Tools;

public class LineForMergingFactory {
    public static LineForMerging createLineForMerging(String line, int blockIndex, String indicator) {
        if(indicator.equals("zone")) {
            return new ZonePostingLine(line, blockIndex);
        }
     //   if(indicator.equals("regular")) {
      //      return new RegularPostingLine(line, blockIndex);
      //  }
        if(indicator.equals("positional")) {
            return new PositionalPostingLine(line, blockIndex);
        }
        else throw new IllegalArgumentException();
    }
}
