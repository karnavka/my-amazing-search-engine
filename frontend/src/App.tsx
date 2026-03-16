declare module "*.png";
declare module "*.svg";
import { useState } from 'react'
import './App.css'
import { SearchBar } from './components/SearchBar.tsx';
import Logo from './assets/guglya.png';

function App() {
  

  return (
        <div className="App">
          <div className="search-container">
             <div className="app-icon">
              <img src={Logo} />
          </div>
          <SearchBar />
          <div>Search results</div>
          </div>
        </div>
       
  );
}

export default App;
