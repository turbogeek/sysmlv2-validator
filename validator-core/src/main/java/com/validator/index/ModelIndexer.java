package com.validator.index;

import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lucene-based full-text index for SysML v2 models.
 * Enables fast keyword search across element names, types, and documentation.
 */
public class ModelIndexer {
    private final Directory index;
    private final StandardAnalyzer analyzer;
    private IndexWriter writer;
    private DirectoryReader reader;
    private IndexSearcher searcher;

    public ModelIndexer() throws IOException {
        // Use in-memory index for prototype (can switch to FSDirectory for persistence)
        this.index = new ByteBuffersDirectory();
        this.analyzer = new StandardAnalyzer();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        this.writer = new IndexWriter(index, config);
    }

    /**
     * Index a complete symbol table.
     */
    public void indexModel(SymbolTable symbolTable) throws IOException {
        for (Symbol symbol : symbolTable.getAllSymbols()) {
            indexSymbol(symbol);
        }
        writer.commit();
    }

    /**
     * Index a single symbol.
     */
    public void indexSymbol(Symbol symbol) throws IOException {
        Document doc = new Document();

        // Index name (searchable + stored)
        doc.add(new TextField("name", symbol.getName(), Field.Store.YES));

        // Index qualified name (searchable + stored)
        doc.add(new TextField("qualifiedName", symbol.getQualifiedName(), Field.Store.YES));

        // Index element type (exact match + stored)
        doc.add(new StringField("type", symbol.getType().toString(), Field.Store.YES));

        // Index visibility
        doc.add(new StringField("visibility", symbol.getVisibility().toString(), Field.Store.YES));

        // Store location (not searchable, just stored for retrieval)
        if (symbol.getLocation() != null) {
            doc.add(new StoredField("location", symbol.getLocation().toString()));
        }

        // TODO: Index doc comments when available
        // doc.add(new TextField("docComment", getDocComment(symbol), Field.Store.YES));

        // TODO: Index attribute values when available
        // doc.add(new TextField("attributes", getAttributeValues(symbol), Field.Store.NO));

        writer.addDocument(doc);
    }

    /**
     * Search the index with a query string.
     *
     * @param queryStr Lucene query string (e.g., "name:Engine", "type:PART_DEFINITION")
     * @param maxResults Maximum number of results to return
     * @return List of search results
     */
    public List<SearchResult> search(String queryStr, int maxResults) throws IOException, ParseException {
        // Ensure index is readable
        if (reader == null) {
            reader = DirectoryReader.open(index);
            searcher = new IndexSearcher(reader);
        } else {
            // Reopen to get latest changes
            DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
            if (newReader != null) {
                reader.close();
                reader = newReader;
                searcher = new IndexSearcher(reader);
            }
        }

        // Parse query (default field is "name")
        QueryParser parser = new QueryParser("name", analyzer);
        Query query = parser.parse(queryStr);

        // Execute search
        TopDocs topDocs = searcher.search(query, maxResults);

        // Convert results
        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            SearchResult result = new SearchResult(
                doc.get("name"),
                doc.get("qualifiedName"),
                doc.get("type"),
                doc.get("visibility"),
                doc.get("location"),
                scoreDoc.score
            );
            results.add(result);
        }

        return results;
    }

    /**
     * Search by element name (simple keyword search).
     */
    public List<SearchResult> searchByName(String name, int maxResults) throws IOException, ParseException {
        return search("name:" + escapeQuery(name), maxResults);
    }

    /**
     * Search by element type.
     */
    public List<SearchResult> searchByType(String type, int maxResults) throws IOException, ParseException {
        return search("type:" + type, maxResults);
    }

    /**
     * Search by qualified name pattern.
     */
    public List<SearchResult> searchByQualifiedName(String pattern, int maxResults) throws IOException, ParseException {
        return search("qualifiedName:" + escapeQuery(pattern), maxResults);
    }

    /**
     * Wildcard search across all fields.
     */
    public List<SearchResult> searchAll(String keyword, int maxResults) throws IOException, ParseException {
        String queryStr = String.format("name:%s OR qualifiedName:%s OR type:%s",
            escapeQuery(keyword), escapeQuery(keyword), escapeQuery(keyword));
        return search(queryStr, maxResults);
    }

    /**
     * Get index statistics.
     */
    public IndexStats getStats() throws IOException {
        if (reader == null) {
            reader = DirectoryReader.open(index);
        }
        return new IndexStats(reader.numDocs(), reader.maxDoc());
    }

    /**
     * Close the indexer and release resources.
     */
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
        if (reader != null) {
            reader.close();
        }
        index.close();
    }

    /**
     * Escape special characters in Lucene queries.
     */
    private String escapeQuery(String query) {
        // Escape Lucene special characters: + - && || ! ( ) { } [ ] ^ " ~ * ? : \
        return query.replaceAll("([+\\-&|!(){}\\[\\]^\"~*?:\\\\])", "\\\\$1");
    }

    /**
     * Search result container.
     */
    public static class SearchResult {
        private final String name;
        private final String qualifiedName;
        private final String type;
        private final String visibility;
        private final String location;
        private final float score;

        public SearchResult(String name, String qualifiedName, String type,
                          String visibility, String location, float score) {
            this.name = name;
            this.qualifiedName = qualifiedName;
            this.type = type;
            this.visibility = visibility;
            this.location = location;
            this.score = score;
        }

        public String getName() { return name; }
        public String getQualifiedName() { return qualifiedName; }
        public String getType() { return type; }
        public String getVisibility() { return visibility; }
        public String getLocation() { return location; }
        public float getScore() { return score; }

        @Override
        public String toString() {
            return String.format("%s [%s] (type=%s, score=%.2f) at %s",
                name, qualifiedName, type, score, location);
        }
    }

    /**
     * Index statistics.
     */
    public static class IndexStats {
        private final int numDocs;
        private final int maxDoc;

        public IndexStats(int numDocs, int maxDoc) {
            this.numDocs = numDocs;
            this.maxDoc = maxDoc;
        }

        public int getNumDocs() { return numDocs; }
        public int getMaxDoc() { return maxDoc; }

        @Override
        public String toString() {
            return String.format("Index Stats: %d documents (max=%d)", numDocs, maxDoc);
        }
    }
}
