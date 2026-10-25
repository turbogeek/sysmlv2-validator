// SysML v2 connections, allocations, successions, ends and multiplicities (SysML v2 8.2.2.6.6, 8.2.2.13, 8.2.2.15).
// Exercises: end features with a multiplicity and a kind ('end [0..1] item') or 'ref' ('end [*] ref'), an n-ary
// 'allocate' with named ends, a multiplicity bound that is a name, and 'succession ... first a then b'.
package SysmlConnections {
    private import ScalarValues::*;

    part def Product;
    part def ShoppingCart;
    part def Situation;
    attribute max : Integer = 3;

    connection def Selection {
        end [0..1] item cart : ShoppingCart[1];
        end [*] ref sit : Situation;
    }

    part logical { part l1; part l2; }
    part physical { part p1; part p2; }
    allocation map allocate (logic ::> logical.l1, phys ::> physical.p1);

    part system {
        part a [1..max];
        part b;
        succession s first a then b;
    }
}
