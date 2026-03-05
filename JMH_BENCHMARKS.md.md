## JMH Benchmarks

### 1. (sin(3) + cos(4 - sin(2))) ^ (-2) 
```
Benchmark             Mode Cnt Score   Error     Units
ParserNGWars.exp4j    avgt 10 194.676  ± 27.136   ns/op
ParserNGWars.javaMep  avgt 10 2109.033 ± 1149.320 ns/op
ParserNGWars.parserNg avgt 10 275.365  ± 111.143  ns/op
```

### 2. ((12+5)*3 - (45/9))^2
```
Benchmark              Mode  Cnt     Score     Error  Units
ParserNGWars.exp4j     avgt   10   155.566 ±  19.795  ns/op
ParserNGWars.javaMep   avgt   10  2086.303 ± 461.743  ns/op
ParserNGWars.parserNg  avgt   10   144.043 ±   7.669  ns/op
```