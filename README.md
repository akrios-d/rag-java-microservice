
# 🧠 RAG Java Microservice

This project is a **Java 24 Spring AI RAG service** that integrates with **Ollama** for LLMs and **ChromaDB** for vector storage.  
It supports:
- **Multi-query retrieval**
- **Chat memory**
- **Requirements Analyst → Doc Writer agent workflow**
- **Confluence-ready document generation**

---

## 🚀 Getting Started

### 1. Clone the repo
```bash
git clone https://github.com/akrios-d/rag-java-microservice.git
cd rag-java-microservice
```

### 2. Build & Run with Docker Compose
```bash
docker compose up --build
```

This will start:
- `rag-service` → Spring Boot app (port **8081**)  
- `ollama` → LLM backend (port **11434**)  
- `chroma` → Vector database (port **8000**)  

---

## 🔍 Verify Services

- RAG API → [http://localhost:8081/query](http://localhost:8081/query)  
- Ollama health → [http://localhost:11434/api/tags](http://localhost:11434/api/tags)  
- Chroma API → [http://localhost:8000/api/v2/heartbeat](http://localhost:8000/api/v2/heartbeat)  

---

## 📡 Example Usage

### Ask a question
```bash
curl -X POST "http://localhost:8081/query?userId=test&multiQuery=true"      -H "Content-Type: application/json"      -d '{"question":"What is retrieval augmented generation?"}'
```

### Requirements Analyst Mode
```bash
curl -X POST "http://localhost:8081/requirements?userId=test"      -H "Content-Type: application/json"      -d '{"input":"I need a system for handling support tickets"}'
```

Once you type `generate`, the system switches to **Doc Writer** and produces a Confluence-ready document.

---

## ⚡ Models

Pull the embedding model before running queries:
```bash
docker exec -it ollama ollama pull all-minilm
```

And optionally pull LLMs:
```bash
docker exec -it ollama ollama pull llama3.2
```

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
id UUID PRIMARY KEY,
content TEXT NOT NULL,
metadata JSONB,
embedding VECTOR(384) -- ajuste para o tamanho do embedding do seu modelo
);
```

---

## 🔒 Security Notes
- Runs as **non-root** (distroless base image).  
- Minimal runtime environment (no shell, no package manager).  
- Data for Ollama and Chroma is persisted in **Docker volumes**.  

---

## 🛠 Development
Rebuild after changes:
```bash
docker compose up --build rag-service
```



