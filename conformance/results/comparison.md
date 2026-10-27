# Conformance: Validator, Pilot

| Tool | Version | Machine | Run | Directory |
|---|---|---|---|---|
| Validator | 0.2.0-SNAPSHOT | MacBook-Pro-2.local | 20261027-151718 | `validator_MacBook-Pro-2.local_20261027-101544` |
| Pilot | 0.63.0 | MacBook-Pro-2.local | 20261027-151723 | `pilot_MacBook-Pro-2.local_20261027-101719` |

- Validator: library `[/private/tmp/claude-502/-Users-turbogeek-Google-Drive-git/a83dabe3-77d1-4c0b-abcd-d157856e492d/scratchpad/pilot/kernel63/sysml/sysml.library]`; library.dassault `[/Users/turbogeek/Desktop/git/sysmlv2-validator/CATIA_SysML Libraries/26xR2]`
- Pilot: library `/private/tmp/claude-502/-Users-turbogeek-Google-Drive-git/a83dabe3-77d1-4c0b-abcd-d157856e492d/scratchpad/pilot/kernel63/sysml/sysml.library`; library.extra `CATIA_SysML Libraries/26xR2`

## Summary

277 models.

- Validator and Pilot: the same verdict on 198; Validator accepts and Pilot rejects 77; Validator rejects and Pilot accepts 0; not compared 2.
- Pilot has no verdict on skipped 2.

## Validator and Pilot disagree

21 of these 77 are models in which the pilot cannot resolve the names of Dassault's libraries (see the notes).

- positive/07_requirement_with_constraint: Validator accepted, Pilot rejected
- positive/09_ports_and_connections: Validator accepted, Pilot rejected
- positive/11_breakfast_example_corrected: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- positive/12_simple_vehicle_model: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- positive/13_actions_drone_delivery: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- positive/14_flows: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- positive/15_states: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- positive/16_interfaces: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- positive/17_use_case: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- positive/18_allocation_definitions: Validator accepted, Pilot rejected
- positive/19_analysis_calculations: Validator accepted, Pilot rejected
- positive/21_metadata_annotations: Validator accepted, Pilot rejected
- positive/23_occurrence_temporal: Validator accepted, Pilot rejected
- positive/25_trade_studies: Validator accepted, Pilot rejected
- positive/26_message_accept: Validator accepted, Pilot rejected
- positive/27_complex_multiplicities: Validator accepted, Pilot rejected
- positive/28_feature_redefinition: Validator accepted, Pilot rejected
- positive/29_abstract_variation: Validator accepted, Pilot rejected
- positive/30_exhibit_perform: Validator accepted, Pilot rejected
- positive/32_reference_subsetting: Validator accepted, Pilot rejected
- positive/33_conjugate_interface: Validator accepted, Pilot rejected
- positive/36_sequence_collections: Validator accepted, Pilot rejected
- positive/38_readonly_derived: Validator accepted, Pilot rejected
- positive/39_succession_flow: Validator accepted, Pilot rejected
- positive/40_objective_use_case: Validator accepted, Pilot rejected
- positive/44_variant_configuration: Validator accepted, Pilot rejected
- positive/45_import_membership: Validator accepted, Pilot rejected
- positive/47_expression_operators: Validator accepted, Pilot rejected
- positive/48_library_package: Validator accepted, Pilot rejected
- positive/49_actor_stakeholder: Validator accepted, Pilot rejected
- positive/60_cameo_26xR2_constructs: Validator accepted, Pilot rejected
- negative/02_action_then_invalid: Validator accepted, Pilot rejected
- negative/11_action_then_succession: Validator accepted, Pilot rejected
- negative/32_undefined_action_output: Validator accepted, Pilot rejected
- negative/33_undefined_state_reference: Validator accepted, Pilot rejected
- negative/34_undefined_port_reference: Validator accepted, Pilot rejected
- negative/42_wrong_succession_target: Validator accepted, Pilot rejected
- negative/52_invalid_enum_value: Validator accepted, Pilot rejected
- lint/unit_mismatches: Validator accepted, Pilot rejected
- lint/wildcard_imports: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/ActionPatterns_4AcceptVariations: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/ActionPatterns_5SendVariations: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/ActionPatterns_7AdvancedSend: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Actions_5SendAcceptSignal: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Actions_Tutorial: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Behavior_2Transitions: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Cases_1UseCaseDefinition: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Cases_4CaseUsage: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Connections_3InterfaceConnection: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Connections_4SuccessionFlow: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Connections_Tutorial: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Filters_Tutorial: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- cheatsheet/pastableExamples/Flows_Tutorial: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Metadata_2ElementAnnotation: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Patterns_2Views: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Patterns_4AbstractvsIndividual: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/PortsInterfaces_Tutorial: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Shorthand_5FeatureValues: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/StateMachine_Tutorial: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- cheatsheet/pastableExamples/Variants_Tutorial: Validator accepted, Pilot rejected
- cheatsheet/pastableExamples/Views_Tutorial: Validator accepted, Pilot rejected
- dassault/Actions: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- dassault/Flows: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- dassault/Import: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- dassault/Index: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- dassault/Individual: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- dassault/Interfaces: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- dassault/N-ary Interfaces: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- dassault/Requirements Derivation: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- dassault/States: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- dassault/Structural and Behavior Modeling: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- dassault/Use Case: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- dassault/ViewsRenderingAndExposedElements: Validator accepted, Pilot rejected (the pilot: unresolved Dassault library names)
- pilot-repro/V10d: Validator accepted, Pilot rejected
- pilot-repro/V10e: Validator accepted, Pilot rejected
- misc/test_extend: Validator accepted, Pilot rejected
- examples/16_medical_device_stability_v2: Validator accepted, Pilot rejected

## Rejected by all, with errors in different places

### No error line in common

- **negative/20_missing_brace**: none in common; only Validator 11; only Pilot 10, 8
- **lint/good_practices**: none in common; only Validator 25, 26, 27, 31, 32, 36, 37, 41 … (9); only Pilot 17, 18
- **lint/refactoring_example**: none in common; only Validator 37, 38, 39, 40, 41, 46, 47, 51 … (11); only Pilot 20, 23
- **lint/unused_elements**: none in common; only Validator 32, 37, 42, 47, 52, 53, 58, 76 … (9); only Pilot 14, 17

### Some error lines in common

- **negative/01_action_parallel_invalid**: in common 8; only Validator none; only Pilot 12
- **negative/03_action_decide_invalid**: in common 8; only Validator none; only Pilot 10, 11, 14
- **negative/04_action_if_invalid**: in common 10; only Validator none; only Pilot 14, 8
- **negative/05_multiple_action_parallel**: in common 11, 14, 8; only Validator none; only Pilot 18
- **negative/06_action_parallel_then_combined**: in common 8; only Validator none; only Pilot 11, 15
- **negative/07_nested_action_if**: in common 10, 11; only Validator none; only Pilot 16, 8
- **negative/08_action_decide_with_if**: in common 11, 14, 19; only Validator none; only Pilot 10, 8
- **negative/09_all_invalid_modifiers**: in common 17, 8; only Validator none; only Pilot 11, 14, 21
- **negative/10_action_parallel_with_params**: in common 10; only Validator none; only Pilot 11, 12, 15, 8
- **negative/12_action_decide_boolean**: in common 8; only Validator none; only Pilot 10, 11, 15
- **negative/19_missing_semicolon**: in common 9; only Validator none; only Pilot 8
- **negative/21_invalid_identifier**: in common 7; only Validator none; only Pilot 8
- **negative/22_reserved_keyword_as_name**: in common 7; only Validator none; only Pilot 8
- **negative/24_invalid_assignment_operator**: in common 11; only Validator none; only Pilot 13, 8, 9
- **negative/26_unclosed_comment**: in common 7; only Validator 11, 8; only Pilot none
- **negative/28_double_def_keyword**: in common 7; only Validator none; only Pilot 8
- **negative/29_missing_package_brace**: in common 10, 7; only Validator none; only Pilot 8
- **negative/30_invalid_attribute_syntax**: in common 8; only Validator none; only Pilot 10
- **negative/35_wrong_scope_access**: in common 8; only Validator none; only Pilot 14
- **negative/39_undefined_constraint_variable**: in common 12; only Validator none; only Pilot 14
- **negative/40_missing_required_parameter**: in common 8, 9; only Validator none; only Pilot 15
- **negative/45_constraint_not_boolean**: in common 12; only Validator none; only Pilot 14
- **negative/47_redefines_nonexistent**: in common 8; only Validator none; only Pilot 12
- **negative/50_requirement_without_subject**: in common 8; only Validator none; only Pilot 10, 12
- **negative/55_breakfast_example_original**: in common 102, 107, 143, 148, 97; only Validator 226; only Pilot 103, 108, 109, 114, 118, 128, 129, 130 … (23)
- **negative/MissingDefinitions_Actions_8Trigger**: in common 5; only Validator none; only Pilot 6
- **lint/public_imports**: in common 31, 32, 36; only Validator 59; only Pilot 20, 21, 26, 38
- **cheatsheet/cheatsheet_validation**: in common 100, 335; only Validator 542, 552, 561, 574; only Pilot 221, 230, 231, 259, 286, 293, 337, 340 … (14)
- **cheatsheet/pastableExamples/Actions_8Trigger**: in common 9; only Validator none; only Pilot 12, 2, 7
- **cheatsheet/pastableExamples/Evaluation_Tutorial**: in common 36, 46, 58; only Validator none; only Pilot 66
- **cheatsheet/pastableExamples/MergedExamples**: in common 709; only Validator none; only Pilot 1053, 111, 149, 206, 281, 282, 455, 501 … (18)
- **dassault/Metadata**: in common 31, 32; only Validator none; only Pilot 19, 35, 36, 37, 38, 41, 43
- **examples/lint_metadata_examples**: in common 114, 121, 122, 146, 147, 175, 69, 76 … (10); only Validator none; only Pilot 108, 109, 110, 111, 133, 134, 135, 18 … (32)
- **kpar/stockticker**: in common StockTickerPhysical.sysml:154, StockTickerPhysical.sysml:162, StockTickerPhysical.sysml:351, StockTickerPhysical.sysml:352, StockTickerPhysical.sysml:353, StockTickerPhysical.sysml:354, StockTickerPhysical.sysml:355, StockTickerPhysical.sysml:361 … (10); only Validator none; only Pilot StockTickerLogical.sysml:732, StockTickerLogical.sysml:742, StockTickerLogical.sysml:752, StockTickerLogical.sysml:815, StockTickerLogical.sysml:837, StockTickerLogical.sysml:859, StockTickerLogical.sysml:880, StockTickerLogical.sysml:896 … (204)
- **kpar/two-members-error**: in common Car.sysml:3; only Validator none; only Pilot Car.sysml:4, Car.sysml:5

## All models

Errors and warnings: counts; for this validator, warnings of added checks in parentheses.

| Model | Expected | Validator | Pilot | Validator errors / warnings | Pilot errors / warnings |
|---|---|---|---|---|---|
| positive/01_simple_part_def | clean | accepted | accepted | 0 / 0 | 0 / 0 |
| positive/02_action_with_succession | clean | accepted | accepted | 0 / 0 (+1) | 0 / 1 |
| positive/03_state_machine | clean | accepted | accepted | 0 / 0 | 0 / 0 |
| positive/04_compact_flow_notation | clean | accepted | accepted | 0 / 0 (+2) | 0 / 0 |
| positive/05_multiple_successions | clean | accepted | accepted | 0 / 0 (+3) | 0 / 0 |
| positive/06_state_with_conditions | clean | accepted | accepted | 0 / 0 (+1) | 0 / 1 |
| positive/07_requirement_with_constraint | clean | accepted | rejected | 0 / 0 | 3 / 0 |
| positive/08_part_specialization | clean | accepted | accepted | 0 / 0 | 0 / 0 |
| positive/09_ports_and_connections | clean | accepted | rejected | 0 / 0 (+1) | 1 / 2 |
| positive/10_enum_definition | clean | accepted | accepted | 0 / 0 | 0 / 0 |
| positive/11_breakfast_example_corrected | clean | accepted | rejected | 0 / 0 (+20) | 44 / 0 |
| positive/12_simple_vehicle_model | errors | accepted | rejected | 0 / 0 (+1) | 88 / 0 |
| positive/13_actions_drone_delivery | clean | accepted | rejected | 0 / 0 | 24 / 0 |
| positive/14_flows | clean | accepted | rejected | 0 / 0 | 9 / 0 |
| positive/15_states | clean | accepted | rejected | 0 / 0 | 3 / 0 |
| positive/16_interfaces | clean | accepted | rejected | 0 / 0 | 9 / 0 |
| positive/17_use_case | clean | accepted | rejected | 0 / 0 | 12 / 0 |
| positive/18_allocation_definitions | clean | accepted | rejected | 0 / 0 (+5) | 1 / 12 |
| positive/19_analysis_calculations | clean | accepted | rejected | 0 / 0 (+5) | 3 / 2 |
| positive/20_verification_cases | clean | accepted | accepted | 0 / 0 (+5) | 0 / 1 |
| positive/21_metadata_annotations | clean | accepted | rejected | 0 / 0 (+2) | 2 / 0 |
| positive/22_item_flow_definitions | clean | accepted | accepted | 0 / 0 (+2) | 0 / 0 |
| positive/23_occurrence_temporal | clean | accepted | rejected | 0 / 0 (+12) | 2 / 6 |
| positive/24_rendering_definitions | clean | accepted | accepted | 0 / 0 (+3) | 0 / 8 |
| positive/25_trade_studies | clean | accepted | rejected | 0 / 0 (+10) | 3 / 9 |
| positive/26_message_accept | clean | accepted | rejected | 0 / 0 (+1) | 1 / 0 |
| positive/27_complex_multiplicities | clean | accepted | rejected | 0 / 0 (+11) | 3 / 2 |
| positive/28_feature_redefinition | clean | accepted | rejected | 0 / 0 (+6) | 9 / 2 |
| positive/29_abstract_variation | clean | accepted | rejected | 0 / 0 (+3) | 2 / 0 |
| positive/30_exhibit_perform | clean | accepted | rejected | 0 / 0 (+3) | 6 / 0 |
| positive/31_binding_connections | clean | accepted | accepted | 0 / 0 (+2) | 0 / 0 |
| positive/32_reference_subsetting | clean | accepted | rejected | 0 / 0 (+2) | 1 / 0 |
| positive/33_conjugate_interface | clean | accepted | rejected | 0 / 0 (+1) | 5 / 5 |
| positive/34_constraint_assertions | clean | accepted | accepted | 0 / 0 (+2) | 0 / 0 |
| positive/35_dependency_concern | clean | accepted | accepted | 0 / 0 | 0 / 0 |
| positive/36_sequence_collections | clean | accepted | rejected | 0 / 0 (+5) | 17 / 5 |
| positive/37_default_values | clean | accepted | accepted | 0 / 0 (+4) | 0 / 0 |
| positive/38_readonly_derived | clean | accepted | rejected | 0 / 0 (+5) | 3 / 0 |
| positive/39_succession_flow | clean | accepted | rejected | 0 / 0 (+3) | 8 / 0 |
| positive/40_objective_use_case | clean | accepted | rejected | 0 / 0 (+1) | 1 / 0 |
| positive/41_state_entry_exit | clean | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| positive/42_feature_typing | clean | accepted | accepted | 0 / 0 (+5) | 0 / 0 |
| positive/43_enumeration_definitions | clean | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| positive/44_variant_configuration | clean | accepted | rejected | 0 / 0 (+5) | 8 / 5 |
| positive/45_import_membership | clean | accepted | rejected | 0 / 0 (+7) | 2 / 0 |
| positive/46_loop_conditional | clean | accepted | accepted | 0 / 0 (+9) | 0 / 0 |
| positive/47_expression_operators | clean | accepted | rejected | 0 / 0 (+10) | 5 / 0 |
| positive/48_library_package | clean | accepted | rejected | 0 / 0 (+3) | 5 / 2 |
| positive/49_actor_stakeholder | clean | accepted | rejected | 0 / 0 (+3) | 6 / 2 |
| positive/50_accept_via | clean | accepted | accepted | 0 / 0 (+5) | 0 / 0 |
| positive/60_cameo_26xR2_constructs | clean | accepted | rejected | 0 / 0 | 2 / 3 |
| positive/61_diamond_resolved | clean | accepted | accepted | 0 / 0 | 0 / 0 |
| positive/62_diamond_unresolved_warnings | clean | accepted | accepted | 0 / 0 (+8) | 0 / 8 |
| negative/01_action_parallel_invalid | errors | rejected | rejected | 1 / 0 (+1) | 2 / 0 |
| negative/02_action_then_invalid | errors | accepted | rejected | 0 / 0 (+1) | 2 / 0 |
| negative/03_action_decide_invalid | errors | rejected | rejected | 1 / 0 (+1) | 5 / 1 |
| negative/04_action_if_invalid | errors | rejected | rejected | 1 / 0 (+1) | 5 / 0 |
| negative/05_multiple_action_parallel | errors | rejected | rejected | 3 / 0 (+3) | 4 / 0 |
| negative/06_action_parallel_then_combined | errors | rejected | rejected | 1 / 0 (+2) | 3 / 0 |
| negative/07_nested_action_if | errors | rejected | rejected | 3 / 0 (+2) | 7 / 0 |
| negative/08_action_decide_with_if | errors | rejected | rejected | 5 / 0 (+3) | 8 / 2 |
| negative/09_all_invalid_modifiers | errors | rejected | rejected | 2 / 0 (+4) | 5 / 0 |
| negative/10_action_parallel_with_params | errors | rejected | rejected | 1 / 0 (+1) | 7 / 1 |
| negative/11_action_then_succession | errors | accepted | rejected | 0 / 0 (+1) | 2 / 0 |
| negative/12_action_decide_boolean | errors | rejected | rejected | 1 / 0 (+1) | 5 / 1 |
| negative/13_undefined_type | errors | rejected | rejected | 1 / 0 (+1) | 2 / 0 |
| negative/14_missing_scalar_import | errors | rejected | rejected | 1 / 0 (+2) | 2 / 0 |
| negative/15_wrong_import_path | errors | rejected | rejected | 1 / 0 (+1) | 1 / 0 |
| negative/16_undefined_attribute_type | errors | rejected | rejected | 2 / 0 (+2) | 4 / 0 |
| negative/17_undefined_part_reference | errors | rejected | rejected | 2 / 0 (+2) | 4 / 0 |
| negative/18_circular_import | errors | rejected | rejected | 2 / 0 (+1) | 3 / 0 |
| negative/19_missing_semicolon | errors | rejected | rejected | 1 / 0 (+1) | 5 / 0 |
| negative/20_missing_brace | errors | rejected | rejected | 1 / 0 (+1) | 3 / 0 |
| negative/21_invalid_identifier | errors | rejected | rejected | 1 / 0 (+1) | 3 / 0 |
| negative/22_reserved_keyword_as_name | errors | rejected | rejected | 1 / 0 (+1) | 3 / 0 |
| negative/23_malformed_package | errors | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| negative/24_invalid_assignment_operator | errors | rejected | rejected | 1 / 0 (+1) | 8 / 0 |
| negative/25_missing_colon_type | errors | rejected | rejected | 1 / 0 (+1) | 1 / 0 |
| negative/26_unclosed_comment | errors | rejected | rejected | 5 / 0 | 1 / 0 |
| negative/27_invalid_multiplicity_syntax | errors | rejected | rejected | 2 / 0 (+1) | 3 / 0 |
| negative/28_double_def_keyword | errors | rejected | rejected | 1 / 0 (+1) | 3 / 0 |
| negative/29_missing_package_brace | errors | rejected | rejected | 2 / 0 (+1) | 4 / 0 |
| negative/30_invalid_attribute_syntax | errors | rejected | rejected | 1 / 0 (+1) | 4 / 0 |
| negative/31_type_mismatch_assignment | errors | accepted | accepted | 0 / 0 (+1) | 0 / 1 |
| negative/32_undefined_action_output | errors | accepted | rejected | 0 / 0 (+1) | 1 / 0 |
| negative/33_undefined_state_reference | errors | accepted | rejected | 0 / 0 (+1) | 2 / 0 |
| negative/34_undefined_port_reference | errors | accepted | rejected | 0 / 0 (+1) | 1 / 0 |
| negative/35_wrong_scope_access | errors | rejected | rejected | 1 / 0 (+1) | 4 / 0 |
| negative/36_incompatible_connection | errors | rejected | rejected | 2 / 0 (+1) | 4 / 0 |
| negative/37_array_index_out_of_bounds | errors | accepted | accepted | 0 / 0 (+1) | 0 / 1 |
| negative/38_wrong_parameter_type | errors | accepted | accepted | 0 / 0 (+1) | 0 / 1 |
| negative/39_undefined_constraint_variable | errors | rejected | rejected | 1 / 0 (+1) | 4 / 0 |
| negative/40_missing_required_parameter | errors | rejected | rejected | 2 / 0 (+2) | 6 / 1 |
| negative/41_duplicate_attribute_name | errors | accepted | accepted | 0 / 0 (+3) | 0 / 2 |
| negative/42_wrong_succession_target | errors | accepted | rejected | 0 / 0 (+1) | 1 / 0 |
| negative/43_circular_specialization | errors | accepted | accepted | 0 / 0 (+2) | 0 / 0 |
| negative/44_invalid_multiplicity_range | errors | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| negative/45_constraint_not_boolean | errors | rejected | rejected | 1 / 0 (+1) | 4 / 0 |
| negative/46_abstract_part_instantiation | errors | rejected | rejected | 1 / 0 (+1) | 2 / 0 |
| negative/47_redefines_nonexistent | errors | rejected | rejected | 1 / 0 (+1) | 3 / 0 |
| negative/48_invalid_flow_direction | errors | accepted | accepted | 0 / 0 (+3) | 0 / 0 |
| negative/49_state_transition_missing_source | errors | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| negative/50_requirement_without_subject | errors | rejected | rejected | 1 / 0 (+1) | 7 / 1 |
| negative/51_port_without_definition | errors | rejected | rejected | 1 / 0 (+1) | 2 / 0 |
| negative/52_invalid_enum_value | errors | accepted | rejected | 0 / 0 (+1) | 1 / 1 |
| negative/53_incompatible_redefinition_type | errors | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| negative/54_succession_creates_deadlock | errors | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| negative/55_breakfast_example_original | errors | rejected | rejected | 8 / 0 (+32) | 40 / 15 |
| negative/56_unresolved_library_type | errors | rejected | rejected | 2 / 0 | 4 / 0 |
| negative/MissingDefinitions_Actions_8Trigger | errors | rejected | rejected | 1 / 0 | 4 / 2 |
| lint/good_practices | - | rejected | rejected | 9 / 0 | 3 / 0 |
| lint/naming_conventions | - | rejected | rejected | 11 / 0 | 22 / 0 |
| lint/public_imports | - | rejected | rejected | 4 / 0 | 11 / 0 |
| lint/refactoring_example | - | rejected | rejected | 11 / 0 | 3 / 0 |
| lint/unit_mismatches | - | accepted | rejected | 0 / 0 (+8) | 25 / 12 |
| lint/unused_elements | - | rejected | rejected | 9 / 0 | 3 / 0 |
| lint/wildcard_imports | - | accepted | rejected | 0 / 0 | 3 / 0 |
| cheatsheet/cheatsheet_validation | - | rejected | rejected | 6 / 0 | 23 / 6 |
| cheatsheet/pastableExamples/ActionPatterns_1WhileLoop | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/ActionPatterns_2ForLoop | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/ActionPatterns_2bLoopVariations | - | accepted | accepted | 0 / 0 | 0 / 2 |
| cheatsheet/pastableExamples/ActionPatterns_3IfElse | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/ActionPatterns_4AcceptVariations | - | accepted | rejected | 0 / 0 | 2 / 2 |
| cheatsheet/pastableExamples/ActionPatterns_5SendVariations | - | accepted | rejected | 0 / 0 | 1 / 2 |
| cheatsheet/pastableExamples/ActionPatterns_6ControlNodes | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/ActionPatterns_7AdvancedSend | - | accepted | rejected | 0 / 0 | 2 / 0 |
| cheatsheet/pastableExamples/Actions_1ActionDefinition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Actions_2ActionUsage | - | accepted | accepted | 0 / 0 | 0 / 4 |
| cheatsheet/pastableExamples/Actions_4Parameters | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Actions_5SendAcceptSignal | - | accepted | rejected | 0 / 0 | 1 / 1 |
| cheatsheet/pastableExamples/Actions_6Successionfirstthen | - | accepted | accepted | 0 / 0 | 0 / 1 |
| cheatsheet/pastableExamples/Actions_7Assignmentassign | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Actions_8Trigger | - | rejected | rejected | 1 / 0 | 5 / 1 |
| cheatsheet/pastableExamples/Actions_Tutorial | - | accepted | rejected | 0 / 0 | 15 / 0 |
| cheatsheet/pastableExamples/Allocation_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Analysis_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Behavior_1StateDefinition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Behavior_2Transitions | - | accepted | rejected | 0 / 0 | 2 / 2 |
| cheatsheet/pastableExamples/Behavior_3GuardsEffects | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Behavior_3bInternalTransition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Behavior_4ActionDefinition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Behavior_5ActionUsage | - | accepted | accepted | 0 / 0 | 0 / 2 |
| cheatsheet/pastableExamples/Behavior_6UseCases | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Calculations_1CalculationDefinition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Calculations_2CalculationUsage | - | accepted | accepted | 0 / 0 | 0 / 2 |
| cheatsheet/pastableExamples/Calculations_4ConstraintUsage | - | accepted | accepted | 0 / 0 | 0 / 1 |
| cheatsheet/pastableExamples/Calculations_5Assertions | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Calculations_6Requirements | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Cases_1UseCaseDefinition | - | accepted | rejected | 0 / 0 | 1 / 1 |
| cheatsheet/pastableExamples/Cases_2TestCaseVerification | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Cases_3AnalysisCase | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Cases_4CaseUsage | - | accepted | rejected | 0 / 0 | 4 / 5 |
| cheatsheet/pastableExamples/Connections_1ConnectionDefinition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Connections_1bConnectionUsage | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Connections_2BindingConnector | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Connections_3InterfaceConnection | - | accepted | rejected | 0 / 0 | 1 / 2 |
| cheatsheet/pastableExamples/Connections_4SuccessionFlow | - | accepted | rejected | 0 / 0 | 1 / 2 |
| cheatsheet/pastableExamples/Connections_Tutorial | - | accepted | rejected | 0 / 0 | 2 / 2 |
| cheatsheet/pastableExamples/Constraints_1ConstraintDefinition | - | accepted | accepted | 0 / 0 | 0 / 2 |
| cheatsheet/pastableExamples/Constraints_2ConstraintUsageAssert | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Constraints_2bInlineAssertion | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Constraints_3CalculationDefinition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Constraints_4CalculationUsage | - | accepted | accepted | 0 / 0 | 0 / 2 |
| cheatsheet/pastableExamples/CoreTypes_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/DataTypes_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 2 |
| cheatsheet/pastableExamples/DomainLibs_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 1 |
| cheatsheet/pastableExamples/Evaluation_Tutorial | - | rejected | rejected | 3 / 0 | 4 / 5 |
| cheatsheet/pastableExamples/Features_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Filters_Tutorial | - | accepted | rejected | 0 / 0 | 10 / 0 |
| cheatsheet/pastableExamples/Flows_Tutorial | - | accepted | rejected | 0 / 0 | 6 / 0 |
| cheatsheet/pastableExamples/MergedExamples | - | rejected | rejected | 1 / 0 (+113) | 22 / 37 |
| cheatsheet/pastableExamples/Metadata_1MetadataDefinition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Metadata_2ElementAnnotation | - | accepted | rejected | 0 / 0 | 1 / 0 |
| cheatsheet/pastableExamples/Metadata_3RelationshipAnnotation | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Metadata_4StandardMetadata | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Metadata_5MetadataExtension | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Metadata_6ShortAnnotation | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Multiplicity_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Naming_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/PartsAndAttributes_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 2 |
| cheatsheet/pastableExamples/Patterns_1MetadataAnnotations | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Patterns_2Views | - | accepted | rejected | 0 / 0 | 1 / 0 |
| cheatsheet/pastableExamples/Patterns_3CustomUnitsNanoBanana | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Patterns_4AbstractvsIndividual | - | accepted | rejected | 0 / 0 | 1 / 0 |
| cheatsheet/pastableExamples/PortsInterfaces_Tutorial | - | accepted | rejected | 0 / 0 | 2 / 2 |
| cheatsheet/pastableExamples/Reference_1CommonKeywords | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Reference_2PrimitiveTypes | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Reference_3Relationships | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Reference_4Comments | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Reference_5Multiplicity | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Reference_6Visibility | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Requirements_1RequirementDefinition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Requirements_2RequirementUsage | - | accepted | accepted | 0 / 0 (+1) | 0 / 2 |
| cheatsheet/pastableExamples/Requirements_3Satisfy | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Requirements_4Verify | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Requirements_5ConstraintDefinition | - | accepted | accepted | 0 / 0 | 0 / 1 |
| cheatsheet/pastableExamples/Requirements_6AssertConstraint | - | accepted | accepted | 0 / 0 | 0 / 1 |
| cheatsheet/pastableExamples/Requirements_6Assertions | - | accepted | accepted | 0 / 0 | 0 / 1 |
| cheatsheet/pastableExamples/Requirements_7TraceRefine | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Requirements_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/SemanticMetadata_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Shorthand_1Specialization | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Shorthand_2Subsetting | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Shorthand_3Redefinition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Shorthand_4Conjugation | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Shorthand_5FeatureValues | - | accepted | rejected | 0 / 0 | 1 / 0 |
| cheatsheet/pastableExamples/Shorthand_6Multiplicity | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/StateMachine_Tutorial | - | accepted | rejected | 0 / 0 | 3 / 0 |
| cheatsheet/pastableExamples/StatePatterns_1EntryDoExit | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/StatePatterns_2CompositeState | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/StatePatterns_3ExhibitState | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/StatePatterns_4InternalTransition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/States_1StateDefinition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/States_2Transitions | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/States_3GuardsEffects | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/States_4CompositeStates | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/States_5ParallelStates | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/UseCaseTutorial_APS | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Variants_Tutorial | - | accepted | rejected | 0 / 0 | 4 / 2 |
| cheatsheet/pastableExamples/Viewpoints_Tutorial | - | accepted | accepted | 0 / 0 | 0 / 1 |
| cheatsheet/pastableExamples/Views_Sheet_1ViewDefinition | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Views_Sheet_2ViewUsage | - | accepted | accepted | 0 / 0 | 0 / 1 |
| cheatsheet/pastableExamples/Views_Sheet_3Viewpoint | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Views_Sheet_4ExposeContent | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Views_Sheet_5Filter | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Views_Sheet_6RenderingStyle | - | accepted | accepted | 0 / 0 | 0 / 0 |
| cheatsheet/pastableExamples/Views_Tutorial | - | accepted | rejected | 0 / 0 | 3 / 9 |
| dsl/DSLinSysML | - | accepted | accepted | 0 / 0 | 0 / 0 |
| dassault/Actions | - | accepted | rejected | 0 / 0 | 24 / 0 |
| dassault/Flows | - | accepted | rejected | 0 / 0 | 9 / 0 |
| dassault/Import | - | accepted | rejected | 0 / 0 | 3 / 0 |
| dassault/Index | - | accepted | rejected | 0 / 0 | 3 / 0 |
| dassault/Individual | - | accepted | rejected | 0 / 0 | 3 / 0 |
| dassault/Interfaces | - | accepted | rejected | 0 / 0 | 9 / 0 |
| dassault/Metadata | - | rejected | rejected | 2 / 0 | 20 / 7 |
| dassault/N-ary Interfaces | - | accepted | rejected | 0 / 0 | 3 / 0 |
| dassault/Requirements Derivation | - | accepted | rejected | 0 / 0 | 3 / 0 |
| dassault/States | - | accepted | rejected | 0 / 0 | 3 / 0 |
| dassault/Structural and Behavior Modeling | - | accepted | rejected | 0 / 0 | 3 / 0 |
| dassault/Use Case | - | accepted | rejected | 0 / 0 | 12 / 0 |
| dassault/ViewsRenderingAndExposedElements | - | accepted | rejected | 0 / 0 (+1) | 124 / 0 |
| pilot-repro/V1 | - | accepted | accepted | 0 / 0 (+1) | 0 / 1 |
| pilot-repro/V10a | - | accepted | accepted | 0 / 0 | 0 / 0 |
| pilot-repro/V10b | - | accepted | accepted | 0 / 0 (+3) | 0 / 2 |
| pilot-repro/V10c | - | accepted | accepted | 0 / 0 (+3) | 0 / 2 |
| pilot-repro/V10d | - | accepted | rejected | 0 / 0 (+3) | 3 / 3 |
| pilot-repro/V10e | - | accepted | rejected | 0 / 0 (+1) | 1 / 1 |
| pilot-repro/V10f | - | accepted | accepted | 0 / 0 (+2) | 0 / 2 |
| pilot-repro/V10g | - | accepted | accepted | 0 / 0 (+2) | 0 / 0 |
| pilot-repro/V1ok | - | accepted | accepted | 0 / 0 | 0 / 0 |
| pilot-repro/V2 | - | accepted | accepted | 0 / 0 (+2) | 0 / 2 |
| pilot-repro/V2x | - | accepted | accepted | 0 / 0 (+2) | 0 / 3 |
| pilot-repro/V3 | - | accepted | accepted | 0 / 0 (+4) | 0 / 4 |
| pilot-repro/V4a | - | accepted | accepted | 0 / 0 (+2) | 0 / 2 |
| pilot-repro/V4b | - | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| pilot-repro/V5 | - | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| pilot-repro/V6a | - | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| pilot-repro/V6b | - | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| pilot-repro/V6d | - | accepted | accepted | 0 / 0 | 0 / 0 |
| pilot-repro/V6f | - | rejected | rejected | 10 / 0 (+7) | 10 / 3 |
| pilot-repro/V7a | - | accepted | accepted | 0 / 0 (+2) | 0 / 0 |
| pilot-repro/V7b | - | accepted | accepted | 0 / 0 (+1) | 0 / 0 |
| pilot-repro/V8 | - | rejected | rejected | 1 / 0 (+1) | 1 / 1 |
| pilot-repro/V9 | - | accepted | accepted | 0 / 0 (+1) | 0 / 1 |
| misc/test_extend | - | accepted | rejected | 0 / 0 | 5 / 0 |
| examples/NameCollisionExamples | warnings | accepted | accepted | 0 / 0 (+22) | 0 / 14 |
| examples/NameCollisionErrorForm | errors | rejected | rejected | 1 / 0 (+1) | 1 / 1 |
| examples/MassTypedByMass | errors | rejected | rejected | 3 / 0 (+3) | 3 / 3 |
| examples/16_medical_device_stability_v2 | - | accepted | rejected | 0 / 0 (+45) | 1 / 13 |
| examples/lint_metadata_examples | - | rejected | rejected | 10 / 0 (+1) | 96 / 0 |
| examples/lint_test | - | accepted | accepted | 0 / 0 | 0 / 0 |
| kpar/stockticker | - | rejected | rejected | 10 / 0 (+833) | 356 / 176 |
| kpar/name-collision-examples | warnings | accepted | accepted | 0 / 0 (+22) | 0 / 14 |
| kpar/two-members | clean | accepted | accepted | 0 / 0 | 0 / 0 |
| kpar/two-members-error | errors | rejected | rejected | 1 / 0 | 4 / 0 |
| kpar/invalid-no-project | invalid | rejected | skipped | 1 / 0 | 0 / 0 |
| kpar/invalid-not-a-zip | invalid | rejected | skipped | 1 / 0 | 0 / 0 |

## Notes

- **Verdict**: *accepted*, no errors; *rejected*, at least one error; *failed*, the tool could not load the
  model (counted as a rejection). Cameo's verdict covers its import, its text diagnostics (the build
  step) and its validation. *Unchecked*: Cameo imported the model but its build step did not run, so
  its text was not checked; *skipped*: the tool did not run the model; *missing*: the run has no row for
  it. These three are left out of the agreements and disagreements.
- **Error places** are member and line: the line in a plain file, `member:line` in a KPAR. Cameo's
  validation reports elements, not lines, so only its text errors have places; a tool with no places
  for a model is left out of that model's comparison. Columns are not compared: the tools count them
  differently.
- **Added checks**: warnings of checks this validator adds to the language rules (lint, name collisions,
  diamond inheritance, line comments Cameo does not keep). The other tools do not have them, so they are
  counted apart.
- The pilot does not resolve the short name of a top-level package declared in another file
  (`DS_Views::…` for Dassault's `DassaultSystemesViews`), which Cameo and this validator do. Models that
  use Dassault's libraries through those names are rejected by the pilot for that reason; the
  disagreements mark them.
- The pilot reads no KPARs, and Cameo's KPAR import is not used yet: their runners read the archives,
  and skip one they cannot read rather than count it as rejected by the tool.
