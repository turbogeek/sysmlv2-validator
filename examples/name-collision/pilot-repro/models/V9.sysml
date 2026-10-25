package V9 {
    private import ScalarValues::*;
    private import NumericalFunctions::*;
    private import SequenceFunctions::*;
    item def TestRecord {
        attribute scores[1..*] : Integer;
        derived attribute averageScore[1] : Rational = sum(scores)/size(scores);
        attribute cutoff : Integer default = 0.75 * averageScore;
    }
    item def TestWithCutoff :> TestRecord {
        attribute cutoff : Rational default = 0.75 * averageScore;
    }
}
