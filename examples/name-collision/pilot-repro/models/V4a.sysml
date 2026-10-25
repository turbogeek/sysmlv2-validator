package V4a {
    private import ScalarValues::*;
    part def Powered { attribute rating : Real; }
    part def Insured { attribute rating : Real; }
    part def Truck :> Powered, Insured {
        assert constraint { rating > 0.0 }
    }
    part def Truck2 :> Insured, Powered {
        assert constraint { rating > 0.0 }
    }
}
