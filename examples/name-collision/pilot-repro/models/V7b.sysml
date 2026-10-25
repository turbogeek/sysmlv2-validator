package V7b {
    item def Account;
    action def Transfer { in source : Account; in target : Account; }
    action def Refund :> Transfer {
        in target :>> target;
    }
}
