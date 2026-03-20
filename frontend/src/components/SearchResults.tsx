import React from "react";
import "./SearchResults.css";
import { SearchResultLine } from "./SearchResultLine";
export const SearchResults = ({ results }: { results: string[] }) => {
    return (
        <>
            {results.length !== 0 ? (
                 <div className="results-list">
                      <div className="add-info">Знайдено {results.length} результатів</div>
                    {results.map((result, index) => (
                        <SearchResultLine result={result} key={index} />
                    ))}
                </div>
            ) : null}
        </>
    );
};
