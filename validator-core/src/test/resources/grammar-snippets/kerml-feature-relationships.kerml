// KerML feature relationships, feature prefixes, multiline notes and collect expressions (KerML 8.2.2.2, 8.2.4.3.1,
// 8.2.5.8.2).
// Exercises: a multiline note ('//*' ... '*/', MULTILINE_NOTE), 'typed by' (TYPED_BY), 'featured by'
// (featureRelationship#3, FEATURED_BY), 'crosses' on an end feature (CROSSES), features declared with the
// 'composite' and 'var' prefixes but no 'feature' keyword (BasicFeaturePrefix FeatureDeclaration,
// prefixedFeatureDeclaration), and a collect expression '.{ ... }' (sequenceSuffix#6).
package KermlFeatureRelationships {
    //* A multiline note is not part of the model,
        unlike a comment. */
    class ShoppingCart;
    class Product;
    class Account;

    assoc Selection {
        end cart : ShoppingCart[1] crosses cart::account.inCart {
            feature inCart : ShoppingCart[0..1] featured by Account;
            feature account : Account featured by Product;
        }
        end product typed by Product;
    }

    class Store {
        composite carts : ShoppingCart[0..*];
        var count : ScalarValues::Integer;
        feature doubled = (1, 2).{ in x; x * 2 };
    }
}
