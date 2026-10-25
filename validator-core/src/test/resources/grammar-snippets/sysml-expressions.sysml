// SysML v2 / KerML expressions (KerML 8.2.5.8) that the other corpus files do not use.
// Exercises: '===', '!==', '??', '%', 'implies', unary '~', 'as' with and without an argument, '->' with an argument
// list, a body and a function reference, '.?' select, and 'this' as a name (SysML v2 does not reserve it).
package SysmlExpressions {
    private import ScalarValues::*;
    attribute a : Integer = 7;
    attribute b : Integer = 2;
    attribute flag : Boolean = true;
    attribute identical = a === b;
    attribute different = a !== b;
    attribute fallback = a ?? b;
    attribute remainder = a % b;
    attribute rule = flag implies a > b;
    attribute complement = ~a;
    attribute widened = a as Real;
    attribute selfCast = as Real;
    attribute total = (1, 2, 3)->sum();
    attribute evens = (1, 2, 3)->select { in x; x % 2 == 0 };
    attribute folded = (1, 2, 3)->reduce RealFunctions::'+';
    attribute large = (1, 2, 3).? { in x; x > 1 };
    attribute this : Integer = 1;
}
