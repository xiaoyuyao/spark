#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
Python package for statistical functions in MLlib.
"""

from pyspark.mllib.common import callMLlibFunc, JavaModelWrapper
from pyspark.mllib.linalg import _convert_to_vector


__all__ = ['MultivariateStatisticalSummary', 'Statistics']


class MultivariateStatisticalSummary(JavaModelWrapper):

    """
    Trait for multivariate statistical summary of a data matrix.
    """

    def mean(self):
        return self.call("mean").toArray()

    def variance(self):
        return self.call("variance").toArray()

    def count(self):
        return self.call("count")

    def numNonzeros(self):
        return self.call("numNonzeros").toArray()

    def max(self):
        return self.call("max").toArray()

    def min(self):
        return self.call("min").toArray()


class Statistics(object):

    @staticmethod
    def colStats(rdd):
        """
        Computes column-wise summary statistics for the input RDD[Vector].

        >>> from pyspark.mllib.linalg import Vectors
        >>> rdd = sc.parallelize([Vectors.dense([2, 0, 0, -2]),
        ...                       Vectors.dense([4, 5, 0,  3]),
        ...                       Vectors.dense([6, 7, 0,  8])])
        >>> cStats = Statistics.colStats(rdd)
        >>> cStats.mean()
        array([ 4.,  4.,  0.,  3.])
        >>> cStats.variance()
        array([  4.,  13.,   0.,  25.])
        >>> cStats.count()
        3L
        >>> cStats.numNonzeros()
        array([ 3.,  2.,  0.,  3.])
        >>> cStats.max()
        array([ 6.,  7.,  0.,  8.])
        >>> cStats.min()
        array([ 2.,  0.,  0., -2.])
        """
        cStats = callMLlibFunc("colStats", rdd.map(_convert_to_vector))
        return MultivariateStatisticalSummary(cStats)

    @staticmethod
    def corr(x, y=None, method=None):
        """
        Compute the correlation (matrix) for the input RDD(s) using the
        specified method.
        Methods currently supported: I{pearson (default), spearman}.

        If a single RDD of Vectors is passed in, a correlation matrix
        comparing the columns in the input RDD is returned. Use C{method=}
        to specify the method to be used for single RDD inout.
        If two RDDs of floats are passed in, a single float is returned.

        >>> x = sc.parallelize([1.0, 0.0, -2.0], 2)
        >>> y = sc.parallelize([4.0, 5.0, 3.0], 2)
        >>> zeros = sc.parallelize([0.0, 0.0, 0.0], 2)
        >>> abs(Statistics.corr(x, y) - 0.6546537) < 1e-7
        True
        >>> Statistics.corr(x, y) == Statistics.corr(x, y, "pearson")
        True
        >>> Statistics.corr(x, y, "spearman")
        0.5
        >>> from math import isnan
        >>> isnan(Statistics.corr(x, zeros))
        True
        >>> from pyspark.mllib.linalg import Vectors
        >>> rdd = sc.parallelize([Vectors.dense([1, 0, 0, -2]), Vectors.dense([4, 5, 0, 3]),
        ...                       Vectors.dense([6, 7, 0,  8]), Vectors.dense([9, 0, 0, 1])])
        >>> pearsonCorr = Statistics.corr(rdd)
        >>> print str(pearsonCorr).replace('nan', 'NaN')
        [[ 1.          0.05564149         NaN  0.40047142]
         [ 0.05564149  1.                 NaN  0.91359586]
         [        NaN         NaN  1.                 NaN]
         [ 0.40047142  0.91359586         NaN  1.        ]]
        >>> spearmanCorr = Statistics.corr(rdd, method="spearman")
        >>> print str(spearmanCorr).replace('nan', 'NaN')
        [[ 1.          0.10540926         NaN  0.4       ]
         [ 0.10540926  1.                 NaN  0.9486833 ]
         [        NaN         NaN  1.                 NaN]
         [ 0.4         0.9486833          NaN  1.        ]]
        >>> try:
        ...     Statistics.corr(rdd, "spearman")
        ...     print "Method name as second argument without 'method=' shouldn't be allowed."
        ... except TypeError:
        ...     pass
        """
        # Check inputs to determine whether a single value or a matrix is needed for output.
        # Since it's legal for users to use the method name as the second argument, we need to
        # check if y is used to specify the method name instead.
        if type(y) == str:
            raise TypeError("Use 'method=' to specify method name.")

        if not y:
            return callMLlibFunc("corr", x.map(_convert_to_vector), method).toArray()
        else:
            return callMLlibFunc("corr", x.map(float), y.map(float), method)


def _test():
    import doctest
    from pyspark import SparkContext
    globs = globals().copy()
    globs['sc'] = SparkContext('local[4]', 'PythonTest', batchSize=2)
    (failure_count, test_count) = doctest.testmod(globs=globs, optionflags=doctest.ELLIPSIS)
    globs['sc'].stop()
    if failure_count:
        exit(-1)


if __name__ == "__main__":
    _test()
