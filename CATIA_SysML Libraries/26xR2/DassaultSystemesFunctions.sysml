library package <DS_Functions> DassaultSystemesFunctions {
    private import KerML::*;
    private import ScalarValues::*;
    private import SequenceFunctions::*;
    private import ControlFunctions::*;
    calc def IsEssentialElement {
        doc /* This function checks whether the <input> element is treated as an essential element. Can be used only in a filter condition expression.*/
        in input : Element;
        return [1] : Boolean;
        rep script language "elementFilter" /* com.dassault_systemes.modeler.sysml.model.EssentialElements::isEssentialInOwnershipContext*/
    }
    calc def IsNonStandardLibraryElement {
        doc /* This function checks whether the <input> element comes from a non-standard library. Can be used only in a filter condition expression.*/
        in input : Element;
        return [1] : Boolean;
        rep script language "elementFilter" /* com.dassault_systemes.modeler.kerml.libraries.StandardLibraries::isNotFromLibrary*/
    }
    calc def IsSpecific {
        doc /* This function checks whether the <specific> feature is directly or indirectly specialized by the <general> feature.*/
        in specific : Element;
        in general : Element;
        return [1] : Boolean;
        rep script language "JavaMethod" /* com.dassault_systemes.modeler.sysml.ds_functions.DSFunctions.isSpecific */
    }
    calc def GetAppliedMetadata {
        doc /* This function returns all metadata usages that annotate the <input> element.*/
        in element : Element;
        return [0..*] : Metadata::MetadataItem;
        rep script language "JavaMethod" /* com.dassault_systemes.modeler.sysml.ds_functions.DSFunctions.getAppliedMetadata */
    }
    calc def GetSpecifiedBy {
        doc /* This function returns the <specific> feature if it is directly or indirectly specialized by the <general> feature.*/
        in specific [0..*] : Element;
        in general : Element;
        return [0..*] : Type = specific.? {
            in x;
            IsSpecific(x, general)
        };
    }
    calc def GetValue {
        doc /* This function returns the values of all features in the <contextType> that match or redefine the specified <feature>.*/
        in contextType : Element;
        in feature : Feature;
        return [0..*] : Base::Anything;
        language "JavaMethod" /* com.dassault_systemes.modeler.sysml.ds_functions.DSFunctions.getValue */
    }
    calc def GetSuppliersOfOwnedDependency {
        doc /* This function returns the list of suppliers from Dependency relations owned by the <context>, where <context> acts as the client of the dependency.*/
        in context : Element;
        rep script language "JavaMethod" /* com.dassault_systemes.modeler.sysml.ds_functions.DSFunctions.getSuppliersOfOwnedDependency */
        return [0..*] : Element;
    }
    calc def IsNotSupplierOfOwnedDependency {
        doc /* This function checks whether the <element> is not among the suppliers of Dependency relations owned by the <context>, where <context> acts as the client of the dependency.*/
        in context : Element;
        in element : Element;
        rep script language "JavaMethod" /* com.dassault_systemes.modeler.sysml.ds_functions.DSFunctions.isNotSupplierOfOwnedDependency */
        return [1] : Boolean;
    }
}
