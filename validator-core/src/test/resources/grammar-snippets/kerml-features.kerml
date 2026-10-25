// KerML features, connectors and relationship members (KerML 8.2.3.3, 8.2.4.2-8.2.4.3, 8.2.5.4-8.2.5.9).
// Exercises: 'var' and 'composite' prefixes, 'portion' and 'step' features, 'expr' and 'bool' features, 'inverse of',
// multiplicity members, invariants, binary and n-ary connectors, binding connectors (with and without 'of' and ends),
// and the subtype, subclassifier, subset, redefinition and disjoining relationship members.
package KermlFeatures {
    classifier Thing;
    classifier Machine specializes Thing;
    feature whole : Thing;
    feature piece : Thing;
    feature holder : Thing inverse of whole;
    var feature counter : Thing;
    composite feature body : Thing;
    portion slice : Thing;
    step move;
    expr total;
    bool test;
    multiplicity optional [0..1];
    inv positive { true }
    connector whole to piece;
    connector link from whole to piece;
    connector triple (whole, piece, body);
    binding whole = piece;
    binding same of whole = piece;
    binding unbound;
    specialization MachineIsThing subclassifier Machine specializes Thing;
    subtype Machine :> Thing;
    subset piece subsets whole;
    redefinition body redefines whole;
    disjoint Machine from Thing;
}
