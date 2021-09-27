# Results 

Tests are executed on *Tegner*.

## Test with 2 populations 

### First population

**Description**

The first population is a totally random cluster of 10^7 integers, going from 0 to 10^7.
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
