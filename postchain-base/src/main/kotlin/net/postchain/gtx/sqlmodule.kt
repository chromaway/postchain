// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx

import net.postchain.base.BlockchainRid
import net.postchain.core.*
import net.postchain.gtv.*
import net.postchain.gtv.GtvFactory.gtv
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import org.apache.commons.dbutils.handlers.ScalarHandler
import java.io.File

fun decodeSQLTextArray(a: Any): Array<String> {
    val arr = a as java.sql.Array
    return (arr.array as Array<String>)
}

class SQLOpArg(val name: String,
               val type: GtvType,
               val isSigner: Boolean,
               val isNullable: Boolean)

class SQLOpDesc(val name: String, val query: String, val args: Array<SQLOpArg>)

fun makeSQLQueryDesc(opName: String, argNames: Array<String>, argTypes: Array<String>): SQLOpDesc {
    val fixedArgNames = if (argNames.size > argTypes.size) {
        // Queries returns a table. The column names of that table are also
        // included in argNames for some reason
        argNames.slice(0..argTypes.size - 1).toTypedArray()
    } else argNames
    if (fixedArgNames.size != fixedArgNames.size)
        throw UserMistake("Cannot define SQL op ${opName}: wrong parameter list")

    val args = convertArgs(fixedArgNames, argTypes, opName)

    val query = if (args.size == 0) {
        "SELECT * FROM ${opName} (?)"
    } else {
        "SELECT * FROM ${opName} (?, ${Array(args.size, { "?" }).joinToString()})"
    }
    return SQLOpDesc(opName, query, args.toTypedArray())
}

fun makeSQLOpDesc(opName: String, argNames: Array<String>, argTypes: Array<String>): SQLOpDesc {
    if (argNames.size != argTypes.size)
        throw UserMistake("Cannot define SQL op ${opName}: wrong parameter list")
    if (argTypes[0] != "gtx_ctx")
        throw UserMistake("Cannot define SQL op ${opName}: gtx_ctx must be the first parameter")

    val args = convertArgs(argNames, argTypes, opName)

    val query = if (args.size == 0) {
        "SELECT ${opName} (?::gtx_ctx)"
    } else {
        "SELECT ${opName} (?::gtx_ctx, ${Array(args.size, { "?" }).joinToString()})"
    }
    return SQLOpDesc(opName, query, args.toTypedArray())
}

private fun convertArgs(argNames: Array<String>, argTypes: Array<String>, opName: String): MutableList<SQLOpArg> {
    val args = mutableListOf<SQLOpArg>()
    for (i in 1 until argNames.size) {
        val gtxType = when (argTypes[i]) {
            "bigint" -> Pair(GtvType.INTEGER, false)
            "int" -> Pair(GtvType.INTEGER, false)
            "text" -> Pair(GtvType.STRING, false)
            "gtx_signer" -> Pair(GtvType.BYTEARRAY, true)
            "bytea" -> Pair(GtvType.BYTEARRAY, false)
            else -> throw UserMistake("Unsupported argument type ${argTypes[i]} in ${opName}")
        }
        // only signer is not nullable
        args.add(SQLOpArg(argNames[i], gtxType.first, gtxType.second, !gtxType.second))
    }
    return args
}

fun convertExtOpDataToPrimitives(opDesc: SQLOpDesc, opData: ExtOpData): MutableList<Any?> {
    if (opDesc.args.size != opData.args.size)
        throw GTXOpMistake("Wrong number of arguments", opData)

    val myArgs = mutableListOf<Any?>()
    for (i in 0 until opDesc.args.size) {
        val spec = opDesc.args[i]
        val arg = opData.args[i]
        if (arg.type != spec.type) {
            if (!(arg.isNull() && spec.isNullable)) {
                throw GTXOpMistake("Wrong argument type", opData, i)
            }
        }
        if (spec.isSigner) {
            val signer = arg.asByteArray()
            if (!opData.signers.any { it.contentEquals(signer) })
                throw GTXOpMistake("Signer is not present", opData, i)
        }

        myArgs.add(arg.asPrimitive())
    }
    return myArgs
}

class SQLGTXOperation(val opDesc: SQLOpDesc, opData: ExtOpData) :
        GTXOperation(opData) {
    lateinit var args: Array<Any?>

    override fun isSpecial(): Boolean {
        return opDesc.name.startsWith("__")
    }

    override fun isCorrect(): Boolean {
        val myArgs = convertExtOpDataToPrimitives(opDesc, data)
        args = myArgs.toTypedArray()
        return true
    }

    override fun apply(ctx: TxEContext): Boolean {
        val r = QueryRunner()
        return r.query(ctx.conn, opDesc.query, ScalarHandler<Boolean>(),
                "(${ctx.chainID}, ${ctx.txIID}, ${data.opIndex})",
                *args)
    }
}

class SQLGTXModule(private val moduleFiles: Array<String>) : GTXModule {
    lateinit var ops: Map<String, SQLOpDesc>
    lateinit var queries: Map<String, SQLOpDesc>

    override fun getOperations(): Set<String> {
        return ops.keys
    }

    override fun getQueries(): Set<String> {
        return queries.keys
    }

    override fun makeTransactor(opData: ExtOpData): Transactor {
        if (opData.opName in ops) {
            return SQLGTXOperation(ops[opData.opName]!!, opData)
        } else {
            throw UserMistake("Operation not found")
        }
    }

    override fun query(ctx: EContext, name: String, args: Gtv): Gtv {
        val opDesc = queries.get(name)

        if (opDesc == null) {
            throw UserMistake("Query of type ${name} is not available")
        }

        if (args !is GtvDictionary) {
            throw ProgrammerMistake("args is not a GtvDictionary")
        }

        val myArgs = mutableListOf<Any?>()
        opDesc.args.forEach { spec ->
            val arg = args.get(spec.name) ?: GtvNull

            if (arg.isNull() && !spec.isNullable) {
                throw UserMistake("Missing non-nullable argument ${spec.name}")
            }
            myArgs.add(arg.asPrimitive())
        }

        val primitiveArgs = (myArgs.toTypedArray())

        val r = QueryRunner()
        val qResult = r.query(ctx.conn, opDesc.query, MapListHandler(),
                ctx.chainID, *primitiveArgs)

        val list = mutableListOf<Gtv>()
        qResult.forEach {
            val obj = mutableMapOf<String, Gtv>()
            it.entries.forEach {
                // Integer, String, ByteArray accepted as column type
                val dbValue = it.value
                val gtv = when (dbValue) {
                    is Int, is Long, is Short, is Byte -> gtv((dbValue as Number).toLong())
                    is String -> gtv(dbValue)
                    is ByteArray -> gtv(dbValue)
                    null -> GtvNull
                    else -> throw ProgrammerMistake("Unsupported return type" +
                            " ${dbValue.javaClass.simpleName} of column ${it.key} " +
                            "from query ${name}")
                }
                obj.set(it.key, gtv)
            }
            list.add(GtvDictionary.build(obj))
        }
        return GtvArray(list.toTypedArray())
    }


    private fun getOperatorMap(oplist: MutableList<MutableMap<String, Any>>): Map<String, SQLOpDesc> {
        val opList = mutableListOf<Pair<String, SQLOpDesc>>()
        for (op in oplist) {
            val name = op["name"] as String
            val opDesc = makeSQLOpDesc(name,
                    decodeSQLTextArray(op["argnames"]!!),
                    decodeSQLTextArray(op["argtypes"]!!))
            opList.add(name to opDesc)
        }
        val result = mapOf(*opList.toTypedArray())
        return result
    }

    private fun getQueryMap(oplist: MutableList<MutableMap<String, Any>>): Map<String, SQLOpDesc> {
        val opList = mutableListOf<Pair<String, SQLOpDesc>>()
        for (op in oplist) {
            val name = op["name"] as String
            val opDesc = makeSQLQueryDesc(name,
                    decodeSQLTextArray(op["argnames"]!!),
                    decodeSQLTextArray(op["argtypes"]!!))
            opList.add(name to opDesc)
        }
        val result = mapOf(*opList.toTypedArray())
        return result
    }

    private fun populateOps(ctx: EContext) {
        val r = QueryRunner()
        val oplist = r.query(ctx.conn, "SELECT * FROM gtx_sqlm_get_functions()", MapListHandler())
        ops = getOperatorMap(oplist)
    }


    private fun populateQueries(ctx: EContext) {
        val r = QueryRunner()
        val oplist = r.query(ctx.conn, "SELECT * FROM gtx_sqlm_get_queries()", MapListHandler())
        queries = getQueryMap(oplist)

    }

    override fun initializeDB(ctx: EContext) {
        GTXSchemaManager.autoUpdateSQLSchema(ctx, 0, javaClass, "sqlgtx.sql")
        for (filename in moduleFiles) {
            val moduleName = "SQLM_$filename"
            if (GTXSchemaManager.getModuleVersion(ctx, moduleName) == null) {
                try {
                    val sql = readModuleFileContent(filename)
                    ctx.conn.createStatement().use {
                        it.execute(sql)
                    }
                    GTXSchemaManager.setModuleVersion(ctx, moduleName, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw UserMistake("Failed to load SQL GTX module ${filename}", e)
                }
            }
        }
        populateOps(ctx)
        populateQueries(ctx)
    }

    private fun readModuleFileContent(filename: String): String {
        /*
        * FYI: We use Spring convention here when files under resources are labeled with prefix 'classpath:'.
        * */
        val resourcePrefix = "classpath:"
        return if (filename.startsWith(resourcePrefix)) {
            javaClass.getResource(filename.substringAfter(resourcePrefix))
                    .readText()
        } else {
            File(filename).readText()
        }
    }
}

class SQLGTXModuleFactory : GTXModuleFactory {
    override fun makeModule(data: Gtv, blockchainRID: BlockchainRid): GTXModule {
        return SQLGTXModule(
                data["gtx"]!!["sqlmodules"]
                        ?.asArray()
                        ?.map(Gtv::asString)
                !!.toTypedArray())
    }
}