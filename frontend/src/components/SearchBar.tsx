import React from "react";
import "./SearchBar.css";
import { useState } from "react";

import { FaSearch } from "react-icons/fa";

type SearchBarProps = {
  onSearch: (query: string) => void;
};

export const SearchBar = ({ onSearch }: SearchBarProps) => {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      onSearch(query);
    }
  };

  const [query, setQuery] = useState("");
  return (
    <div className="input-wrapper">
      <FaSearch id="search-icon" />
      <input
        placeholder="Шукати тут..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={handleKeyDown}
      />
    </div>
  );
};
