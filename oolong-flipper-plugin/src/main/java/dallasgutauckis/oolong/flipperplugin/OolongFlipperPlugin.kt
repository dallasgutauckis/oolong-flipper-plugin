package dallasgutauckis.oolong.flipperplugin

import android.util.Log
import com.facebook.flipper.core.FlipperConnection
import com.facebook.flipper.core.FlipperObject
import com.facebook.flipper.core.FlipperPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import oolong.Dispatch
import oolong.Effect
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates.observable

object OolongFlipperPlugin : FlipperPlugin {

    @Suppress("MemberVisibilityCanBePrivate")
    val SERIALIZER_DEFAULT: (Any) -> String = { it.toString() }

    private var runtimeFunctionCalls by observable(listOf<RuntimeFunctionCall>()) { _, _, _ ->
        Unit
    }

    var serializer: (Any) -> String = SERIALIZER_DEFAULT

    override fun getId(): String = "Oolong"

    override fun onConnect(connection: FlipperConnection) {
        var index = 0
        runtimeFunctionCalls.map {
            val type = when (it.functionCall) {
                is FunctionCall.Init<*> -> "init"
                is FunctionCall.Update<*, *> -> "update"
                is FunctionCall.View<*, *> -> "view"
                is FunctionCall.Render<*, *> -> "render"
            }

            val data = when (val functionCall = it.functionCall) {
                is FunctionCall.Init<*> -> flipperObject(mapOf("model" to serializer(functionCall.model)))
                is FunctionCall.Update<*, *> -> flipperObject(
                    mapOf(
                        "msg" to serializer(functionCall.msg),
                        "model" to serializer(functionCall.model),
                        "new_model" to serializer(functionCall.newModel)
                    )
                )
                is FunctionCall.View<*, *> -> flipperObject(
                    mapOf(
                        "model" to serializer(functionCall.model),
                        "props" to serializer(functionCall.newProps)
                    )
                )
                is FunctionCall.Render<*, *> -> flipperObject(
                    mapOf("props" to serializer(functionCall.props), "out" to functionCall.out?.let { serializer(it) })
                )
            }

            FlipperObject.Builder()
                .put("id", index++)
                .put("type", type)
                .put("data", data)
                .build()
        }.forEach {
            connection.send("newRow", it)
        }
    }

    override fun onDisconnect() {
    }

    override fun runInBackground(): Boolean = false

    fun <Model : Any, Msg : Any, Props : Any> runtime(
        init: () -> Pair<Model, Effect<Msg>>,
        update: (Msg, Model) -> Pair<Model, Effect<Msg>>,
        view: (Model, Dispatch<Msg>) -> Props,
        render: (Props) -> Any?,
        runtimeContext: CoroutineContext = Dispatchers.Default,
        renderContext: CoroutineContext = Dispatchers.Main,
        effectContext: CoroutineContext = Dispatchers.Default,
        // TODO see how right this is
        named: String = RuntimeException().stackTrace.first().className,
        actualRuntime: Runtime<Model, Msg, Props> = Runtime { init, update, view, render, runtimeContext, renderContext, effectContext ->
            oolong.runtime(
                init,
                update,
                view,
                render,
                runtimeContext,
                renderContext,
                effectContext
            )
        },
    ): Job {
        val runtimeDebuggingContext = runtimeCalled<Model, Msg, Props>(named)

        return actualRuntime(
            init = {
                init().also {
                    runtimeDebuggingContext.initCalled(runtimeDebuggingContext, it)
                }
            },
            update = { msg, model ->
                update(msg, model).also {
                    runtimeDebuggingContext.updateCalled(
                        runtimeDebuggingContext,
                        msg,
                        model,
                        it
                    )
                }
            },
            view = { model, dispatch ->
                view(
                    model,
                    dispatch
                ).also {
                    runtimeDebuggingContext.viewCalled(runtimeDebuggingContext, model, dispatch, it)
                }
            },
            render = { props ->
                render(props).also {
                    runtimeDebuggingContext.renderCalled(
                        runtimeDebuggingContext,
                        props,
                        it
                    )
                }
            },
            runtimeContext = runtimeContext,
            renderContext = renderContext,
            effectContext = effectContext
        )
    }

    private fun <Model : Any, Msg : Any, Props : Any> runtimeCalled(named: String): RuntimeDebuggingContext<Model, Msg, Props> {
        val plugin = this
        return RuntimeDebuggingContext(
            named = named,
            initCalled = { (newModel, _) -> plugin.initCalled(this, newModel) },
            updateCalled = { msg, model, (newModel, _) ->
                plugin.updateCalled(
                    this,
                    msg,
                    model,
                    newModel
                )
            },
            viewCalled = { model, _, out -> plugin.viewCalled(this, model, out) },
            renderCalled = { props, out -> plugin.renderCalled(this, props, out) }
        )
    }

    private fun <Model : Any> initCalled(
        runtimeDebuggingContext: RuntimeDebuggingContext<*, *, *>,
        newModel: Model
    ) {
        runtimeFunctionCalls = runtimeFunctionCalls.plus(
            RuntimeFunctionCall(
                runtimeDebuggingContext,
                FunctionCall.Init(newModel)
            )
        )
        Log.v("DALLAS", "initCalled! newModel: $newModel")
    }

    private fun <Model : Any, Msg : Any> updateCalled(
        runtimeDebuggingContext: RuntimeDebuggingContext<*, *, *>,
        msg: Msg,
        model: Model,
        newModel: Model
    ) {
        runtimeFunctionCalls = runtimeFunctionCalls.plus(
            RuntimeFunctionCall(
                runtimeDebuggingContext,
                FunctionCall.Update(msg, model, newModel)
            )
        )
        Log.v("DALLAS", "updateCalled: msg: $msg, model: $model; newModel: $newModel")
    }

    private fun <Model : Any, Props : Any> viewCalled(
        runtimeDebuggingContext: RuntimeDebuggingContext<*, *, *>,
        model: Model,
        newProps: Props
    ) {
        runtimeFunctionCalls = runtimeFunctionCalls.plus(
            RuntimeFunctionCall(
                runtimeDebuggingContext,
                FunctionCall.View(model, newProps)
            )
        )
        Log.v("DALLAS", "viewCalled: model: $model; newProps: $newProps")
    }

    private fun <Props : Any> renderCalled(
        runtimeDebuggingContext: RuntimeDebuggingContext<*, *, *>,
        props: Props,
        out: Any?
    ) {
        runtimeFunctionCalls = runtimeFunctionCalls.plus(
            RuntimeFunctionCall(
                runtimeDebuggingContext,
                FunctionCall.Render(props, out)
            )
        )
        Log.v("DALLAS", "renderCalled: props: $props; out: $out")
    }

    data class RuntimeDebuggingContext<Model : Any, Msg : Any, Props : Any>(
        val named: String,
        val initCalled: RuntimeDebuggingContext<Model, Msg, Props>.(out: Pair<Model, Effect<Msg>>) -> Unit,
        val updateCalled: RuntimeDebuggingContext<Model, Msg, Props>.(msg: Msg, model: Model, out: Pair<Model, Effect<Msg>>) -> Unit,
        val viewCalled: RuntimeDebuggingContext<Model, Msg, Props>.(model: Model, dispatch: Dispatch<Msg>, out: Props) -> Unit,
        val renderCalled: RuntimeDebuggingContext<Model, Msg, Props>.(props: Props, out: Any?) -> Unit
    )
}

data class RuntimeFunctionCall(
    val runtimeContext: OolongFlipperPlugin.RuntimeDebuggingContext<*, *, *>,
    val functionCall: FunctionCall
)

sealed interface FunctionCall {
    data class Init<Model : Any>(val model: Model) : FunctionCall
    data class Update<Msg : Any, Model : Any>(val msg: Msg, val model: Model, val newModel: Model) :
        FunctionCall

    data class View<Model : Any, Props : Any>(val model: Model, val newProps: Props) : FunctionCall
    data class Render<Props : Any, Out : Any?>(val props: Props, val out: Out) : FunctionCall
}

fun interface Runtime<Model, Msg, Props> {
    operator fun invoke(
        init: () -> Pair<Model, Effect<Msg>>,
        update: (Msg, Model) -> Pair<Model, Effect<Msg>>,
        view: (Model, Dispatch<Msg>) -> Props,
        render: (Props) -> Any?,
        runtimeContext: CoroutineContext,
        renderContext: CoroutineContext,
        effectContext: CoroutineContext,
    ): Job
}


private fun flipperObject(map: Map<String, String?>): FlipperObject = FlipperObject.Builder()
    .apply {
        map.forEach { (key, value) ->
            put(key, value)
        }
    }
    .build()
