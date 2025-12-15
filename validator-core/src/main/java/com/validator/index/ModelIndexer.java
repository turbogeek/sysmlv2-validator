package com.validator.index;

import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Lucene-based indexer for SysML model symbols.
 * Provides fast search capabilities for symbol lookup.
 */
public final class ModelIndexer implements Closeable {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_QUALIFIED_NAME = "qualifiedName";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_LOCATION = "location";

    private final Directory directory;
    private IndexWriter writer;
    private IndexReader reader;
    private IndexSearcher searcher;

    /**
     * Create a new model indexer with in-memory storage.
     */
    public ModelIndexer() throws IOException {
        this.directory = new ByteBuffersDirectory();
    }

    /**
     * Index all symbols from a symbol table.
     *
     * @param symbolTable the symbol table to index
     */
    public void indexModel(SymbolTable symbolTable) throws IOException {
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        indexSymbols(symbols);
    }

    /**
     * Index a collection of symbols.
     *
     * @param symbols the symbols to index
     */
    public void indexSymbols(Collection<Symbol> symbols) throws IOException {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        writer = new IndexWriter(directory, config);

        for (Symbol symbol : symbols) {
            Document doc = new Document();
            doc.add(new TextField(FIELD_NAME, symbol.getName(), Field.Store.YES));
            doc.add(new StringField(FIELD_QUALIFIED_NAME, symbol.getQualifiedName(),
                Field.Store.YES));
            doc.add(new StringField(FIELD_TYPE, symbol.getType().name(), Field.Store.YES));

            if (symbol.getLocation() != null) {
                doc.add(new StringField(FIELD_LOCATION,
                    symbol.getLocation().toString(), Field.Store.YES));
            }

            writer.addDocument(doc);
        }

        writer.commit();
        initializeSearcher();
    }

    private void initializeSearcher() throws IOException {
        if (reader != null) {
            reader.close();
        }
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
    }

    /**
     * Search for symbols by name.
     *
     * @param name the name to search for
     * @param maxResults maximum number of results
     * @return list of search results
     */
    public List<SearchResult> searchByName(String name, int maxResults) throws IOException {
        if (searcher == null) {
            return Collections.emptyList();
        }
        Query query = new TermQuery(new Term(FIELD_NAME, name.toLowerCase()));
        return search(query, maxResults);
    }

    /**
     * Search for symbols by type.
     *
     * @param type the element type name
     * @param maxResults maximum number of results
     * @return list of search results
     */
    public List<SearchResult> searchByType(String type, int maxResults) throws IOException {
        if (searcher == null) {
            return Collections.emptyList();
        }
        Query query = new TermQuery(new Term(FIELD_TYPE, type));
        return search(query, maxResults);
    }

    /**
     * Search for symbols by qualified name pattern.
     *
     * @param pattern the qualified name pattern (supports wildcards)
     * @param maxResults maximum number of results
     * @return list of search results
     */
    public List<SearchResult> searchByQualifiedName(String pattern, int maxResults)
            throws IOException {
        if (searcher == null) {
            return Collections.emptyList();
        }
        Query query = new WildcardQuery(new Term(FIELD_QUALIFIED_NAME, pattern));
        return search(query, maxResults);
    }

    /**
     * Search across all fields.
     *
     * @param text the text to search for
     * @param maxResults maximum number of results
     * @return list of search results
     */
    public List<SearchResult> searchAll(String text, int maxResults) throws IOException {
        if (searcher == null) {
            return Collections.emptyList();
        }
        // Search by name (case insensitive via TextField analyzer)
        Query query = new TermQuery(new Term(FIELD_NAME, text.toLowerCase()));
        return search(query, maxResults);
    }

    private List<SearchResult> search(Query query, int maxResults) throws IOException {
        TopDocs topDocs = searcher.search(query, maxResults);
        List<SearchResult> results = new ArrayList<>();

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.storedFields().document(scoreDoc.doc);
            results.add(new SearchResult(
                doc.get(FIELD_NAME),
                doc.get(FIELD_QUALIFIED_NAME),
                doc.get(FIELD_TYPE),
                doc.get(FIELD_LOCATION),
                scoreDoc.score
            ));
        }

        return results;
    }

    /**
     * Get index statistics.
     *
     * @return the statistics
     */
    public IndexStats getStats() throws IOException {
        int numDocs = 0;
        if (reader != null) {
            numDocs = reader.numDocs();
        }
        return new IndexStats(numDocs);
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
        if (reader != null) {
            reader.close();
        }
        directory.close();
    }

    /**
     * Represents a search result.
     */
    public static class SearchResult {
        private final String name;
        private final String qualifiedName;
        private final String type;
        private final String location;
        private final float score;

        SearchResult(String name, String qualifiedName, String type,
                     String location, float score) {
            this.name = name;
            this.qualifiedName = qualifiedName;
            this.type = type;
            this.location = location;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public String getQualifiedName() {
            return qualifiedName;
        }

        public String getType() {
            return type;
        }

        public String getLocation() {
            return location;
        }

        public float getScore() {
            return score;
        }

        @Override
        public String toString() {
            return String.format("SearchResult{name='%s', qualifiedName='%s', type=%s, score=%.2f}",
                name, qualifiedName, type, score);
        }
    }

    /**
     * Index statistics.
     */
    public static class IndexStats {
        private final int numDocs;

        IndexStats(int numDocs) {
            this.numDocs = numDocs;
        }

        public int getNumDocs() {
            return numDocs;
        }

        @Override
        public String toString() {
            return String.format("IndexStats{numDocs=%d}", numDocs);
        }
    }
}
