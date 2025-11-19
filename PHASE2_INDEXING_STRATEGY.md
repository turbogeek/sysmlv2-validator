# Phase 2E: Indexing Strategy for LLM Model Queries

## Overview

To enable fast, efficient LLM-based queries on large SysML v2 models, we need a multi-layered indexing strategy combining traditional search indexing with modern vector embeddings.

---

## Indexing Layers

### Layer 1: Symbol Table Index (In-Memory)

**Purpose**: Fast O(1) lookup by qualified name
**Technology**: Java HashMap (already implemented in SymbolTable)
**Use Cases**:
- Direct element lookup: "Get part def Vehicle::Engine"
- Type filtering: "Get all requirement definitions"
- Scope traversal: "Get all elements in package SystemDesign"

**Example**:
```java
Symbol symbol = symbolTable.resolveQualified("Vehicle::Engine::FuelSystem");
List<Symbol> requirements = symbolTable.getSymbolsByType(ElementType.REQUIREMENT_DEFINITION);
```

**Pros**: Ultra-fast, no dependencies, already implemented
**Cons**: No full-text search, no semantic search

---

### Layer 2: Full-Text Search Index (Apache Lucene)

**Purpose**: Fast text search across model content
**Technology**: Apache Lucene (embedded, no server required)
**Use Cases**:
- Keyword search: "Find all elements mentioning 'safety'"
- Doc comment search: "Search documentation for 'emergency shutdown'"
- Fuzzy matching: "Find 'reqirement' (typo tolerance)"

**Implementation**:
```java
class ModelIndexer {
    private Directory index;
    private IndexWriter writer;
    private StandardAnalyzer analyzer;

    void indexModel(SemanticModel model) {
        for (Symbol symbol : model.getAllSymbols()) {
            Document doc = new Document();

            // Indexed fields
            doc.add(new TextField("name", symbol.getName(), Field.Store.YES));
            doc.add(new TextField("qualifiedName", symbol.getQualifiedName(), Field.Store.YES));
            doc.add(new StringField("type", symbol.getType().toString(), Field.Store.YES));
            doc.add(new TextField("docComment", getDocComment(symbol), Field.Store.YES));

            // Store source location
            doc.add(new StoredField("location", symbol.getLocation().toString()));

            writer.addDocument(doc);
        }
        writer.commit();
    }

    List<SearchResult> search(String query, int maxResults) {
        QueryParser parser = new QueryParser("docComment", analyzer);
        Query q = parser.parse(query);

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(index));
        TopDocs docs = searcher.search(q, maxResults);

        return convertToResults(docs);
    }
}
```

**Indexed Fields**:
- Element name
- Qualified name
- Element type
- Doc comments
- Attribute values
- Constraint expressions

**Query Examples**:
```
name:Engine
type:REQUIREMENT_DEFINITION AND docComment:safety
qualifiedName:Vehicle::*
```

**Pros**: Fast text search, fuzzy matching, field-based queries
**Cons**: No semantic understanding, exact keyword matching

**Dependencies**: Apache Lucene 9.x (add to pom.xml)

---

### Layer 3: Vector Embeddings (Semantic Search)

**Purpose**: Semantic similarity search for LLM queries
**Technology**:
- **Vector DB**: Chroma DB (Python) OR Milvus (Java-friendly) OR pgvector (PostgreSQL)
- **Embeddings**: OpenAI `text-embedding-3-small` OR local model (sentence-transformers)

**Use Cases**:
- Semantic search: "Components related to power management"
- Conceptual queries: "Safety-critical subsystems"
- Similar element finding: "Find parts similar to this design"
- LLM context selection: "Most relevant elements for this query"

**Implementation**:
```java
class VectorIndexer {
    private MilvusClient milvusClient;
    private EmbeddingService embeddingService;

    void indexModel(SemanticModel model) {
        for (Symbol symbol : model.getAllSymbols()) {
            // Create text representation
            String text = buildTextRepresentation(symbol);

            // Generate embedding (768 dimensions)
            float[] embedding = embeddingService.embed(text);

            // Store in vector DB
            milvusClient.insert(CollectionName.MODEL_ELEMENTS,
                Map.of(
                    "id", symbol.getQualifiedName(),
                    "embedding", embedding,
                    "metadata", buildMetadata(symbol)
                )
            );
        }
    }

    List<Symbol> semanticSearch(String query, int topK) {
        // Generate query embedding
        float[] queryEmbedding = embeddingService.embed(query);

        // Search for similar vectors
        SearchResult results = milvusClient.search(
            CollectionName.MODEL_ELEMENTS,
            queryEmbedding,
            topK
        );

        return results.stream()
            .map(r -> resolveSymbol(r.getId()))
            .collect(Collectors.toList());
    }

    String buildTextRepresentation(Symbol symbol) {
        StringBuilder sb = new StringBuilder();

        // Include name and type
        sb.append(symbol.getType()).append(": ").append(symbol.getName()).append("\n");

        // Include doc comments
        String doc = getDocComment(symbol);
        if (doc != null) sb.append(doc).append("\n");

        // Include attributes (for parts)
        for (Symbol attr : getAttributes(symbol)) {
            sb.append("  - ").append(attr.getName()).append("\n");
        }

        // Include requirements (for parts)
        for (Symbol req : getRelatedRequirements(symbol)) {
            sb.append("  satisfies: ").append(req.getName()).append("\n");
        }

        return sb.toString();
    }
}
```

**Vector DB Options**:

| Option | Pros | Cons | Recommendation |
|--------|------|------|----------------|
| **Chroma DB** | Easy setup, Python-friendly | Requires Python bridge | Good for prototyping |
| **Milvus** | Java client, scalable | More complex setup | Best for production |
| **pgvector** | PostgreSQL extension, SQL queries | Requires PostgreSQL | Good if already using PostgreSQL |
| **FAISS** | Facebook's library, very fast | C++ with JNI bindings | Best performance |

**Recommendation**: Start with **Milvus Lite** (embedded version, no server) for Phase 2E, migrate to full Milvus if needed.

**Pros**: Semantic understanding, handles synonyms and concepts
**Cons**: Requires embeddings API or local model, additional complexity

---

### Layer 4: Graph Index (Relationship Traversal)

**Purpose**: Fast relationship queries (satisfy, verify, allocate)
**Technology**: In-memory graph OR Neo4j (if scale requires)
**Use Cases**:
- Traceability: "What verifies REQ-001?"
- Impact analysis: "What depends on Engine?"
- Path finding: "Trace from requirement to verification"

**Implementation**:
```java
class RelationshipGraph {
    private Map<String, Set<Relationship>> outgoingEdges;
    private Map<String, Set<Relationship>> incomingEdges;

    void indexRelationships(SemanticModel model) {
        for (Symbol symbol : model.getAllSymbols()) {
            // Index specializations
            for (Symbol spec : symbol.getSpecializations()) {
                addEdge(symbol.getQualifiedName(), spec.getQualifiedName(),
                    RelationshipType.SPECIALIZES);
            }

            // Index redefinitions
            for (Symbol redef : symbol.getRedefinitions()) {
                addEdge(symbol.getQualifiedName(), redef.getQualifiedName(),
                    RelationshipType.REDEFINES);
            }

            // Index satisfy relationships (from model analysis)
            for (Relationship rel : getSatisfyRelationships(symbol)) {
                addEdge(rel.getSource(), rel.getTarget(), RelationshipType.SATISFIES);
            }
        }
    }

    List<Symbol> getRelatedElements(String qualifiedName, RelationshipType type) {
        Set<Relationship> edges = outgoingEdges.get(qualifiedName);
        return edges.stream()
            .filter(e -> e.getType() == type)
            .map(e -> resolveSymbol(e.getTarget()))
            .collect(Collectors.toList());
    }

    List<List<Symbol>> findPaths(String from, String to, int maxDepth) {
        // BFS or DFS to find all paths
        return pathFinder.findAllPaths(from, to, maxDepth);
    }
}
```

**Indexed Relationships**:
- Specialization (`:>`)
- Redefinition (`:>>`)
- Subsetting
- Satisfaction (satisfy)
- Verification (verify)
- Allocation (allocate)
- Feature typing
- Dependency

**Query Examples**:
```java
// What requirements does Engine satisfy?
List<Symbol> reqs = graph.getRelatedElements("Vehicle::Engine", RelationshipType.SATISFIES);

// Trace from requirement to verification
List<List<Symbol>> paths = graph.findPaths("REQ-001", "VERIFY-TEST-001", 5);
```

**Pros**: Fast graph traversal, supports complex queries
**Cons**: Memory usage for large models

**Scale Decision**:
- **< 10K elements**: In-memory graph (Java)
- **> 10K elements**: Consider Neo4j embedded

---

## Indexing Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    SysML v2 Model                           │
│               (Parse Tree → Semantic Model)                 │
└─────────────────────────────────────────────────────────────┘
                             ↓
                ┌────────────┴────────────┐
                ↓                         ↓
┌──────────────────────────┐  ┌──────────────────────────┐
│   Symbol Table Index     │  │  Relationship Graph       │
│   (In-Memory HashMap)    │  │  (In-Memory Adjacency)    │
│   - O(1) lookup          │  │  - Graph traversal        │
│   - Type filtering       │  │  - Path finding           │
└──────────────────────────┘  └──────────────────────────┘
                ↓                         ↓
┌──────────────────────────┐  ┌──────────────────────────┐
│   Lucene Index           │  │  Vector Index             │
│   (On-Disk)              │  │  (Milvus Lite)            │
│   - Full-text search     │  │  - Semantic search        │
│   - Keyword queries      │  │  - Similarity             │
└──────────────────────────┘  └──────────────────────────┘
                             ↓
                ┌────────────┴────────────┐
                ↓                         ↓
┌──────────────────────────┐  ┌──────────────────────────┐
│   LLM Query Router       │  │   Report Generator        │
│   - Select best index    │  │   - Query for content     │
│   - Combine results      │  │   - Build narratives      │
└──────────────────────────┘  └──────────────────────────┘
```

---

## Query Routing Strategy

Different query types use different indexes:

```java
class QueryRouter {
    Symbol byQualifiedName(String qname) {
        // Use Layer 1: Symbol Table
        return symbolTable.resolveQualified(qname);
    }

    List<Symbol> byKeyword(String keyword) {
        // Use Layer 2: Lucene
        return luceneIndex.search(keyword, 100);
    }

    List<Symbol> bySemantic(String query) {
        // Use Layer 3: Vector embeddings
        return vectorIndex.semanticSearch(query, 20);
    }

    List<Symbol> byRelationship(String from, RelationshipType type) {
        // Use Layer 4: Graph
        return relationshipGraph.getRelatedElements(from, type);
    }

    // Hybrid query combining multiple indexes
    List<Symbol> hybridQuery(String naturalLanguageQuery) {
        // 1. Extract entities and intent
        QueryIntent intent = analyzeIntent(naturalLanguageQuery);

        // 2. Route to appropriate indexes
        List<Symbol> candidates = new ArrayList<>();

        if (intent.hasExactName()) {
            Symbol exact = byQualifiedName(intent.getExactName());
            if (exact != null) return List.of(exact);
        }

        if (intent.hasKeywords()) {
            candidates.addAll(byKeyword(intent.getKeywords()));
        }

        if (intent.isSemanticQuery()) {
            candidates.addAll(bySemantic(naturalLanguageQuery));
        }

        if (intent.hasRelationshipConstraint()) {
            candidates = filterByRelationship(candidates, intent.getRelationship());
        }

        // 3. Rank and return
        return rankResults(candidates, naturalLanguageQuery);
    }
}
```

---

## Performance Targets

| Index Type | Index Build Time | Query Time | Memory Usage |
|------------|------------------|------------|--------------|
| Symbol Table | < 1s per 10K elements | < 1ms | ~100 KB per 1K elements |
| Lucene | < 5s per 10K elements | < 50ms | ~10 MB per 10K elements |
| Vector Index | < 30s per 10K elements | < 100ms | ~3 MB per 1K elements (768-dim) |
| Relationship Graph | < 2s per 10K elements | < 10ms | ~50 KB per 1K elements |

**Assumptions**:
- 10K elements = typical medium SysML model
- Vector dimensions = 768 (OpenAI small model)
- Average 10 relationships per element

---

## Implementation Plan

### Week 17: Core Indexing (Part of Phase 2E)

**Day 1-2: Lucene Integration**
```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-core</artifactId>
    <version>9.9.2</version>
</dependency>
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-queryparser</artifactId>
    <version>9.9.2</version>
</dependency>
```
- Implement ModelIndexer with Lucene
- Index symbol names, doc comments, types
- Test with WaterDistributionSystem model

**Day 3-4: Relationship Graph**
- Implement in-memory graph with adjacency lists
- Index all relationship types
- Implement BFS/DFS path finding
- Test traceability queries

**Day 5: Integration & Testing**
- Implement QueryRouter
- Test hybrid queries
- Performance benchmarking

### Week 18: Vector Search & LLM (Part of Phase 2E)

**Day 1-2: Vector Indexing**
```xml
<dependency>
    <groupId>io.milvus</groupId>
    <artifactId>milvus-sdk-java</artifactId>
    <version>2.3.4</version>
</dependency>
```
- Set up Milvus Lite (embedded)
- Implement embedding generation (OpenAI API)
- Index model elements as vectors
- Test semantic search

**Day 3-4: LLM Integration**
- Implement context builder using all indexes
- Create query intent analyzer
- Integrate with Claude API
- Build CLI chat interface

**Day 5: Documentation & Examples**
- Usage examples for each index type
- Performance tuning guide
- API documentation

---

## Open Source Options Summary

| Component | Recommended | Alternatives | License |
|-----------|-------------|--------------|---------|
| Full-Text Search | **Apache Lucene** | Elasticsearch (overkill) | Apache 2.0 |
| Vector DB | **Milvus Lite** | Chroma, FAISS, pgvector | Apache 2.0 |
| Graph DB | **In-Memory** | Neo4j Embedded, JanusGraph | N/A / Apache 2.0 |
| Embeddings | **OpenAI API** | sentence-transformers (local) | Commercial / Apache 2.0 |

**All dependencies are Apache 2.0 or similar permissive licenses** - suitable for commercial use.

---

## Estimated Costs

**For LLM Integration (Phase 2E)**:

### Embedding Costs (OpenAI):
- Model: `text-embedding-3-small` ($0.02 per 1M tokens)
- Average model element: ~200 tokens
- 10K elements = 2M tokens = **$0.04 to embed entire model**
- Cost: **Negligible** (<$1 for most models)

### LLM Query Costs (Claude):
- Model: Claude 3.5 Sonnet ($3 per 1M input tokens)
- Average query context: ~10K tokens
- Average response: ~500 tokens
- Cost per query: ~$0.03 input + ~$0.015 output = **$0.045 per query**
- 100 queries/day = **$4.50/day** or **$135/month**

### Local Alternative (Zero Cost):
- Use sentence-transformers for embeddings (free, runs locally)
- Use local LLM (LLaMA 3.1 8B via Ollama - free)
- Trade-off: Lower quality, but zero operating cost

---

## Summary

Phase 2E indexing strategy provides:
1. ✅ **Fast exact lookup** (Symbol Table - <1ms)
2. ✅ **Full-text search** (Lucene - <50ms)
3. ✅ **Semantic search** (Milvus + embeddings - <100ms)
4. ✅ **Relationship queries** (Graph index - <10ms)
5. ✅ **LLM-ready context** (Hybrid query routing)

**Total additional dependencies**: 3 (Lucene, Milvus, OpenAI client)
**Total implementation time**: 2 weeks (Weeks 17-18 of Phase 2)
**Operating cost**: ~$0.045 per LLM query (or $0 with local models)

This creates a production-ready, scalable foundation for intelligent model querying and report generation assistance.
