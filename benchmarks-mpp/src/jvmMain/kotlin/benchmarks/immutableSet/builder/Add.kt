/*
 * Copyright 2016-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package benchmarks.immutableSet.builder

import benchmarks.*
import kotlinx.collections.immutable.PersistentSet
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Thread)
open class Add {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    @Param(IP_100, IP_99_09, IP_95, IP_70, IP_50, IP_30, IP_0)
    var immutablePercentage: Double = 0.0

    private var elements = listOf<IntWrapper>()

    @Setup(Level.Trial)
    fun prepare() {
        elements = generateElements(hashCodeType, size)
    }

    /**
     * Adds [size] elements to an empty persistent set builder.
     *
     * Measures mean time and memory spent per `add` operation.
     *
     * Expected time: logarithmic
     * Expected memory: logarithmic
     */
    @Benchmark
    fun add(): PersistentSet.Builder<IntWrapper> {
        return persistentSetBuilderAdd(implementation, elements, immutablePercentage)
    }

    /**
     * Adds [size] elements to an empty persistent set builder and then requests if every element is contained.
     *
     * Measures mean time and memory spent per `add` and `contains` operations.
     *
     * Expected time: [add] + [Contains.contains]
     * Expected memory: [add] + [Contains.contains]
     */
    @Benchmark
    fun addAndContains(bh: Blackhole) {
        val builder = persistentSetBuilderAdd(implementation, elements, immutablePercentage)
        repeat(times = size) { index ->
            bh.consume(builder.contains(elements[index]))
        }
    }

    /**
     * Adds [size] elements to an empty persistent set builder and then iterates all elements.
     *
     * Measures mean time and memory spent per `add` and `next` operations.
     *
     * Expected time: [add] + [Iterate.iterate]
     * Expected memory: [add] + [Iterate.iterate]
     */
    @Benchmark
    fun addAndIterate(bh: Blackhole) {
        val set = persistentSetBuilderAdd(implementation, elements, immutablePercentage)
        for (element in set) {
            bh.consume(element)
        }
    }
}