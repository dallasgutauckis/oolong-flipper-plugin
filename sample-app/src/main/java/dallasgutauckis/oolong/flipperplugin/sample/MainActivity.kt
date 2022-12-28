package dallasgutauckis.oolong.flipperplugin.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dallasgutauckis.oolong.flipperplugin.OolongFlipperPlugin
import dallasgutauckis.oolong.flipperplugin.sample.ui.theme.OolongFlipperPluginTheme
import oolong.effect.none

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OolongFlipperPluginTheme {
                var props by remember { mutableStateOf(Props("Android", {})) }
                val runtime = OolongFlipperPlugin.runtime(
                    init = { Model(0) to none<Msg>() },
                    update = { msg, model ->
                        when (msg) {
                            Msg.Increment -> model.copy(count = model.count + 1) to none()
                        }
                    },
                    view = { model, dispatch ->
                        Props("Android & " + model.count, increment = { dispatch(Msg.Increment) })
                    },
                    render = { newProps -> props = newProps; Unit }
                )

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Greeting(props.greeting)
                        Button(onClick = { props.increment() }) {
                            Text("Increment")
                        }
                    }
                }
            }
        }
    }
}

data class Model(
    val count: Int,
)

data class Props(
    val greeting: String,
    val increment: () -> Unit
)

sealed class Msg {
    object Increment : Msg()
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OolongFlipperPluginTheme {
        Greeting("Android")
    }
}
