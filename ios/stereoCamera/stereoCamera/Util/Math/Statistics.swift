//
//  Statistics.swift
//  stereoCamera
//
//  Created by hallmarklabs on 8/23/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class Statistics
{
    static func mean(_ values:[Int]) -> Double
    {
        var sum = 0
        for value in values
        {
            sum += value
        }
        
        let ret = Double(sum) / Double(values.count)
        return ret
    }
    
    static func standardDeviation(_ values:[Int]) -> Double
    {
        let avg = mean(values)
        let count = Double(values.count)
        var sum:Double = 0
        
        for value in values
        {
            let diff = Double(value) - avg
            sum += diff * diff
        }
        
        let ret = sqrt(sum / (count - 1.0))
        return ret
    }
    
    struct LeastSquaresResult
    {
        let slope:Double
        let intercept:Double
    }
    
    static func leastSquares(x:[Int], y:[Int]) -> LeastSquaresResult
    {
        var xd = [Double]()
        var yd = [Double]()
        let sz = x.count
        for i in 0 ... (sz - 1)
        {
            xd.append(Double(x[i]))
            yd.append(Double(y[i]))
        }
        
        let obj = LinearRegression(x: xd, y: yd)
        return LeastSquaresResult(slope: obj.slope, intercept: obj.intercept)
    }
    
    class LinearRegression
    {
        public var intercept:Double = 0, slope:Double = 0
        public var r2:Double = 0
        public var svar0:Double = 0, svar1:Double = 0

        /**
         * Performs a linear regression on the data points {@code (y[i], x[i])}.
         *
         * @param  x the values of the predictor variable
         * @param  y the corresponding values of the response variable
         * @throws IllegalArgumentException if the lengths of the two arrays are not equal
         */
        init(x:[Double], y:[Double])
        {
            if (x.count != y.count)
            {
                return
            }
        
            let n:Int = x.count

            // first pass
            var sumx:Double = 0, sumy:Double = 0, sumx2:Double = 0.0
            for i in 0 ... (n - 1)
            {
                sumx  += x[i]
                sumx2 += x[i]*x[i]
                sumy  += y[i]
            }

            let xbar:Double = sumx / Double(n)
            let ybar:Double = sumy / Double(n)

            // second pass: compute summary statistics
            var xxbar:Double = 0, yybar:Double = 0, xybar:Double = 0.0;
            for i in 0 ... (n - 1)
            {
                xxbar += (x[i] - xbar) * (x[i] - xbar)
                yybar += (y[i] - ybar) * (y[i] - ybar)
                xybar += (x[i] - xbar) * (y[i] - ybar)
            }
            slope  = xybar / xxbar
            intercept = ybar - slope * xbar

            // more statistical analysis
            var rss:Double = 0.0;      // residual sum of squares
            var ssr:Double = 0.0;      // regression sum of squares
            for i in 0 ... (n - 1)
            {
                let fit:Double = slope*x[i] + intercept
                rss += (fit - y[i]) * (fit - y[i])
                ssr += (fit - ybar) * (fit - ybar)
            }

            let degreesOfFreedom:Int = n-2;
            r2    = ssr / yybar;
            let svar:Double = rss / Double(degreesOfFreedom);
            svar1 = svar / xxbar;
            svar0 = svar / Double(n) + xbar*xbar*svar1;
        }

        public func interceptStdErr() -> Double
        {
            return sqrt(svar0);
        }
        
        public func slopeStdErr() -> Double
        {
            return sqrt(svar1);
        }

        public func predict(x:Double) -> Double
        {
            return slope*x + intercept;
        }

        public func toString() -> String
        {
            var ret:String = ""
            ret += NSString(format: "%.2f n + %.2f", slope, intercept) as String
            ret += NSString(format: "  (R^2 = %.3f)", r2) as String
            return ret
        }
    }
}

