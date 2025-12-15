package com.validator.constraint;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the variable scope for constraint expression analysis.
 *
 * <p>This scope tracks variables that are available within a constraint,
 * including:
 * <ul>
 *   <li>Attributes from the containing definition</li>
 *   <li>Parameters from the containing action or calc</li>
 *   <li>Built-in variables like 'self'</li>
 * </ul>
 */
public class ConstraintScope {

    private final Map<String, ConstraintType> variables = new HashMap<>();
    private ConstraintScope parent;

    /**
     * Creates a new empty constraint scope.
     */
    public ConstraintScope() {
        // Add built-in variables
        variables.put("self", ConstraintType.ANY);
        variables.put("true", ConstraintType.BOOLEAN);
        variables.put("false", ConstraintType.BOOLEAN);
        variables.put("null", ConstraintType.ANY);
    }

    /**
     * Creates a child scope with this scope as parent.
     *
     * @return a new child scope
     */
    public ConstraintScope createChild() {
        ConstraintScope child = new ConstraintScope();
        child.parent = this;
        return child;
    }

    /**
     * Adds a variable to this scope.
     *
     * @param name the variable name
     * @param type the variable type
     */
    public void addVariable(String name, ConstraintType type) {
        if (name != null && !name.isEmpty()) {
            variables.put(name, type != null ? type : ConstraintType.ANY);
        }
    }

    /**
     * Checks if a variable is defined in this scope or parent scopes.
     *
     * @param name the variable name
     * @return true if defined
     */
    public boolean isDefined(String name) {
        if (name == null) {
            return false;
        }
        if (variables.containsKey(name)) {
            return true;
        }
        return parent != null && parent.isDefined(name);
    }

    /**
     * Gets the type of a variable.
     *
     * @param name the variable name
     * @return the type, or ANY if not found
     */
    public ConstraintType getType(String name) {
        if (name == null) {
            return ConstraintType.ANY;
        }
        ConstraintType type = variables.get(name);
        if (type != null) {
            return type;
        }
        if (parent != null) {
            return parent.getType(name);
        }
        return ConstraintType.ANY;
    }

    /**
     * Gets all variables in this scope (not including parent).
     *
     * @return map of variable names to types
     */
    public Map<String, ConstraintType> getLocalVariables() {
        return new HashMap<>(variables);
    }

    /**
     * Gets the parent scope.
     *
     * @return the parent scope, or null if none
     */
    public ConstraintScope getParent() {
        return parent;
    }

    /**
     * Gets the number of variables in this scope (including parent).
     *
     * @return total variable count
     */
    public int size() {
        int count = variables.size();
        if (parent != null) {
            count += parent.size();
        }
        return count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ConstraintScope{");
        sb.append("variables=").append(variables.keySet());
        if (parent != null) {
            sb.append(", parent=").append(parent.variables.keySet());
        }
        sb.append("}");
        return sb.toString();
    }
}
