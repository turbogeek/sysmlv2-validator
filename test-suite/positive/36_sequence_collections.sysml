package CollectionTests;
private import ScalarValues::*;

part def DataSet {
    attribute values : Real[*] ordered;
    
    // All elements
    attribute allValues = values;
    
    // Any element
    attribute anyValue = values->any;
    
    // Sequence operations  
    attribute firstValue = values#(1);
    attribute lastValue = values->last;
    attribute size = values->size;
}

part def FilteredData :> DataSet {
    // Collection filtering
    attribute positiveValues = values->select(v | v > 0.0);
    
    // Collection mapping
    attribute squaredValues = values->collect(v | v * v);
}
