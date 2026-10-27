# SysML v2 Pilot Implementation 0.63.0 on MacBook-Pro-2.local

Milliseconds: first repetition / median of the others. `process` is parsing and validating the model with SysMLInteractive (the members of a KPAR together). Errors and warnings are from the first repetition.

Startup (standard library loaded): 7537 ms

| Model | Lines | process | Errors | Warnings |
|---|---:|---:|---:|---:|
| system-xs | 105 | 2439.2 / 885.4 | 0 | 0 |
| system-s | 573 | 2412.3 / 2200.7 | 0 | 0 |
| system-m | 5253 | 11531.0 / 10749.7 | 0 | 0 |
| system-l | 26053 | 115525.6 | 0 | 0 |
| system-xl | 65053 | 686070.3 | 0 | 0 |
| inheritance-200 | 809 | 6530.3 / 6910.4 | 0 | 0 |
| imports-100 | 1892 | 825.5 / 781.9 | 0 | 0 |
| errors-m | 5253 | 10085.2 / 10485.5 | 60 | 60 |
| real-cameo-constructs | 89 | 1396.5 / 946.2 | 2 | 3 |
| real-name-collisions | 323 | 774.5 / 647.7 | 0 | 14 |
| real-mass-typed-by-mass | 47 | 528.3 / 447.4 | 3 | 3 |
| real-vehicle-legacy | 1493 | 7468.9 / 5229.0 | 88 | 0 |
| release-annex-a-vehicle | 1581 | 5016.4 / 4741.5 | 0 | 0 |
