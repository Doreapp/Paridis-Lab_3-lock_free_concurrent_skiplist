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

We first build the two populations with 10^6 elements.

Then for each combination, we fill a new List with a population, and run the test.

### Distribution 1
* 10% add
* 10% remove
* 80% contains

#### First population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 182.87s                  |
| 12                      | 54.83s                  |
| 30                      | 46.08s                  |
| 46                      | 44.84s                  |

#### Second population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 221.79s                  |
| 12                      | 54.54s                  |
| 30                      | 47.05s                  |
| 46                      | 44.33s                  |

### Distribution 2
* 50% add
* 50% remove
* 0% contains

#### First population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 244.96s                  |
| 12                      | 56.04s                  |
| 30                      | 50.66s                  |
| 46                      | 49.38s                  |

#### Second population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 247.65s                  |
| 12                      | 59.29s                  |
| 30                      | 54.05s                  |
| 46                      | 53.0s                  |

### Distribution 3
* 25% add
* 25% remove
* 50% contains

#### First population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 221.6s                  |
| 12                      | 51.35s                  |
| 30                      | 46.74s                  |
| 46                      | 46.42s                  |

#### Second population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 215.34s                  |
| 12                      | 53.5s                  |
| 30                      | 48.4s                  |
| 46                      | 48.11s                  |

### Distribution 4
* 5% add
* 5% remove
* 90% contains

#### First population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 212.79s                  |
| 12                      | 50.13s                  |
| 30                      | 44.51s                  |
| 46                      | 44.21s                  |

#### Second population
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 218.96s                  |
| 12                      | 49.48s                  |
| 30                      | 45.04s                  |
| 46                      | 44.22s                  |

