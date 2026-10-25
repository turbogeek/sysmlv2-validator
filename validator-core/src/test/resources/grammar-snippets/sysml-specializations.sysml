// SysML v2 specializations spelled with keywords, cross subsetting, global qualified names, extents and features that
// have modifiers but no kind keyword (SysML v2 8.2.2.6.2-8.2.2.6.5; KerML 8.2.3.4.1, 8.2.5.8.1).
// Exercises: 'defined by' (DEFINED_BY), 'crosses' and '=>' on connection ends (crossesClause, CROSSES, CROSSES_OP),
// a qualified name from the global namespace ('$::', DOLLAR), the extent 'all T' as a value (valueInit#2) and in
// parentheses (baseExpression#13), unary '+' (unaryExpression#1), and reference usages declared with 'derived' or
// 'constant' only (DefaultReferenceUsage = RefPrefix Usage, prefixedFeatureDeclaration and featureModifier).
package SysmlSpecializations {
    private import ScalarValues::*;

    item def ShoppingCart {
        item selectedProducts : Product[0..*];
    }
    item def Product {
        item inCart : ShoppingCart[0..1];
    }
    connection def ProductSelection {
        end item cart : ShoppingCart[1] crosses selectedProduct.inCart;
        end item selectedProduct : Product[1] => cart.selectedProducts;
    }

    part def Engine;
    part engine defined by Engine;
    part spare : $::SysmlSpecializations::Engine;
    part engineChoice : Engine[1..*];
    part chosen : Engine[1..*] = all engineChoice;
    attribute engineCount : Integer = (all Engine)->size();
    attribute offset : Integer = +1;
    derived totalMass : Real;
    constant wheelCount : Integer = 4;
}
