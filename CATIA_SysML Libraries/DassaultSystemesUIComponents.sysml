library package <DS_UIComponents> DassaultSystemesUIComponents {
    package CoreUIComponents {
        private import ScalarValues::*;
        private import Metaobjects::*;
        package Operations {
            abstract action def Operation;
            abstract action def PaletteOperation :> Operation;
            abstract action def DialogOperation :> Operation;
            action def OperationFromCode :> PaletteOperation { in actionId [0..1] : String; }
            action def OperationFromTemplate :> PaletteOperation, DialogOperation { in element [0..1] : Metaobject; }
            action def OperationByMetaclass :> DialogOperation { in elementMetaclass [0..1] : Metaobject; }
        }
        package Components {
            abstract part def AbstractItem {
                attribute label : String;
                attribute description : String;
            }
            abstract part def Item :> AbstractItem {
                perform action operation [0..1] : Operations::Operation;
            }
            abstract part def ItemsCategory :> AbstractItem {
                attribute isCollapsed : Boolean;
                attribute hasVisibleLabels : Boolean;
                part abstractItems [*] : AbstractItem;
            }
            abstract part def Component {
                part itemCategories [*] : ItemsCategory;
            }
        }
        package Palette {
            abstract part def AbstractButton;
            enum def SymbolicRepresentationKind {
                naryShape;
                path;
                shapeOnBorder;
                shape;
            }
            part def Button :> Components::Item, AbstractButton {
                perform action : Operations::PaletteOperation :>> operation;
                attribute representation : SymbolicRepresentationKind [0..1];
            }
            part def ButtonsCategory :> Components::ItemsCategory, AbstractButton {
                part abstractButtons : AbstractButton :>> abstractItems;
                ref part focusedButton [0..1] : Button;
            }
            part def Palette :> Components::Component {
                part buttonCategories [*] : ButtonsCategory :>> itemCategories;
            }
        }
        package Dialogs {
            part def DialogItem :> Components::Item {
                perform action : Operations::DialogOperation :>> operation;
            }
            part def DialogItemsCategory :> Components::ItemsCategory {
                part : DialogItem :>> abstractItems;
            }
            abstract part def Dialog :> Components::Component {
                attribute actionName : String;
                part : DialogItemsCategory :>> itemCategories;
            }
            part def RelationshipCreationDialog :> Dialog {
                part :>> itemCategories {
                    attribute isIncoming : Boolean;
                }
            }
            part def ElementCreationDialog :> Dialog;
            part def ViewCreationDialog :> Dialog;
            part def ElementCreationDialogConfiguration {
                part activeElementCreationDialog [0..1] : ElementCreationDialog;
            }
            part def RelationshipCreationDialogConfiguration {
                part activeRelationshipCreationDialog [0..1] : RelationshipCreationDialog;
            }
            part def ViewCreationDialogConfiguration {
                part activeViewCreationDialog [0..1] : ViewCreationDialog;
            }
        }
    }
    package UIComponents {
        private import KerML::*;
        private import CoreUIComponents::*;
        private import ViewsTemplates::*;
        part def SysMLViewCreationDialog :> Dialogs::ViewCreationDialog {
            attribute :>> actionName default "Create View";
            part sysMLViewsMenu :> itemCategories {
                attribute :>> isCollapsed default false;
                attribute :>> label default "Symbolic Views";
                part gvItem : Dialogs::DialogItem :> abstractItems {
                    attribute :>> label default "General View";
                    perform action : Operations::OperationFromTemplate :>> operation { in ref = (generalViewTemplate meta Package).ownedElement; }
                }
                part ivItem : Dialogs::DialogItem :> abstractItems {
                    attribute :>> label default "Interconnection View";
                    perform action : Operations::OperationFromTemplate :>> operation { in ref = (interconnectionViewTemplate meta Package).ownedElement; }
                }
                part afvItem : Dialogs::DialogItem :> abstractItems {
                    attribute :>> label default "Action Flow View";
                    perform action : Operations::OperationFromTemplate :>> operation { in ref = (actionFlowViewTemplate meta Package).ownedElement; }
                }
                part stvItem : Dialogs::DialogItem :> abstractItems {
                    attribute :>> label default "State Transition View";
                    perform action : Operations::OperationFromTemplate :>> operation { in ref = (stateTransitionViewTemplate meta Package).ownedElement; }
                }
            }
            part sysMLTableMenu :> itemCategories {
                attribute :>> isCollapsed default false;
                attribute :>> label default "Tabular Views";
                part gtItem : Dialogs::DialogItem :> abstractItems {
                    attribute :>> label default "Generic Table";
                    perform action : Operations::OperationFromTemplate :>> operation { in ref = (genericTableTemplate meta Package).ownedElement; }
                }
                part rtItem : Dialogs::DialogItem :> abstractItems {
                    attribute :>> label default "Requirements Table";
                    perform action : Operations::OperationFromTemplate :>> operation { in ref = (requirementsTableTemplate meta Package).ownedElement; }
                }
            }
        }
        package ViewsTemplates {
            package generalViewTemplate {
                view : DS_Views::SymbolicViews::gv;
            }
            package interconnectionViewTemplate {
                view : DS_Views::SymbolicViews::iv;
            }
            package actionFlowViewTemplate {
                view : DS_Views::SymbolicViews::afv;
            }
            package stateTransitionViewTemplate {
                view : DS_Views::SymbolicViews::stv;
            }
            package genericTableTemplate {
                view : DS_Views::TabularViews::gt;
            }
            package requirementsTableTemplate {
                view : DS_Views::TabularViews::rt;
            }
        }
        part def SysMLElementCreationDialog :> Dialogs::ElementCreationDialog {
            attribute :>> actionName default "Create Element";
            part sysMLElementsMenu :> itemCategories {
                attribute :>> isCollapsed default false;
                attribute :>> hasVisibleLabels default true;
                attribute :>> label default "General";
                part actionUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action actionUsageItem : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ActionUsage.metadata; }
                }
                part actionDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ActionDefinition.metadata; }
                }
                part allocationDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::AllocationDefinition.metadata; }
                }
                part analysisCaseUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::AnalysisCaseUsage.metadata; }
                }
                part analysisCaseDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::AnalysisCaseDefinition.metadata; }
                }
                part assertConstraintUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::AssertConstraintUsage.metadata; }
                }
                part attributeUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::AttributeUsage.metadata; }
                }
                part attributeDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::AttributeDefinition.metadata; }
                }
                part calculationUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::CalculationUsage.metadata; }
                }
                part calculationDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::CalculationDefinition.metadata; }
                }
                part caseUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::CaseUsage.metadata; }
                }
                part caseDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::CaseDefinition.metadata; }
                }
                part commentItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = Comment.metadata; }
                }
                part concernUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ConcernUsage.metadata; }
                }
                part concernDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ConcernDefinition.metadata; }
                }
                part connectionDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ConnectionDefinition.metadata; }
                }
                part constraintUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ConstraintUsage.metadata; }
                }
                part constraintDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ConstraintDefinition.metadata; }
                }
                part documentationItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = Documentation.metadata; }
                }
                part enumerationUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::EnumerationUsage.metadata; }
                }
                part enumerationDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::EnumerationDefinition.metadata; }
                }
                part eventOccurrenceUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::EventOccurrenceUsage.metadata; }
                }
                part exhibitStateUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ExhibitStateUsage.metadata; }
                }
                part includeUseCaseUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::IncludeUseCaseUsage.metadata; }
                }
                part interfaceDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::InterfaceDefinition.metadata; }
                }
                part itemUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ItemUsage.metadata; }
                }
                part itemDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ItemDefinition.metadata; }
                }
                part libraryPackageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = LibraryPackage.metadata; }
                }
                part metadataDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::MetadataDefinition.metadata; }
                }
                part occurrenceUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::OccurrenceUsage.metadata; }
                }
                part occurrenceDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::OccurrenceDefinition.metadata; }
                }
                part packageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = Package.metadata; }
                }
                part partUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::PartUsage.metadata; }
                }
                part partDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::PartDefinition.metadata; }
                }
                part performActionUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::PerformActionUsage.metadata; }
                }
                part portUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::PortUsage.metadata; }
                }
                part portDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::PortDefinition.metadata; }
                }
                part referenceUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ReferenceUsage.metadata; }
                }
                part renderingUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::RenderingUsage.metadata; }
                }
                part renderingDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::RenderingDefinition.metadata; }
                }
                part requirementUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::RequirementUsage.metadata; }
                }
                part requirementDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::RequirementDefinition.metadata; }
                }
                part satisfyRequirementUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::SatisfyRequirementUsage.metadata; }
                }
                part stateUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::StateUsage.metadata; }
                }
                part stateDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::StateDefinition.metadata; }
                }
                part useCaseUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::UseCaseUsage.metadata; }
                }
                part useCaseDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::UseCaseDefinition.metadata; }
                }
                part verificationCaseUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::VerificationCaseUsage.metadata; }
                }
                part verificationCaseDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::VerificationCaseDefinition.metadata; }
                }
                part viewUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ViewUsage.metadata; }
                }
                part viewDefItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ViewDefinition.metadata; }
                }
            }
        }
        part def SysMLRelationshipCreationDialog :> Dialogs::RelationshipCreationDialog {
            attribute :>> actionName default "Create Relation";
            part sysMLSourceRelationshipsMenu :> itemCategories {
                attribute :>> label default "Outgoing";
                attribute :>> isCollapsed default false;
                attribute :>> hasVisibleLabels default true;
                part allocationUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::AllocationUsage.metadata; }
                }
                part annotationItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = Annotation.metadata; }
                }
                part assertConstraintUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::AssertConstraintUsage.metadata; }
                }
                part bindingConnectorItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::BindingConnectorAsUsage.metadata; }
                }
                part connectionUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ConnectionUsage.metadata; }
                }
                part crossSubsettingItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = CrossSubsetting.metadata; }
                }
                part dependencyItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = Dependency.metadata; }
                }
                part exhibitStateUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::ExhibitStateUsage.metadata; }
                }
                part featureTypingItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = FeatureTyping.metadata; }
                }
                part flowUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::FlowUsage.metadata; }
                }
                part includeUseCaseUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::IncludeUseCaseUsage.metadata; }
                }
                part interfaceUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::InterfaceUsage.metadata; }
                }
                part performActionUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::PerformActionUsage.metadata; }
                }
                part redefinitionItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = Redefinition.metadata; }
                }
                part referenceSubsettingItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = ReferenceSubsetting.metadata; }
                }
                part satisfyRequirementUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::SatisfyRequirementUsage.metadata; }
                }
                part subclassificationItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = Subclassification.metadata; }
                }
                part subsettingItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = Subsetting.metadata; }
                }
                part successionItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::SuccessionAsUsage.metadata; }
                }
                part successionFlowItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::SuccessionFlowUsage.metadata; }
                }
                part verificationCaseUsageItem : Dialogs::DialogItem :> abstractItems {
                    perform action : Operations::OperationByMetaclass :>> operation { in ref = SysML::Systems::VerificationCaseUsage.metadata; }
                }
            }
            part sysMLTargetRelationshipsMenu :> sysMLSourceRelationshipsMenu {
                attribute :>> isIncoming default true;
                attribute :>> label default "Incoming";
            }
        }
    }
}
