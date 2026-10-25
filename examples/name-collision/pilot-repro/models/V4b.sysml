package RedefinitionDiamond {
    part A  { part p[*]; }
    part A1 :> A { p1 :>> p; }
    part A2 :> A { }
    part B :> A1, A2 {
        part p;
    }
}
