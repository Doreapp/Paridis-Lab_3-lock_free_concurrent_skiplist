# Results 

Tests are executed on *Tegner*.

**We are using a P of 0.75, and not 0.50 as described into the slide.**
It seems faster, and as we are consistent between the different tests we estimated that it won't change the overall observation.

*Note*: The P is the probability to have 0 a `topLevel`

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


## Linearization points

I created an `Operation` class containing 
* a name describing the operation (like "add failed")
* the value related (integer) 
* the nano time at which it happened

Finally, after sorting the operations made by time, we optained an ordered listing of the operations.

Here is one of the outputs:
```
        at 921496718786200      add     true    (0)     [0,]
        at 921496718793800      remove  false   (3)
        at 921496718803500      cont.   false   (4)
        at 921496718805200      cont.   false   (2)
        at 921496719064700      remove  false   (8)
        at 921496719163500      remove  false   (10)
        at 921496719168700      add     true    (8)     [0,8,]
        at 921496719178600      cont.   false   (16)
        at 921496719184300      add     true    (17)    [0,8,17,]
        at 921496719207200      add     true    (15)    [0,8,17,15,]
        at 921496719216300      remove  false   (3)
        at 921496719225900      add     false   (0)
        at 921496719254700      add     true    (11)    [0,8,17,15,11,]
        at 921496719464300      add     true    (4)     [0,8,17,15,11,4,]
        at 921496719482800      remove  false   (16)
        at 921496719622700      add     false   (4)
        at 921496719635800      remove  false   (2)
        at 921496719639900      add     false   (11)
        at 921496719653800      remove  false   (2)
        at 921496719661600      remove  false   (2)
        at 921496719661800      add     true    (1)     [0,8,17,15,11,4,1,]
        at 921496719679700      add     true    (18)    [0,8,17,15,11,4,1,18,]
        at 921496719681100      cont.   false   (7)
        at 921496719691900      remove  false   (10)
        at 921496719700100      cont.   false   (7)
        at 921496719716500      add     false   (1)
        at 921496719726100      cont.   true    (15)
        at 921496719728200      add     true    (13)    [0,8,17,15,11,4,1,18,13,]
        at 921496719730500      remove  true    (15)    [0,8,17,11,4,1,18,13,]
        at 921496719732900      cont.   true    (1)
        at 921496719746300      cont.   false   (7)
        at 921496719746300      cont.   false   (19)
        at 921496719754800      remove  false   (9)
        at 921496719757000      remove  false   (10)
        at 921496719761400      cont.   true    (8)
        at 921496719764700      add     false   (11)
        at 921496719773100      cont.   false   (2)
        at 921496719775700      cont.   true    (13)
        at 921496719780900      add     false   (1)
        at 921496719783200      cont.   false   (12)
        at 921496719785200      add     true    (2)     [0,8,17,11,4,1,18,13,2,]
        at 921496719787900      cont.   false   (10)
        at 921496719798700      add     false   (8)
        at 921496719801700      add     true    (15)    [0,8,17,11,4,1,18,13,2,15,]
        at 921496719836800      cont.   false   (10)
        at 921496719857900      add     false   (8)
        at 921496719858300      cont.   true    (13)
        at 921496719868200      remove  false   (7)
        at 921496719869700      cont.   false   (12)
        at 921496719878100      cont.   false   (12)
```

You can follow the list content at the right of the lines.

Note that this execution is fine (linearized without obvious errors), but some others that we tried add errors, mostly on `contains` returning false while the element was in the list. 
I may come from the fact that `nanoTime` is not atomic with the linearization point itself.
