
class DataRequester {
    /**
     * @param {Buffer} buffer
     * @param {String} pluginExecutionId
     */
    constructor(pluginExecutionId) {
        this._buffer = undefined;
        this._hasDataRequestError = false;
        this._pluginExecutionId = pluginExecutionId;

        this._workerProxy = new Worker("/loganimation2/js/ap/dataRequestWorker.js");
        let self = this;
        this._workerProxy.onmessage = function(e) {
            let result = e.data;
            let requestToken = e.requestToken;
            if (result.success) {
                if (self._buffer) {
                    self._buffer.write(result.data, requestToken);
                }
            }

            //Testing
            console.log("Result: ");
            console.log("Response code: " + result.code);
            console.log("Response text: " + result.data);

            //this.doPointlessComputationsWithBlocking();
        }
    }

    getPluginExecutionId() {
        return this._pluginExecutionId;
    }

    /**
     *
     * @param {Number} frameIndex
     * @param {Buffer} buffer
     * @param {Number} requestToken
     */
    requestData(frameIndex, buffer, requestToken) {
        this._buffer = buffer;
        this._requestToken = requestToken;
        this._workerProxy.postMessage({startFrame: frameIndex, pluginExecutionId: this._pluginExecutionId, requestToken: requestToken});
    }

    calculatePrimes(iterations, multiplier) {
        var primes = [];
        for (var i = 0; i < iterations; i++) {
            var candidate = i * (multiplier * Math.random());
            var isPrime = true;
            for (var c = 2; c <= Math.sqrt(candidate); ++c) {
                if (candidate % c === 0) {
                    // not prime
                    isPrime = false;
                    break;
                }
            }
            if (isPrime) {
                primes.push(candidate);
            }
        }
        return primes;
    }

    doPointlessComputationsWithBlocking() {
        var primes = calculatePrimes(iterations, multiplier);
        console.log(primes);
    }
}
