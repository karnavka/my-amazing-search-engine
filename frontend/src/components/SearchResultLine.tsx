import React from "react";
import "./SearchResultLine.css";
export const SearchResultLine = ( { result }: { result: string} ) => {
  return <div className = "search-result-line">{result}</div>;
};
