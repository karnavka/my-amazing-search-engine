import React from 'react';
import './SearchBar.css';

import {FaSearch} from 'react-icons/fa';

export const SearchBar = () => {
    return <div className = "input-wrapper">
        <FaSearch id = 'search-icon' />
        <input placeholder="Шукати тут..." />
    </div>
};