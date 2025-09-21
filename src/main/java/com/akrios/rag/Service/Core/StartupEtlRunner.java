package com.akrios.rag.Service.Core;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class StartupEtlRunner {

    private final EtlPipelineService etl;

    public StartupEtlRunner(EtlPipelineService etl) {
        this.etl = etl;
    }

    @PostConstruct
    public void init() {
        etl.runEtl();
    }
}