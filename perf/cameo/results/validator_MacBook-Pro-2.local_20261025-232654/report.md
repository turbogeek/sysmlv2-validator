# sysmlv2-validator 0.2.0-SNAPSHOT on MacBook-Pro-2.local

Milliseconds: first repetition / median of the others. `validate` is the whole validation of the file, parse included.

Startup (standard library loaded): 1014 ms

| Model | Lines | validate | parse | index | Elements | Errors | Warnings |
|---|---:|---:|---:|---:|---:|---:|---:|
| system-xs | 105 | 2331 / 126 | 15.3 / 14.5 | 16.6 / 14.4 | 75 | 0 | 0 |
| system-s | 573 | 315 / 180 | 37.3 / 14.6 | 20.0 / 8.3 | 399 | 0 | 0 |
| system-m | 5253 | 439 / 296 | 62.8 / 47.1 | 20.9 / 18.1 | 3639 | 0 | 0 |
| system-l | 26053 | 1155 | 399 | 83.1 | 18039 | 0 | 0 |
| system-xl | 65053 | 3068 | 2004 | 175 | 45039 | 0 | 0 |
| inheritance-200 | 809 | 313 / 188 | 4.7 / 4.8 | 3.6 / 2.9 | 604 | 0 | 0 |
| imports-100 | 1892 | 138 / 114 | 7.3 / 8.6 | 3.6 / 3.4 | 1491 | 0 | 0 |
| errors-m | 5253 | 419 / 288 | 34.4 / 38.8 | 12.7 / 13.6 | 3639 | 20 | 40 |
| real-cameo-constructs | 89 | 1245 / 171 | 12.2 / 3.9 | 5.2 / 5.6 | 133 | 0 | 0 |
| real-name-collisions | 323 | 817 / 191 | 5.3 / 4.6 | 18.4 / 8.2 | 155 | 0 | 22 |
| real-mass-typed-by-mass | 47 | 204 / 147 | 3.0 / 2.8 | 2.2 / 1.8 | 16 | 3 | 3 |
| real-vehicle-legacy | 1493 | 16976 / 215 | 40.2 / 19.6 | 32.1 / 8.7 | 917 | 0 | 1 |
| release-annex-a-vehicle | 1581 | 1897 / 224 | 51.9 / 20.5 | 5.7 / 4.7 | 891 | 0 | 46 |
