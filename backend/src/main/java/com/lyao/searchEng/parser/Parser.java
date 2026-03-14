package com.lyao.searchEng.parser;

import java.util.Iterator;

public interface Parser extends Iterable<LineOfTerms> {
 int getDocId();
  Iterator<LineOfTerms> iterator();
}
