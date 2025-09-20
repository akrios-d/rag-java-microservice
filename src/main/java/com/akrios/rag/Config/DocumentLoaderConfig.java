package com.akrios.rag.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentLoaderConfig {

    @Value("${rag.useConfluence:false}")
    public boolean useConfluence;
}