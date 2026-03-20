declare module "*.png";
declare module "*.svg";
import { useState } from "react";
import "./App.css";
import { SearchBar } from "./components/SearchBar.tsx";
import Logo from "./assets/guglya.png";
import { SearchResults } from "./components/SearchResults.tsx";

function App() {

  const [results, setResults] = useState([]);

  const search = async (query: string) => {
    console.log("Searching for:", query);
    fetch(`http://localhost:8080/searchByZone?q=${query}`)
      .then((responce) => responce.json())
      .then((json) => {
        setResults(json);
      });
  };

  return (
    <div className="App">
      <div className="search-container">
        <img src={Logo} /> 
        <SearchBar onSearch={search} />
          {/*  <div>SearchHints </div> */}
        <SearchResults results={results} />
      </div>
    </div>
  );
}

export default App;
