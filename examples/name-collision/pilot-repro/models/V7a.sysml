package V7a {
    item def Account;
    action def Transfer { in source : Account; in target : Account; }
    action def AuditedTransfer :> Transfer {
        in target : Account;
        in source : Account;
    }
}
