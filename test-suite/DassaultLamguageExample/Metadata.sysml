package Metadata {
    package ApprovalMetadata {
        public import ScalarValues::**;
        metadata def Approval {
            attribute approved : Real;
            attribute approver : String;
            attribute level : Positive;
        }
    }
    package DesignModel {
        public import ApprovalMetadata::*;
        part System {
            @Approval {
                approved = true;
                approver = "Paul";
                level = 1;
            }
        }
        view 'non semantic metadata applied' : DS_Views::SymbolicViews::gv;
    }
    package hardware {
        public import ScalarValues::**;
        public import SI::**;
        public import Metaobjects::*;
        part def Hardwares;
        part hardwares : Hardwares [0..*] nonunique;
        metadata def <hardware> HardwareMetadata :> SemanticMetadata {
            :> annotatedElement : SysML::Systems::PartDefinition, SysML::Systems::PartUsage;
            :>> baseType = hardwares meta SysML::Systems::PartUsage;
            attribute Manufacturer : String;
            attribute PowerConsumption : W;
            attribute Weight : kg;
        }
        part def Processor {
            @Metadata::hardware::hardware {
                Manufacturer = "Intel";
                PowerConsumption = 95.0;
                Weight = 0.5;
            }
        }
        view 'semantic metadata applied' : DS_Views::SymbolicViews::gv;
    }
    view 'metadata' : DS_Views::SymbolicViews::gv;
}
