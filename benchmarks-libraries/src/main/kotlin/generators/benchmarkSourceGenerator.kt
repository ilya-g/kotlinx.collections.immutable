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

package generators

import generators.immutableList.*
import generators.immutableList.impl.*
import generators.immutableListBuilder.*
import generators.immutableListBuilder.impl.*
import generators.immutableMap.*
import generators.immutableMap.impl.*
import generators.immutableMapBuilder.*
import generators.immutableMapBuilder.impl.*
import generators.immutableSet.*
import generators.immutableSet.impl.*
import generators.immutableSetBuilder.*
import generators.immutableSetBuilder.impl.*
import org.xml.sax.InputSource
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.xpath.XPathFactory

abstract class SourceGenerator {
    abstract val outputFileName: String
    open fun getPackage(): String = "benchmarks"

    fun generate(out: PrintWriter) {
        out.println(readCopyrightNoticeFromProfile(File(".idea/copyright/apache_2_0.xml")))
        // Don't include generator class name in the message: these are built-in sources,
        // and we don't want to scare users with any internal information about our project
        out.println("// Auto-generated file. DO NOT EDIT!")
        out.println()
        out.println("package ${getPackage()}")
        out.println()
        imports.forEach {
            out.println("import $it")
        }
        out.println()

        generateBody(out)
    }

    protected abstract val imports: Set<String>
    protected abstract fun generateBody(out: PrintWriter): Unit
}


abstract class BenchmarkSourceGenerator: SourceGenerator() {
    override val imports: Set<String> = setOf(
            "org.openjdk.jmh.annotations.*",
            "java.util.concurrent.TimeUnit"
    )

    override fun generateBody(out: PrintWriter) {
        out.println("""
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
        """.trimIndent()
        )
        generateBenchmark(out)
    }

    protected abstract fun generateBenchmark(out: PrintWriter)
}

abstract class UtilsSourceGenerator: SourceGenerator() {
    override val imports: Set<String> = setOf()
}

fun readCopyrightNoticeFromProfile(copyrightProfile: File): String {
    val template = copyrightProfile.reader().use { reader ->
        XPathFactory.newInstance().newXPath().evaluate("/component/copyright/option[@name='notice']/@value", InputSource(reader))
    }
    val yearTemplate = "&#36;today.year"
    val year = java.time.LocalDate.now().year.toString()
    assert(yearTemplate in template)

    return template.replace(yearTemplate, year).lines().joinToString("", prefix = "/*\n", postfix = " */\n") { " * $it\n" }
}

private const val BENCHMARKS_ROOT = "src/jmh/java/"


private val listImpls = listOf(
        KotlinListImplementation,
        PaguroListImplementation,
        CyclopsListImplementation,
        ClojureListImplementation,
        ScalaListImplementation,
        VavrListImplementation
)
private val listBuilderImpls = listOf(
        KotlinListBuilderImplementation,
        PaguroListBuilderImplementation,
        ClojureListBuilderImplementation
)

private val mapImpls = listOf(
        KotlinMapImplementation,
        KotlinOrderedMapImplementation,
        CapsuleMapImplementation,
        PaguroMapImplementation,
        PaguroSortedMapImplementation,
        CyclopsMapImplementation,
        CyclopsOrderedMapImplementation,
        CyclopsTrieMapImplementation,
        ClojureMapImplementation,
        ClojureSortedMapImplementation,
        ScalaMapImplementation,
        ScalaSortedMapImplementation,
        VavrMapImplementation,
        VavrSortedMapImplementation,
        VavrOrderedMapImplementation
)
private val mapBuilderImpls = listOf(
        MapBuilderKotlinBenchmark(),
        MapBuilderKotlinOrderedBenchmark(),
        MapBuilderCapsuleBenchmark(),
        MapBuilderPaguroBenchmark(),
        MapBuilderClojureBenchmark()
)

private val setImpls = listOf(
        SetKotlinBenchmark(),
        SetKotlinOrderedBenchmark(),
        SetCapsuleBenchmark(),
        SetCyclopsBenchmark(),
        SetCyclopsTrieBenchmark(),
        SetCyclopsSortedBenchmark(),
        SetClojureBenchmark(),
        SetClojureSortedBenchmark(),
        SetScalaBenchmark(),
        SetScalaSortedBenchmark(),
        SetVavrBenchmark(),
        SetVavrSortedBenchmark(),
        SetVavrOrderedBenchmark()
)
private val setBuilderImpls = listOf(
        SetBuilderKotlinBenchmark(),
        SetBuilderKotlinOrderedBenchmark(),
        SetBuilderCapsuleBenchmark(),
        SetBuilderClojureBenchmark()
)

fun generateBenchmarks() {
    val listBenchmarks = listImpls.map { listOf(
            ListAddBenchmarkGenerator(it),
            ListGetBenchmarkGenerator(it),
            ListIterateBenchmarkGenerator(it),
            ListRemoveBenchmarkGenerator(it),
            ListSetBenchmarkGenerator(it),
            ListUtilsGenerator(it)
    ) }
    /*listOf(
            listImpls.filterIsInstance<ListAddBenchmark>().map { ListAddBenchmarkGenerator(it) },
            listImpls.filterIsInstance<ListGetBenchmark>().map { ListGetBenchmarkGenerator(it) },
            listImpls.filterIsInstance<ListIterateBenchmark>().map { ListIterateBenchmarkGenerator(it) },
            listImpls.filterIsInstance<ListRemoveBenchmark>().map { ListRemoveBenchmarkGenerator(it) },
            listImpls.filterIsInstance<ListSetBenchmark>().map { ListSetBenchmarkGenerator(it) }
    )*/
    val listBuilderBenchmarks = listBuilderImpls.map { listOf(
            ListBuilderAddBenchmarkGenerator(it),
            ListBuilderGetBenchmarkGenerator(it),
            ListBuilderRemoveBenchmarkGenerator(it),
            ListBuilderSetBenchmarkGenerator(it),
            ListBuilderUtilsGenerator(it)
    ) } + listBuilderImpls.filter(ListBuilderImplementation::isIterable).map { listOf(
            ListBuilderIterateBenchmarkGenerator(it)
    ) }
    /*listOf(
            listBuilderImpls.filterIsInstance<ListBuilderAddBenchmark>().map { ListBuilderAddBenchmarkGenerator(it) },
            listBuilderImpls.filterIsInstance<ListBuilderGetBenchmark>().map { ListBuilderGetBenchmarkGenerator(it) },
            listBuilderImpls.filterIsInstance<ListBuilderIterateBenchmark>().map { ListBuilderIterateBenchmarkGenerator(it) },
            listBuilderImpls.filterIsInstance<ListBuilderRemoveBenchmark>().map { ListBuilderRemoveBenchmarkGenerator(it) },
            listBuilderImpls.filterIsInstance<ListBuilderSetBenchmark>().map { ListBuilderSetBenchmarkGenerator(it) }
    )*/

    val mapBenchmarks = mapImpls.map { listOf(
            MapGetBenchmarkGenerator(it),
            MapIterateBenchmarkGenerator(it),
            MapPutBenchmarkGenerator(it),
            MapRemoveBenchmarkGenerator(it),
            MapUtilsGenerator(it)
    ) }
    /*listOf(
            mapImpls.filterIsInstance<MapGetBenchmark>().map { MapGetBenchmarkGenerator(it) },
            mapImpls.filterIsInstance<MapIterateBenchmark>().map { MapIterateBenchmarkGenerator(it) },
            mapImpls.filterIsInstance<MapPutBenchmark>().map { MapPutBenchmarkGenerator(it) },
            mapImpls.filterIsInstance<MapRemoveBenchmark>().map { MapRemoveBenchmarkGenerator(it) }
    )*/
    val mapBuilderBenchmarks = listOf(
            mapBuilderImpls.filterIsInstance<MapBuilderGetBenchmark>().map { MapBuilderGetBenchmarkGenerator(it) },
            mapBuilderImpls.filterIsInstance<MapBuilderIterateBenchmark>().map { MapBuilderIterateBenchmarkGenerator(it) },
            mapBuilderImpls.filterIsInstance<MapBuilderPutBenchmark>().map { MapBuilderPutBenchmarkGenerator(it) },
            mapBuilderImpls.filterIsInstance<MapBuilderRemoveBenchmark>().map { MapBuilderRemoveBenchmarkGenerator(it) }
    )

    val setBenchmarks = listOf(
            setImpls.filterIsInstance<SetAddBenchmark>().map { SetAddBenchmarkGenerator(it) },
            setImpls.filterIsInstance<SetContainsBenchmark>().map { SetContainsBenchmarkGenerator(it) },
            setImpls.filterIsInstance<SetIterateBenchmark>().map { SetIterateBenchmarkGenerator(it) },
            setImpls.filterIsInstance<SetRemoveBenchmark>().map { SetRemoveBenchmarkGenerator(it) }
    )
    val setBuilderBenchmarks = listOf(
            setBuilderImpls.filterIsInstance<SetBuilderAddBenchmark>().map { SetBuilderAddBenchmarkGenerator(it) },
            setBuilderImpls.filterIsInstance<SetBuilderContainsBenchmark>().map { SetBuilderContainsBenchmarkGenerator(it) },
            setBuilderImpls.filterIsInstance<SetBuilderIterateBenchmark>().map { SetBuilderIterateBenchmarkGenerator(it) },
            setBuilderImpls.filterIsInstance<SetBuilderRemoveBenchmark>().map { SetBuilderRemoveBenchmarkGenerator(it) }
    )


    val allBenchmarks = (listBenchmarks + listBuilderBenchmarks + mapBenchmarks + mapBuilderBenchmarks + setBenchmarks + setBuilderBenchmarks).flatten()

    allBenchmarks.forEach { benchmark ->
        val path = benchmark.getPackage().replace('.', '/') + "/" + benchmark.outputFileName + ".kt"
        val file = File(BENCHMARKS_ROOT + path)
        file.parentFile?.mkdirs()
        val out = PrintWriter(file)
        benchmark.generate(out)
        out.flush()
    }
}

fun generateUtils() {
    val mapBuilderUtils = mapBuilderImpls.filterIsInstance<MapBuilderBenchmarkUtils>().map { MapBuilderUtilsGenerator(it) }

    val setUtils = setImpls.filterIsInstance<SetBenchmarkUtils>().map { SetUtilsGenerator(it) }
    val setBuilderUtils = setBuilderImpls.filterIsInstance<SetBuilderBenchmarkUtils>().map { SetBuilderUtilsGenerator(it) }

    val commonUtils = listOf(
            IntWrapperGenerator(),
            CommonUtilsGenerator()
    )

    val utils = mapBuilderUtils + setUtils + setBuilderUtils + commonUtils

    utils.forEach { util ->
        val path = util.getPackage().replace('.', '/') + "/" + util.outputFileName + ".kt"
        val file = File(BENCHMARKS_ROOT + path)
        file.parentFile?.mkdirs()
        val out = PrintWriter(file)
        util.generate(out)
        out.flush()
    }
}

fun main() {
    Files.walk(Paths.get(BENCHMARKS_ROOT))
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach { it.delete() }

    generateUtils()
    generateBenchmarks()
}