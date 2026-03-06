## JMH Benchmarks

### 1. (sin(3) + cos(4 - sin(2))) ^ (-2) 
```
Benchmark              Mode  Cnt    Score    Error  Units
ParserNGWars.exp4j     avgt   10  184.211 ± 10.622  ns/op
ParserNGWars.parserNg  avgt   10  175.858 ±  6.081  ns/op
```

### 2. ((12+5)*3 - (45/9))^2
```
Benchmark              Mode  Cnt     Score     Error  Units
ParserNGWars.exp4j     avgt   10   155.566 ±  19.795  ns/op
ParserNGWars.javaMep   avgt   10  2086.303 ± 461.743  ns/op
ParserNGWars.parserNg  avgt   10   144.043 ±   7.669  ns/op
```

### 3. 5*sin(3+2)/(4*3-2)
```
Benchmark              Mode  Cnt     Score    Error  Units
ParserNGWars.exp4j     avgt   10   131.007 ± 19.236  ns/op
ParserNGWars.javaMep   avgt   10  1026.484 ± 16.357  ns/op
ParserNGWars.parserNg  avgt   10   145.447 ± 20.124  ns/op

```

### 4. (1+1)*(1+2)*(3+4)*(8+9)*(6-1)*(4^3.14159265357)-(3+2)^1.8
```
Benchmark              Mode  Cnt      Score       Error  Units
ParserNGWars.exp4j     avgt   10    318.317 ±    34.100  ns/op
ParserNGWars.javaMep   avgt   10  10814.830 ± 15245.221  ns/op
ParserNGWars.parserNg  avgt   10    275.664 ±    50.407  ns/op
```