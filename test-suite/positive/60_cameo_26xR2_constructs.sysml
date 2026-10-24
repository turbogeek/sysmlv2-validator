/**
 * POSITIVE TEST: constructs used by the CATIA Magic 26xR2 Dassault libraries, models exported by
 * Cameo 26xR2 (Refresh2 examples) and the OMG standard library. Each package is one construct.
 */
package Positive60_Cameo26xR2Constructs {
    private import ScalarValues::*;
    package T01_entry_send {
        item def Go; part def P { exhibit state s { state a { entry send new Go() to self; exit send new Go() to self; } transition first start then a; } }
    }
    package T02_enum_literal_body {
        attribute def D { attribute priority : Integer; }
        enum def E :> D { fatal { attribute :>> priority default 1; } error { attribute :>> priority default 2; } }
    }
    package T03_perform_in_ref {
        action def Op { in element : Integer; } part def P { perform action op : Op; } part def Q :> P { perform action : Op :>> op { in ref = 3; } }
    }
    package T04_part_redef_typed_body {
        part def C { attribute x : Integer; } part def P { part condition : C; } part def Q :> P { part :>> condition : C { attribute :>> x = 1; } }
    }
    package T05_param_named_feature {
        calc def F { in feature : Integer; return : Integer = feature; }
    }
    package T06_filter_classification {
        package Pk { part def A; filter not (istype A); filter hastype A | hastype A; filter @A; }
    }
    package T07_metadata_shortname_annot {
        metadata def <'my md'> M :> Metaobjects::SemanticMetadata { :> annotatedElement : SysML::Systems::ItemDefinition; :> annotatedElement : SysML::Systems::ItemUsage; }
    }
    package T08_if_expression {
        calc def F { in x : Real; return : Real = if x > 0.0 ? x else if x < -1.0 ? -1.0 else 0.0; }
    }
    package T09_empty_sequence {
        part def P { attribute s : Integer [*]; } part q : P { attribute :>> s = (); }
    }
    package T10_then_assign {
        part def P { attribute a : Boolean; action run { first start; then assign a := true; then done; } }
    }
    package T11_le_and_ge {
        part def P { attribute x : Real; attribute t : Real; constraint c { x <= t + 1.0 and x >= t - 1.0 } }
    }
    package T12_le_amp_ge {
        part def P { attribute x : Real; constraint c { x <= 3 & x >= 1 } }
    }
    package T13_ge_only {
        part def P { attribute x : Real; constraint c { x >= 1 } }
    }
    package T14_le_only {
        part def P { attribute x : Real; constraint c { x <= 1 } }
    }
    package T15_lt_and_gt {
        part def P { attribute x : Real; constraint c { x < 3 and x > 1 } }
    }
    package T16_calc_call_return {
        calc def PassIf { in c : Boolean; return : Boolean = c; } calc def V { in x : Real; return verdict : Boolean = PassIf(x > 1.0 and x < 2.0); }
    }
    package T17_le_paren {
        part def P { attribute x : Real; constraint c { (x <= 3) } }
    }
    package T18_ge_neg {
        part def P { attribute x : Real; constraint c { x >= -60 } }
    }
    package T21_metadata_access {
        part def A; attribute x = A.metadata;
    }
    package T22_ref_item_redef {
        part def C { ref item m [*]; } part def D :> C { ref item :>> m = 1; }
    }
    package T23_ref_item_redef_seq {
        part def C { ref item m [*]; } part def D :> C { ref item :>> m = (1, 2); }
    }
    package T24_default_no_eq {
        part def C { ref item m [*]; } part def D :> C { ref item :>> m default (1, 2); }
    }
    package T25_in_ref_value {
        action def Op { in ref r; } action o : Op { in ref = 1; }
    }
    package T26_call_with_metadata {
        part def A; calc def F { in x; in y; return : Boolean; } attribute b = F(A, A.metadata);
    }
    package T31_accept_qualified {
        package Payloads { item def On; } part def P { exhibit state s { state Off; state On; transition t1 first start then Off; transition t2 first Off accept Payloads::On then On; } }
    }
    package T32_do_send_via {
        item def X; part def P { port p; exhibit state s { state On { do send new X() via p; } } }
    }
    package T33_then_if_action {
        action def A; action def B { first start; then if true { action a : A; } then if 1 > 0 { action b : A; } else { action c : A; } then done; }
    }
}
