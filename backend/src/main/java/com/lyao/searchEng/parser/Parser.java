package com.lyao.searchEng.parser;

import java.util.Iterator;
import java.util.List;

public interface Parser extends Iterable<LineOfTerms> {
  int getDocId();
  Iterator<LineOfTerms> iterator();
  public List<String> getAuthor();
  public List<String> getTitle();
}
