# Results 

Tests are executed on *Tegner*.

## Tests with 2 populations 

### First population

**Description**

The first population is a random cluster of 10^7 integers, going from 0 to 10^7.
The distribution is supposed to be uniform.
The random is generated through `Math.random()`. 

**How to reproduce**

```
java Main first
```

**Results**

* Execution time: `36.9s`
* Mean: `5.0003*10^6`
    * Expected Mean: `5*10^6` 
* Variance: `8.335 * 10^12`
    * Expected Variance: `(1*10^7)^2 / 12 = 10^14/12 = 8.333*10^12`
* Comments: 
    The distribution seems good.

### Second population 

**Description**

The second population is a random cluster of 10^7 integers, going from 0 to 10^7, using a normal distribution.
To generate those random numbers following this distribution, we are using the *Box-muller transform* - described [here](https://www.baeldung.com/cs/uniform-to-normal-distribution) - using as an uniform distributed input the function `Math.random()`

**How to reproduce**

```
java Main second
```

**Results**

* Execution time: `46.3s`
* Mean: `5.0003*10^6`
    * Expected Mean: `5*10^6` 
* Variance: `1.0006 *10^12`
    This means a mean deviation of `1*10^6`, that seems ok
* Comments: 
    The distribution seems good. It takes a bit more time, but it may be related to the random generation (that requires more mathematics calculations). 

## Tests with Several threads

## Distribution 1
* 10% add
* 10% remove
* 80% contains

### First population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                       | 0.699s                  |
| 12                      | 0.378s                  |
| 30                      | 0.373s                  |
| 46                      | 0.358s                  |

### Second population 
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                       | 0.788s                  |
| 12                      | 0.388s                  |
| 30                      | 0.390s                  |
| 46                      | 0.365s                  |

## Distribution 2
* 50% add
* 50% remove

### First population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                       | 1.343s                  |
| 12                      | 0.496s                  |
| 30                      | 0.457s                  |
| 46                      | 0.433s                  |

### Second population 
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                       | 1.370s                  |
| 12                      | 0.503s                  |
| 30                      | 0.445s                  |
| 46                      | 0.458s                  |


## Distribution 3
* 25% add
* 25% remove
* 50% contains

### First population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                       | 1.118s                  |
| 12                      | 0.437s                  |
| 30                      | 0.396s                  |
| 46                      | 0.393s                  |

### Second population 
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                       | 1.145s                  |
| 12                      | 0.445s                  |
| 30                      | 0.415s                  |
| 46                      | 0.415s                  |


## Distribution 3
* 5% add
* 5% remove
* 90% contains

### First population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                       | 0.503s                  |
| 12                      | 0.437s                  |
| 30                      | 0.396s                  |
| 46                      | 0.393s                  |

### Second population 
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                       | 0.591s                  |
| 12                      | 0.445s                  |
| 30                      | 0.415s                  |
| 46                      | 0.415s                  |


==== Distribution 5 - 5 - 90 ====
== With 2 threads ==
 - With generator 0 -
Total duration: 5'036'459'216'
Average duration: 0'503'645'921'
 - With generator 1 -
Total duration: 5'917'725'676'
Average duration: 0'591'772'567'
== With 12 threads ==
 - With generator 0 -
Total duration: 3'637'959'950'
Average duration: 0'363'795'995'
 - With generator 1 -
Total duration: 3'577'822'974'
Average duration: 0'357'782'297'
== With 30 threads ==
 - With generator 0 -
Total duration: 3'710'297'817'
Average duration: 0'371'029'781'
 - With generator 1 -
Total duration: 3'669'058'735'
Average duration: 0'366'905'873'
== With 46 threads ==
 - With generator 0 -
Total duration: 3'351'703'096'
Average duration: 0'335'170'309'
 - With generator 1 -
Total duration: 3'419'753'811'
Average duration: 0'341'975'381'
