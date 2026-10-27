# Performance: this validator and the pilot implementation

Compared: `validator_MacBook-Pro-2.local_20261027-102056` and `pilot_MacBook-Pro-2.local_20261027-095701`.

| | Validator | Pilot |
|---|---|---|
| tool | sysmlv2-validator 0.2.0-SNAPSHOT | pilot 0.63.0 |
| cpus | 12 | 12 |
| heap.max.mb | 16384 | 4096 |
| host | MacBook-Pro-2.local | MacBook-Pro-2.local |
| java | Homebrew 25.0.2 | Homebrew 25.0.2 |
| library | [/private/tmp/claude-502/-Users-turbogeek-Google-Drive-git/a83dabe3-77d1-4c0b-abcd-d157856e492d/scratchpad/pilot/kernel63/sysml/sysml.library] | /private/tmp/claude-502/-Users-turbogeek-Google-Drive-git/a83dabe3-77d1-4c0b-abcd-d157856e492d/scratchpad/pilot/kernel63/sysml/sysml.library |
| library.dassault | [/Users/turbogeek/Desktop/git/sysmlv2-validator/CATIA_SysML Libraries/26xR2] |  |
| library.extra |  |  |
| load.average.end | 9.47 | 4.15 |
| load.average.start | 12.75 | 14.02 |
| manifest |  | perf/corpus/manifest.csv |
| manifest.sha256 | d160b5385a4b2fc9ae02842da805d83d11893a816b8c43cc21288b1e79bbe94b | d160b5385a4b2fc9ae02842da805d83d11893a816b8c43cc21288b1e79bbe94b |
| only |  |  |
| os | Mac OS X 26.5 x86_64 | Mac OS X 26.5 x86_64 |
| phases | validate, parse, index |  |
| pilot.jar |  | /private/tmp/claude-502/-Users-turbogeek-Google-Drive-git/a83dabe3-77d1-4c0b-abcd-d157856e492d/scratchpad/pilot/kernel63/sysml/jupyter-sysml-kernel-0.63.0-all.jar |
| pilot.kpar |  | members parsed into one resource each, indexed together, then validated |
| release |  | /Users/turbogeek/Desktop/git/SysML-v2-Release |
| repetitions | 3 (1 above 10000 lines) | 3 (1 above 10000 lines) |
| run.id | 20261027-152137 | 20261027-145705 |

## Startup: loading the standard library

| Validator | Pilot |
|---:|---:|
| 1042 | 7537 |

## Time per model (ms)

| Model | Lines | Validator `validate` | Pilot `process` | Ratio | Validator errors / warnings | Pilot errors / warnings |
|---|---:|---:|---:|---:|---:|---:|
| system-xs | 105 | 2371 / 129 | 2439 / 885 | 6.8 | 0 / 0 | 0 / 0 |
| system-s | 573 | 313 / 201 | 2412 / 2201 | 10.9 | 0 / 0 | 0 / 0 |
| system-m | 5253 | 410 / 291 | 11531 / 10750 | 36.9 | 0 / 0 | 0 / 0 |
| system-l | 26053 | 1127 | 115526 | 102.5 (1 run) | 0 / 0 | 0 / 0 |
| system-xl | 65053 | 3283 | 686070 | 209.0 (1 run) | 0 / 0 | 0 / 0 |
| inheritance-200 | 809 | 376 / 242 | 6530 / 6910 | 28.5 | 0 / 0 | 0 / 0 |
| imports-100 | 1892 | 150 / 124 | 826 / 782 | 6.3 | 0 / 0 | 0 / 0 |
| errors-m | 5253 | 451 / 262 | 10085 / 10485 | 40.0 | 20 / 40 | 60 / 60 |
| real-cameo-constructs | 89 | 1346 / 168 | 1396 / 946 | 5.6 | 0 / 0 | 2 / 3 |
| real-name-collisions | 323 | 927 / 193 | 775 / 648 | 3.4 | 0 / 22 | 0 / 14 |
| real-mass-typed-by-mass | 47 | 223 / 154 | 528 / 447 | 2.9 | 3 / 3 | 3 / 3 |
| real-vehicle-legacy | 1493 | 16786 / 236 | 7469 / 5229 | 22.2 | 0 / 1 | 88 / 0 |
| release-annex-a-vehicle | 1581 | 1958 / 263 | 5016 / 4741 | 18.0 | 0 / 46 | 0 / 0 |

## Notes

- Times are milliseconds: the first repetition of each model, then the median of the others. Models
  above 10,000 lines run once on both sides; their ratio compares the first runs and is marked "(1 run)".
- **Ratio** is the pilot's time over this validator's: above 1, the validator is faster.
- The counts differ by design where this validator adds checks the pilot does not have (lint, name
  collisions, diamonds); see conformance/ for the comparison of what each tool reports.
- `load.average.*` is the machine's load during the run: other work on the machine skews the times.
