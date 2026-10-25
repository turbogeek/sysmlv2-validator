// KerML namespaces, types and classifiers (KerML 8.2.3.2, 8.2.4.1, 8.2.5).
// Exercises: namespaceDeclaration and both namespaceBody alternatives; the type, classifier, datatype, class, struct,
// assoc ('assoc' and 'assoc struct'), behavior, function, predicate, interaction and metaclass declarations; the
// 'specializes', 'conjugates', 'unions', 'intersects', 'differences' and 'disjoint from' type relationships.
package KermlTypes {
    namespace Outer {
        namespace Inner;
    }
    classifier Base;
    classifier Other;
    type all Root [1] specializes Base;
    type Mirror conjugates Root;
    datatype Measure;
    class Vehicle specializes Base;
    struct Frame;
    assoc Link;
    assoc struct Bond;
    behavior Drive;
    function Speed;
    predicate IsFast;
    interaction Handshake;
    metaclass Marker;
    classifier Either unions Base, Other;
    classifier Both intersects Base, Other;
    classifier OnlyBase differences Base, Other;
    classifier Apart disjoint from Other;
}
