package ProjectStyleConfig {
    individual part def myProjectStyleConfig :> DS_Styles::CoreStylesComponents::StyleConfiguration {
        part : DIStyle :> activeStyleSheets;
        part : DS_Styles::StyleSheets::DefaultStyleSheet :> activeStyleSheets;
    }
    part def DIStyle :> DS_Styles::CoreStylesComponents::StyleSheet {
        part portInside :> rule {
            part :>> condition : DS_Styles::CoreStylesComponents::Predicates::ByElementMetaclassCondition {
                ref item :>> elementMetaclass = SysML::Systems::PortUsage.metadata;
            }
            part :>> style : DS_Styles::CoreStylesComponents::KerMLStyles::ShapeOnBorderStyle {
                attribute :>> labelPosition = labelPosition::Inside;
            }
        }
    }
}
