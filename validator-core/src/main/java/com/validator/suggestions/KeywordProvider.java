package com.validator.suggestions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides SysML v2 language keywords for spelling suggestions.
 * Keywords are extracted from the SysMLv2Lexer.g4 grammar file.
 */
public final class KeywordProvider implements SuggestionProvider {

    private static final Set<String> KEYWORDS;

    static {
        Set<String> keywords = new HashSet<>();

        // Core Structure
        keywords.add("package");
        keywords.add("import");
        keywords.add("public");
        keywords.add("private");
        keywords.add("protected");

        // Definitions (compound keywords stored as single words for matching)
        keywords.add("part");
        keywords.add("def");
        keywords.add("action");
        keywords.add("state");
        keywords.add("requirement");
        keywords.add("use");
        keywords.add("case");
        keywords.add("view");
        keywords.add("viewpoint");
        keywords.add("constraint");
        keywords.add("attribute");
        keywords.add("enum");
        keywords.add("connection");
        keywords.add("interface");
        keywords.add("allocation");
        keywords.add("port");
        keywords.add("item");

        // Flow Control
        keywords.add("first");
        keywords.add("then");
        keywords.add("start");
        keywords.add("done");
        keywords.add("succession");
        keywords.add("transition");
        keywords.add("decide");
        keywords.add("merge");
        keywords.add("fork");
        keywords.add("join");
        keywords.add("if");
        keywords.add("else");

        // Usage
        keywords.add("perform");
        keywords.add("exhibit");
        keywords.add("satisfy");
        keywords.add("allocate");
        keywords.add("connect");
        keywords.add("bind");
        keywords.add("flow");
        keywords.add("message");

        // Relationships
        keywords.add("specializes");
        keywords.add("redefines");
        keywords.add("subsets");
        keywords.add("references");
        keywords.add("chains");
        keywords.add("inverses");
        keywords.add("conjugates");

        // Modifiers
        keywords.add("abstract");
        keywords.add("variation");
        keywords.add("readonly");
        keywords.add("derived");
        keywords.add("end");
        keywords.add("ordered");
        keywords.add("nonunique");
        keywords.add("parallel");

        // Parameters/Features
        keywords.add("in");
        keywords.add("out");
        keywords.add("inout");
        keywords.add("default");
        keywords.add("ref");
        keywords.add("value");

        // Documentation
        keywords.add("doc");
        keywords.add("comment");
        keywords.add("metadata");

        // Calculations
        keywords.add("calc");
        keywords.add("assert");
        keywords.add("assume");
        keywords.add("require");

        // Views
        keywords.add("expose");
        keywords.add("render");
        keywords.add("as");
        keywords.add("asDefault");

        // Others
        keywords.add("about");
        keywords.add("from");
        keywords.add("to");
        keywords.add("at");
        keywords.add("all");
        keywords.add("any");
        keywords.add("sequence");
        keywords.add("accept");
        keywords.add("verify");
        keywords.add("via");
        keywords.add("send");
        keywords.add("new");
        keywords.add("entry");
        keywords.add("exit");
        keywords.add("do");
        keywords.add("not");
        keywords.add("by");
        keywords.add("subject");

        // Literals
        keywords.add("true");
        keywords.add("false");
        keywords.add("null");

        KEYWORDS = Collections.unmodifiableSet(keywords);
    }

    @Override
    public Set<String> getCandidates() {
        return KEYWORDS;
    }

    @Override
    public String getSourceName() {
        return "keyword";
    }

    /**
     * Returns the total number of keywords available.
     *
     * @return keyword count
     */
    public static int getKeywordCount() {
        return KEYWORDS.size();
    }
}
