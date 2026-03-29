package com.lyao.searchEng.config;

import com.lyao.searchEng.services.Clusterizer;
import com.lyao.searchEng.IR.TreeIndex;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class AppConfig {

    @Bean
    public TreeIndex treeIndex() throws IOException {
        return new TreeIndex("E:\\Study Second Year\\2_trim\\IR\\my-amazing-search-engine\\backend\\data\\clust"); 
    }

    @Bean
    public Clusterizer clusterizer(TreeIndex treeIndex) throws IOException {
        return new Clusterizer(treeIndex);
    }
}