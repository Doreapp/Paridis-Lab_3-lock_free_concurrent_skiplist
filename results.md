# Results 

Tests are executed on *Tegner*.

**We are using a P of 0.75, and not 0.50 as described into the slide.**
It seems faster, and as we are consistent between the different tests, we estimated that it won't change the overall observations.

*Note*: The P is the probability to have 0 as `topLevel`

## Tests with 2 populations 

We are generating two different population randomly, with different distribution.
To calculate values about the distribution, we are storing the generated values in a `LinkedList` as well as in the `SkipListSet` class.

See `populationTest` method in `Main.java` for more details

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

* Execution time : `36.9s`
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

Then for each combination, we fill a new list with one of the population, and run the test.

For each population, the random generator used for the additionnal operations are related. It means that for the population created from the normal-random generator, the threads will use normal-random generator for the operations

**How to reproduce**

```
java Main threads
```

### Distribution 1
10% add, 10% remove, 80% contains

*First population*:
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 182.87s                  |
| 12                      | 54.83s                  |
| 30                      | 46.08s                  |
| 46                      | 44.84s                  |

*Second population*:
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 221.79s                  |
| 12                      | 54.54s                  |
| 30                      | 47.05s                  |
| 46                      | 44.33s                  |

### Distribution 2
50% add, 50% remove, 0% contains

*First population*:
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 244.96s                  |
| 12                      | 56.04s                  |
| 30                      | 50.66s                  |
| 46                      | 49.38s                  |

*Second population*: 
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 247.65s                  |
| 12                      | 59.29s                  |
| 30                      | 54.05s                  |
| 46                      | 53.0s                  |

### Distribution 3
25% add, 25% remove, 50% contains

*First population*: 
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 221.6s                  |
| 12                      | 51.35s                  |
| 30                      | 46.74s                  |
| 46                      | 46.42s                  |

*Second population*: 
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 215.34s                  |
| 12                      | 53.5s                  |
| 30                      | 48.4s                  |
| 46                      | 48.11s                  |

### Distribution 4
5% add, 5% remove, 90% contains

*First population*: 
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 212.79s                  |
| 12                      | 50.13s                  |
| 30                      | 44.51s                  |
| 46                      | 44.21s                  |

*Second population*: 
| Number of threads       | Average time            |
|-------------------------|-------------------------|
| 2                      | 218.96s                  |
| 12                      | 49.48s                  |
| 30                      | 45.04s                  |
| 46                      | 44.22s                  |

### Conclusions

The second population requires more time for `add` and `remove` operations than the first population. However, `contains` operation seems to handle both populations the same way.

`contains` operation is the quickest of the 3 operations.

The more threads we use, the less the improvement of adding new threads is worth it: passing from 2 to 12 threads (+10) divide the duration by ~4 each time (~400%), but passing from 30 to 46 (+16) doesn't improve that much the execution time (~2%).


## Linearization points

We created an `Operation` class containing 
* a name describing the operation ("add","remove" or "cont.")
* the result value of the operation (true or false)
* the value related (integer) 
* the nano time at which it happened

Rather than just printing the time, we built a way to store the operations with their related time. Doing that we built our own method to check the linearizability of the SkipListSet. Indeed, for the [First method](#first-method), we are storing the operations in data structure owned by the skiplist itself.

### First method

First, we store each operation in a `ConcurrentLinkedQueue`. This class offered by java provide a wait-free `add`, that can be called by several threads.

Then, after every operations are finished, we sort the operation list by time. 

Finally, we follow operation by operation the result of a linear execution and the compare with the results of the operations. 
To do that, we are using a `Set` and reproducing the operations, to check that the result is correct. 

In order to run parallel operations, we are using `population.parallelStream().forEach()`, with a pre-built population, so that the test are using the exact same population. We are using 33% of each operation (`add`, `contains` and `remove`)

For a small set of operation, we can output traces like:

```
        at 7079866528218087     cont.   false   (0)
        at 7079866528274496     remove  false   (19)
        at 7079866528297523     cont.   false   (3)
        at 7079866528360520     cont.   false   (2)
        at 7079866528383544     cont.   false   (12)
        at 7079866528421596     add     true    (15)    [15,]
        at 7079866528435495     cont.   false   (9)
        at 7079866528442838     cont.   false   (19)
        at 7079866528454039     remove  false   (16)
        at 7079866528456420     cont.   false   (17)
        at 7079866528466422     remove  false   (10)
        at 7079866528467843     remove  false   (10)
        at 7079866528491485     add     true    (3)     [15,3,]
        at 7079866528492173     add     false   (15)
        at 7079866528493635     cont.   false   (12)
        at 7079866528517929     cont.   false   (7)
        at 7079866528518695     remove  true    (3)     [15,]
        at 7079866528529483     cont.   false   (5)
        at 7079866528537843     remove  false   (8)
        at 7079866528539729     remove  false   (18)
        at 7079866528557753     remove  false   (12)
        at 7079866528562800     add     true    (2)     [15,2,]
        at 7079866528576547     remove  false   (13)
        at 7079866528584783     remove  false   (3)
        at 7079866528589282     add     true    (7)     [15,2,7,]
        at 7079866528594477     cont.   false   (3)
        at 7079866528605741     cont.   false   (4)
        at 7079866528607693     add     true    (11)    [15,2,7,11,]
        at 7079866528616567     add     true    (18)    [15,2,7,11,18,]
        at 7079866528622215     remove  false   (1)
        at 7079866528642155     cont.   false   (5)
        at 7079866528643415     add     true    (5)     [15,2,7,11,18,5,]
        at 7079866528644465     cont.   true    (18)
        at 7079866528647791     remove  false   (16)
        at 7079866528667057     add     false   (18)
        at 7079866528672269     add     true    (16)    [15,2,7,11,18,5,16,]
        at 7079866529409476     add     true    (8)     [15,2,7,11,18,5,16,8,]
        at 7079866529409562     add     true    (17)    [15,2,7,11,18,5,16,8,17,]
        at 7079866530019525     cont.   false   (13)
        at 7079866530031476     remove  true    (15)    [2,7,11,18,5,16,8,17,]
        at 7079866530031646     remove  true    (17)    [2,7,11,18,5,16,8,]
        at 7079866530111658     cont.   false   (6)
        at 7079866530125870     add     true    (9)     [2,7,11,18,5,16,8,9,]
        at 7079866530214517     cont.   false   (15)
        at 7079866530218159     remove  false   (10)
        at 7079866530221502     add     false   (16)
        at 7079866530223904     cont.   false   (1)
        at 7079866530279016     cont.   true    (11)
        at 7079866530279260     cont.   false   (15)
        at 7079866531252507     remove  false   (1)
```

This execution is fine (linearized without obvious errors), but some others executions had errors, mostly on `contains` returning false while the element was in the list. 

I may come from the fact that `nanoTime` is not atomic with the linearization point itself.

**How to reproduce**

```
java Main linear
```

### Second method, using a lock

We added a global lock, so that the measurement of the execution time and the linearization point are atomic. 

After tracing with some small and large amount of operations (from 50 to 100'000), we haven't encounter errors in the linearization.

**How to reproduce**

The small amount of operations (50) :
```
java Main linear-lock 
```

The large amount of operations (100000) :
```
java Main large-linear-lock 
```

### Third method, one counter by thread

The idea here is to add a counter owned by each thread, so that we do not need to use a concurrent data structure.

We tested this method with different number of operations, different distributions and different number of thread. The results are bellow

**Running with 10 operations**

*Distribution 1*: 10% add, 10% remove, 80% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 2*: 50% add, 50% remove, 0% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 3*: 25% add, 25% remove, 50% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 4*: 5% add, 5% remove, 90% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

**Running with 100 operations**

*Distribution 1*: 10% add, 10% remove, 80% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 2*: 50% add, 50% remove, 0% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 3*: 25% add, 25% remove, 50% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 4*: 5% add, 5% remove, 90% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

**Running with 1000 operations**

*Distribution 1*: 10% add, 10% remove, 80% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 2*: 50% add, 50% remove, 0% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 3*: 25% add, 25% remove, 50% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 4*: 5% add, 5% remove, 90% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

**Running with 10000 operations**

*Distribution 1*: 10% add, 10% remove, 80% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 2*: 50% add, 50% remove, 0% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 3*: 25% add, 25% remove, 50% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 4*: 5% add, 5% remove, 90% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

**Running with 100000 operations**

*Distribution 1*: 10% add, 10% remove, 80% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 2*: 50% add, 50% remove, 0% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 3*: 25% add, 25% remove, 50% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 4*: 5% add, 5% remove, 90% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

**Running with 1000000 operations**

*Distribution 1*: 10% add, 10% remove, 80% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 2*: 50% add, 50% remove, 0% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 3*: 25% add, 25% remove, 50% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

*Distribution 4*: 5% add, 5% remove, 90% contains

| Number of threads       | Linearisation test      |
|-------------------------|-------------------------|
| 2                      | PASS              |
| 12                      | PASS              |
| 30                      | PASS              |
| 46                      | PASS              |

**Conclusions**

Every tests passed. By experience, sometime tests failed (on my own computer), once again probably because of the measurment not being atomic.

### Conclusions

After those three tests, the skip list seems linearizable, even if we cannot be 100% sure about it. 