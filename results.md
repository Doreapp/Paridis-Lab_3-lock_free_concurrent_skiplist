# Results 

Tests are executed on *Tegner*.

## Test with 2 populations 

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
