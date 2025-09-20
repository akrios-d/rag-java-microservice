```mermaid
flowchart TD
    A["Local Files & Confluence"] --> B["DocumentLoaderService"]
    B --> C["VectorStoreService"]
    C --> D["Vector Store (PGVector / Chroma)"]
    C -->|Chunking & Embeddings| D
    E["User Question"] --> F["RagService"]
    F --> D
    D --> F
    F --> G["Generated Answer"]

    style A fill:#f9f,stroke:#333,stroke-width:1px
    style B fill:#9f9,stroke:#333,stroke-width:1px
    style C fill:#ff9,stroke:#333,stroke-width:1px
    style D fill:#9ff,stroke:#333,stroke-width:1px
    style F fill:#f99,stroke:#333,stroke-width:1px
    style G fill:#ccc,stroke:#333,stroke-width:1px
